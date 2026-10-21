import { createReadStream, createWriteStream, realpathSync } from "node:fs";
import { execFile } from "node:child_process";
import { appendFile, lstat, mkdir, mkdtemp, readFile, realpath, rename, rm, stat } from "node:fs/promises";
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
 * the file-panel verbs — download, delete, rename, mkdir and upload — and confines every path to
 * the configured workspace roots. The route is intended to be mounted behind the self-hosted
 * server's own front gate; the original host plugin's extra `dsh_gate` cookie check is intentionally
 * removed here.
 */
export function apply(ctx, config = {}) {
    const configured = [
        ...(process.env.DSH_FILE_OPS_ROOTS || "").split(path.delimiter),
        ...(Array.isArray(config.fileOpsRoots) ? config.fileOpsRoots : []),
    ].filter(Boolean);
    const roots = (configured.length > 0 ? configured : [process.cwd()]).map((root) => path.resolve(root));
    const auditLog = String(config.auditLog || process.env.DSH_MOBILE_AUDIT_LOG || "").trim();
    if (auditLog !== "") {
        mkdir(path.dirname(path.resolve(auditLog)), { recursive: true })
            .catch((error) => ctx.logger?.warn?.(error));
    }

    /** Append-only local audit trail; a failure to write it must never fail the file action. */
    const audit = (req, action, target, result) => {
        if (auditLog === "") return;
        const entry = {
            time: new Date().toISOString(),
            action,
            path: target,
            result,
            remote: req.socket?.remoteAddress || "",
            agent: String(req.headers?.["user-agent"] || "").slice(0, 200),
        };
        appendFile(auditLog, JSON.stringify(entry) + "\n").catch((error) => ctx.logger?.warn?.(error));
    };

    /**
     * Canonical form for a containment check.
     *
     * Windows hands out 8.3 short names (`C:\Users\ADMINI~1\...`) for temporary directories while
     * `realpath` answers with the long name, so comparing the two spellings rejected every real
     * path under such a root. Resolve the parent when the target itself does not exist yet (the
     * `mkdir` case), and fall back to a plain resolve so a check can never throw.
     */
    const canonical = (target) => {
        const resolved = path.resolve(target);
        try {
            return realpathSync.native(resolved);
        } catch {
            try {
                return path.join(realpathSync.native(path.dirname(resolved)), path.basename(resolved));
            } catch {
                return resolved;
            }
        }
    };

    const insideRoots = (target) => {
        const normalized = canonical(target);
        return roots.some((root) => {
            const base = canonical(root);
            return normalized === base || normalized.startsWith(base + path.sep);
        });
    };

    /**
     * Preview deliberately has a small ceiling: it exists to read a note or look at a screenshot
     * on a phone, not to stream a video. Anything larger, or binary, gets `kind: "binary"` and the
     * panel offers the normal download instead.
     */
    const IMAGE_MIME = {
        ".png": "image/png",
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".gif": "image/gif",
        ".webp": "image/webp",
        ".bmp": "image/bmp",
        ".svg": "image/svg+xml",
    };
    const PREVIEW_TEXT_LIMIT = 512 * 1024;
    const PREVIEW_IMAGE_LIMIT = 6 * 1024 * 1024;

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
            const url = new URL(req.url || "/", "http://localhost");
            const action = url.searchParams.get("action") || "";

            const childName = (value) => {
                const name = String(value || "").trim();
                if (!name || name === "." || name === ".." || /[\\/]/.test(name)) return null;
                return name;
            };

            if (action === "mkdir" || action === "rename") {
                if (req.method !== "POST") {
                    json(res, 405, { error: "POST required" });
                    return;
                }
                const body = await readJson(req);
                const targetPath = path.resolve(String(body.path || ""));
                const name = childName(body.name);
                if (!name) {
                    json(res, 400, { error: "a valid name is required" });
                    return;
                }
                if (!insideRoots(targetPath)) {
                    json(res, 403, { error: "path is outside the workspace roots" });
                    return;
                }
                if (action === "rename" && roots.includes(targetPath)) {
                    json(res, 400, { error: "refusing to rename a workspace root" });
                    return;
                }
                const destination = path.join(
                    action === "rename" ? path.dirname(targetPath) : targetPath,
                    name,
                );
                if (!insideRoots(destination)) {
                    json(res, 403, { error: "destination is outside the workspace roots" });
                    return;
                }
                if (action === "mkdir") {
                    await mkdir(destination);
                    audit(req, "mkdir", destination, "ok");
                    json(res, 201, { ok: true, path: destination });
                } else {
                    await rename(targetPath, destination);
                    audit(req, "rename", destination, "ok");
                    json(res, 200, { ok: true, path: destination });
                }
                return;
            }

            if (action === "upload") {
                if (req.method !== "POST") {
                    json(res, 405, { error: "POST required" });
                    return;
                }
                const directory = path.resolve(String(url.searchParams.get("path") || ""));
                const name = childName(url.searchParams.get("name"));
                if (!name) {
                    json(res, 400, { error: "a valid name is required" });
                    return;
                }
                if (!insideRoots(directory)) {
                    json(res, 403, { error: "path is outside the workspace roots" });
                    return;
                }
                const destination = path.join(directory, name);
                if (!insideRoots(destination)) {
                    json(res, 403, { error: "destination is outside the workspace roots" });
                    return;
                }
                const output = createWriteStream(destination, { flags: "wx" });
                await new Promise((resolve, reject) => {
                    output.on("finish", resolve);
                    output.on("error", reject);
                    req.on("error", reject);
                    req.pipe(output);
                });
                audit(req, "upload", destination, "ok");
                json(res, 201, { ok: true, path: destination });
                return;
            }

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
                audit(req, "delete", resolved, "ok");
                json(res, 200, { ok: true, path: resolved });
                return;
            }
            if (action === "preview") {
                const entry = await lstat(resolved);
                if (entry.isSymbolicLink() || !entry.isFile()) {
                    json(res, 400, { error: "not a previewable file" });
                    return;
                }
                const real = await realpath(resolved);
                if (!insideRoots(real)) {
                    json(res, 403, { error: "resolved path is outside the workspace roots" });
                    return;
                }
                const info = await stat(real);
                const name = path.basename(real);
                const ext = path.extname(real).toLowerCase();
                const mime = IMAGE_MIME[ext];
                if (mime !== undefined && info.size <= PREVIEW_IMAGE_LIMIT) {
                    const bytes = await readFile(real);
                    json(res, 200, {
                        ok: true,
                        kind: "image",
                        name,
                        size: info.size,
                        mime,
                        data: `data:${mime};base64,${bytes.toString("base64")}`,
                    });
                    return;
                }
                if (info.size <= PREVIEW_TEXT_LIMIT) {
                    const bytes = await readFile(real);
                    const text = bytes.toString("utf8");
                    // A NUL byte in the first block is the cheap, reliable signal that this is not
                    // text; rendering a binary as UTF-8 would produce megabytes of replacement
                    // characters on a phone.
                    if (!text.includes("\u0000")) {
                        json(res, 200, {
                            ok: true,
                            kind: "text",
                            name,
                            size: info.size,
                            content: text.slice(0, 256 * 1024),
                            truncated: text.length > 256 * 1024,
                        });
                        return;
                    }
                }
                json(res, 200, { ok: true, kind: "binary", name, size: info.size });
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
            audit(req, "download", real, "ok");
            createReadStream(real).pipe(res);
        } catch (error) {
            ctx.logger?.warn?.(error);
            if (!res.headersSent) json(res, 500, { error: error instanceof Error ? error.message : String(error) });
            else res.destroy();
        }
    };

    ctx.effect(() => {
        try {
            return ctx.webServer.register({
                kind: "exact",
                path: "/dsh-file-ops",
                handler,
            });
        } catch (error) {
            // A previously activated copy may still own the route during a live plugin swap.
            if (String(error).includes("duplicate exact route")) {
                ctx.logger?.warn?.("dsh-file-ops: route already owned by a previous activation; reusing it");
                return () => {};
            }
            throw error;
        }
    }, "dsh-file-ops.route");
    ctx.logger?.info?.("dsh-file-ops: /dsh-file-ops ready (roots: " + roots.join(", ") + ")");
}
