import assert from "node:assert/strict";
import fs from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

/**
 * lib/client.js is a browser bundle (`window.__ModuleLoader__.load(...)` + DOM access), so it
 * cannot be imported in Node. These guards read it as text and pin the plugin-side contracts the
 * Android shell and the phone layout rely on:
 *  - no periodic timers: the page must not refresh itself (the phone kept reporting it did),
 *  - a reading is written to the composer element (card / dock), not to <html>, and never to a
 *    React-owned node's inline style,
 *  - the speed/usage slots are width-pinned and tabular, so streaming digits cannot reflow the
 *    dock row; the model name ellipsizes instead,
 *  - `window.__dshInsertComposerText` exists and uses the React-safe insertion path,
 *  - the reported bundle version matches package.json.
 */

const here = path.dirname(fileURLToPath(import.meta.url));
const pluginRoot = path.resolve(here, "..");
// The file is checked out with CRLF on Windows; normalise so anchors are written once, in LF.
const source = (await fs.readFile(path.join(pluginRoot, "lib", "client.js"), "utf8")).replace(/\r\n/g, "\n");
const lines = source.split("\n");
const pkg = JSON.parse(await fs.readFile(path.join(pluginRoot, "package.json"), "utf8"));

/** Slice the source between two anchors so an assertion cannot be satisfied by an unrelated match. */
function sliceBetween(startAnchor, endAnchor) {
    const start = source.indexOf(startAnchor);
    assert.notEqual(start, -1, `missing anchor: ${startAnchor}`);
    const end = source.indexOf(endAnchor, start + startAnchor.length);
    assert.notEqual(end, -1, `missing end anchor: ${endAnchor}`);
    return source.slice(start, end);
}

/** Lines that carry code, i.e. not the comments explaining why the old timers were removed. */
function codeLines(region) {
    return region
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => !line.startsWith("//") && !line.startsWith("*") && !line.startsWith("/*"));
}

/** The debounce delay an observer scheduler is armed with. */
function debounceDelay(anchor, endAnchor) {
    const block = sliceBetween(anchor, endAnchor);
    const match = block.match(/\}, (\d+)\);/);
    assert.ok(match, `no window.setTimeout delay found after ${anchor}`);
    return Number(match[1]);
}

const composerRegion = () => sliceBetween("const varHost = (name) => {", "installFileActions();");
const filePanelRegion = () => sliceBetween("function installFileActions() {", "    const inject = [];");

test("the bundle still parses as a script", () => {
    assert.doesNotThrow(() => new Function(source));
});

test("no repeating timers outside the browser-skill elapsed clock", () => {
    const hits = lines
        .map((line, index) => ({ lineNumber: index + 1, line: line.trim() }))
        .filter(({ line }) => !line.startsWith("//") && !line.startsWith("*") && !line.startsWith("/*"))
        .filter(({ line }) => /\bsetInterval\(/.test(line));

    assert.equal(
        hits.length,
        1,
        "exactly one setInterval is allowed (the browser-skill elapsed clock), found " + JSON.stringify(hits),
    );
    assert.match(hits[0].line, /^const timer = setInterval\(\(\) => setNow\(Date\.now\(\)\), 1e3\);$/);
    // The two DOM-decoration polls that made the page look like it refreshed itself are gone: no
    // window-qualified call survives anywhere in the bundle, and neither region polls at all.
    assert.doesNotMatch(source, /window\.setInterval\(/);
    assert.deepEqual(codeLines(composerRegion()).filter((line) => line.includes("setInterval")), []);
    assert.deepEqual(codeLines(filePanelRegion()).filter((line) => line.includes("setInterval")), []);
});

test("composer readings are re-attached by a debounced observer plus one visibility retry", () => {
    const region = composerRegion();
    assert.match(
        region,
        /new MutationObserver\(scheduleStructural\)\.observe\(document\.documentElement, \{ subtree: true, childList: true \}\)/,
        "the composer structural probe must be MutationObserver-driven",
    );
    const delay = debounceDelay("const scheduleStructural = () => {", "new MutationObserver");
    assert.ok(delay >= 200 && delay <= 300, `composer observer debounce is ${delay}ms, expected 200-300ms`);

    const visibility = region.slice(region.indexOf('document.addEventListener("visibilitychange"'));
    assert.match(visibility, /observe\(\);/, "returning to the foreground must re-run the structural probe");
    assert.match(visibility, /window\.setTimeout\(observe, \d+\);/, "one delayed retry is expected");

    // Only write a custom property when the value actually changed: unchanged writes are what made
    // the bottom row churn while the model was streaming.
    assert.match(
        source,
        /if \(\(hostStyle\.getPropertyValue\(name\) \|\| ""\) !== quoted\) hostStyle\.setProperty\(name, quoted\);/,
        "setVar must compare before writing",
    );
});

test("reading variables live on the composer, with <html> only as a fallback", () => {
    const region = composerRegion();
    const varHost = sliceBetween("const varHost = (name) => {", "const setVar = (name, value) => {");
    // The model label belongs to the card; the two stats belong to the dock, with the seat as the
    // fallback for a build that renders the dock under a different slot name.
    assert.match(varHost, /return document\.querySelector\("\[data-composer-card\]"\) \|\| document\.documentElement;/);
    assert.match(varHost, /document\.querySelector\('\[data-slot="conversation\.composer\.dock"\]'\) \|\|/);
    assert.match(varHost, /document\.querySelector\("\[data-composer-seat\]"\) \|\|/);

    const setVar = sliceBetween("const setVar = (name, value) => {", "const compactActivity");
    assert.match(setVar, /const hostStyle = varHost\(name\)\.style;/);
    assert.match(
        setVar,
        /if \(\(hostStyle\.getPropertyValue\(name\) \|\| ""\) !== quoted\) hostStyle\.setProperty\(name, quoted\);/,
    );
    // A reading must not invalidate the whole document's style: nothing in this region touches
    // documentElement.style directly.
    assert.doesNotMatch(region, /document\.documentElement\.style/);
});

test("a reading is written to its composer host, once per change", () => {
    class FakeStyle {
        constructor() {
            this.props = new Map();
            this.writes = [];
        }
        getPropertyValue(name) {
            return this.props.get(name) || "";
        }
        setProperty(name, value) {
            this.props.set(name, value);
            this.writes.push([name, value]);
        }
    }
    class FakeElement {
        constructor() {
            this.style = new FakeStyle();
        }
    }

    const card = new FakeElement();
    const dock = new FakeElement();
    const seat = new FakeElement();
    const html = new FakeElement();
    let table = {
        "[data-composer-card]": card,
        '[data-slot="conversation.composer.dock"]': dock,
        "[data-composer-seat]": seat,
    };
    const fakeDocument = { querySelector: (selector) => table[selector] ?? null, documentElement: html };
    const factorySource = sliceBetween("const varHost = (name) => {", "const compactActivity").trimEnd();
    const setVar = new Function("document", `${factorySource}\nreturn setVar;`)(fakeDocument);

    setVar("--dsh-mobile-model", "DeepSeek V4");
    setVar("--dsh-mobile-speed", "9 tok/s");
    setVar("--dsh-mobile-usage", "24.3k · 45%");
    assert.equal(card.style.props.get("--dsh-mobile-model"), '"DeepSeek V4"');
    assert.equal(dock.style.props.get("--dsh-mobile-speed"), '"9 tok/s"');
    assert.equal(dock.style.props.get("--dsh-mobile-usage"), '"24.3k · 45%"');
    assert.equal(html.style.writes.length, 0, "<html> must stay untouched while the composer exists");
    assert.equal(card.style.props.has("--dsh-mobile-speed"), false, "dock readings must not leak onto the card");

    const before = dock.style.writes.length;
    setVar("--dsh-mobile-speed", "9 tok/s");
    assert.equal(dock.style.writes.length, before, "an unchanged reading must not be written again");
    setVar("--dsh-mobile-speed", "128.4 tok/s");
    assert.equal(dock.style.writes.length, before + 1, "a changed reading must be written");

    table = {
        "[data-composer-card]": null,
        '[data-slot="conversation.composer.dock"]': null,
        "[data-composer-seat]": null,
    };
    setVar("--dsh-mobile-speed", "42 tok/s");
    assert.equal(html.style.props.get("--dsh-mobile-speed"), '"42 tok/s"', "<html> is the fallback host");
});

test("CSS pins the reading slots so streaming digits cannot reflow the dock", () => {
    const css = sliceBetween("const CSS = `", "\n`;");
    assert.match(css, /font-variant-numeric: tabular-nums/, "digits must be tabular");
    const activity = sliceBetween('[data-composer-stat="activity"] button::after', '[data-composer-stat="usage"]');
    assert.match(activity, /min-width: \d+ch;/);
    assert.match(activity, /text-align: right;/);
    const usage = sliceBetween('[data-composer-stat="usage"] button::after', '[data-composer-stat] svg');
    assert.match(usage, /min-width: \d+ch;/);
    assert.match(usage, /text-align: right;/);
    // The model name is one line: it takes the leftover row space (no 132px cap), and ellipsis is
    // only the fallback for a name that cannot fit even that. The row itself never wraps.
    const model = sliceBetween('button[aria-label*="选择模型"]::after', 'button[aria-label*="选择模型"] span');
    assert.match(model, /max-width: 100%;/);
    assert.match(model, /overflow: hidden;/);
    assert.match(model, /text-overflow: ellipsis;/);
    assert.doesNotMatch(css, /flex-wrap: wrap !important;/);
    assert.match(css, /flex-wrap: nowrap !important;/);
    assert.doesNotMatch(css, /max-width: 132px/);
});

test("the plugin writes no inline style to a React-owned node", () => {
    assert.doesNotMatch(source, /root\.style\.display/);
    assert.doesNotMatch(source, /style\.display = ""/);
    // The only inline-style write left is the hidden <input> the plugin created itself.
    assert.deepEqual(
        codeLines(source).filter((line) => /\.style\.display\s*=/.test(line)),
        ['input.style.display = "none";'],
    );
});

test("both plugin stylesheets are id-guarded against duplicate injection", () => {
    assert.match(source, /if \(document\.getElementById\("dsh-plugin-responsive-style"\) === null\) \{/);
    assert.match(source, /style\.id = "dsh-plugin-responsive-style";/);
    assert.match(source, /if \(document\.getElementById\("dsh-file-actions-style"\) === null\) \{/);
    assert.match(source, /style\.id = "dsh-file-actions-style";/);
});

test("file-panel decoration keeps its semantics on a debounced observer", () => {
    const region = filePanelRegion();
    assert.match(
        region,
        /new MutationObserver\(scheduleDecorate\)\.observe\(document\.documentElement, \{ subtree: true, childList: true \}\)/,
        "row decoration must be MutationObserver-driven",
    );
    const delay = debounceDelay("const scheduleDecorate = () => {", "new MutationObserver");
    assert.ok(delay >= 200 && delay <= 300, `decorate debounce is ${delay}ms, expected 200-300ms`);

    // One append per row, ever: the dataset marker is the "already handled this round" flag.
    assert.match(region, /if \(row\.dataset\.dshFileActions === "1"\) return;/);
    assert.match(region, /row\.dataset\.dshFileActions = "1";/);
    // Header tools are mounted once and self-guard.
    assert.match(region, /if \(header === null \|\| header\.querySelector\("\.dsh-file-tools"\) !== null\) return;/);
    // Folder expansion still refreshes the list once, 900ms after an empty expansion.
    assert.match(region, /if \(after <= before\) refreshFiles\(\);/);
    assert.match(region, /\}, 900\);/);
    // The empty-panel nudge is a single deferred look instead of a poll.
    assert.match(region, /window\.setTimeout\(decorate, \d+\);/);

    const visibility = region.slice(region.indexOf('document.addEventListener("visibilitychange"'));
    assert.match(visibility, /decorate\(\);/, "a visible tab must decorate once");
    assert.match(visibility, /window\.setTimeout\(decorate, \d+\);/, "and retry once after the paint");
});

test("share-text bridge inserts like typing and never throws without a composer", () => {
    assert.match(source, /installComposerTextBridge\(\);/, "the bridge must be installed from apply()");
    const bridge = sliceBetween("function installComposerTextBridge()", "function installAttachmentMenuDefault()");

    // React-owned textarea: prototype setter, then a bubbling input event.
    assert.match(bridge, /Object\.getOwnPropertyDescriptor\(HTMLTextAreaElement\.prototype, "value"\)\?\.set/);
    assert.match(bridge, /new Event\("input", \{ bubbles: true \}\)/);
    // Lexical/contenteditable fallback for the current composer.
    assert.match(bridge, /isContentEditable/);
    assert.match(bridge, /execCommand\("insertText"/);
    // Caret ends up after the inserted text, composer is focused, missing composer returns false.
    assert.match(bridge, /element\.focus\(\);/);
    assert.match(bridge, /setSelectionRange\(next\.length, next\.length\)/);
    assert.match(bridge, /window\.__dshInsertComposerText = \(text\) => \{/);
    assert.match(bridge, /if \(value === ""\) return false;/);
    assert.match(bridge, /\n        return false;\n      \};/, "a missing composer must resolve to false");
});

test("share-text bridge behaves like typing on a fake composer", () => {
    // Pull the function out of the bundle and run it against a minimal DOM: the insertion path is
    // the one thing a text-only guard cannot prove.
    const factorySource = sliceBetween(
        "function installComposerTextBridge()",
        "    /**\n     * Skip the attachment menu",
    ).trimEnd();

    class FakeTextarea {
        constructor() {
            this._value = "";
            this.events = [];
            this.focused = false;
            this.selection = null;
            this.isContentEditable = false;
        }
        get value() {
            return this._value;
        }
        set value(next) {
            this._value = String(next);
        }
        focus() {
            this.focused = true;
        }
        setSelectionRange(from, to) {
            this.selection = [from, to];
        }
        dispatchEvent(event) {
            this.events.push(event);
            return true;
        }
    }
    class FakeEditor {
        constructor(text) {
            this.isContentEditable = true;
            this.textContent = text;
            this.events = [];
            this.focused = false;
        }
        focus() {
            this.focused = true;
        }
        dispatchEvent(event) {
            this.events.push(event);
            return true;
        }
    }
    class FakeEvent {
        constructor(type, init) {
            this.type = type;
            this.bubbles = Boolean(init?.bubbles);
        }
    }
    class FakeInputEvent extends FakeEvent {
        constructor(type, init) {
            super(type, init);
            this.data = init?.data;
        }
    }

    const bridgeFor = (nodes) => {
        const range = { selectNodeContents() {}, collapse() {} };
        const fakeWindow = { getSelection: () => ({ removeAllRanges() {}, addRange() {} }) };
        const fakeDocument = {
            querySelectorAll: () => nodes,
            createRange: () => range,
            execCommand: (command, showUI, value) => {
                const editor = nodes.find((node) => node.isContentEditable);
                editor.textContent += value;
                return true;
            },
        };
        const factory = new Function(
            "window",
            "document",
            "HTMLTextAreaElement",
            "Event",
            "InputEvent",
            `${factorySource}\nreturn installComposerTextBridge;`,
        );
        factory(fakeWindow, fakeDocument, FakeTextarea, FakeEvent, FakeInputEvent)();
        return fakeWindow;
    };

    const area = new FakeTextarea();
    let bridge = bridgeFor([area]);
    assert.equal(bridge.__dshInsertComposerText("hello"), true);
    assert.equal(area.value, "hello");
    assert.equal(area.events[0].type, "input");
    assert.equal(area.events[0].bubbles, true);
    assert.equal(area.focused, true);
    assert.deepEqual(area.selection, [5, 5]);

    const draft = new FakeTextarea();
    draft.value = "draft";
    bridge = bridgeFor([draft]);
    assert.equal(bridge.__dshInsertComposerText("hello"), true);
    assert.equal(draft.value, "draft hello", "shared text must not overwrite a draft");

    const editor = new FakeEditor("draft");
    bridge = bridgeFor([editor]);
    assert.equal(bridge.__dshInsertComposerText("hello"), true);
    assert.equal(editor.textContent, "drafthello");
    assert.equal(editor.events[0].type, "input");
    assert.equal(editor.focused, true);

    assert.equal(bridgeFor([]).__dshInsertComposerText("hello"), false, "no composer must not throw");
    const empty = new FakeTextarea();
    bridge = bridgeFor([empty]);
    assert.equal(bridge.__dshInsertComposerText(""), false);
    assert.equal(bridge.__dshInsertComposerText(null), false);
    assert.equal(empty.value, "");
});

test("the attachment menu is skipped by text, and it picks the file row", () => {
    assert.match(source, /installAttachmentMenuDefault\(\);/, "the skip must be installed from apply()");
    const region = sliceBetween("function installAttachmentMenuDefault()", "function installFileActions()");

    // The trigger is never identified: only the mounted menu is watched, because its markup and
    // its trigger's markup both change between builds while the row labels do not.
    assert.match(region, /new MutationObserver\(/, "the menu mount is the trigger");
    assert.doesNotMatch(region, /dsh-attachment-picker-trigger/, "no dependency on the trigger class");
    assert.match(region, /createTreeWalker\(root, NodeFilter\.SHOW_TEXT/, "rows are found by their text");
    // The file row wins: a file picker can reach photos, an image picker cannot reach documents.
    assert.match(region, /上传附件\|上传文件\|Upload \(file\|attachment\)/);
    assert.match(region, /IMAGE_ROW/, "only a subtree that already holds the image row is walked");
    assert.match(region, /element\.click\(\)/, "the row itself is clicked");
    assert.match(region, /now - lastClickAt < \d+/, "a second mount must not be clicked twice");
});

test("the file panel hands downloads to the shell and uploads with progress", () => {
    // A blob URL plus `a.download` is silently ignored by a WebView: that was the download button
    // doing nothing at all on the phone. The shell bridge enqueues the real URL instead.
    const downloadHook = sliceBetween("const downloadToDisk =", "const newFolder =");
    assert.match(downloadHook, /bridge\.download\(url, name\);/, "downloads go through the shell bridge");
    assert.doesNotMatch(downloadHook, /createObjectURL/, "a blob URL cannot be saved by the WebView");
    assert.match(downloadHook, /window\.location\.origin \+ "\/dsh-file-ops\?action=download/,
        "DownloadManager needs an absolute URL, not a bare path");

    // fetch cannot report upload progress; XMLHttpRequest is the only API a page has for it, and
    // the failure has to carry the HTTP status instead of a generic message.
    assert.match(source, /new XMLHttpRequest\(\)/);
    assert.match(source, /xhr\.upload\.addEventListener\("progress"/);
    assert.match(source, /"HTTP " \+ xhr\.status/);
    assert.match(source, /dsh-transfer-bar/, "progress must be visible, not just logged");
});

test("the phone header and composer survive dsh-claude-style", () => {
    // The theme draws the web GUI for a desktop window. At 393 px its session header held the
    // subagent chip, the background-job chip and both utility buttons on one row: the job chip ran
    // from x=254 to x=432, across the "…" menu (298) and the panel toggle (343), and the title was
    // squeezed to 11 px. Both halves of the fix are CSS scoped to the phone form.
    const claude = sliceBetween("/* dsh-claude-style on a phone.", "/* Frosted/glass themes");
    assert.match(claude, /header \[data-dsh-claude-part~="view-tabs"\] \{\n  display: none !important;/,
        "the 对话/轨迹 view switch is a desktop control the phone does not need");
    assert.match(claude, /header button\[aria-label\*="后台任务"\][\s\S]{0,240}width: 35px !important;/,
        "the job chip must collapse to its spinner instead of running across the header");
    assert.match(claude, /header nav\[aria-label="会话层级"\] \{\n  flex: 1 1 0% !important;/,
        "the title must take the row's free width");
    assert.doesNotMatch(claude, /header nav\[aria-label="会话层级"\] \{\n  display: none/,
        "hiding the title was one of the two options; the width fix is the one that shipped");

    // The theme renders its own model control and hides the core trigger. Our ::after rewrite is for
    // the core trigger only; on the theme's button it printed a second model name over the first.
    assert.match(claude, /button\[aria-label\*="选择模型"\]\.dsh-claude-model-btn::after,[\s\S]{0,200}content: none !important;/,
        "the theme's model label must not be overprinted by our ::after");
    assert.match(claude, /body\[data-dsh-claude-style\] \.dsh-claude-model-seat \{\n  min-width: 0 !important;/,
        "the theme's seat still has to shrink inside the composer row");

    // Vertical rhythm: the composer card sat 8px off the bottom edge, on the gesture pill. The header
    // is deliberately NOT touched — dsh-claude-style computes its page box from the header it rendered,
    // so pinning the rows left the hero page taller than the viewport and the composer below the fold.
    assert.match(claude, /\[data-dsh-claude-part~="composer-stack"\] \{\n  padding-bottom: 12px !important;/,
        "the composer needs a gap over the navigation bar");
    assert.doesNotMatch(claude, /header\[data-dsh-claude-part~="conversation-header"\][\s\S]{0,200}(grid-template-rows|height: auto)/,
        "header geometry belongs to the theme; changing it hides the composer");
});

test("the page zoom dsh-claude-style applies is divided back out of the phone form", () => {
    // The theme's 页面缩放 preference sets an inline `body { zoom: 1.25 }`. Zoom scales every fixed
    // length inside <body> — including the device-pixel safe-area insets the shell publishes, so the
    // phone reserved 50 px for a 40 px status bar — but it does not scale viewport units. Upstream
    // sizes the fullscreen right panel `100vw`, so on a 400 px phone the panel rendered 500 px wide
    // and hung 100 px off the left edge, cutting every card's left margin away.
    const claude = sliceBetween("/* dsh-claude-style on a phone.", "/* Frosted/glass themes");
    assert.match(claude, /--dsh-mobile-top-inset: max\([\s\S]{0,180}var\(--dsh-android-system-top, 0px\) \/ var\(--dsh-phone-zoom, 1\)/,
        "the status-bar inset is device pixels; it has to be divided by the page zoom");
    assert.match(claude, /\[data-sidebar-right-panel="fullscreen"\] \{\n  width: calc\(100vw \/ var\(--dsh-phone-zoom, 1\)\) !important;/,
        "the fullscreen right panel is sized in 100vw and needs the same correction");
    assert.match(claude, /--dsh-sidebar-width: calc\(100vw \/ var\(--dsh-phone-zoom, 1\)\) !important;/,
        "the dock transition offsets by the panel width, so it follows the corrected one");
    assert.match(claude, /\[data-dsh-claude-part="panel-guide"\] kbd,[\s\S]{0,120}display: none !important;/,
        "the panel guide's keyboard hints have no phone equivalent and squeeze the row text");

    // The factor is read from the rendered page, never hardcoded: a page without the preference (a
    // zoomless theme, the desktop shell) falls back to 1 and stays byte-identical.
    assert.match(source, /installPageZoomVar\(\);/, "the zoom probe must be installed from apply()");
    const region = sliceBetween("function installPageZoomVar() {", "Use the Android speech recognizer");
    assert.match(region, /getComputedStyle\(body\)\.zoom/, "the factor comes from the rendered body");
    assert.match(region, /setProperty\("--dsh-phone-zoom", factor\)/, "the factor is published for the sheet");
    assert.match(region, /if \(root\.style\.getPropertyValue\("--dsh-phone-zoom"\) !== factor\)/,
        "no <html> style write unless the factor actually changed");
    assert.doesNotMatch(region, /setInterval\(/, "no repeating timer");
});

test("a drawer row that navigates also collapses the phone drawer", () => {
    // The theme paints its own sidebar and never closed the phone drawer: the panel stayed over the
    // conversation it had just opened, until the user tapped beside it.
    assert.match(source, /installDrawerAutoClose\(\);/, "the drawer close must be installed from apply()");
    const region = sliceBetween("function installDrawerAutoClose() {", "Use the Android speech recognizer");
    assert.match(region, /data-dsh-claude-part~="session-row"/);
    assert.match(region, /data-dsh-claude-part~="new-session"/);
    assert.match(region, /data-dsh-claude-part~="panel-row"/);
    assert.match(region, /document\.documentElement\.hasAttribute\("data-dsh-mobile-form"\)/,
        "the close is a phone behaviour, never a desktop one");
    assert.match(region, /aria-expanded"\) !== "true"\) return;/, "only an open drawer is closed");
    assert.match(region, /document\.querySelector\("\[data-dsh-sidebar-toggle\]"\)[\s\S]{0,300}current\.click\(\)/,
        "the drawer closes through the shell's own toggle, re-queried after the header re-render");
    assert.match(region, /\}, 90\);/, "the row's own handler runs first, then the drawer closes");
});

test("the reported bundle version matches package.json and is new enough for the bridge", () => {
    const declared = source.match(/const PLUGIN_VERSION = "([^"]+)";/);
    assert.ok(declared, "PLUGIN_VERSION must be declared in client.js");
    assert.equal(declared[1], pkg.version, "PLUGIN_VERSION and package.json version drifted apart");
    assert.match(source, /window\.__dshMobilePlugin = \{ version: PLUGIN_VERSION \};/);
    assert.match(source, /document\.documentElement\.setAttribute\("data-dsh-plugin-version", PLUGIN_VERSION\)/);

    const atLeast = (version, [major, minor, patch]) => {
        const [a, b, c] = version.split(".").map(Number);
        if ([a, b, c].some(Number.isNaN)) return false;
        if (a !== major) return a > major;
        if (b !== minor) return b > minor;
        return c >= patch;
    };
    assert.ok(atLeast(pkg.version, [0, 1, 13]), `plugin version ${pkg.version} predates the reading-scope fix`);
});
