import http from "node:http";
import https from "node:https";
import os from "node:os";
import path from "node:path";
import { readFileSync } from "node:fs";
import { readFile } from "node:fs/promises";

/**
 * Host-side facts for the Settings → 手机访问 panel.
 *
 * The DSH desktop host deliberately binds its web server to one concrete address, and on a
 * desktop that address is loopback. A phone cannot reach that listener directly. This route does
 * not try to punch a hole in it: it reports the real bind address, the LAN candidates and whether
 * a dsh-relay listener answers, so the panel can tell the user which connection path will work.
 *
 * It is a read-only status route. Pairing and device management stay behind the relay's own
 * authenticated pages; no credential or token is exposed here.
 */
export const name = "dsh-mobile-access";
export const inject = ["webServer"];

const APP_VERSION = (() => {
    try {
        const pkg = JSON.parse(readFileSync(new URL("../package.json", import.meta.url), "utf8"));
        return typeof pkg.version === "string" ? pkg.version : "0.0.0";
    } catch {
        return "0.0.0";
    }
})();

function json(res, status, value) {
    const body = JSON.stringify(value);
    res.writeHead(status, {
        "content-type": "application/json; charset=utf-8",
        "cache-control": "no-store",
        "content-length": Buffer.byteLength(body),
    });
    res.end(body);
}

/** All non-internal IPv4 addresses, with the interface that owns each one. */
export function localIpv4s() {
    const result = [];
    for (const [name, addresses] of Object.entries(os.networkInterfaces())) {
        for (const address of addresses || []) {
            if (address.internal) continue;
            if (address.family !== "IPv4" && address.family !== 4) continue;
            result.push({ name, address: address.address, netmask: address.netmask });
        }
    }
    // Wi-Fi/Ethernet first; virtual adapters (VPN, Hyper-V, Docker) last. The order is only a
    // presentation hint — the phone still has to be on the same subnet as the chosen address.
    return result.sort((a, b) => {
        const rank = (item) => /^(wlan|wi-?fi|eth|en)/i.test(item.name) ? 0 : 1;
        return rank(a) - rank(b) || a.name.localeCompare(b.name);
    });
}

/** One relay health request. The TLS path deliberately accepts the self-signed default cert. */
function probeRelayHealthOn(protocol, port, timeoutMs) {
    const target = `${protocol}://127.0.0.1:${port}/relay/health`;
    return new Promise((resolve) => {
        let settled = false;
        const finish = (value) => {
            if (settled) return;
            settled = true;
            resolve(value);
        };
        const client = protocol === "https" ? https : http;
        let request;
        try {
            request = client.get(target, { rejectUnauthorized: false, timeout: timeoutMs }, (response) => {
                response.resume();
                finish({
                    ok: response.statusCode !== undefined && response.statusCode >= 200 && response.statusCode < 500,
                    status: response.statusCode ?? null,
                    url: target,
                });
            });
        } catch (error) {
            finish({ ok: false, error: error instanceof Error ? error.message : String(error) });
            return;
        }
        request.on("timeout", () => {
            request.destroy(new Error("timeout"));
        });
        request.on("error", (error) => {
            finish({ ok: false, error: error instanceof Error ? error.message : String(error) });
        });
    });
}

/** Probe both relay transports: TLS is the default, plain HTTP is the 3444 compatibility path. */
export async function probeRelayHealth(port, timeoutMs = 900) {
    const secure = await probeRelayHealthOn("https", port, timeoutMs);
    if (secure.ok) return secure;
    const plain = await probeRelayHealthOn("http", port, timeoutMs);
    return plain.ok ? plain : { ...secure, plain };
}

function safeOrigin(webServer) {
    try {
        return typeof webServer?.getOrigin === "function" ? webServer.getOrigin() : null;
    } catch {
        return null;
    }
}

function configuredRoots(config) {
    const fromEnv = String(process.env.DSH_FILE_OPS_ROOTS || "").split(path.delimiter);
    const fromConfig = Array.isArray(config?.fileOpsRoots) ? config.fileOpsRoots : [];
    return [...fromEnv, ...fromConfig].filter(Boolean).map((root) => path.resolve(root));
}

function auditPath(config) {
    return String(config?.auditLog || process.env.DSH_MOBILE_AUDIT_LOG || "").trim();
}

/** Read the newest audit entries; malformed trailing lines are ignored rather than fatal. */
export async function readAudit(config, limit = 20) {
    const file = auditPath(config);
    if (file === "") return { configured: false, entries: [] };
    let text = "";
    try {
        text = await readFile(file, "utf8");
    } catch (error) {
        if (error?.code !== "ENOENT") throw error;
    }
    const entries = [];
    for (const line of text.split("\n")) {
        if (line.trim() === "") continue;
        try {
            entries.push(JSON.parse(line));
        } catch {
            // A process killed mid-append may leave one partial line; it is not actionable.
        }
    }
    return { configured: true, entries: entries.slice(-Math.max(1, Math.min(100, limit))) };
}

/** Build the panel's read-only snapshot. Exported so the shape can be unit-tested. */
export async function collectInfo(ctx, config = {}) {
    const webServer = ctx?.webServer;
    const port = Number(webServer?.port) || null;
    const bindHost = typeof webServer?.host === "string" ? webServer.host : null;
    const origin = safeOrigin(webServer);
    const addresses = localIpv4s();
    const loopbackOnly = bindHost === "127.0.0.1" || bindHost === "::1" || bindHost === "localhost";
    const relayPort = Number(config.relayPort) || 3443;
    const relay = await probeRelayHealth(relayPort);
    const urls = port === null
        ? []
        : addresses.map((item) => ({
            name: item.name,
            address: item.address,
            url: `http://${item.address}:${port}/`,
        }));
    const firstLanAddress = urls[0]?.address ?? null;
    const pairUrl = firstLanAddress === null
        ? null
        : `https://${firstLanAddress}:${relayPort}/relay/pair`;
    const devicesUrl = firstLanAddress === null
        ? null
        : `https://${firstLanAddress}:${relayPort}/relay/devices`;
    return {
        ok: true,
        app: "@dsh-android/deepseek-harness-app-plugin",
        version: APP_VERSION,
        hostname: os.hostname(),
        platform: process.platform,
        arch: process.arch,
        origin,
        bindHost,
        port,
        loopbackOnly,
        lanReachable: Boolean(port !== null && bindHost !== null && !loopbackOnly),
        addresses,
        urls,
        relay: {
            port: relayPort,
            pairUrl,
            devicesUrl,
            ...relay,
        },
        fileRoots: configuredRoots(config),
        now: new Date().toISOString(),
    };
}

export function apply(ctx, config = {}) {
    const handler = async (req, res) => {
        if (req.method !== "GET" && req.method !== "HEAD") {
            json(res, 405, { ok: false, error: "GET required" });
            return;
        }
        try {
            const info = await collectInfo(ctx, config);
            json(res, 200, info);
        } catch (error) {
            ctx.logger?.warn?.(error);
            json(res, 500, {
                ok: false,
                error: error instanceof Error ? error.message : String(error),
            });
        }
    };

    const auditHandler = async (req, res) => {
        if (req.method !== "GET" && req.method !== "HEAD") {
            json(res, 405, { ok: false, error: "GET required" });
            return;
        }
        try {
            const url = new URL(req.url || "/", "http://localhost");
            const limit = Number(url.searchParams.get("limit")) || 20;
            json(res, 200, { ok: true, ...(await readAudit(config, limit)) });
        } catch (error) {
            ctx.logger?.warn?.(error);
            json(res, 500, {
                ok: false,
                error: error instanceof Error ? error.message : String(error),
            });
        }
    };

    ctx.effect(() => ctx.webServer.register({
        kind: "exact",
        path: "/dsh-mobile/info",
        handler,
    }), "dsh-mobile-access.info-route");
    ctx.effect(() => ctx.webServer.register({
        kind: "exact",
        path: "/dsh-mobile/audit",
        handler: auditHandler,
    }), "dsh-mobile-access.audit-route");
    ctx.logger?.info?.("dsh-mobile-access: /dsh-mobile/info and /dsh-mobile/audit ready");
}
