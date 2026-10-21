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
 * `/dsh-mobile/info` and `/dsh-mobile/audit` are read-only. `/dsh-mobile/relay/pair` is the one
 * write-shaped route: it asks the relay for the live invitation so the phone can draw the pairing
 * QR itself, instead of sending the user to a desktop browser. It runs in the host process, whose
 * loopback request is what the relay accepts as "the machine running the harness"; no password,
 * token or device credential is ever returned by it.
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

/**
 * One request to the relay's loopback listener.
 *
 * Pairing codes may only be *issued* from the machine running the harness, and the relay decides
 * that by the address the request arrives from. This module runs inside that machine, so its
 * loopback request carries the operator credential without any password or token.
 */
function relayRequest(port, requestPath, { method = "GET", timeoutMs = 2_500 } = {}) {
    const target = `https://127.0.0.1:${port}${requestPath}`;
    return new Promise((resolve) => {
        let settled = false;
        const finish = (value) => {
            if (settled) return;
            settled = true;
            resolve(value);
        };
        let request;
        try {
            request = https.request(
                target,
                {
                    method,
                    rejectUnauthorized: false,
                    timeout: timeoutMs,
                    headers: { accept: "text/html", "content-length": "0" },
                },
                (response) => {
                    let body = "";
                    response.setEncoding("utf8");
                    response.on("data", (chunk) => {
                        body += chunk;
                    });
                    response.on("end", () => {
                        finish({ ok: true, status: response.statusCode ?? 0, body });
                    });
                    response.on("error", (error) => {
                        finish({ ok: false, error: error instanceof Error ? error.message : String(error) });
                    });
                },
            );
        } catch (error) {
            finish({ ok: false, error: error instanceof Error ? error.message : String(error) });
            return;
        }
        request.on("timeout", () => request.destroy(new Error("timeout")));
        request.on("error", (error) => {
            finish({ ok: false, error: error instanceof Error ? error.message : String(error) });
        });
        request.end();
    });
}

/**
 * Read the operator's pairing page back into the facts a QR needs.
 *
 * `dsh-relay` renders that page as HTML only — it has no JSON view of a live invitation — so the
 * code, its remaining lifetime, the paired origins and the certificate pin are read from the same
 * markup the desktop browser would show. Kept as a pure function so the parser can be tested
 * against a captured page rather than against a running relay.
 */
export function parsePairingPage(html) {
    const text = typeof html === "string" ? html : "";
    const code = /<div class="code">\s*([0-9]{4,12})\s*<\/div>/.exec(text)?.[1] ?? null;
    const expiresIn = Number(/Expires in\s+(\d+)\s+seconds/.exec(text)?.[1] ?? "");
    const pairPageUrl = /open\s*<span class="mono">([^<]+)<\/span>\s*there/.exec(text)?.[1] ?? null;
    const fingerprint = /<p class="caption fingerprint">[\s\S]{0,240}?<span class="mono">([^<]+)<\/span>/.exec(text)?.[1] ?? null;
    const plainUrl = /DSH Mobile[\s\S]{0,240}?<span class="mono">([^<]+)<\/span>/.exec(text)?.[1] ?? null;
    const origin = pairPageUrl === null ? null : pairPageUrl.replace(/\/relay\/pair\/*$/, "");
    return {
        code,
        origin,
        plainUrl: plainUrl !== null && /^https?:\/\//.test(plainUrl) ? plainUrl.replace(/\/+$/, "") : null,
        fingerprint,
        expiresInSeconds: Number.isFinite(expiresIn) ? expiresIn : null,
    };
}

/** Whether a URL points at this machine rather than at the network it lives on. */
function isLoopbackUrl(value) {
    try {
        const host = new URL(value).hostname;
        return host === "127.0.0.1" || host === "::1" || host === "[::1]" || host === "localhost";
    } catch {
        return false;
    }
}

/**
 * Mint (or re-read) the live pairing invitation and return it in the exact shape a QR carries.
 *
 * [fresh] asks the relay for a new code, which is what the phone's "refresh" button means; without
 * it the outstanding code is reused so two screens showing a QR do not invalidate each other.
 */
export async function collectPairing(relayPort, { fresh = false, now = Date.now() } = {}) {
    if (fresh) {
        const minted = await relayRequest(relayPort, "/relay/pair/new", { method: "POST" });
        if (!minted.ok) return { ok: false, error: "relay-unreachable", message: minted.error };
        if (minted.status >= 400) return { ok: false, error: "relay-refused", status: minted.status };
    }
    const page = await relayRequest(relayPort, "/relay/pair");
    if (!page.ok) return { ok: false, error: "relay-unreachable", message: page.error };
    if (page.status !== 200) return { ok: false, error: "relay-refused", status: page.status };
    const parsed = parsePairingPage(page.body);
    if (parsed.code === null || parsed.origin === null) {
        return { ok: false, error: "relay-page-unreadable" };
    }
    // The page names the origin the relay was *reached* by, and this request came over loopback.
    // A QR carrying 127.0.0.1 would send the next device to itself, so the invitation is rewritten
    // to the machine's first LAN address — the same address the settings panel already advertises.
    const lanAddress = localIpv4s()[0]?.address ?? null;
    const origin = lanAddress === null ? parsed.origin : `https://${lanAddress}:${relayPort}`;
    if (isLoopbackUrl(origin)) return { ok: false, error: "relay-no-lan-address" };
    const plainUrl = parsed.plainUrl !== null && !isLoopbackUrl(parsed.plainUrl) ? parsed.plainUrl : null;
    const payload = {
        v: 1,
        kind: "dsh-relay-pair",
        url: origin,
        ...(plainUrl !== null && { plainUrl }),
        ...(parsed.fingerprint !== null && { fingerprint: parsed.fingerprint }),
        code: parsed.code,
        expiresAt: now + (parsed.expiresInSeconds ?? 300) * 1_000,
    };
    return {
        ok: true,
        payload,
        pairPage: `${origin}/relay/pair`,
        devicesPage: `${origin}/relay/devices`,
        expiresInSeconds: parsed.expiresInSeconds,
        fingerprint: parsed.fingerprint,
    };
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

    // One invitation at a time: the relay keeps only the newest code, so two parallel refreshes
    // would leave the QR on screen showing a code that the other request already replaced.
    let pairingInFlight = null;
    const pairHandler = async (req, res) => {
        if (req.method !== "GET" && req.method !== "HEAD") {
            json(res, 405, { ok: false, error: "GET required" });
            return;
        }
        const relayPort = Number(config.relayPort) || 3443;
        const url = new URL(req.url || "/", "http://localhost");
        const fresh = url.searchParams.get("new") === "1";
        try {
            if (pairingInFlight === null) {
                pairingInFlight = collectPairing(relayPort, { fresh }).finally(() => {
                    pairingInFlight = null;
                });
            }
            const result = await pairingInFlight;
            json(res, result.ok ? 200 : 502, result);
        } catch (error) {
            ctx.logger?.warn?.(error);
            json(res, 502, {
                ok: false,
                error: "relay-unreachable",
                message: error instanceof Error ? error.message : String(error),
            });
        }
    };

    ctx.effect(() => ctx.webServer.register({
        kind: "exact",
        path: "/dsh-mobile/relay/pair",
        handler: pairHandler,
    }), "dsh-mobile-access.relay-pair-route");
    ctx.logger?.info?.("dsh-mobile-access: /dsh-mobile/info, /dsh-mobile/audit and /dsh-mobile/relay/pair ready");
}
