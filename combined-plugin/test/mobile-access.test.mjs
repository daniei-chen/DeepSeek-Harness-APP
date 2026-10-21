import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { apply, collectInfo, localIpv4s, probeRelayHealth, readAudit } from "../lib/mobile-access.js";

function context(host, port) {
    return {
        webServer: {
            host,
            port,
            getOrigin: () => `http://${host}:${port}`,
        },
    };
}

test("loopback bind is reported as unreachable from LAN", async () => {
    const info = await collectInfo(context("127.0.0.1", 56072), { relayPort: 1 });
    assert.equal(info.ok, true);
    assert.equal(info.loopbackOnly, true);
    assert.equal(info.lanReachable, false);
    assert.equal(info.origin, "http://127.0.0.1:56072");
    assert.equal(info.relay.ok, false);
});

test("a concrete LAN bind is reported as reachable and carries a pair URL", async () => {
    const addresses = localIpv4s();
    if (addresses.length === 0) return;
    const info = await collectInfo(context("192.168.1.5", 3080), { relayPort: 1 });
    assert.equal(info.loopbackOnly, false);
    assert.equal(info.lanReachable, true);
    assert.equal(info.urls[0].url, `http://${addresses[0].address}:3080/`);
    assert.equal(info.relay.pairUrl, `https://${addresses[0].address}:1/relay/pair`);
});

test("relay health probe fails closed on a closed port", async () => {
    const result = await probeRelayHealth(1, 150);
    assert.equal(result.ok, false);
});

test("readAudit returns the newest valid entries", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "dsh-audit-"));
    const file = path.join(root, "audit.jsonl");
    await fs.writeFile(file, [
        JSON.stringify({ time: "2026-01-01T00:00:00Z", action: "delete", path: "a" }),
        "partial",
        JSON.stringify({ time: "2026-01-01T00:00:01Z", action: "upload", path: "b" }),
    ].join("\n"));
    const result = await readAudit({ auditLog: file }, 1);
    assert.equal(result.configured, true);
    assert.equal(result.entries.length, 1);
    assert.equal(result.entries[0].action, "upload");
});

test("apply registers the read-only info route", async () => {
    const routes = [];
    const ctx = {
        webServer: {
            host: "127.0.0.1",
            port: 56072,
            getOrigin: () => "http://127.0.0.1:56072",
            register: (route) => {
                routes.push(route);
                return () => {};
            },
        },
        effect: (fn) => fn(),
        logger: { info: () => {}, warn: () => {} },
    };
    apply(ctx, { relayPort: 1 });
    assert.equal(routes.length, 2);
    const infoRoute = routes.find((route) => route.path === "/dsh-mobile/info");
    assert.equal(infoRoute.kind, "exact");
    assert.equal(routes.some((route) => route.path === "/dsh-mobile/audit"), true);

    let body = "";
    const res = {
        writeHead: () => {},
        end: (chunk) => {
            body += chunk;
        },
    };
    await infoRoute.handler({ method: "GET" }, res);
    const parsed = JSON.parse(body);
    assert.equal(parsed.ok, true);
    assert.equal(parsed.loopbackOnly, true);
    assert.equal(parsed.port, 56072);
});
