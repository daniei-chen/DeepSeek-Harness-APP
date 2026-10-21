#!/usr/bin/env node
/**
 * Write the repository's update manifest (`latest.json`).
 *
 * The app reads this static file instead of asking the GitHub API, so the release order of
 * unrelated tags cannot make it report "up to date" and an unauthenticated rate limit cannot
 * make it report nothing at all.
 *
 * Usage:
 *   node setup/make-latest-json.mjs --version 0.9.37 --apk dist/DeepSeek-Harness-0.9.37-personal.97.apk
 *
 * Optional: --page <release page>, --plugin <minimum host plugin version>, --notes <text>,
 *           --out <path> (default: latest.json in the repository root).
 */
import { createHash } from "node:crypto";
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const REPO = "daniei-chen/DeepSeek-Harness-APP";
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

export function versionCodeFor(version) {
    const cleaned = String(version).trim().replace(/^v/, "");
    const parts = cleaned
        .split("-")[0]
        .split(".")
        .map((part) => Number.parseInt(part, 10))
        .filter((part) => Number.isFinite(part));
    const [major = 0, minor = 0, patch = 0] = parts;
    const base = major * 10_000 + minor * 100 + patch;
    return cleaned.startsWith("0.") ? base + 31_000 : base;
}

function arg(name, fallback = undefined) {
    const index = process.argv.indexOf(`--${name}`);
    if (index < 0) return fallback;
    const value = process.argv[index + 1];
    if (value === undefined || value.startsWith("--")) throw new Error(`--${name} needs a value`);
    return value;
}

async function main() {
    const version = arg("version");
    const apk = arg("apk");
    if (!version || !apk) {
        console.error("usage: node setup/make-latest-json.mjs --version 0.9.37 --apk <file>");
        process.exit(2);
    }
    const file = path.resolve(ROOT, apk);
    const bytes = await readFile(file);
    const sha256 = createHash("sha256").update(bytes).digest("hex").toUpperCase();
    const name = path.basename(file);
    const page = arg("page", `https://github.com/${REPO}/releases/tag/v${version.replace(/^v/, "")}`);
    const manifest = {
        version: version.replace(/^v/, ""),
        versionCode: Number(versionCodeFor(version)),
        apk: `https://github.com/${REPO}/releases/download/v${version.replace(/^v/, "")}/${name}`,
        sha256,
        page,
        minPluginVersion: arg("plugin", "0.1.8"),
        notes: arg("notes", ""),
        generatedAt: new Date().toISOString(),
    };
    const out = path.resolve(ROOT, arg("out", "latest.json"));
    await writeFile(out, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");
    console.log(`wrote ${out}`);
    console.log(JSON.stringify(manifest, null, 2));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await main();
}
