import assert from "node:assert/strict";
import fs from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

/**
 * lib/client.js is a browser bundle (`window.__ModuleLoader__.load(...)` + DOM access), so it
 * cannot be imported in Node. These guards read it as text and pin the three plugin-side contracts
 * the Android shell relies on:
 *  - no periodic timers: the page must not refresh itself (the phone kept reporting it did),
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

const composerRegion = () => sliceBetween("const compactActivity = () => {", "installFileActions();");
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
        /if \(\(rootStyle\.getPropertyValue\(name\) \|\| ""\) !== quoted\) rootStyle\.setProperty\(name, quoted\);/,
        "setVar must compare before writing",
    );
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
    const bridge = sliceBetween("function installComposerTextBridge()", "function installImageUploadDefault()");

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
        "    /**\n     * On the phone the attachment trigger",
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
    assert.ok(atLeast(pkg.version, [0, 1, 12]), `plugin version ${pkg.version} predates the share bridge`);
});
