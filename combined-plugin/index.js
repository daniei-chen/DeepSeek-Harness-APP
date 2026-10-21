import { apply as applyFileOps, inject as fileOpsInject } from "./lib/file-ops.js";
import { apply as applyMobileAccess, inject as mobileAccessInject } from "./lib/mobile-access.js";
import { apply as applyBrowserSkill } from "@wxg-prc-cpg/browser-skill-dsh-plugin";

/**
 * One installable bundle for the Android shell:
 * - the phone WebView/client adaptation layer (lib/client.js)
 * - the host file operations route (/dsh-file-ops)
 * - the Settings → 手机访问 status route (/dsh-mobile/info)
 * - BrowserSkill browser_* tools, imported from its published host plugin
 *
 * The theme plugin is intentionally separate.
 */
export const name = "deepseek-harness-app-plugin";
export const inject = [...new Set([...fileOpsInject, ...mobileAccessInject])];

export function apply(ctx, config = {}, options = {}) {
    applyFileOps(ctx, config);
    applyMobileAccess(ctx, config);
    return applyBrowserSkill(ctx, config, options);
}
