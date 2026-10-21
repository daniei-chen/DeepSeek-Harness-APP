import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
  applyManagedBlock,
  backupProfile,
  buildManagedBlock,
  bundleNamesFor,
  ensureBundles,
  materializeSpec,
  parseArgs,
  resolveContext,
  restoreBackup,
  yamlQuote,
} from "./lib.mjs";

test("yamlQuote preserves Windows paths and escapes quotes", () => {
  assert.equal(yamlQuote("D:\\AI\\DeepSeek"), "'D:\\AI\\DeepSeek'");
  assert.equal(yamlQuote("it's"), "'it''s'");
});

test("managed block contains file roots, relay settings and hostnames", () => {
  const block = buildManagedBlock({
    fileRoots: ["D:\\AI\\DeepSeek", "/srv/work"],
    relayPort: 3443,
    relayStateDir: "C:\\Users\\me\\.dsh\\profiles\\desktop\\.dsh-mobile\\relay",
    publicHostnames: ["dsh.example.com"],
    relay: true,
  });
  assert.match(block, /fileOpsRoots:/);
  assert.match(block, /- 'D:\\AI\\DeepSeek'/);
  assert.match(block, /port: 3443/);
  assert.match(block, /addressGrants: false/);
  assert.match(block, /- 'dsh\.example\.com'/);
});

test("managed block can omit relay", () => {
  const block = buildManagedBlock({
    fileRoots: ["/srv/work"],
    relayPort: 3443,
    relayStateDir: "/tmp/relay",
    relay: false,
  });
  assert.doesNotMatch(block, /id: relay/);
  assert.match(block, /id: deepseek-app/);
});

test("managed block replacement is idempotent", () => {
  const first = buildManagedBlock({
    fileRoots: ["/srv/a"],
    relayPort: 3443,
    relayStateDir: "/tmp/relay",
    relay: true,
  });
  const initial = "# profile patch\n- id: keep-me\n  config:\n    value: 1\n";
  const once = applyManagedBlock(initial, first);
  assert.equal(once.changed, true);
  assert.match(once.text, /keep-me/);
  const twice = applyManagedBlock(once.text, first);
  assert.equal(twice.changed, false);
  assert.equal(twice.text, once.text);
});

test("managed block replaces an older block without duplicating it", () => {
  const oldBlock = buildManagedBlock({
    fileRoots: ["/old"],
    relayPort: 3443,
    relayStateDir: "/tmp/relay",
    relay: true,
  });
  const newBlock = buildManagedBlock({
    fileRoots: ["/new"],
    relayPort: 3555,
    relayStateDir: "/tmp/relay",
    relay: true,
  });
  const applied = applyManagedBlock(`# base\n\n${oldBlock}`, newBlock);
  assert.equal((applied.text.match(/>>> dsh-mobile-setup managed block/g) || []).length, 1);
  assert.match(applied.text, /- '\/new'/);
  assert.doesNotMatch(applied.text, /- '\/old'/);
  assert.match(applied.text, /port: 3555/);
});

test("ensureBundles appends only missing entries", () => {
  const pkg = {
    dsh: { profile: { bundles: ["@deepseek-ai/dsh-base", "dsh-relay"] } },
  };
  const result = ensureBundles(pkg, ["dsh-relay", "@dsh-android/deepseek-harness-app-plugin"]);
  assert.deepEqual(result.added, ["@dsh-android/deepseek-harness-app-plugin"]);
  assert.deepEqual(result.packageJson.dsh.profile.bundles, [
    "@deepseek-ai/dsh-base",
    "dsh-relay",
    "@dsh-android/deepseek-harness-app-plugin",
  ]);
});

test("bundleNamesFor follows the optional modules", () => {
  assert.deepEqual(bundleNamesFor({ relay: true, notify: true }), [
    "@dsh-android/deepseek-harness-app-plugin",
    "dsh-relay",
    "@dsh-android/dsh-mobile-notify-bundle",
  ]);
  assert.deepEqual(bundleNamesFor({ relay: false, notify: false }), [
    "@dsh-android/deepseek-harness-app-plugin",
  ]);
});

test("parseArgs supports repeated roots and boolean switches", () => {
  const opts = parseArgs([
    "install",
    "--profile",
    "work",
    "--file-root",
    "D:\\AI\\DeepSeek",
    "--file-root=/srv/work",
    "--no-relay",
    "--dry-run",
    "--yes",
  ]);
  assert.equal(opts.command, "install");
  assert.equal(opts.profile, "work");
  assert.deepEqual(opts.fileRoots, ["D:\\AI\\DeepSeek", "/srv/work"]);
  assert.equal(opts.relay, false);
  assert.equal(opts.dryRun, true);
  assert.equal(opts.yes, true);
});

test("resolveContext puts state and backups inside the profile", () => {
  const ctx = resolveContext({ profile: "work", dshHome: "C:\\Users\\me\\.dsh" });
  assert.equal(ctx.profileDir, "C:\\Users\\me\\.dsh\\profiles\\work");
  assert.equal(ctx.stateDir, "C:\\Users\\me\\.dsh\\profiles\\work\\.dsh-mobile-setup");
});

test("scoped npm specs are not mistaken for file paths", async () => {
  const result = await materializeSpec("@dsh-android/deepseek-harness-app-plugin", {
    cacheDir: path.join(os.tmpdir(), "dsh-mobile-setup-test"),
    pack: false,
  });
  assert.deepEqual(result, {
    spec: "@dsh-android/deepseek-harness-app-plugin",
    local: false,
  });
});

test("backup and restore round-trips profile files", async () => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), "dsh-mobile-setup-"));
  const profileDir = path.join(root, "profiles", "test");
  await fs.mkdir(profileDir, { recursive: true });
  const packagePath = path.join(profileDir, "package.json");
  const patchPath = path.join(profileDir, "cordis.patch.yml");
  await fs.writeFile(packagePath, '{"name":"before"}\n', "utf8");
  await fs.writeFile(patchPath, "# before\n", "utf8");

  const ctx = resolveContext({ profile: "test", dshHome: root });
  const backup = await backupProfile(ctx, "test");
  await fs.writeFile(packagePath, '{"name":"after"}\n', "utf8");
  await fs.writeFile(patchPath, "# after\n", "utf8");

  const restored = await restoreBackup(ctx, backup.backupDir);
  assert.deepEqual(restored.restored.sort(), ["cordis.patch.yml", "package.json"]);
  assert.equal(await fs.readFile(packagePath, "utf8"), '{"name":"before"}\n');
  assert.equal(await fs.readFile(patchPath, "utf8"), "# before\n");

  await fs.rm(root, { recursive: true, force: true });
});

test("restore removes a patch file the installer created", async () => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), "dsh-mobile-setup-"));
  const profileDir = path.join(root, "profiles", "test");
  await fs.mkdir(profileDir, { recursive: true });
  await fs.writeFile(path.join(profileDir, "package.json"), '{"name":"before"}\n', "utf8");

  const ctx = resolveContext({ profile: "test", dshHome: root });
  const backup = await backupProfile(ctx, "test");
  const block = buildManagedBlock({
    fileRoots: ["/srv/work"],
    relayPort: 3443,
    relayStateDir: "/tmp/relay",
    relay: true,
  });
  await fs.writeFile(path.join(profileDir, "cordis.patch.yml"), block, "utf8");

  const restored = await restoreBackup(ctx, backup.backupDir);
  assert.deepEqual(restored.removed, ["cordis.patch.yml"]);
  await assert.rejects(fs.access(path.join(profileDir, "cordis.patch.yml")));

  await fs.rm(root, { recursive: true, force: true });
});
