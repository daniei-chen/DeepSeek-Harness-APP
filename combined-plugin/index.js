import { apply as applyFileOps, inject as fileOpsInject } from "./lib/file-ops.js";
import {
  apply as applyBrowserSkill,
  inject as browserSkillInject,
} from "@wxg-prc-cpg/browser-skill-dsh-plugin";

/**
 * One installable bundle for the Android shell:
 * - the phone WebView/client adaptation layer (lib/client.js)
 * - the host file operations route (/dsh-file-ops)
 * - BrowserSkill browser_* tools, imported from its published host plugin
 *
 * The theme plugin is intentionally separate.
 */
export const name = "deepseek-harness-app-plugin";
export const inject = [...new Set([...fileOpsInject, ...browserSkillInject])];

export function apply(ctx, config = {}, options = {}) {
    applyFileOps(ctx);
    return applyBrowserSkill(ctx, config, options);
}
