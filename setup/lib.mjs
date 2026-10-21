import fs from "node:fs";
import fsp from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import net from "node:net";
import https from "node:https";
import { createHash } from "node:crypto";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

export const VERSION = "0.1.0";

export const MANAGED_START = "# >>> dsh-mobile-setup managed block";
export const MANAGED_END = "# <<< dsh-mobile-setup managed block";

export const DEFAULT_PROFILE = "desktop";
export const DEFAULT_RELAY_PORT = 3443;
export const DEFAULT_APP_PLUGIN = "combined-plugin";
export const DEFAULT_NOTIFY_PLUGIN = "notify-plugin";
export const DEFAULT_BASE_PLUGIN =
  "vendor/dsh-android-dsh-client-ui-responsive-0.3.4-dsh021.2.tgz";
export const DEFAULT_RELAY_SPEC = "dsh-relay@0.2.1";

const HERE = path.dirname(fileURLToPath(import.meta.url));
export const REPO_ROOT = path.resolve(HERE, "..");

const APP_BUNDLE = "@dsh-android/deepseek-harness-app-plugin";
const NOTIFY_BUNDLE = "@dsh-android/dsh-mobile-notify-bundle";
const RELAY_BUNDLE = "dsh-relay";

export function usage() {
  return `DeepSeek Harness APP - host setup

Usage:
  node setup/dsh-mobile-setup.mjs install [options]
  node setup/dsh-mobile-setup.mjs status [options]
  node setup/dsh-mobile-setup.mjs doctor [options]
  node setup/dsh-mobile-setup.mjs uninstall [options]

Commands:
  install    Install and enable the host packages, then write the managed patch.
  status     Show what is installed, enabled and listening.
  doctor     Check the profile, file roots and relay listener.
  uninstall  Restore the profile files from the latest (or a chosen) backup.

Options:
  --profile <name>        DSH profile name (default: ${DEFAULT_PROFILE})
  --dsh-home <path>       DSH home (default: $DSH_HOME or ~/.dsh)
  --file-root <path>      File-operations root; repeatable (default: home directory)
  --relay-port <port>     Relay listener port (default: ${DEFAULT_RELAY_PORT})
  --public-hostname <h>   Relay public hostname; repeatable
  --app-plugin <spec>     App plugin directory, tarball or npm spec
  --notify-plugin <spec>  Notify plugin directory, tarball or npm spec
  --base-plugin <spec>    Responsive base plugin directory, tarball or npm spec
  --relay <spec>          Relay package spec (default: ${DEFAULT_RELAY_SPEC})
  --no-relay              Do not install or configure dsh-relay
  --no-notify             Do not install the ntfy publisher
  --no-base               Do not install the responsive base plugin
  --dry-run               Print the plan without changing anything
  --yes                   Do not ask for confirmation
  --json                  Print a machine-readable result
  --backup <dir>          Backup directory for uninstall
  -h, --help              Show this help
`;
}

function takeValue(argv, index, name) {
  const value = argv[index + 1];
  if (value === undefined || value.startsWith("--")) {
    throw new Error(`${name} requires a value`);
  }
  return value;
}

export function parseArgs(argv) {
  const opts = {
    command: "help",
    profile: DEFAULT_PROFILE,
    dshHome: undefined,
    fileRoots: [],
    relayPort: DEFAULT_RELAY_PORT,
    publicHostnames: [],
    appPlugin: DEFAULT_APP_PLUGIN,
    notifyPlugin: DEFAULT_NOTIFY_PLUGIN,
    basePlugin: DEFAULT_BASE_PLUGIN,
    relaySpec: DEFAULT_RELAY_SPEC,
    relay: true,
    notify: true,
    base: true,
    dryRun: false,
    yes: false,
    json: false,
    backup: undefined,
  };

  let i = 0;
  if (argv[0] && !argv[0].startsWith("-")) {
    opts.command = argv[0];
    i = 1;
  }

  for (; i < argv.length; i += 1) {
    const raw = argv[i];
    const eq = raw.indexOf("=");
    const name = eq >= 0 ? raw.slice(0, eq) : raw;
    const inline = eq >= 0 ? raw.slice(eq + 1) : undefined;
    const value = () => {
      if (inline !== undefined) return inline;
      const v = takeValue(argv, i, name);
      i += 1;
      return v;
    };

    switch (name) {
      case "--profile":
        opts.profile = value();
        break;
      case "--dsh-home":
        opts.dshHome = value();
        break;
      case "--file-root":
        opts.fileRoots.push(value());
        break;
      case "--relay-port": {
        const port = Number(value());
        if (!Number.isInteger(port) || port < 1 || port > 65535) {
          throw new Error("--relay-port must be an integer between 1 and 65535");
        }
        opts.relayPort = port;
        break;
      }
      case "--public-hostname":
        opts.publicHostnames.push(value());
        break;
      case "--app-plugin":
        opts.appPlugin = value();
        break;
      case "--notify-plugin":
        opts.notifyPlugin = value();
        break;
      case "--base-plugin":
        opts.basePlugin = value();
        break;
      case "--relay":
        opts.relaySpec = value();
        break;
      case "--no-relay":
        opts.relay = false;
        break;
      case "--no-notify":
        opts.notify = false;
        break;
      case "--no-base":
        opts.base = false;
        break;
      case "--dry-run":
        opts.dryRun = true;
        break;
      case "--yes":
        opts.yes = true;
        break;
      case "--json":
        opts.json = true;
        break;
      case "--backup":
        opts.backup = value();
        break;
      case "--help":
      case "-h":
        opts.command = "help";
        break;
      default:
        throw new Error(`unknown option: ${name}`);
    }
  }

  return opts;
}

export function resolveContext(opts) {
  const dshHome = path.resolve(
    opts.dshHome || process.env.DSH_HOME || path.join(os.homedir(), ".dsh"),
  );
  const profile = opts.profile || DEFAULT_PROFILE;
  const profileDir = path.join(dshHome, "profiles", profile);
  const stateDir = path.join(profileDir, ".dsh-mobile-setup");
  const cacheKey = createHash("sha256").update(profileDir).digest("hex").slice(0, 12);
  return {
    dshHome,
    profile,
    profileDir,
    stateDir,
    packageJsonPath: path.join(profileDir, "package.json"),
    patchPath: path.join(profileDir, "cordis.patch.yml"),
    lockPath: path.join(profileDir, "pnpm-lock.yaml"),
    backupRoot: path.join(stateDir, "backups"),
    // dsh plugin forwards arguments through several shells; a cache path containing spaces
    // makes it split the tarball path. os.tmpdir() is normally space-free.
    cacheDir: path.join(os.tmpdir(), "dsh-mobile-setup", cacheKey),
  };
}

export function yamlQuote(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

export function buildManagedBlock({ fileRoots, relayPort, relayStateDir, publicHostnames = [], relay = true }) {
  const roots = fileRoots.length > 0 ? fileRoots : [os.homedir()];
  const lines = [
    MANAGED_START,
    "# Generated by setup/dsh-mobile-setup.mjs. Remove this block to disable the overrides.",
    "- id: deepseek-app",
    `  name: "${APP_BUNDLE}"`,
    "  config:",
    "    fileOpsRoots:",
    ...roots.map((root) => `      - ${yamlQuote(root)}`),
  ];

  if (relay) {
    lines.push(
      "- id: relay",
      `  name: "${RELAY_BUNDLE}"`,
      "  config:",
      "    bind: '0.0.0.0'",
      `    port: ${relayPort}`,
      `    stateDir: ${yamlQuote(relayStateDir)}`,
      "    tls: 'self-signed'",
      publicHostnames.length > 0
        ? "    publicHostnames:"
        : "    publicHostnames: []",
      ...publicHostnames.map((host) => `      - ${yamlQuote(host)}`),
      "    privilegedMethods: 'allow-authenticated'",
      "    compat:",
      "      addressGrants: false",
    );
  }

  lines.push(MANAGED_END);
  return `${lines.join("\n")}\n`;
}

export function applyManagedBlock(text, block) {
  let next = String(text ?? "");
  let start = next.indexOf(MANAGED_START);
  while (start >= 0) {
    const end = next.indexOf(MANAGED_END, start);
    if (end < 0) {
      throw new Error("cordis.patch.yml has a managed block without an end marker");
    }
    next = `${next.slice(0, start)}${next.slice(end + MANAGED_END.length)}`;
    start = next.indexOf(MANAGED_START);
  }

  next = next.replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trimEnd();
  const result = `${next}${next.length > 0 ? "\n\n" : ""}${block.trimEnd()}\n`;
  return { text: result, changed: result !== String(text ?? "") };
}

export function ensureBundles(packageJson, bundleNames) {
  const pkg = packageJson ?? {};
  pkg.dsh ??= {};
  pkg.dsh.profile ??= {};
  pkg.dsh.profile.bundles ??= [];
  if (!Array.isArray(pkg.dsh.profile.bundles)) {
    throw new Error("package.json dsh.profile.bundles is not an array");
  }
  const added = [];
  for (const name of bundleNames) {
    if (name && !pkg.dsh.profile.bundles.includes(name)) {
      pkg.dsh.profile.bundles.push(name);
      added.push(name);
    }
  }
  return { packageJson: pkg, added };
}

export function bundleNamesFor(opts) {
  const names = [APP_BUNDLE];
  if (opts.relay) names.push(RELAY_BUNDLE);
  if (opts.notify) names.push(NOTIFY_BUNDLE);
  return names;
}

export function relayPairUrl(port) {
  return `https://127.0.0.1:${port}/relay/pair`;
}

export function relayDevicesUrl(port) {
  return `https://127.0.0.1:${port}/relay/devices`;
}

async function exists(filePath) {
  try {
    await fsp.access(filePath);
    return true;
  } catch {
    return false;
  }
}

async function readJson(filePath) {
  return JSON.parse(await fsp.readFile(filePath, "utf8"));
}

async function writeJsonAtomic(filePath, value) {
  const temp = `${filePath}.tmp-${process.pid}`;
  await fsp.writeFile(temp, `${JSON.stringify(value, null, 2)}\n`, "utf8");
  await fsp.rename(temp, filePath);
}

async function writeTextAtomic(filePath, value) {
  const temp = `${filePath}.tmp-${process.pid}`;
  await fsp.writeFile(temp, value, "utf8");
  await fsp.rename(temp, filePath);
}

/**
 * DSH 0.2.1's client runtime does not expose the `settingsScope` service that dsh-relay 0.2.1's
 * optional settings card injects. The relay's host half is fine and keeps listening; only the
 * browser card hangs the client-module sync with "waiting for activation". The app has its own
 * Settings → 手机访问 panel for the same facts, so removing the card metadata is the safe
 * compatibility step: the relay stays fully functional, and the plugin page can finish syncing.
 *
 * This is deliberately a narrow package-metadata edit, not a fork of the relay. Reinstalling the
 * relay restores the field, which is why the installer runs this after every install.
 */
export async function disableRelayClientCard(profileDir) {
  const packagePath = path.join(profileDir, "node_modules", "dsh-relay", "package.json");
  if (!(await exists(packagePath))) return false;
  const pkg = await readJson(packagePath);
  if (!pkg?.dsh?.client) return false;
  delete pkg.dsh.client;
  await writeJsonAtomic(packagePath, pkg);
  return true;
}

export async function assertProfile(ctx) {
  if (!(await exists(ctx.packageJsonPath))) {
    throw new Error(
      `profile not found: ${ctx.profileDir}\n` +
        "Start DSH once with this profile, or pass --profile <name> / --dsh-home <path>.",
    );
  }
}

export async function backupProfile(ctx, reason = "install") {
  const stamp = new Date().toISOString().replace(/[:.]/g, "-");
  const backupDir = path.join(ctx.backupRoot, stamp);
  await fsp.mkdir(backupDir, { recursive: true });

  const candidates = [
    "package.json",
    "cordis.patch.yml",
    "pnpm-lock.yaml",
    "pnpm-workspace.yaml",
    "compatibility.json",
  ];
  const copied = [];
  const missing = [];
  for (const name of candidates) {
    const from = path.join(ctx.profileDir, name);
    if (await exists(from)) {
      await fsp.copyFile(from, path.join(backupDir, name));
      copied.push(name);
    } else {
      missing.push(name);
    }
  }

  const manifest = {
    version: VERSION,
    reason,
    createdAt: new Date().toISOString(),
    dshHome: ctx.dshHome,
    profile: ctx.profile,
    profileDir: ctx.profileDir,
    files: copied,
    missing,
  };
  await writeJsonAtomic(path.join(backupDir, "manifest.json"), manifest);
  await writeJsonAtomic(path.join(ctx.stateDir, "last-backup.json"), {
    backupDir,
    createdAt: manifest.createdAt,
  });
  return { backupDir, manifest };
}

async function latestBackup(ctx) {
  if (!(await exists(ctx.backupRoot))) return null;
  const entries = await fsp.readdir(ctx.backupRoot, { withFileTypes: true });
  const dirs = entries
    .filter((entry) => entry.isDirectory())
    .map((entry) => path.join(ctx.backupRoot, entry.name))
    .sort()
    .reverse();
  return dirs[0] ?? null;
}

function isManagedOnlyPatch(text) {
  const start = text.indexOf(MANAGED_START);
  if (start < 0) return false;
  const end = text.indexOf(MANAGED_END, start);
  if (end < 0) return false;
  return `${text.slice(0, start)}${text.slice(end + MANAGED_END.length)}`.trim() === "";
}

export async function restoreBackup(ctx, backupDir) {
  const target = backupDir || (await latestBackup(ctx));
  if (!target) throw new Error("no backup found; nothing to restore");
  const manifestPath = path.join(target, "manifest.json");
  if (!(await exists(manifestPath))) {
    throw new Error(`not a setup backup (missing manifest.json): ${target}`);
  }
  const manifest = await readJson(manifestPath);
  const restored = [];
  for (const name of manifest.files || []) {
    const from = path.join(target, name);
    if (await exists(from)) {
      await fsp.copyFile(from, path.join(ctx.profileDir, name));
      restored.push(name);
    }
  }

  const removed = [];
  for (const name of manifest.missing || []) {
    const current = path.join(ctx.profileDir, name);
    if (!(await exists(current))) continue;
    // Only remove a file this installer created. A patch that now contains unrelated
    // user entries is left alone and reported instead of being deleted.
    if (name === "cordis.patch.yml") {
      const text = await fsp.readFile(current, "utf8");
      if (isManagedOnlyPatch(text)) {
        await fsp.rm(current);
        removed.push(name);
      }
    }
  }

  return { backupDir: target, restored, removed, manifest };
}

export async function listBackups(ctx) {
  if (!(await exists(ctx.backupRoot))) return [];
  const entries = await fsp.readdir(ctx.backupRoot, { withFileTypes: true });
  const out = [];
  for (const entry of entries.filter((e) => e.isDirectory())) {
    const dir = path.join(ctx.backupRoot, entry.name);
    const manifestPath = path.join(dir, "manifest.json");
    if (await exists(manifestPath)) {
      out.push(await readJson(manifestPath));
    }
  }
  return out.sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)));
}

function npmCommand() {
  return process.platform === "win32" ? "npm.cmd" : "npm";
}

function npmInvocation() {
  const cli = path.join(path.dirname(process.execPath), "node_modules", "npm", "bin", "npm-cli.js");
  if (fs.existsSync(cli)) return { command: process.execPath, args: [cli] };
  return { command: npmCommand(), args: [] };
}

function dshCommand() {
  return process.env.DSH_BIN || (process.platform === "win32" ? "dsh.cmd" : "dsh");
}

export async function runCommand(command, args, options = {}) {
  const isWindowsBatch =
    process.platform === "win32" && /\.(cmd|bat)$/i.test(command);
  return new Promise((resolve, reject) => {
    let child;
    if (isWindowsBatch) {
      // `cmd /c call dsh.cmd ...` keeps %~dp0 anchored to the wrapper. Don't quote the
      // arguments: dsh plugin already quotes when it forwards to pnpm, and a second layer
      // turns `"C:\path\x.tgz"` into a literal quote inside the package name. All local
      // specs are copied to a space-free cache first.
      const spaced = args.find((arg) => /\s/.test(String(arg)));
      if (spaced) {
        reject(new Error(`refusing to pass an argument containing whitespace to ${command}: ${spaced}`));
        return;
      }
      const line = ["call", command, ...args].join(" ");
      child = spawn(process.env.ComSpec || "cmd.exe", ["/d", "/s", "/c", line], {
        cwd: options.cwd,
        env: { ...process.env, ...(options.env || {}) },
        stdio: options.stdio || "inherit",
        shell: false,
      });
    } else {
      child = spawn(command, args, {
        cwd: options.cwd,
        env: { ...process.env, ...(options.env || {}) },
        stdio: options.stdio || "inherit",
        shell: options.shell ?? false,
      });
    }
    child.on("error", reject);
    child.on("exit", (code, signal) => {
      if (signal) reject(new Error(`${command} terminated by ${signal}`));
      else resolve(code ?? 1);
    });
  });
}

function packageTarballName(packageJson) {
  const name = String(packageJson.name || "").replace(/^@/, "").replace("/", "-");
  const version = String(packageJson.version || "0.0.0");
  return `${name}-${version}.tgz`;
}

export async function packLocalPackage(dir, cacheDir) {
  const packageJsonPath = path.join(dir, "package.json");
  if (!(await exists(packageJsonPath))) {
    throw new Error(`not a package directory (missing package.json): ${dir}`);
  }
  const packageJson = await readJson(packageJsonPath);
  await fsp.mkdir(cacheDir, { recursive: true });
  const expected = path.join(cacheDir, packageTarballName(packageJson));
  const npm = npmInvocation();
  const code = await runCommand(
    npm.command,
    [...npm.args, "pack", "--pack-destination", cacheDir],
    { cwd: dir },
  );
  if (code !== 0) throw new Error(`npm pack failed in ${dir}`);
  if (!(await exists(expected))) {
    throw new Error(`npm pack did not produce ${expected}`);
  }
  return expected;
}

export async function materializeSpec(spec, { cacheDir, pack = true }) {
  const trimmed = String(spec || "").trim();
  if (!trimmed) throw new Error("empty package spec");

  // npm specs, including scoped names and git/https specs, are passed through.
  if (/^[a-z]+:\/\//i.test(trimmed) || trimmed.startsWith("@")) {
    return { spec: trimmed, local: false };
  }

  const raw = trimmed.startsWith("file:") ? trimmed.slice(5) : trimmed;
  const candidate = path.isAbsolute(raw) ? raw : path.resolve(REPO_ROOT, raw);
  const looksLikePath =
    path.isAbsolute(trimmed) ||
    trimmed.startsWith(".") ||
    trimmed.startsWith("file:") ||
    trimmed.includes("\\") ||
    (await exists(candidate));
  if (!looksLikePath) return { spec: trimmed, local: false };

  if (!(await exists(candidate))) {
    throw new Error(`package path not found: ${candidate}`);
  }
  const stat = await fsp.stat(candidate);
  if (stat.isDirectory()) {
    if (!pack) return { spec: candidate, local: true, directory: candidate };
    const tarball = await packLocalPackage(candidate, cacheDir);
    return { spec: tarball, local: true, directory: candidate, tarball };
  }
  // Normalize local tarballs into the space-free cache. dsh plugin's Windows argument
  // forwarding cannot carry a path containing spaces reliably.
  if (!pack) return { spec: candidate, local: true, original: candidate };
  await fsp.mkdir(cacheDir, { recursive: true });
  const copied = path.join(cacheDir, path.basename(candidate));
  if (path.resolve(candidate) !== path.resolve(copied)) {
    await fsp.copyFile(candidate, copied);
  }
  if (/\s/.test(copied)) {
    throw new Error(
      `the setup cache path contains whitespace (${copied}); ` +
        "set TEMP to a space-free directory and retry",
    );
  }
  return { spec: copied, local: true, original: candidate };
}

export function installPlan(ctx, opts, materialized) {
  const roots =
    opts.fileRoots.length > 0
      ? opts.fileRoots.map((root) => path.resolve(root))
      : [os.homedir()];
  const relayStateDir = path.join(ctx.profileDir, ".dsh-mobile", "relay");
  const block = buildManagedBlock({
    fileRoots: roots,
    relayPort: opts.relayPort,
    relayStateDir,
    publicHostnames: opts.publicHostnames,
    relay: opts.relay,
  });
  return {
    profile: ctx.profile,
    profileDir: ctx.profileDir,
    bundles: bundleNamesFor(opts),
    fileRoots: roots,
    relayPort: opts.relayPort,
    relayStateDir,
    pairUrl: opts.relay ? relayPairUrl(opts.relayPort) : null,
    devicesUrl: opts.relay ? relayDevicesUrl(opts.relayPort) : null,
    packages: materialized.map((item) => item.spec),
    managedBlock: block,
  };
}

export async function install(opts) {
  const ctx = resolveContext(opts);
  await assertProfile(ctx);
  if (!opts.dryRun) {
    await fsp.mkdir(ctx.cacheDir, { recursive: true });
  }

  const materialized = [];
  materialized.push(
    await materializeSpec(opts.appPlugin, { cacheDir: ctx.cacheDir, pack: !opts.dryRun }),
  );
  if (opts.notify) {
    materialized.push(
      await materializeSpec(opts.notifyPlugin, { cacheDir: ctx.cacheDir, pack: !opts.dryRun }),
    );
  }
  if (opts.base) {
    materialized.push(
      await materializeSpec(opts.basePlugin, { cacheDir: ctx.cacheDir, pack: !opts.dryRun }),
    );
  }
  if (opts.relay) {
    materialized.push(
      await materializeSpec(opts.relaySpec, { cacheDir: ctx.cacheDir, pack: !opts.dryRun }),
    );
  }

  const plan = installPlan(ctx, opts, materialized);
  if (opts.dryRun) {
    return { dryRun: true, ...plan };
  }

  const backup = await backupProfile(ctx, "install");
  try {
    const addCode = await runCommand(
      dshCommand(),
      ["plugin", "--profile", ctx.profile, "add", ...materialized.map((item) => item.spec)],
      { cwd: ctx.profileDir, env: { DSH_HOME: ctx.dshHome } },
    );
    if (addCode !== 0) {
      throw new Error(`dsh plugin add failed with exit code ${addCode}`);
    }
    const relayClientDisabled = opts.relay
      ? await disableRelayClientCard(ctx.profileDir)
      : false;

    const pkg = await readJson(ctx.packageJsonPath);
    const ensured = ensureBundles(pkg, plan.bundles);
    await writeJsonAtomic(ctx.packageJsonPath, ensured.packageJson);

    const patch = (await exists(ctx.patchPath))
      ? await fsp.readFile(ctx.patchPath, "utf8")
      : "";
    const applied = applyManagedBlock(patch, plan.managedBlock);
    await writeTextAtomic(ctx.patchPath, applied.text);

    const manifest = {
      ...plan,
      version: VERSION,
      installedAt: new Date().toISOString(),
      backupDir: backup.backupDir,
      bundlesAdded: ensured.added,
      relayClientDisabled,
    };
    await writeJsonAtomic(path.join(ctx.stateDir, "install.json"), manifest);
    return { ok: true, ...manifest, backupDir: backup.backupDir };
  } catch (error) {
    await restoreBackup(ctx, backup.backupDir);
    throw new Error(
      `${error.message}\n` +
        `Profile files were restored from ${backup.backupDir}. ` +
        "If package installation partially completed, run `dsh plugin --profile " +
        `${ctx.profile} install` + "`.",
    );
  }
}

export async function readStatus(ctx) {
  await assertProfile(ctx);
  const pkg = await readJson(ctx.packageJsonPath);
  const bundles = Array.isArray(pkg.dsh?.profile?.bundles) ? pkg.dsh.profile.bundles : [];
  const deps = pkg.dependencies || {};
  const patch = (await exists(ctx.patchPath))
    ? await fsp.readFile(ctx.patchPath, "utf8")
    : "";
  const installPath = path.join(ctx.stateDir, "install.json");
  const install = (await exists(installPath)) ? await readJson(installPath) : null;
  const port = install?.relayPort || DEFAULT_RELAY_PORT;
  const relayConfigured = Boolean(install?.relayPort);
  const relayPortOpen = await checkTcp(port);
  return {
    profile: ctx.profile,
    profileDir: ctx.profileDir,
    installed: {
      app: Boolean(deps[APP_BUNDLE]),
      relay: Boolean(deps[RELAY_BUNDLE]),
      notify: Boolean(deps[NOTIFY_BUNDLE]),
      base: Boolean(deps["@dsh-android/dsh-client-ui-responsive"]),
    },
    bundles,
    managedBlock:
      Boolean(install) && patch.includes(MANAGED_START) && patch.includes(MANAGED_END),
    relayPort: port,
    relayPortOpen,
    pairUrl: relayConfigured ? relayPairUrl(port) : null,
    devicesUrl: relayConfigured ? relayDevicesUrl(port) : null,
    fileRoots: install?.fileRoots || [],
    install,
  };
}

async function checkTcp(port, host = "127.0.0.1", timeoutMs = 1200) {
  return new Promise((resolve) => {
    const socket = net.connect({ host, port });
    let settled = false;
    const done = (value) => {
      if (settled) return;
      settled = true;
      socket.destroy();
      resolve(value);
    };
    socket.setTimeout(timeoutMs);
    socket.once("connect", () => done(true));
    socket.once("timeout", () => done(false));
    socket.once("error", () => done(false));
  });
}

async function checkRelayHealth(port) {
  return new Promise((resolve) => {
    const req = https.request(
      {
        host: "127.0.0.1",
        port,
        path: "/relay/health",
        method: "GET",
        rejectUnauthorized: false,
        timeout: 2000,
      },
      (res) => {
        res.resume();
        resolve({ ok: (res.statusCode || 0) >= 200 && (res.statusCode || 0) < 500, status: res.statusCode });
      },
    );
    req.on("timeout", () => {
      req.destroy();
      resolve({ ok: false, error: "timeout" });
    });
    req.on("error", (error) => resolve({ ok: false, error: error.message }));
    req.end();
  });
}

export async function doctor(ctx) {
  const status = await readStatus(ctx);
  const checks = [];
  const add = (name, ok, detail) => checks.push({ name, ok: Boolean(ok), detail });

  add("profile", true, ctx.profileDir);
  add("app plugin", status.installed.app, status.installed.app ? "installed" : "missing dependency");
  add("relay", status.installed.relay, status.installed.relay ? "installed" : "missing dependency");
  add("notify plugin", status.installed.notify, status.installed.notify ? "installed" : "missing dependency");
  add("base plugin", status.installed.base, status.installed.base ? "installed" : "missing dependency");
  add("bundle entries", status.installed.app && status.installed.relay, status.bundles.join(", "));
  add("managed patch", status.managedBlock, status.managedBlock ? "present" : "missing");
  for (const root of status.fileRoots) {
    add("file root", await exists(root), root);
  }
  add("relay port", status.relayPortOpen, `127.0.0.1:${status.relayPort}`);
  if (status.relayPortOpen) {
    const health = await checkRelayHealth(status.relayPort);
    add("relay health", health.ok, health.status ? `HTTP ${health.status}` : health.error || "unreachable");
  }

  return { ...status, checks, ok: checks.every((check) => check.ok) };
}

export async function uninstall(opts) {
  const ctx = resolveContext(opts);
  await assertProfile(ctx);
  const restored = await restoreBackup(ctx, opts.backup);
  const installPath = path.join(ctx.stateDir, "install.json");
  if (await exists(installPath)) await fsp.rm(installPath);
  return { ok: true, ...restored };
}

export function formatStatus(status) {
  const lines = [
    `Profile: ${status.profile} (${status.profileDir})`,
    `App plugin: ${status.installed.app ? "installed" : "missing"}`,
    `Relay: ${status.installed.relay ? "installed" : "missing"}`,
    `Notify: ${status.installed.notify ? "installed" : "missing"}`,
    `Base: ${status.installed.base ? "installed" : "missing"}`,
    `Bundles: ${status.bundles.join(", ") || "(none)"}`,
    `Managed patch: ${status.managedBlock ? "present" : "missing"}`,
    `Relay port ${status.relayPort}: ${status.relayPortOpen ? "open" : "closed"}`,
    `Pair: ${status.pairUrl || "(not configured)"}`,
    `Devices: ${status.devicesUrl || "(not configured)"}`,
  ];
  if (status.fileRoots.length > 0) {
    lines.push(`File roots: ${status.fileRoots.join(", ")}`);
  }
  return lines.join("\n");
}

export function formatDoctor(result) {
  const lines = [`Profile: ${result.profile} (${result.profileDir})`, ""];
  for (const check of result.checks) {
    lines.push(`${check.ok ? "PASS" : "FAIL"}  ${check.name}: ${check.detail}`);
  }
  lines.push("", result.ok ? "All checks passed." : "Some checks failed.");
  return lines.join("\n");
}

export async function confirm(opts, message) {
  if (opts.yes) return true;
  if (!process.stdin.isTTY || !process.stdout.isTTY) {
    throw new Error(`${message}\nRe-run with --yes for non-interactive use.`);
  }
  process.stdout.write(`${message} [y/N] `);
  return new Promise((resolve) => {
    process.stdin.setEncoding("utf8");
    process.stdin.once("data", (data) => {
      resolve(/^y(es)?$/i.test(String(data).trim()));
    });
  });
}
