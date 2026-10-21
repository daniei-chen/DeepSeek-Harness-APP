#!/usr/bin/env node
import {
  confirm,
  doctor,
  formatDoctor,
  formatStatus,
  install,
  listBackups,
  parseArgs,
  readStatus,
  resolveContext,
  uninstall,
  usage,
} from "./lib.mjs";

function printJson(value) {
  process.stdout.write(`${JSON.stringify(value, null, 2)}\n`);
}

function printPlan(plan) {
  console.log("DeepSeek Harness APP - host setup plan");
  console.log(`Profile: ${plan.profile} (${plan.profileDir})`);
  console.log(`File roots: ${plan.fileRoots.join(", ")}`);
  console.log(`Bundles to enable: ${plan.bundles.join(", ")}`);
  console.log("Packages:");
  for (const pkg of plan.packages) console.log(`  - ${pkg}`);
  if (plan.pairUrl) {
    console.log(`Pair page: ${plan.pairUrl}`);
    console.log(`Devices:   ${plan.devicesUrl}`);
  }
  console.log("No changes made (--dry-run).");
}

async function main(argv) {
  const opts = parseArgs(argv);

  if (opts.command === "help" || opts.command === "--help") {
    process.stdout.write(usage());
    return;
  }

  if (opts.command === "install") {
    const ctx = resolveContext(opts);
    if (opts.dryRun) {
      const result = await install(opts);
      if (opts.json) printJson(result);
      else printPlan(result);
      return;
    }
    const ok = await confirm(
      opts,
      `Install the DSH mobile host packages into profile "${ctx.profile}"?\n` +
        `A timestamped backup will be written to ${ctx.stateDir}\\backups.`,
    );
    if (!ok) {
      console.log("Cancelled.");
      return;
    }
    const result = await install(opts);
    if (opts.json) {
      printJson(result);
      return;
    }
    console.log("Installed.");
    console.log(`Profile: ${result.profile} (${result.profileDir})`);
    console.log(`Backup:  ${result.backupDir}`);
    console.log(`Bundles: ${result.bundles.join(", ")}`);
    console.log(`File roots: ${result.fileRoots.join(", ")}`);
    if (result.pairUrl) {
      console.log("");
      console.log("Next:");
      console.log(`  1. Restart DSH so the relay and plugin rows load.`);
      console.log(`  2. Open ${result.pairUrl} on the computer.`);
      console.log(`  3. Scan the QR with the app, or type the eight-digit code.`);
      console.log(`  4. Manage paired devices at ${result.devicesUrl}.`);
    }
    return;
  }

  if (opts.command === "status") {
    const status = await readStatus(resolveContext(opts));
    if (opts.json) printJson(status);
    else console.log(formatStatus(status));
    return;
  }

  if (opts.command === "doctor") {
    const result = await doctor(resolveContext(opts));
    if (opts.json) printJson(result);
    else console.log(formatDoctor(result));
    if (!result.ok) process.exitCode = 1;
    return;
  }

  if (opts.command === "uninstall") {
    const ctx = resolveContext(opts);
    if (!opts.backup) {
      const backups = await listBackups(ctx);
      if (backups.length === 0) throw new Error("no setup backups found");
      opts.backup = backups[0].backupDir;
    }
    const ok = await confirm(
      opts,
      `Restore profile files from ${opts.backup}?\n` +
        "This restores package.json / cordis.patch.yml; it does not delete the profile directory.",
    );
    if (!ok) {
      console.log("Cancelled.");
      return;
    }
    const result = await uninstall(opts);
    if (opts.json) printJson(result);
    else {
      console.log(`Restored from ${result.backupDir}`);
      console.log(`Files: ${result.restored.join(", ") || "(none)"}`);
      console.log(
        `Run \`dsh plugin --profile ${ctx.profile} install\` to prune packages from node_modules.`,
      );
    }
    return;
  }

  throw new Error(`unknown command: ${opts.command}\n\n${usage()}`);
}

main(process.argv.slice(2)).catch((error) => {
  console.error(`error: ${error.message}`);
  process.exitCode = 1;
});
