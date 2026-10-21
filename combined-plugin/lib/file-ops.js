import { createReadStream } from "node:fs";
import { execFile } from "node:child_process";
import { lstat, mkdtemp, realpath, rm, stat } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

export const name = "dsh-file-ops";
export const inject = ["webServer"];

/**
 * A deliberately tiny file-operation surface for the phone's workspace file panel.
 *
 * The shipped workspaceFiles API is read-only (list/stat/read/readBytes/changes). This route adds
 * exactly the two missing verbs — download and delete — and confines every path to the configured
 * workspace roots. It lives behind the same gate cookie as the rest of the harness.
 */
export function apply(ctx) {
    const configured = (process.env.DSH_FILE_OPS_ROOTS || "").split(path.delimiter).filter(Boolean);
    const roots = (configured.length > 0 ? configured : [process.cwd()]).map((root) => path.resolve(root));

    const insideRoots = (target) => {
        const normalized = path.resolve(target);
        return roots.some((root) => normalized === root || normalized.startsWith(root + path.sep));
    };

    const json = (res, status, value) => {
        const body = JSON.stringify(value);
        res.writeHead(status, {
            "content-type": "application/json; charset=utf-8",
            "cache-control": "no-store",
            "content-length": Buffer.byteLength(body),
        });
        res.end(body);
    };

    const readJson = async (req) => {
        const chunks = [];
        for await (const chunk of req) chunks.push(chunk);
        return JSON.parse(Buffer.concat(chunks).toString("utf8"));
    };

    const handler = async (req, res) => {
        try {
            const cookie = String(req.headers.cookie || "");
            if (!cookie.includes("dsh_gate=")) {
                json(res, 401, { error: "gate cookie required" });
                return;
            }
            const url = new URL(req.url || "/", "http://localhost");
            const action = url.searchParams.get("action") || "";
            let target = "";
            if (action === "delete") {
                if (req.method !== "POST") {
                    json(res, 405, { error: "POST required" });
                    return;
                }
                const body = await readJson(req);
                target = String(body.path || "");
            } else {
                target = String(url.searchParams.get("path") || "");
            }
            if (!target) {
                json(res, 400, { error: "path is required" });
                return;
            }
            const resolved = path.resolve(target);
            if (!insideRoots(resolved)) {
                json(res, 403, { error: "path is outside the workspace roots" });
                return;
            }
            if (action === "delete") {
                if (roots.includes(resolved)) {
                    json(res, 400, { error: "refusing to delete a workspace root" });
                    return;
                }
                await rm(resolved, { recursive: true, force: false });
                json(res, 200, { ok: true, path: resolved });
                return;
            }
            if (action !== "download") {
                json(res, 400, { error: "unknown action" });
                return;
            }
            // Do not follow a symlink out of the workspace. The top-level entry must be a real
            // file/directory; its real path is checked again after resolution.
            const entry = await lstat(resolved);
            if (entry.isSymbolicLink()) {
                json(res, 403, { error: "symlinks are not downloadable" });
                return;
            }
            const real = await realpath(resolved);
            if (!insideRoots(real)) {
                json(res, 403, { error: "resolved path is outside the workspace roots" });
                return;
            }
            const info = await stat(real);
            if (info.isDirectory()) {
                const temp = await mkdtemp(path.join(tmpdir(), "dsh-file-ops-"));
                const zip = path.join(temp, `${path.basename(real) || "workspace"}.zip`);
                try {
                    await execFileAsync("tar", [
                        "-a",
                        "-c",
                        "-f",
                        zip,
                        "-C",
                        path.dirname(real),
                        path.basename(real),
                    ]);
                } catch (error) {
                    await rm(temp, { recursive: true, force: true });
                    throw error;
                }
                const name = `${path.basename(real) || "workspace"}.zip`;
                res.writeHead(200, {
                    "content-type": "application/zip",
                    "content-disposition": `attachment; filename*=UTF-8''${encodeURIComponent(name)}`,
                    "cache-control": "no-store",
                });
                const stream = createReadStream(zip);
                stream.pipe(res);
                stream.on("close", () => { rm(temp, { recursive: true, force: true }).catch(() => {}); });
                return;
            }
            if (!info.isFile()) {
                json(res, 400, { error: "not a regular file" });
                return;
            }
            const name = path.basename(real);
            res.writeHead(200, {
                "content-type": "application/octet-stream",
                "content-length": info.size,
                "content-disposition": `attachment; filename*=UTF-8''${encodeURIComponent(name)}`,
                "cache-control": "no-store",
            });
            createReadStream(real).pipe(res);
        } catch (error) {
            ctx.logger?.warn?.(error);
            if (!res.headersSent) json(res, 500, { error: error instanceof Error ? error.message : String(error) });
            else res.destroy();
        }
    };

    ctx.effect(() => ctx.webServer.register({
        kind: "exact",
        path: "/dsh-file-ops",
        handler,
    }), "dsh-file-ops.route");
    ctx.logger?.info?.("dsh-file-ops: /dsh-file-ops ready (roots: " + roots.join(", ") + ")");
}
