import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { Readable } from "node:stream";
import test from "node:test";
import { apply } from "../lib/file-ops.js";

async function harness(extra = {}) {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "dsh-file-ops-"));
    const routes = [];
    const ctx = {
        webServer: {
            register: (route) => {
                routes.push(route);
                return () => {};
            },
        },
        effect: (fn) => fn(),
        logger: { info: () => {}, warn: () => {} },
    };
    const config = { fileOpsRoots: [root], ...extra };
    if (typeof config.auditLog === "string" && !path.isAbsolute(config.auditLog)) {
        config.auditLog = path.join(root, config.auditLog);
    }
    apply(ctx, config);
    return { root, route: routes[0] };
}

function request(method, body = null, url = "/dsh-file-ops") {
    const stream = body === null ? Readable.from([]) : Readable.from([Buffer.from(body)]);
    stream.method = method;
    stream.url = url;
    return stream;
}

function response() {
    return {
        status: 0,
        body: "",
        headers: {},
        writeHead(status) {
            this.status = status;
        },
        end(chunk) {
            this.body += chunk ?? "";
        },
    };
}

test("mkdir and rename stay inside the workspace root", async () => {
    const { root, route } = await harness();
    const mkdirResponse = response();
    await route.handler(
        request("POST", JSON.stringify({ path: root, name: "drafts" }), "/dsh-file-ops?action=mkdir"),
        mkdirResponse,
    );
    assert.equal(mkdirResponse.status, 201);
    assert.equal(await fs.stat(path.join(root, "drafts")).then((s) => s.isDirectory()), true);

    const renameResponse = response();
    await route.handler(
        request("POST", JSON.stringify({ path: path.join(root, "drafts"), name: "final" }), "/dsh-file-ops?action=rename"),
        renameResponse,
    );
    assert.equal(renameResponse.status, 200);
    assert.equal(await fs.stat(path.join(root, "final")).then((s) => s.isDirectory()), true);
});

test("upload writes bytes and refuses traversal names", async () => {
    const { root, route } = await harness();
    const uploadResponse = response();
    await route.handler(
        Object.assign(request("POST", "hello"), { url: `/dsh-file-ops?action=upload&path=${encodeURIComponent(root)}&name=note.txt` }),
        uploadResponse,
    );
    assert.equal(uploadResponse.status, 201);
    assert.equal(await fs.readFile(path.join(root, "note.txt"), "utf8"), "hello");

    const badResponse = response();
    await route.handler(
        Object.assign(request("POST", "bad"), { url: `/dsh-file-ops?action=upload&path=${encodeURIComponent(root)}&name=..%2Fescape.txt` }),
        badResponse,
    );
    assert.equal(badResponse.status, 400);
});

test("file actions append an audit entry when auditLog is configured", async () => {
    const { root, route } = await harness({ auditLog: "audit.jsonl" });
    const auditLog = path.join(root, "audit.jsonl");
    const result = response();
    await route.handler(
        request("POST", JSON.stringify({ path: root, name: "audited" }), "/dsh-file-ops?action=mkdir"),
        result,
    );
    assert.equal(result.status, 201);
    await new Promise((resolve) => setTimeout(resolve, 50));
    const lines = (await fs.readFile(auditLog, "utf8")).trim().split("\n");
    assert.equal(JSON.parse(lines[0]).action, "mkdir");
});

test("delete still refuses to remove a workspace root", async () => {
    const { root, route } = await harness();
    const remove = response();
    await route.handler(
        request("POST", JSON.stringify({ path: root }), "/dsh-file-ops?action=delete"),
        remove,
    );
    assert.equal(remove.status, 400);
    assert.equal(await fs.stat(root).then((s) => s.isDirectory()), true);
});
