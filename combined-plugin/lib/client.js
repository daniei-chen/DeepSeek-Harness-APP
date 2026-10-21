window.__ModuleLoader__.load({
	id: "@dsh-android/dsh-client-ui-responsive",
	factory: (require) => {
		var module = { exports: {} };
		var exports = module.exports;
		Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
		let react = require("react");
		let react_jsx_runtime = require("react/jsx-runtime");
		let _deepseek_ai_dsh_client_ui_primitives = require("@deepseek-ai/dsh-client-ui-primitives");
		let _deepseek_ai_dsh_client_store = require("@deepseek-ai/dsh-client-store");
		//#region \0dsh-css:/home/agentuser/DeepSeek 2/dsh-upgrade-021/plugin/dsh-client-ui-responsive/src/client/ExportResultDialog.module.css.mjs
		const css$4 = "._3f93ua_backdrop{z-index:1;background:var(--dsw-alias-bg-mask-1,#0000003d);justify-content:center;align-items:center;padding:16px;display:flex;position:absolute;inset:0}._3f93ua_dialog{box-sizing:border-box;background:var(--dsw-alias-bg-base,#fff);border:1px solid var(--dsw-alias-border-l2,#0000001a);width:min(440px,100%);max-height:70%;color:var(--dsw-alias-label-primary,#0f1115);border-radius:12px;flex-direction:column;gap:12px;padding:16px;display:flex;overflow:auto}._3f93ua_title{margin:0;font-size:16px;font-weight:600}._3f93ua_detail{overflow-wrap:anywhere;white-space:pre-wrap;margin:0;font-size:13px;line-height:1.5}._3f93ua_detail[data-status=success]{color:var(--dsw-alias-state-success-primary)}._3f93ua_detail[data-status=error]{color:var(--dsw-alias-state-error-primary)}._3f93ua_actions{justify-content:flex-end;display:flex}._3f93ua_button{border:1px solid var(--dsw-alias-border-l2,#0000001a);background:var(--dsw-alias-button-primary-fill);color:var(--dsw-alias-label-primary-foreground,#fff);cursor:pointer;border-radius:8px;padding:8px 14px;font-size:13px}._3f93ua_button:hover{background:var(--dsw-alias-button-primary-hover)}";
		const tagId$4 = "@dsh-android/dsh-client-ui-responsive/ExportResultDialog.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$4) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@dsh-android/dsh-client-ui-responsive";
			tag.dataset.pluginCss = tagId$4;
			tag.textContent = css$4;
			document.head.appendChild(tag);
		}
		var ExportResultDialog_module_css_default = {
			"actions": "_3f93ua_actions",
			"backdrop": "_3f93ua_backdrop",
			"button": "_3f93ua_button",
			"detail": "_3f93ua_detail",
			"dialog": "_3f93ua_dialog",
			"title": "_3f93ua_title"
		};
		//#endregion
		//#region src/client/ExportResultDialog.tsx
		/**
		* Export-result dialog: the `shell.overlay` entry that renders the Android
		* shell's session-export outcome (and this plugin's own native-action
		* failures). Pure component: state arrives through the framework-bound
		* `useExportResult` hook, dismissal through the injected callback. The markup
		* reuses the web-ui dialog conventions (role=dialog / aria-modal) and the
		* shared design tokens, so the dialog matches the app's modal surfaces.
		*/
		/** The single entry component; renders nothing while no result is open. */
		function ExportResultDialog({ useExportResult, close }) {
			const state = useExportResult((snapshot) => snapshot);
			(0, react.useEffect)(() => {
				if (!state.open) return;
				const onKeyDown = (event) => {
					if (event.key === "Escape") close();
				};
				window.addEventListener("keydown", onKeyDown);
				return () => {
					window.removeEventListener("keydown", onKeyDown);
				};
			}, [state.open, close]);
			if (!state.open) return null;
			return /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
				className: ExportResultDialog_module_css_default.backdrop,
				onClick: () => close(),
				children: /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
					role: "dialog",
					"aria-modal": "true",
					"aria-labelledby": "dsh-export-result-title",
					className: ExportResultDialog_module_css_default.dialog,
					onClick: (event) => {
						event.stopPropagation();
					},
					children: [
						/* @__PURE__ */ (0, react_jsx_runtime.jsx)("h2", {
							id: "dsh-export-result-title",
							className: ExportResultDialog_module_css_default.title,
							children: state.title
						}),
						/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
							className: ExportResultDialog_module_css_default.detail,
							"data-status": state.ok ? "success" : "error",
							children: state.detail
						}),
						/* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
							className: ExportResultDialog_module_css_default.actions,
							children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: ExportResultDialog_module_css_default.button,
								onClick: () => close(),
								children: "关闭"
							})
						})
					]
				})
			});
		}
		//#endregion
		//#region src/client/mobile-settings.css.ts
		/**
		* Mobile settings-panel adaptation (issue #1; 2026-09-03 rework, 2026-09-10 de-fork).
		* Upstream SettingsRoot draws a fixed overlay with an 800px two-column panel
		* (nav + options). On the phone form it must reflow to a single column and fill
		* the viewport (user requirement: 设置页全屏显示).
		*
		* The panel renders inside the sidebar subtree, whose CSS-Module class names are
		* hashed and unreachable from here — and its own markup carries no stable
		* attribute. The mobile marker therefore tags the panel
		* (`data-dsh-settings-dialog`, written from its nav/content structure) and this
		* sheet keys on that tag plus the phone-form flag: pure attribute selectors,
		* effective on old kernels too (no `:has()`).
		*/
		const MOBILE_SETTINGS_CSS = `
  html[data-dsh-mobile-form] [data-dsh-settings-dialog] {
    box-sizing: border-box;
    width: 100vw;
    max-width: none;
    /* The panel is centred by its fixed overlay, so shrinking the height would
       move its header back under the status bar. Keep the full height and inset
       the content instead: border-box keeps the total box at 100vh. */
    height: 100vh;
    max-height: none;
    padding-top: var(--dsh-mobile-top-inset, 0px);
    border-radius: 0;
    flex-direction: column;
  }

  html[data-dsh-mobile-form] [data-dsh-settings-dialog] > nav {
    width: 100%;
    height: auto;
    flex: none;
    flex-direction: row;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    overflow-x: auto;
    border-right: none;
    border-bottom: 1px solid var(--dsw-alias-border-l1);
  }

  html[data-dsh-mobile-form] [data-dsh-settings-dialog] > nav > div:first-child {
    flex: none;
    padding: 0;
    white-space: nowrap;
  }

  html[data-dsh-mobile-form] [data-dsh-settings-dialog] > nav > div:nth-child(2) {
    flex-direction: row;
    gap: 4px;
    flex: 1;
    min-width: 0;
    overflow-x: auto;
  }

  html[data-dsh-mobile-form] [data-dsh-settings-dialog] > nav > div:nth-child(2) > button {
    /* Review 2026-08-18: the original rule was an unclosed empty block since #2 and never
       applied. Completed by container semantics: the nav button container is
       flex-direction: row + overflow-x: auto, so buttons need flex: none to avoid being
       compressed and to scroll horizontally with the container. */
    flex: none;
  }

  /* Content column: flex:1 but min-height:auto would hold the options scroll area's
     full content height and overflow the panel; allow it to shrink so the options
     area scrolls inside. */
  html[data-dsh-mobile-form] [data-dsh-settings-dialog] > div:nth-child(2) {
    min-height: 0;
  }
`;
		//#endregion
		//#region src/client/composer-menu.css.ts
		/**
		* Composer popup geometry (upstream ui-input-trigger + ui-model-selection):
		* - The slash menu's scroll container (`.viewport`, the `[role='listbox']`) is a
		*   flex child without flex:1, so when the candidate list exceeds max-height the
		*   viewport grows past the menu and is clipped by the menu's overflow:hidden —
		*   the scrollbar lands outside the visible area and the list appears
		*   unscrollable. Fix: let the viewport fill the menu and scroll inside it.
		* - The upstream menus clamp against viewport y=0 only, and size themselves
		*   against their trigger, so on phones they can leave the viewport sideways or
		*   rise above the fixed top bar. `ComposerPopupGuard` measures each open popup
		*   and writes the caps below; the width cap is applied to the scroll container
		*   and to the painted card alike so the card never stays wider than its
		*   content (a blank strip with a detached scrollbar — issue apk#135).
		*/
		const COMPOSER_MENU_CSS = `
[data-composer-card] [role='listbox'] > div {
  flex: 1 1 0%;
  min-height: 0;
}

/* 宽度钳制只作用在绘制卡片上（[data-dsh-popup]），滚动容器（listbox）必须铺满卡片：
   实测（450px 视口）卡片 424 宽而 listbox 只有 340 → 滚动条离卡片右缘 64px，
   看起来就是「滚动条没吸在最右侧、和布局边界不匹配」（#135 的回归形态）。
   注意：本段注释在模板字符串内，**不要写反引号**（会提前终止字符串，tsc 报 TS1005）。 */
html[data-dsh-mobile-form] [data-composer-card] [data-dsh-popup] {
  max-width: var(--dsh-mobile-popup-max-width, min(96vw, 420px)) !important;
}
html[data-dsh-mobile-form] [data-composer-card] [role='menu'] {
  max-width: var(--dsh-mobile-popup-max-width, min(96vw, 420px)) !important;
}
html[data-dsh-mobile-form] [data-composer-card] [role='listbox'] {
  max-width: none !important;
}

html[data-dsh-mobile-form] [data-composer-card] [role='listbox'] {
  max-height: var(--dsh-mobile-menu-max-height, 320px) !important;
}

/* The model menu is its own painted surface; its height cap only exists while
   the guard measures one, so the upstream 360px design cap stays in charge. */
html[data-dsh-mobile-form] [data-composer-card] [role='menu'] {
  max-height: var(--dsh-mobile-menu-max-height, none) !important;
}

/* Horizontal containment: the guard marks the painted card of every open
   popup and writes its shift, keeping the card inside the viewport. */
[data-dsh-popup] {
  transform: translateX(var(--dsh-mobile-popup-shift, 0px));
}
`;
		//#endregion
		//#region src/client/attachment-picker-menu.css.ts
		/** Composer paperclip source chooser, built from DSH semantic surface and elevation tokens. */
		const ATTACHMENT_PICKER_MENU_CSS = `
/* Our own paperclip (D2/D4). Upstream 0.1.7-rc.1 ships no paperclip, so this plugin mounts its own
   button into the standard '[data-slot="conversation.input.left"]' anchor. The slot outlet renders
   that anchor with 'display: contents' (ui-renderer scoped-slots.tsx:1064-1091), so the button becomes
   a flex item of the upstream '.tools' row and inherits its gap. Chrome mirrors '.add'
   (InputBar.module.css:314-326): a round control on the selector fill with a primary glyph.

   Narrow-screen width bound (issue #54 / D9). '.row' is 'flex-wrap: wrap' and '.trailing' is
   'flex: none' (InputBar.module.css:251-302): whatever the left group gains must fit the row's spare
   width, or the trailing group wraps to a second line and the controls misalign vertically.

   Arithmetic at 360dp, from the files that own these numbers:
     row content width   302 = card 318 - row padding 16   (composer-row.css.ts:6-8)
     .tools baseline      88 = 28 add + 8 gap + 44 permission chip + 8 gap
     .row gap             12                                (InputBar.module.css:256)
     .trailing           182 = model pill capped at 104     (composer-row.css.ts:10-12,18-20)
     baseline total      282 = 88 + 12 + 182, so spare = 20
   A control appended to '.tools' costs one more '.tools' gap (8px in the '<=560px' container,
   InputBar.module.css:305-311) plus its own box: 8 + 28 = 36px > the 20px spare, so the trailing
   group wraps by 16px and the add button misaligns with the model pill (issue #54).

   Below 400px the box is 24px and 'margin-inline: -8px' recovers 16px of outer width, 8px of it
   exactly cancelling the seat gap the control introduced: '.tools' = 88 + 8 - 8 + 24 - 8 = 104, the
   row total = 104 + 12 + 182 = 298, so 4px of spare remain. Nothing overlaps another control: the
   start margin only closes the gap in front of this button, and nothing follows it inside '.tools'.
   The remaining assumption is the permission chip's 44px icon-only width at this width
   (PermissionSelect @container <=460px, PermissionSelect.module.css:94-98); a 'conversation.input.plan'
   chip mounted alongside would consume the spare and wrap. */
[data-dsh-attachment-picker-trigger] {
  display: grid;
  place-items: center;
  flex: none;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 999px;
  corner-shape: round;
  background: var(--dsw-specific-selector);
  color: var(--dsw-alias-label-primary);
  cursor: pointer;
}
[data-dsh-attachment-picker-trigger]:hover {
  background: var(--dsw-alias-interactive-bg-hover-solid);
}
[data-dsh-attachment-picker-trigger]:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--dsw-alias-border-l3);
}
[data-dsh-attachment-picker-trigger]:disabled {
  opacity: 0.5;
  cursor: default;
}
@media (max-width: 400px) {
  [data-dsh-attachment-picker-trigger] {
    width: 24px;
    height: 24px;
    margin-inline: -8px;
  }
}
[data-dsh-attachment-picker-menu] {
  position: fixed;
  z-index: 2147483000;
  display: grid;
  gap: 2px;
  box-sizing: border-box;
  padding: 6px;
  border: 1px solid var(--dsw-alias-border-l4);
  border-radius: 12px;
  background: var(--dsw-specific-menu);
  backdrop-filter: var(--dsw-menu-backdrop-filter);
  box-shadow: var(--dsw-elevation-panel);
  color: var(--dsw-alias-label-primary);
  font: var(--dsw-font-markdown-base);
}
[data-dsh-attachment-picker-menu] [data-dsh-attachment-picker-item] {
  display: flex;
  min-height: 40px;
  width: 100%;
  align-items: center;
  gap: 10px;
  box-sizing: border-box;
  padding: 0 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: start;
}
[data-dsh-attachment-picker-menu] [data-dsh-attachment-picker-item] svg {
  flex: none;
  color: var(--dsw-alias-label-secondary);
}
[data-dsh-attachment-picker-menu] [data-dsh-attachment-picker-item]:hover,
[data-dsh-attachment-picker-menu] [data-dsh-attachment-picker-item]:focus-visible {
  outline: none;
  background: var(--dsw-alias-bg-layer-2);
}
[data-dsh-attachment-picker-menu] [data-dsh-attachment-picker-item]:active {
  background: var(--dsw-alias-bg-layer-1);
}
`;
		//#endregion
		//#region \0@oxc-project+runtime@0.152.0/helpers/esm/typeof.js
		function _typeof(o) {
			"@babel/helpers - typeof";
			return _typeof = "function" == typeof Symbol && "symbol" == typeof Symbol.iterator ? function(o) {
				return typeof o;
			} : function(o) {
				return o && "function" == typeof Symbol && o.constructor === Symbol && o !== Symbol.prototype ? "symbol" : typeof o;
			}, _typeof(o);
		}
		//#endregion
		//#region \0@oxc-project+runtime@0.152.0/helpers/esm/toPrimitive.js
		function toPrimitive(t, r) {
			if ("object" != _typeof(t) || !t) return t;
			var e = t[Symbol.toPrimitive];
			if (void 0 !== e) {
				var i = e.call(t, r || "default");
				if ("object" != _typeof(i)) return i;
				throw new TypeError("@@toPrimitive must return a primitive value.");
			}
			return ("string" === r ? String : Number)(t);
		}
		//#endregion
		//#region \0@oxc-project+runtime@0.152.0/helpers/esm/toPropertyKey.js
		function toPropertyKey(t) {
			var i = toPrimitive(t, "string");
			return "symbol" == _typeof(i) ? i : i + "";
		}
		//#endregion
		//#region \0@oxc-project+runtime@0.152.0/helpers/esm/defineProperty.js
		function _defineProperty(e, r, t) {
			return (r = toPropertyKey(r)) in e ? Object.defineProperty(e, r, {
				value: t,
				enumerable: !0,
				configurable: !0,
				writable: !0
			}) : e[r] = t, e;
		}
		//#endregion
		//#region src/client/mobile/attachment-picker-menu.ts
		/**
		* Composer attachment-source chooser (D2/D4, 2026-09-25).
		*
		* Upstream 0.1.7-rc.1 has NO dedicated paperclip. The only composer button is the command "+"
		* (InputBar.tsx:419-431: aria-label t('input.commands') = "添加文件或调用指令",
		* aria-haspopup="listbox", onClick=onToggleCommandMenu) and the hidden multiple-file input is its
		* own row sibling. The previous revision claimed "the last BUTTON before the hidden input" as the
		* paperclip, so it claimed that "+": preventDefault + stopImmediatePropagation on the capture phase
		* swallowed onToggleCommandMenu, the command menu never opened, and the user saw "the paperclip is
		* gone and + uploads the file" (D4). The structural pairing itself is the defect, not its lookup.
		*
		* The enhancer now owns its own control and claims no upstream button at all:
		* - a paperclip button is mounted INTO the standard 'conversation.input.left' slot anchor
		*   ('[data-slot="conversation.input.left"]'). The anchor renders with 'display: contents', so the
		*   button becomes a flex item of the same '.tools' toolbar at the slot's own seat, after the
		*   permission/plan seats - the upstream InputBar is not modified and nothing is inserted into its
		*   private '.tools' structure;
		* - only that button opens the menu (its own click listener). No other button is intercepted, so the
		*   upstream "+" keeps its own onClick by construction;
		* - a chosen source is still delegated to InputBar's existing hidden multiple-file input: the menu
		*   item sets that input's accept filter inside its own user gesture, clicks it, then restores the
		*   filter. No second bridge, input, upload state, or attachment rail is introduced.
		*
		* Mounting is best-effort. The composer card may not exist yet (boot order) and the anchor comes and
		* goes with the session; a failure to mount must never propagate into the plugin body, because that
		* body is the phone runtime the user asked to keep alive.
		*/
		const MENU_ATTR = "data-dsh-attachment-picker-menu";
		const ITEM_ATTR = "data-dsh-attachment-picker-item";
		const TRIGGER_ATTR = "data-dsh-attachment-picker-trigger";
		/** The standard upstream slot seat for extra composer-left controls. */
		const SLOT_SELECTOR = "[data-slot=\"conversation.input.left\"]";
		const CARD_SELECTOR = "[data-composer-card]";
		function copyForDocument() {
			return (document.documentElement.lang || navigator.language || "").toLowerCase().startsWith("zh") ? {
				file: "上传附件",
				image: "上传图片"
			} : {
				file: "Upload attachment",
				image: "Upload image"
			};
		}
		/**
		* The hidden multiple-file input the upstream InputBar wired its own admission path to.
		*
		* Card-scoped on purpose: the enhancer no longer derives a button from the input (that derivation is
		* what claimed the "+"), it derives the input from our own trigger's composer card. The card holds
		* exactly one such input (InputBar.tsx:433-440), and a second one would be an upstream change the
		* attachment flow has to re-review anyway.
		*/
		function hiddenFileInputIn(button) {
			var _button$closest;
			return ((_button$closest = button.closest(CARD_SELECTOR)) === null || _button$closest === void 0 ? void 0 : _button$closest.querySelector("input[type=\"file\"][multiple]")) ?? null;
		}
		/** Glyph for one menu row. */
		function icon(kind) {
			const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
			svg.setAttribute("viewBox", "0 0 20 20");
			svg.setAttribute("width", "17");
			svg.setAttribute("height", "17");
			svg.setAttribute("aria-hidden", "true");
			svg.setAttribute("fill", "none");
			svg.setAttribute("stroke", "currentColor");
			svg.setAttribute("stroke-width", "1.6");
			svg.setAttribute("stroke-linecap", "round");
			svg.setAttribute("stroke-linejoin", "round");
			const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
			path.setAttribute("d", kind === "file" ? "M5 2.75h6l4 4v10.5H5zM11 2.75v4h4M7.25 11h5.5M7.25 14h5.5" : "M3.25 4.25h13.5v11.5H3.25zM5.5 13l3-3 2.25 2.25 1.5-1.5 2.5 2.5M7.25 7.75h.01");
			svg.appendChild(path);
			return svg;
		}
		/** Paperclip glyph for our own trigger (the "+" glyph belongs to upstream and stays there). */
		function paperclipIcon() {
			const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
			svg.setAttribute("viewBox", "0 0 20 20");
			svg.setAttribute("width", "16");
			svg.setAttribute("height", "16");
			svg.setAttribute("aria-hidden", "true");
			svg.setAttribute("fill", "none");
			svg.setAttribute("stroke", "currentColor");
			svg.setAttribute("stroke-width", "1.6");
			svg.setAttribute("stroke-linecap", "round");
			svg.setAttribute("stroke-linejoin", "round");
			const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
			path.setAttribute("d", "M12.9 5.2 7 11.1a1.6 1.6 0 0 0 2.26 2.26l6.32-6.32a3.3 3.3 0 0 0-4.67-4.67L4.4 8.86a4.9 4.9 0 0 0 6.93 6.93l5.2-5.2");
			svg.appendChild(path);
			return svg;
		}
		/**
		* Owns the paperclip trigger, the short-lived source menu, and the delegation to InputBar's input.
		*/
		var AttachmentPickerMenuEnhancer = class {
			constructor() {
				_defineProperty(this, "menu", null);
				_defineProperty(this, "trigger", null);
				_defineProperty(this, "restoreActivePicker", null);
				_defineProperty(this, "observer", null);
				_defineProperty(this, "mountScheduled", false);
				_defineProperty(this, "attached", false);
				_defineProperty(this, "mounted", []);
				_defineProperty(this, "onPointerDown", (event) => {
					var _this$trigger;
					if (this.menu === null || !(event.target instanceof Node)) return;
					if (this.menu.contains(event.target) || ((_this$trigger = this.trigger) === null || _this$trigger === void 0 ? void 0 : _this$trigger.contains(event.target)) === true) return;
					this.close();
				});
				_defineProperty(this, "onKeyDown", (event) => {
					if (event.key !== "Escape" || this.menu === null) return;
					event.preventDefault();
					this.close(true);
				});
				_defineProperty(this, "onViewportChange", () => {
					if (this.trigger === null || this.menu === null) return;
					this.place(this.trigger, this.menu);
				});
			}
			attach() {
				if (this.attached) return;
				this.attached = true;
				try {
					document.addEventListener("pointerdown", this.onPointerDown, true);
					document.addEventListener("keydown", this.onKeyDown, true);
					window.addEventListener("resize", this.onViewportChange);
					window.addEventListener("scroll", this.onViewportChange, true);
					this.observer = new MutationObserver((records) => {
						this.onMutations(records);
					});
					this.observer.observe(document.documentElement, {
						childList: true,
						subtree: true
					});
					this.syncMounts();
				} catch (error) {
					console.warn("[dsh-attachment-picker] attach failed; composer stays usable without the paperclip", error);
				}
			}
			detach() {
				var _this$observer, _this$restoreActivePi;
				if (!this.attached) return;
				this.attached = false;
				document.removeEventListener("pointerdown", this.onPointerDown, true);
				document.removeEventListener("keydown", this.onKeyDown, true);
				window.removeEventListener("resize", this.onViewportChange);
				window.removeEventListener("scroll", this.onViewportChange, true);
				(_this$observer = this.observer) === null || _this$observer === void 0 || _this$observer.disconnect();
				this.observer = null;
				this.mountScheduled = false;
				this.close();
				for (const entry of this.mounted) entry.trigger.remove();
				this.mounted.length = 0;
				(_this$restoreActivePi = this.restoreActivePicker) === null || _this$restoreActivePi === void 0 || _this$restoreActivePi.call(this);
			}
			/**
			* Whether a tracked anchor or trigger left the DOM.
			*
			* Deliberately false while nothing is mounted: "no anchor seen yet" is answered by the cheap
			* added-node probe in onMutations, so an enhancer that has not mounted (no composer yet) does not
			* run a document-wide query on every unrelated mutation batch.
			*/
			mountSetDirty() {
				for (const entry of this.mounted) if (!entry.anchor.isConnected || !entry.trigger.isConnected) return true;
				return false;
			}
			/** Whether a mutated node is a slot anchor, or contains one. */
			carriesSlotAnchor(node) {
				if (!(node instanceof Element)) return false;
				return node.matches(SLOT_SELECTOR) || node.querySelector(SLOT_SELECTOR) !== null;
			}
			onMutations(records) {
				let relevant = this.mountSetDirty();
				if (!relevant) for (const record of records) {
					for (const node of record.addedNodes) if (this.carriesSlotAnchor(node)) {
						relevant = true;
						break;
					}
					if (relevant) break;
				}
				if (relevant) this.scheduleMounts();
			}
			scheduleMounts() {
				if (this.mountScheduled) return;
				this.mountScheduled = true;
				queueMicrotask(() => {
					this.mountScheduled = false;
					if (!this.attached) return;
					this.syncMounts();
				});
			}
			/** Mount our trigger into every live slot anchor that does not have one yet. */
			syncMounts() {
				for (let index = this.mounted.length - 1; index >= 0; index -= 1) {
					const entry = this.mounted[index];
					if (!entry.anchor.isConnected || !entry.trigger.isConnected) this.mounted.splice(index, 1);
				}
				for (const anchor of document.querySelectorAll(SLOT_SELECTOR)) try {
					if (this.mounted.some((entry) => entry.anchor === anchor)) continue;
					if (anchor.querySelector("[" + TRIGGER_ATTR + "]") !== null) continue;
					const trigger = this.createTrigger();
					anchor.append(trigger);
					this.mounted.push({
						anchor,
						trigger
					});
				} catch (error) {
					console.warn("[dsh-attachment-picker] mount into " + SLOT_SELECTOR + " failed", error);
				}
			}
			createTrigger() {
				const copy = copyForDocument();
				const button = document.createElement("button");
				button.type = "button";
				button.className = "dsh-attachment-picker-trigger";
				button.setAttribute(TRIGGER_ATTR, "");
				button.setAttribute("aria-label", copy.file);
				button.setAttribute("aria-haspopup", "menu");
				button.setAttribute("aria-expanded", "false");
				button.append(paperclipIcon());
				button.addEventListener("click", (event) => {
					event.preventDefault();
					this.toggle(button);
				});
				return button;
			}
			toggle(button) {
				if (this.trigger === button && this.menu !== null) {
					this.close(true);
					return;
				}
				this.open(button);
			}
			open(button) {
				this.close();
				const copy = copyForDocument();
				const menu = document.createElement("div");
				menu.setAttribute(MENU_ATTR, "");
				menu.setAttribute("role", "menu");
				menu.setAttribute("aria-label", copy.file + " / " + copy.image);
				for (const [kind, label] of [["file", copy.file], ["image", copy.image]]) {
					const item = document.createElement("button");
					item.type = "button";
					item.setAttribute(ITEM_ATTR, kind);
					item.setAttribute("role", "menuitem");
					item.setAttribute("aria-label", label);
					item.append(icon(kind));
					const text = document.createElement("span");
					text.textContent = label;
					item.append(text);
					item.addEventListener("click", (event) => {
						event.preventDefault();
						event.stopPropagation();
						this.choose(button, kind);
					});
					menu.append(item);
				}
				document.body.append(menu);
				button.setAttribute("aria-expanded", "true");
				this.menu = menu;
				this.trigger = button;
				this.place(button, menu);
			}
			place(button, menu) {
				const rect = button.getBoundingClientRect();
				const width = Math.min(264, Math.max(180, window.innerWidth - 24));
				const left = Math.min(Math.max(rect.left, 12), Math.max(12, window.innerWidth - width - 12));
				menu.style.setProperty("width", width + "px");
				menu.style.setProperty("left", left + "px");
				menu.style.setProperty("bottom", Math.max(12, window.innerHeight - rect.top + 8) + "px");
			}
			choose(button, kind) {
				var _this$restoreActivePi2;
				const input = hiddenFileInputIn(button);
				if (input === null || input.disabled || !input.isConnected) {
					this.close();
					return;
				}
				(_this$restoreActivePi2 = this.restoreActivePicker) === null || _this$restoreActivePi2 === void 0 || _this$restoreActivePi2.call(this);
				const hadAccept = input.hasAttribute("accept");
				const originalAccept = input.getAttribute("accept");
				let restored = false;
				let pickerBackgrounded = document.visibilityState === "hidden";
				let fallbackTimer;
				const restore = () => {
					if (restored) return;
					restored = true;
					input.removeEventListener("change", restore);
					input.removeEventListener("cancel", restore);
					document.removeEventListener("visibilitychange", onVisibilityChange);
					window.removeEventListener("focus", onWindowFocus);
					if (fallbackTimer !== void 0) window.clearTimeout(fallbackTimer);
					if (this.restoreActivePicker === restore) this.restoreActivePicker = null;
					if (hadAccept) input.setAttribute("accept", originalAccept ?? "");
					else input.removeAttribute("accept");
				};
				function onVisibilityChange() {
					if (document.visibilityState === "hidden") {
						pickerBackgrounded = true;
						return;
					}
					if (pickerBackgrounded) window.setTimeout(restore, 0);
				}
				function onWindowFocus() {
					if (pickerBackgrounded) window.setTimeout(restore, 0);
				}
				input.setAttribute("accept", kind === "image" ? "image/*" : "*/*");
				input.addEventListener("change", restore, { once: true });
				input.addEventListener("cancel", restore, { once: true });
				document.addEventListener("visibilitychange", onVisibilityChange);
				window.addEventListener("focus", onWindowFocus);
				this.restoreActivePicker = restore;
				fallbackTimer = window.setTimeout(restore, 12e4);
				try {
					input.click();
				} finally {
					this.close();
				}
			}
			close(focus = false) {
				var _this$menu;
				const trigger = this.trigger;
				(_this$menu = this.menu) === null || _this$menu === void 0 || _this$menu.remove();
				this.menu = null;
				this.trigger = null;
				if (trigger !== null) {
					trigger.setAttribute("aria-expanded", "false");
					if (focus) trigger.focus({ preventScroll: true });
				}
			}
		};
		//#endregion
		//#region src/client/composer-row.css.ts
		/**
		* Composer control-row narrow-screen fixes:
		* - The model-selection pill is 176px fixed; on phones below the 400px
		*   breakpoint it overlaps the permission/access pill (device-observed on
		*   360dp phones). Cap its width and ellipsize so both stay tappable.
		* - The row is flex-wrap: wrap (upstream InputBar). On 360dp the left group
		*   (add + access-mode, 88px) + gap (12px) + trailing group (model pill +
		*   context meter + send, 204px at pill 118px) = 304px > 302px content width
		*   (318px card - 16px padding), so the trailing group wraps to a second
		*   line and the add/access controls misalign vertically with the model
		*   picker (issue #54). Capping the pill at 104px shrinks the trailing group
		*   to 182px (282px total), keeping the whole toolbar on one line; the pill's
		*   own content (label + effort + chevron, ~98px) still fits without
		*   truncation. Verified on-device (vivo V2425A, 360dp): tools y 627→670 and
		*   aligns with the model trigger.
		*/
		const COMPOSER_ROW_CSS = `
@media (max-width: 400px) {
  [data-composer-card] [aria-label*='选择模型'],
  [data-composer-card] [aria-label*='model'] {
    max-width: 104px;
  }
  [data-composer-card] [aria-label*='选择模型'] span,
  [data-composer-card] [aria-label*='model'] span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
`;
		//#endregion
		//#region src/client/composer-insets.css.ts
		/**
		* Composer insets adaptation for mobile/edge-to-edge:
		* On Android edge-to-edge mode, the shell supplies --dsh-android-system-bottom
		* (gesture / nav-bar height) and --dsh-android-ime-bottom (soft keyboard height).
		* Padding the whole composer seat ([data-composer-seat]) pushes the input card,
		* mode pills, anchored command menu, and the StatsLine footer above the navigation
		* bar / gesture pill and the soft keyboard.
		* On desktop / non-Android environments where CSS variables are unset,
		* max(0px, 0px, 0px) evaluates cleanly to 0px (zero side-effects).
		*/
		const COMPOSER_INSETS_CSS = `
[data-composer-seat] {
  padding-bottom: max(
    env(safe-area-inset-bottom, 0px),
    var(--dsh-android-system-bottom, 0px),
    var(--dsh-android-ime-bottom, 0px)
  );
}
`;
		//#endregion
		//#region src/client/trajectory-details.css.ts
		/**
		* Trajectory local details panel (upstream ui-trajectory) on narrow screens
		* (issue apk#67): the upstream ≤760px media query positions the panel
		* absolute within the ledger region — sandwiched between the trajectory
		* timeline bar above and the composer seat below (which also covers its
		* bottom), leaving a cramped reading band. Overlay it full-viewport inside
		* the mobile frame: fixed positioning escapes the ledger, so the panel spans
		* the whole screen (header + tabs fixed, body scrolls) and the input bar
		* never covers it. The upstream col-resize handle is pointless on touch.
		*/
		const TRAJECTORY_DETAILS_CSS = `
@media (max-width: 760px) {
  /* aside-scoped: the upstream panel's tablist ALSO carries aria-label="Event
     details", so a bare attribute selector would also turn the tabs into a
     fixed full-screen overlay covering the header and the close button. */
  html[data-dsh-mobile-form] aside[aria-label="Event details"] {
    position: fixed;
    inset: 0;
    z-index: 40;
    box-sizing: border-box;
    width: 100%;
    max-width: 100%;
    border-left: none;
    box-shadow: none;
    padding-top: var(--dsh-mobile-top-inset, 0px);
    padding-bottom: env(safe-area-inset-bottom, 0px);
  }
  html[data-dsh-mobile-form] aside[aria-label="Event details"] [aria-label="Resize event details"] {
    display: none;
  }
  /* The panel's fixed z-index lives inside the ledger's stacking context
     (position:relative; z-index:0; isolation:isolate), so it loses to the
     top bar (z3), the tabs and the timeline bar (z1) — the banner then
     covers the panel and the tabs/timeline stay visible above it. While the
     panel is open, raise the ledger itself so the whole subtree (panel
     included) covers them.
     2026-08-23 (#17 回归修复)：:has() 是 Chromium 105+；MIUI12 旧 WebView
     (Chromium 83) 整条规则被丢弃 → 面板遮挡回归。保留 :has() 路径（新内核
     零开销，无 JS 依赖）并追加 class 路径（旧内核由 TrajectoryPanelsObserver
     在面板开合时切换 dsh-mobile-ledger-raised）。 */
  html[data-dsh-mobile-form] [class*="ledger"]:has(aside[aria-label="Event details"]) {
    z-index: 12;
  }
  html[data-dsh-mobile-form] [class*="ledger"].dsh-mobile-ledger-raised {
    z-index: 12;
  }
}
`;
		//#endregion
		//#region src/client/trajectory-panels-observer.ts
		/**
		* 轨迹详情面板开合的 class 降级路径（2026-08-23，#17 回归修复）：
		* 主 CSS 用 :has()（Chromium 105+）抬升 ledger z-index；旧 WebView（MIUI12
		* 时代 Chromium 83）不支持 :has()，整条规则被丢弃 → 面板被顶部 banner 遮挡。
		* 本观察器用 MutationObserver 检测 aside[aria-label="Event details"] 的存在，
		* 给所属 ledger 切换 dsh-mobile-ledger-raised class（trajectory-details.css.ts
		* 的伴随规则兜底），并在浏览器原生支持 :has() 时自动停摆（零重复开销）。
		*/
		var TrajectoryPanelsObserver = class {
			constructor(ledger) {
				_defineProperty(this, "mutationObserver", new MutationObserver((records) => {
					if (records.some((record) => this.isRelevantMutation(record))) this.sync();
				}));
				_defineProperty(this, "ledger", void 0);
				_defineProperty(this, "attached", false);
				this.ledger = ledger;
			}
			/** 开始监听面板开合（幂等）。 */
			attach() {
				if (this.attached) return;
				if (this.supportsHasSelector()) return;
				this.attached = true;
				this.mutationObserver.observe(document.body, {
					childList: true,
					subtree: true
				});
				this.sync();
			}
			/** 停止监听并清除 class。 */
			detach() {
				this.mutationObserver.disconnect();
				if (this.attached && this.ledger !== null) this.ledger.classList.remove("dsh-mobile-ledger-raised");
				this.attached = false;
			}
			/** 面板存在 → 抬升 ledger（class 路径，CSS .dsh-mobile-ledger-raised）；否则移除。 */
			sync() {
				if (this.ledger === null) return;
				const panel = this.ledger.querySelector("aside[aria-label=\"Event details\"]");
				this.ledger.classList.toggle("dsh-mobile-ledger-raised", panel !== null);
			}
			isRelevantMutation(record) {
				return [
					record.target,
					...record.addedNodes,
					...record.removedNodes
				].some((node) => {
					if (!(node instanceof Element)) return false;
					return node.matches("aside[aria-label=\"Event details\"]") || node.querySelector("aside[aria-label=\"Event details\"]") !== null;
				});
			}
			/** CSS 支持探测：:has() 对旧内核很可能是 SyntaxError 整条丢弃后的误报，
			*  用 CSS.supports 的官方探测（Chromium 105+ 才有 CSS.supports('selector(:has(*))') 真值）。 */
			supportsHasSelector() {
				try {
					return typeof CSS !== "undefined" && CSS.supports !== void 0 && CSS.supports("selector(:has(*))");
				} catch {
					return false;
				}
			}
		};
		//#endregion
		//#region src/client/snapshot-panels-observer.ts
		/**
		* Raise the conversation header while its vendored snapshot manager is open (#288).
		*
		* vendor/dsh-undo-savepoint/lib/client.js renders SnapshotPanel in
		* conversation.session.header.actions, not a body portal. Its exact hooks are
		* div.u_overlay[data-undo-panel] > div.u_panel and the panel's direct u_* rows.
		* ConversationHeader publishes data-window-drag and a direct leading seat;
		* ConversationMainPanel renders that header as a flex item under div[data-phase].
		* Raising this header puts its titleRow container context above transcript paint
		* without moving React nodes, changing code blocks, or raising the frame itself.
		* The deployed stacking chain/paint order has not been measured; this is the
		* source-backed header path, not a claim about every possible overlay ancestor.
		*/
		const OVERLAY_SELECTOR = "div.u_overlay[data-undo-panel]";
		const CANDIDATE_SELECTOR = "div[data-undo-panel]";
		const HEADER_SELECTOR = "header[data-window-drag]";
		const RAISED_CLASS = "dsh-mobile-snapshot-header-raised";
		const LEASES_KEY = Symbol.for("dsh-client-ui-responsive.snapshot-panels.header-leases");
		/** Share class ownership across overlapping instances, including module reloads. */
		function headerLeases(document) {
			const existing = Reflect.get(document, LEASES_KEY);
			if (existing !== void 0) return existing;
			const leases = /* @__PURE__ */ new WeakMap();
			Reflect.set(document, LEASES_KEY, leases);
			return leases;
		}
		/** Match direct vendor-owned children without requiring :has() or CSS-module guesses. */
		function hasChild(parent, selector) {
			return Array.from(parent.children).some((child) => child.matches(selector));
		}
		/** Distinguish SnapshotPanel from MessagePanel, settings, and text/code lookalikes. */
		function snapshotHeader(overlay) {
			var _header$parentElement;
			const panel = Array.from(overlay.children).find((child) => child.matches("div.u_panel"));
			if (panel === void 0 || ![
				"u_head",
				"u_toolbar",
				"u_tbody",
				"u_foot"
			].every((row) => hasChild(panel, "div." + row))) return null;
			if (overlay.closest("[data-shell-overlay], [data-sidebar-right-panel]") !== null) return null;
			const header = overlay.closest(HEADER_SELECTOR);
			if (header === null || !((_header$parentElement = header.parentElement) === null || _header$parentElement === void 0 ? void 0 : _header$parentElement.matches("div[data-phase]")) || !hasChild(header, "div[data-conversation-header-leading]")) return null;
			return header;
		}
		/** Own only the temporary header class; the companion stylesheet owns its paint level. */
		var SnapshotPanelsObserver = class {
			/** @param document - The document containing the conversation headers. */
			constructor(document = globalThis.document) {
				this.document = document;
				_defineProperty(this, "observer", null);
				_defineProperty(this, "headers", /* @__PURE__ */ new Set());
				_defineProperty(this, "leases", void 0);
				this.leases = headerLeases(document);
			}
			/** Observe existing and newly mounted snapshot managers; repeated attachment is harmless. */
			attach() {
				if (this.observer !== null || this.document.defaultView === null) return;
				this.observer = new this.document.defaultView.MutationObserver((records) => {
					if (this.observer !== null && records.some((record) => this.relevant(record))) this.sync();
				});
				this.observer.observe(this.document.documentElement, {
					childList: true,
					subtree: true,
					attributes: true,
					attributeFilter: [
						"class",
						"data-undo-panel",
						"data-phase",
						"data-window-drag",
						"data-conversation-header-leading",
						"data-shell-overlay",
						"data-sidebar-right-panel"
					]
				});
				this.sync();
			}
			/** Stop observation and release only classes this observer leased, including detached headers. */
			detach() {
				var _this$observer;
				(_this$observer = this.observer) === null || _this$observer === void 0 || _this$observer.disconnect();
				this.observer = null;
				for (const header of this.headers) this.release(header);
				this.headers.clear();
			}
			sync() {
				const next = /* @__PURE__ */ new Set();
				for (const overlay of this.document.querySelectorAll(OVERLAY_SELECTOR)) {
					const header = snapshotHeader(overlay);
					if (header !== null) next.add(header);
				}
				for (const header of this.headers) if (!next.has(header)) this.release(header);
				for (const header of next) {
					if (!this.headers.has(header)) {
						const lease = this.leases.get(header);
						if (lease !== void 0) lease.users += 1;
						else this.leases.set(header, {
							users: 1,
							added: !header.classList.contains(RAISED_CLASS)
						});
					}
					if (!header.classList.contains(RAISED_CLASS)) {
						const lease = this.leases.get(header);
						if (lease !== void 0) lease.added = true;
						header.classList.add(RAISED_CLASS);
					}
				}
				this.headers.clear();
				for (const header of next) this.headers.add(header);
			}
			release(header) {
				const lease = this.leases.get(header);
				if (lease === void 0 || --lease.users > 0) return;
				if (lease.added) header.classList.remove(RAISED_CLASS);
				this.leases.delete(header);
			}
			relevant(record) {
				const target = record.target;
				if (target.nodeType === 1) {
					const element = target;
					for (const header of this.headers) if (header.contains(element) || element.contains(header)) return true;
					if (element.closest(CANDIDATE_SELECTOR) !== null) return true;
					const header = element.closest(HEADER_SELECTOR);
					if (header !== null && header.querySelector(CANDIDATE_SELECTOR) !== null) return true;
					if (record.type === "attributes" && element.querySelector(CANDIDATE_SELECTOR) !== null) return true;
				}
				return [...record.addedNodes, ...record.removedNodes].some((node) => {
					if (node.nodeType !== 1) return false;
					const element = node;
					return element.matches(CANDIDATE_SELECTOR) || element.querySelector(CANDIDATE_SELECTOR) !== null;
				});
			}
		};
		//#endregion
		//#region src/client/snapshot-panels.css.ts
		/**
		* Snapshot manager ancestor promotion (#288), installed with SnapshotPanelsObserver.
		* The header is a flex item in ConversationRoot, so z-index needs no position or
		* transform override. Level 16 clears transcript CodeBlock banners (6), composer
		* chrome (7/9), and local trajectory details (12); it stays below the separate
		* frame overlay seat (20), mobile drawer (30), and fullscreen right panel (40).
		* No width gate: the Android shell also displays the manager in landscape.
		*/
		const SNAPSHOT_PANELS_CSS = `
div[data-phase] > header[data-window-drag].dsh-mobile-snapshot-header-raised {
  z-index: 16;
}
`;
		//#endregion
		//#region src/client/composer-popup-guard.ts
		/**
		* Composer popup geometry guard (issues apk#135).
		*
		* Two popups anchor to the composer card: the slash-command menu
		* (`[role='listbox']` inside a surface card) and the model menu
		* (`[role='menu']`, which is its own surface). Both are sized and positioned
		* against their trigger rather than the viewport, so on phones they can
		*   (a) grow past the left viewport edge — long model ids lose their prefix
		*       ("deepseek-v4-…" renders as "eek-v4-…"),
		*   (b) keep a surface card wider than its content once a width cap applies to
		*       the inner scroll container only, leaving a blank strip and a scrollbar
		*       floating away from the card edge, and
		*   (c) rise above the mobile top bar and hide their first rows.
		* The guard measures each open popup and writes the corrections as a width cap,
		* a horizontal shift and a height cap. Measuring rather than matching upstream
		* class names keeps the fix alive across upstream CSS-module renames.
		*/
		/** Design cap on the slash-command menu height (figma SLASH 39:26572 MenuDropdown). */
		const LISTBOX_HEIGHT_CAP = 320;
		/** Design cap on the model menu height (upstream ModelSelect .menu). */
		const MENU_HEIGHT_CAP = 360;
		/** Space kept between a popup and the mobile top bar. */
		const TOPBAR_CLEARANCE = 12;
		/** Design cap on a popup's width. */
		const POPUP_WIDTH_CAP = 340;
		/** Fraction of the viewport width a popup may occupy (mirrors the shell's injected cap). */
		const POPUP_VIEWPORT_FRACTION = .92;
		/**
		* Width cap for a composer popup: the design cap, never more than the fraction
		* of the viewport the shell's injected stylesheet allows.
		* @param viewportWidth - layout viewport width in CSS pixels.
		* @returns the cap in whole CSS pixels.
		*/
		function popupMaxWidth(viewportWidth) {
			return Math.max(0, Math.floor(Math.min(POPUP_WIDTH_CAP, viewportWidth * POPUP_VIEWPORT_FRACTION)));
		}
		/**
		* Horizontal shift that brings a popup back inside the viewport.
		* The right edge wins when the popup is wider than the viewport, so the
		* reading order (labels at the left) stays visible.
		* @param left - untransformed left edge.
		* @param right - untransformed right edge.
		* @param viewportWidth - layout viewport width in CSS pixels.
		* @param gap - minimum clearance to each edge.
		* @returns the shift in whole CSS pixels (0 when already inside).
		*/
		function popupShiftLeft(left, right, viewportWidth, gap = 8) {
			if (right - left > viewportWidth - gap * 2) return Math.round(gap - left);
			if (right > viewportWidth - gap) return Math.round(viewportWidth - gap - right);
			if (left < gap) return Math.round(gap - left);
			return 0;
		}
		/**
		* Usable height for an upward-opening popup.
		* The popup bottom is anchored to the composer, while the top chrome
		* (upstream's header) occupies part of the viewport above it.
		* @param popupBottom - popup bottom edge.
		* @param topbarBottom - bottom edge of the top chrome above the popup.
		* @param chromeHeight - popup padding/border excluded from a content-box cap.
		* @param cap - design height cap for this popup kind.
		* @returns the height cap in whole CSS pixels.
		*/
		function composerPopupMaxHeight(popupBottom, topbarBottom, chromeHeight = 0, cap = LISTBOX_HEIGHT_CAP) {
			return Math.max(0, Math.min(cap, Math.floor(popupBottom - topbarBottom - TOPBAR_CLEARANCE - chromeHeight)));
		}
		/**
		* Bottom edge of the top chrome above the composer, or null when there is none.
		*
		* 0.14.2 P4 removed the self-drawn 44px band ([data-dsh-mobile-topbar]); the real top
		* chrome is now upstream's own conversation header. The header element carries no
		* unique attribute of its own (data-window-drag is on several rows), so it is resolved
		* through the leading seat this plugin registers the drawer toggle into — that seat is
		* inside exactly one header.
		* @returns the header's bottom edge, or null when the header is absent.
		*/
		function headerTopChrome() {
			const leading = document.querySelector("[data-conversation-header-leading]");
			return (leading === null || leading === void 0 ? void 0 : leading.closest("header")) ?? null;
		}
		/** The painted surface of a popup: the role element itself, or its card parent. */
		function surfaceOf(popup) {
			return popup.getAttribute("role") === "menu" ? popup : popup.parentElement ?? popup;
		}
		/** Height excluded from a content-box max-height (padding + border). */
		function chromeHeight(element) {
			const style = getComputedStyle(element);
			if (style.boxSizing === "border-box") return 0;
			return [
				"paddingTop",
				"paddingBottom",
				"borderTopWidth",
				"borderBottomWidth"
			].map((property) => Number.parseFloat(style[property]) || 0).reduce((total, value) => total + value, 0);
		}
		/** Current inline horizontal shift of a surface. */
		function readShift(surface) {
			return Number.parseFloat(surface.style.getPropertyValue("--dsh-mobile-popup-shift")) || 0;
		}
		/**
		* Keeps every open composer popup inside the viewport: width cap on the surface
		* and its scroll container, horizontal shift on the surface, height cap on the
		* scrolling element.
		*/
		var ComposerPopupGuard = class {
			constructor() {
				_defineProperty(this, "onViewportChange", () => {
					this.queue();
				});
				_defineProperty(this, "mutationObserver", new MutationObserver((records) => {
					if (records.some((record) => this.isRelevantMutation(record))) this.queue();
				}));
				_defineProperty(this, "resizeObserver", new ResizeObserver(() => {
					this.queue();
				}));
				_defineProperty(this, "frame", null);
				_defineProperty(this, "observed", []);
				_defineProperty(this, "styled", /* @__PURE__ */ new Set());
			}
			/** Start observing composer popup geometry. */
			attach() {
				var _window$visualViewpor;
				this.mutationObserver.observe(document.body, {
					childList: true,
					subtree: true
				});
				window.addEventListener("resize", this.onViewportChange);
				window.addEventListener("scroll", this.onViewportChange, true);
				(_window$visualViewpor = window.visualViewport) === null || _window$visualViewpor === void 0 || _window$visualViewpor.addEventListener("resize", this.onViewportChange);
				this.queue();
			}
			/** Stop observing and remove every geometric correction. */
			detach() {
				var _window$visualViewpor2;
				this.mutationObserver.disconnect();
				this.resizeObserver.disconnect();
				window.removeEventListener("resize", this.onViewportChange);
				window.removeEventListener("scroll", this.onViewportChange, true);
				(_window$visualViewpor2 = window.visualViewport) === null || _window$visualViewpor2 === void 0 || _window$visualViewpor2.removeEventListener("resize", this.onViewportChange);
				if (this.frame !== null) cancelAnimationFrame(this.frame);
				this.frame = null;
				this.clear();
				this.observed = [];
			}
			queue() {
				if (this.frame !== null) return;
				this.frame = requestAnimationFrame(() => {
					this.frame = null;
					this.apply();
				});
			}
			apply() {
				const card = document.querySelector("[data-composer-card]");
				const topbar = headerTopChrome() ?? document.querySelector("[data-dsh-mobile-topbar]");
				if (card === null) {
					this.clear();
					return;
				}
				const popups = Array.from(card.querySelectorAll("[role='menu'], [role='listbox']"));
				if (popups.length === 0) {
					this.clear();
					return;
				}
				const viewportWidth = document.documentElement.clientWidth;
				const widthCap = `${popupMaxWidth(viewportWidth)}px`;
				const topbarBottom = (topbar === null || topbar === void 0 ? void 0 : topbar.getBoundingClientRect().bottom) ?? 0;
				const styled = /* @__PURE__ */ new Set();
				const measured = topbar === null ? [] : [topbar, card];
				for (const popup of popups) {
					const surface = surfaceOf(popup);
					styled.add(surface);
					styled.add(popup);
					measured.push(surface, popup);
					for (const element of surface === popup ? [popup] : [popup, surface]) if (element.style.getPropertyValue("--dsh-mobile-popup-max-width") !== widthCap) element.style.setProperty("--dsh-mobile-popup-max-width", widthCap);
					const currentShift = readShift(surface);
					const rect = surface.getBoundingClientRect();
					const shift = popupShiftLeft(rect.left - currentShift, rect.right - currentShift, viewportWidth);
					if (shift !== currentShift) surface.style.setProperty("--dsh-mobile-popup-shift", `${shift}px`);
					surface.setAttribute("data-dsh-popup", "");
					const heightCap = `${composerPopupMaxHeight(rect.bottom, topbarBottom, chromeHeight(popup), popup.getAttribute("role") === "menu" ? MENU_HEIGHT_CAP : LISTBOX_HEIGHT_CAP)}px`;
					if (popup.style.getPropertyValue("--dsh-mobile-menu-max-height") !== heightCap) popup.style.setProperty("--dsh-mobile-menu-max-height", heightCap);
				}
				for (const element of this.styled) if (!styled.has(element)) this.clearElement(element);
				this.styled = styled;
				this.syncObserved(measured);
			}
			/** Drop every correction and forget the touched elements. */
			clear() {
				for (const element of this.styled) this.clearElement(element);
				this.styled = /* @__PURE__ */ new Set();
				this.syncObserved([]);
			}
			clearElement(element) {
				element.style.removeProperty("--dsh-mobile-popup-max-width");
				element.style.removeProperty("--dsh-mobile-popup-shift");
				element.style.removeProperty("--dsh-mobile-menu-max-height");
				element.removeAttribute("data-dsh-popup");
			}
			syncObserved(next) {
				if (next.length === this.observed.length && next.every((element, index) => element === this.observed[index])) return;
				this.resizeObserver.disconnect();
				for (const element of next) this.resizeObserver.observe(element);
				this.observed = next;
			}
			isRelevantMutation(record) {
				if (record.target instanceof Element && record.target.closest("[data-composer-card]") !== null) return true;
				return [...record.addedNodes, ...record.removedNodes].some((node) => {
					if (!(node instanceof Element)) return false;
					return node.matches("[data-composer-card], [role=\"listbox\"], [role=\"menu\"]") || node.querySelector("[data-composer-card], [role=\"listbox\"], [role=\"menu\"]") !== null;
				});
			}
		};
		//#endregion
		//#region src/client/session-log-dialog.css.ts
		/**
		* Hide the upstream session-log-export modal on Android shell builds.
		*
		* The shell APK already owns the export result surface: MainActivity pushes
		* the final success/failure through `window.__dshExportResult`, and
		* `ExportResultDialog` renders it in `shell.overlay`. The upstream
		* `session-log-export` modal also opens (preparing → success/error), so two
		* dialogs stack. The upstream CSS Module class names are hashed, so this
		* stylesheet targets the modal's stable ARIA attributes instead.
		*
		* ST-14: the `:has()` rules are the primary path (Chromium 105+); an old kernel
		* drops the whole rule as a syntax error — the exact shape of the #17 regression
		* the trajectory ledger already hit. So the companion class rules below are the
		* fallback path, applied by `SessionLogDialogObserver` only when
		* `CSS.supports('selector(:has(*))')` is false.
		*/
		/** Class the fallback path toggles on the modal's `[role=presentation]` (or the dialog itself). */
		const SESSION_LOG_DIALOG_HIDE_CLASS = "dsh-mobile-hide-session-log-dialog";
		/** ARIA labels the upstream export modal opens with (localized; keep in one place). */
		const SESSION_LOG_DIALOG_LABEL_PREFIXES = [
			"正在导出 Session",
			"Session 导出",
			"Exporting Session",
			"Session download",
			"Session export"
		];
		const SESSION_LOG_DIALOG_HIDE_CSS = `
[role="presentation"]:has([role="dialog"][aria-label^="正在导出 Session"]),
[role="presentation"]:has([role="dialog"][aria-label^="Session 导出"]),
[role="presentation"]:has([role="dialog"][aria-label^="Exporting Session"]),
[role="presentation"]:has([role="dialog"][aria-label^="Session download"]),
[role="presentation"]:has([role="dialog"][aria-label^="Session export"]) {
  display: none !important;
}

/* ST-14 兜底路径（旧内核无 :has()）：由 SessionLogDialogObserver 打 class。 */
[role="presentation"].${SESSION_LOG_DIALOG_HIDE_CLASS},
[role="dialog"].${SESSION_LOG_DIALOG_HIDE_CLASS} {
  display: none !important;
}
`;
		//#endregion
		//#region src/client/session-log-dialog-observer.ts
		/**
		* 导出弹窗隐藏的 **class 降级路径**（ST-14，F-UI-06）。
		*
		* 主路径是 `session-log-dialog.css.ts` 的 `:has()` 规则（Chromium 105+）。旧内核把整条
		* 规则当语法错误丢弃 —— 与 #17 的轨迹面板遮挡同一形态（TrajectoryPanelsObserver 已踩过），
		* 而"规则文本还在页面里"会让 grep 类检查全绿（假绿）。本观察器照
		* `TrajectoryPanelsObserver` 的形状实现：
		*  - 能力探测：`CSS.supports('selector(:has(*))')` 为真 → 不启用（零重复开销）；
		*  - 为假 → MutationObserver 盯住上游导出弹窗，给其 `[role=presentation]`（无则由弹窗自身承担）
		*    打上 `dsh-mobile-hide-session-log-dialog` class，由伴随规则隐藏。
		*/
		/** 上游导出弹窗的判定：`[role=dialog]` 且 aria-label 命中导出文案前缀。 */
		function isSessionLogDialog(element) {
			if (!element.matches("[role=\"dialog\"]")) return false;
			const label = element.getAttribute("aria-label") ?? "";
			return SESSION_LOG_DIALOG_LABEL_PREFIXES.some((prefix) => label.startsWith(prefix));
		}
		var SessionLogDialogObserver = class {
			constructor() {
				_defineProperty(this, "mutationObserver", new MutationObserver((records) => {
					if (records.some((record) => this.isRelevantMutation(record))) this.sync();
				}));
				_defineProperty(this, "attached", false);
				_defineProperty(this, "tagged", /* @__PURE__ */ new Set());
			}
			/** 开始监听导出弹窗（幂等）；原生支持 :has() 时 CSS 路径已足够，class 降级不启用。 */
			attach() {
				if (this.attached) return;
				if (supportsHasSelector()) return;
				this.attached = true;
				this.mutationObserver.observe(document.body, {
					childList: true,
					subtree: true,
					attributes: true,
					attributeFilter: ["aria-label"]
				});
				this.sync();
			}
			/** 停止监听并清除所有 class。 */
			detach() {
				this.mutationObserver.disconnect();
				for (const element of this.tagged) element.classList.remove(SESSION_LOG_DIALOG_HIDE_CLASS);
				this.tagged.clear();
				this.attached = false;
			}
			/** 同步一次：命中导出弹窗 → 打 class；弹窗消失 → 收 class（绝不残留）。 */
			sync() {
				const next = /* @__PURE__ */ new Set();
				for (const dialog of document.querySelectorAll("[role=\"dialog\"]")) {
					if (!isSessionLogDialog(dialog)) continue;
					const target = dialog.closest("[role=\"presentation\"]") ?? dialog;
					target.classList.add(SESSION_LOG_DIALOG_HIDE_CLASS);
					next.add(target);
				}
				for (const element of [...this.tagged]) {
					if (next.has(element)) continue;
					element.classList.remove(SESSION_LOG_DIALOG_HIDE_CLASS);
					this.tagged.delete(element);
				}
				for (const element of next) this.tagged.add(element);
			}
			isRelevantMutation(record) {
				if (record.type === "attributes") return record.target instanceof Element && isSessionLogDialog(record.target);
				return [
					record.target,
					...record.addedNodes,
					...record.removedNodes
				].some((node) => {
					if (!(node instanceof Element)) return false;
					return isSessionLogDialog(node) || node.querySelector("[role=\"dialog\"]") !== null;
				});
			}
		};
		/** CSS 支持探测：旧内核缺 `selector(:has(*))` 支持（真值需 Chromium 105+）。 */
		function supportsHasSelector() {
			try {
				return typeof CSS !== "undefined" && CSS.supports !== void 0 && CSS.supports("selector(:has(*))");
			} catch {
				return false;
			}
		}
		//#endregion
		//#region src/client/mobile/notify-landing.ts
		/**
		* 打开目标会话。
		* @param face - 会话服务面；不在场（undefined）即返回 false，不抛。
		* @param sessionId - 通知携带的会话 id。
		*/
		function openSessionForNotify(face, sessionId) {
			if (face === void 0 || face === null) return false;
			if (typeof sessionId !== "string" || sessionId === "") return false;
			try {
				if (face.scope(sessionId) === void 0) {
					face.open(sessionId);
					return face.scope(sessionId) !== void 0;
				}
				face.open(sessionId);
				return true;
			} catch {
				return false;
			}
		}
		//#endregion
		//#region src/client/mobile/use-shell-state.ts
		/**
		* 壳侧状态订阅钩子（ST-09 / ST-27：禁止裸写一次性桥读）。
		*
		* 「从系统设置返回」是本项目最高频、最容易踩的路径（F-UI-12）：在系统侧改了状态
		* （权限/开关/偏好）后回到页面，React 不会重挂载，一次性 `useState(() => bridge.getX())`
		* 读到的旧值会一直显示 —— 展示值与真源分裂。
		*
		* 本钩子统一提供：**挂载读一次 + `visibilitychange`/`focus` 重读 + 可选轮询**，
		* 并返回一个 `refresh()` 供"写后回读"（§4.5 七模式之五）使用。
		*
		* 纪律：组件里禁止「useState 初值器直读 window.androidBridge」这类一次性裸读（ST-27 的 grep 判据）；
		* 设备侧/壳侧状态一律经本钩子进入 React state（回归见 tests/shell-state-discipline.spec.ts）。
		*/
		/**
		* @param getter - 真源读函数（壳桥或由其派生的值）；必须同步、无副作用。
		* @param options - 可选轮询间隔。
		* @returns `[当前值, refresh]`：refresh 立即重读真源（供写后回读），失败保留上一次值。
		*/
		function useShellState(getter, options = {}) {
			const getterRef = (0, react.useRef)(getter);
			getterRef.current = getter;
			const [value, setValue] = (0, react.useState)(() => {
				try {
					return getter();
				} catch {
					return;
				}
			});
			const refresh = (0, react.useCallback)(() => {
				let next;
				try {
					next = getterRef.current();
				} catch {
					return;
				}
				setValue((prev) => Object.is(prev, next) ? prev : next);
			}, []);
			const pollMs = options.pollMs;
			(0, react.useEffect)(() => {
				const onVisible = () => {
					if (document.visibilityState === "visible") refresh();
				};
				document.addEventListener("visibilitychange", onVisible);
				window.addEventListener("focus", onVisible);
				const timer = pollMs !== void 0 && pollMs > 0 ? window.setInterval(onVisible, pollMs) : void 0;
				return () => {
					document.removeEventListener("visibilitychange", onVisible);
					window.removeEventListener("focus", onVisible);
					if (timer !== void 0) window.clearInterval(timer);
				};
			}, [refresh, pollMs]);
			return [value, refresh];
		}
		//#endregion
		//#region src/client/user-copy.ts
		/**
		* 用户面文案唯一真源（**页面侧**，0.14.1 批 3 / P3-1）。
		*
		* 规则（`docs/0.14.1-COPY-STANDARD.md`）：
		*  1. **机器码不上屏**。壳侧 `reason` / HTTP 状态码 / 内部 key 只允许进 `data-*` 属性与日志；
		*     用户看到的必须是这里给出的「发生了什么 + 你现在能做什么」。
		*  2. **映射表落一处**。页面侧所有「码 → 中文」都在本文件；任何组件里再写一张局部表
		*     （副本）都算回归——两张表必然漂移，这正是本批要收的形态。
		*  3. 表里查不到的码**也要给人话**：回一句可反馈的兜底，而不是把码原样抛给用户。
		*     兜底句里不带原码，原码由调用方放进 `data-*`（可截图/可 grep，但不打断阅读）。
		*
		* 为什么页面侧与壳侧各有一张表：两侧是**两种语言的两个渲染面**（WebView 页面 / 原生 UI），
		* 不存在共享的常量载体。故约定「每个渲染面一张表、各一张」，并在规范文档里登记这两张表的
		* 位置与覆盖的码集合；壳侧那张是 `UserCopy.kt`。
		*/
		/**
		* 调用失败原因 → 人话。
		*
		* 码集合来自壳侧（`AndroidBridge` / `ExternalLinks` / `PathOpen` / `ControlCarrier` /
		* `BrowserHost`）与页面侧自查（`open-path.ts`）。两侧都可能新增码，**新增时必须在这里补一行**：
		* `user-copy.spec.ts` 有一条断言把页面侧自查码集合钉住（缺行即判红）。
		*/
		const CALL_REASON = {
			unavailable: "这个功能需要安卓应用内打开（浏览器里不可用）",
			refused: "系统选择器拒绝了这次打开请求（可能是该目录不允许外部应用访问）",
			"empty-answer": "系统选择器没有返回结果（可能是系统组件异常）——请重试，或直接在文件管理器里打开",
			"bridge-error": "应用内调用出错——请重试；多次失败可复制日志反馈",
			"bridge not wired": "应用与页面的连接未接好（安装包不完整）——请重新安装应用",
			"no-shell-context": "应用上下文尚未就绪（刚启动或正在重启）——请稍后重试",
			"browser-host-not-wired": "内置浏览器组件未接好（安装包不完整）——请重新安装应用",
			"carrier-not-started": "设备控制服务尚未启动——请到「手机控制」开启无障碍服务后重试",
			"a11y-unavailable": "无障碍服务未开启——该操作需要「无障碍」通道；浏览器与虚拟屏操作不受影响",
			"unknown-op": "本版不认识这个浏览器操作——请更新应用后再试",
			"unknown-key": "这个链接没有在本版登记——请更新应用后再试",
			"insecure-url": "链接不是 https，出于安全已拒绝打开",
			"no-handler": "设备上没有能打开它的应用——请先安装浏览器或文件管理器",
			"not-installed": "还没安装 Shizuku——请先点「下载 Shizuku」",
			"consent-required": "先勾选「已阅读」并查看《AI root 权限免责声明》，才能开启 AI root 权限（升级后需重新确认）",
			"not-root-channel": "Shizuku 通道不是 root 身份（无法在未 root 的设备上赋予该权限）——请让 Shizuku 以 root 启动后再来开启",
			"missing-asset": "应用内文档缺失（安装包不完整）——请重新安装应用后再试",
			"ui-thread-timeout": "应用界面正忙——请稍后重试；多次失败可复制日志反馈",
			"root-not-granted": "尚未获得 root 授权——请在你自己使用的 Root 管理器里允许本应用使用 root（多数管理器不会自动弹授权框）",
			"no-su": "本机没有可用的 su（未 root 或未安装 Root 管理器）——请先在你自己使用的 Root 管理器里完成 root 后再试",
			"already-requesting": "正在等待 root 授权结果——若管理器没有弹出授权框，请自己打开它允许本应用",
			"request-started": "已开始检测 root 授权——若管理器没有弹出授权框，请自己打开它允许本应用；授权后本页会自动续开开关",
			requesting: "正在等待 root 授权结果——若管理器没有弹出授权框，请自己打开它允许本应用",
			"su-timeout": "root 命令超时后被终止——请重试或缩短命令",
			"su-exec-failed": "root 命令执行失败——请稍后重试；多次失败可复制日志反馈",
			"empty-command": "命令为空",
			"no-data-dir": "读不到应用数据目录（应用上下文异常）——请重启应用后再试",
			"bad-path": "路径无法解析",
			"out-of-app-data": "只允许修复应用数据目录内的文件",
			"repair-unsupported": "当前通道不支持属主修复（旧服务）——请到「手机控制」点「重置链接」重建通道后再试",
			"repair-item-failed": "有属主条目修复失败——请检查 root 授权后重试",
			"repair-incomplete": "属主修复未完成（仍有条目属主不对）——请重试",
			"repair-truncated": "目标目录达到遍历/深度上限，只处理了一部分——请查看诊断后再试",
			"repair-deadline": "属主维护达到时间预算，剩余结果未知——请查看诊断后再试",
			"repair-result-unknown": "特权工作结果不明，已暂停派发和维护；不要重复提交，仅重启应用不能证明结算。必要时重启设备",
			"root-maintenance-epoch-unavailable": "无法确认设备启动标识，本次不派发特权工作",
			"root-maintenance-lease-unavailable": "不能持久化特权执行租约，本次不派发",
			"repair-started": "已提交属主维护，尚未完成，结果会自动刷新",
			"repair-running": "属主维护已经在进行中，请等待原任务结算",
			"root-maintenance-busy": "属主维护占用特权通道，本次没有派发命令；请等待维护结算",
			"repair-configuration-required": "维护服务没有确认应用身份，请重置 Shizuku 连接",
			"shizuku-configuration-required": "UserService 配置未确认，本次未派发；请重置连接",
			"shizuku-identity-failed": "无法确认执行服务的实际身份，本次拒绝派发",
			"repair-transport-incomplete": "维护进程或输出不完整，无法确认全部结果",
			"repair-helper-output-invalid": "维护 helper 没有返回完整有效结果，请复制诊断日志",
			"repair-native-failed": "原生维护异常，未能确认结果，请复制诊断日志",
			"invalid-app-data-anchor": "不能确认本应用可信数据目录，已拒绝维护",
			"invalid-repair-arguments": "属主维护参数非法，未开始变更",
			"installed-apk-unavailable": "无法读取已安装的签名应用代码，已拒绝维护",
			"requires-uid-0": "当前维护进程不是 root，未修改属主",
			"hardlink-protection-unavailable": "内核硬链接保护不可确认或已关闭，已拒绝维护",
			"invalid-app-uid": "完整应用 UID 不合法，已拒绝维护",
			"invalid-entry-cap": "属主维护条目上限非法，未开始变更",
			"invalid-deadline": "属主维护时间预算非法，未开始变更",
			"invalid-depth": "属主维护深度限制非法，未开始变更",
			"invalid-data-dir": "属主维护数据目录非法，未开始变更",
			"invalid-subpath": "属主维护相对路径非法，未开始变更",
			"subpath-depth-exceeded": "目标路径超过维护深度上限，已拒绝",
			"foreign-owner": "遇到非本应用且非 root 的属主，拒绝修改该条目",
			"shared-root-file": "root 文件允许其它主体写入，无法确认安全归属，已拒绝修改",
			"unsafe-regular-link-count": "普通文件有不安全的硬链接数量，已拒绝修改",
			"cross-device-entry": "遇到其它挂载设备上的条目，已拒绝修改",
			"symlink-target": "目标是符号链接，已拒绝修改",
			"symlink-ancestor": "目标祖先是符号链接，已拒绝维护",
			"unsupported-node": "遇到特殊文件节点，未打开或修改",
			"directory-cycle": "遇到目录循环，已停止该分支",
			"inode-changed": "维护期间文件身份发生变化，已拒绝继续变更",
			"pinned-inode-changed": "已固定文件身份不一致，已拒绝继续变更",
			"chown-failed": "属主修改失败，未记为已修复",
			"ownership-verification-failed": "属主修改后验证失败，结果不能算完成",
			"repair-unexpected-failure": "属主维护发生未预期异常，请复制诊断日志",
			"anchor-open-failed": "无法打开可信应用数据锚点，未开始维护",
			"anchor-stat-failed": "无法确认应用数据锚点属性，未开始维护",
			"anchor-not-directory": "应用数据锚点不是目录，已拒绝维护",
			"ancestor-not-directory": "目标祖先不是目录，已拒绝维护",
			"listing-open-failed": "无法打开目录枚举，维护未完成",
			"listing-read-failed": "目录枚举失败，剩余条目未知",
			"invalid-child-name": "目录条目名非法，已拒绝访问",
			"pre-mutation-stat-failed": "变更前不能确认文件属性，未修改该条目",
			"verification-stat-failed": "变更后不能读取文件属性，结果未验证",
			"stat-failed": "无法读取文件属性，维护未完成",
			"open-failed": "无法打开维护条目，未修改该条目",
			"close-failed": "关闭维护描述符失败，请查看诊断日志",
			"load-error": "内置浏览器加载失败——请检查网址，或换用系统浏览器打开"
		};
		/** 未知码的兜底（**不带原码**；原码由调用方放进 `data-*`）。 */
		const UNKNOWN_CALL_REASON = "调用失败（原因未在本版登记）——请重试；多次失败可复制日志反馈";
		/**
		* 壳侧/页面侧的失败原因 → 人话。
		*
		* `load-error:<code>` 这类**带前缀的复合码**按前缀归类（后缀是 WebView 的内部错误码，
		* 对用户无意义）；`reason` 里混进异常消息时也走兜底——绝不把异常措辞当文案。
		* @param reason - 壳侧回的原因码，或页面自查码。
		* @returns 用户可读的一句话（含下一步）。
		*/
		function describeCallReason(reason) {
			const code = (reason ?? "").trim();
			if (code === "") return UNKNOWN_CALL_REASON;
			const table = CALL_REASON[code];
			if (table !== void 0) return table;
			const prefix = code.split(":", 1)[0];
			const byPrefix = CALL_REASON[prefix];
			if (byPrefix !== void 0) return byPrefix;
			return UNKNOWN_CALL_REASON;
		}
		/**
		* HTTP 状态 → 人话（**状态码不上屏**）。
		*
		* 缺陷现场（审查档 §4.1）：界面上出现过「未获授权（HTTP 401）」「扫描失败（HTTP 500）」——
		* 用户拿不到任何可执行信息，只知道有个编号。这里按语义分档，`status` 仅留给调用方放进
		* `data-http` 与诊断日志。
		* @param action - 动作名（「读取来件状态」「清理运行时缓存」…），拼进句子。
		* @param status - HTTP 状态码（仅用于分档，不拼进返回串）。
		* @returns 用户可读的一句话（含下一步）。
		*/
		function describeHttpFailure(action, status) {
			if (status === 401 || status === 403) return action + "未获授权——请确认是在本机应用内操作；仍失败请重新打开应用";
			if (status === 404) return action + "的接口不存在（本版不匹配或应用安装包不完整）——请更新或重新安装应用";
			if (status === 405) return action + "不被允许——请更新应用到较新版本后再试";
			if (status >= 500) return action + "时应用内部出错——请稍后重试；仍失败可复制日志反馈";
			if (status >= 400) return action + "被应用拒绝——请稍后重试；仍失败可复制日志反馈";
			return action + "失败——请稍后重试";
		}
		/**
		* 通知设置写失败原因 → 人话（P3-1 + P3-6）。
		*
		* 壳侧 `NotifyCenter.applySetting` 回 `unknown-key` / `readback-mismatch` 两个码：
		* 前者是本版不认识该开关（不该发生），后者是写完读回与预期不一致（系统拦了写入）。
		* 旧实现把码与内部 key 一起上屏（「未生效（readback-mismatch）：cat.question」），
		* 这里改成「哪一类开关没生效 + 下一步」，key 不收进句子。
		*/
		function describeNotifyWriteFailure(reason) {
			const code = (reason ?? "").trim();
			if (code === "readback-mismatch") return "系统没有接受这次改动（写入后读回不一致）——请重试；仍失败请到系统设置里直接修改通知权限";
			if (code === "unknown-key") return "本版不认识这个开关（应用安装包与页面版本不匹配）——请更新或重新安装应用";
			return "开关未生效——请重试；仍失败可复制日志反馈";
		}
		/**
		* 通知渠道重要性（壳侧 `importance` 数字）→ 人话。
		*
		* 旧实现直接把数字印成「（重要性 4）」——数字档位对用户没有意义，用户要看的是
		* 「会不会响、会不会弹」。档位语义与 Android `NotificationManager.IMPORTANCE_*` 一一对应。
		* @param importance - 壳侧回的重要性整数。
		* @returns 「高（会弹出并响铃）」这类人话；非数字返回空串（调用方整段省略）。
		*/
		function describeImportance(importance) {
			if (typeof importance !== "number" || !Number.isFinite(importance)) return "";
			if (importance >= 4) return "高（会弹到屏幕上并响铃）";
			if (importance === 3) return "默认（会响铃，不弹到屏幕上）";
			if (importance === 2) return "低（只在通知栏提示，不响铃）";
			if (importance === 1) return "极低（不响铃、不提示，仅在通知栏可见）";
			return "已关闭（系统不再显示该渠道的通知）";
		}
		/**
		* [Notice] → 可直接展开进 JSX 的 `data-*` 属性（空值不产生属性）。
		* @param notice - 回执。
		* @returns `{'data-code'?: string, 'data-http'?: string}`。
		*/
		function noticeDataAttrs(notice) {
			const attrs = {};
			if (notice.code !== void 0 && notice.code !== "") attrs["data-code"] = notice.code;
			if (notice.http !== void 0) attrs["data-http"] = String(notice.http);
			return attrs;
		}
		//#endregion
		//#region src/client/dev-section/runtime-cache.tsx
		/**
		* 开发者选项「清除运行时缓存」面板（0.14.1 块 E 的客户端半）。
		*
		* 交互口径（`docs/0.14.1-preview-LEGACY-AND-PERF.md` §4.3③「先给可回收体积再执行」）：
		*  1. 挂载即扫描（GET `/api/android/runtime-cache/scan`，只读）→ 展示**实测**可回收体积与逐项清单；
		*  2. 用户点「清理」→ 二次确认（列出即将删除的项与体积）→ POST `.../execute`；
		*  3. 结果按项展示（removed/failed/skipped 与原因）——失败与跳过都如实呈现，不粉饰成「已清干净」。
		*
		* 纪律：两次请求都带 `credentials: 'same-origin'`（浏览器面凭据 = same-origin 会话 cookie；
		* 与 `DevSection` 的 file-incoming 面同款），且**绝不展示绝对路径**——宿主只下发
		* `$DSH_HOME`/`$DSH_FILES_DIR` 形态的标签。
		*
		* 用户裁定 7（仅用户平面，不给模型工具）：本面板只有页面按钮 + 受鉴权宿主能力，零新增模型可见工具。
		*/
		/** 字节格式化（与设置页其它面同款口径）。 */
		function fmtBytes(n) {
			if (n >= 1048576) return (n / 1024 / 1024).toFixed(1) + " MB";
			if (n >= 1024) return (n / 1024).toFixed(1) + " KB";
			return n + " B";
		}
		/**
		* 跳过原因的中文短标签（未知原因**不原样透出**，见 P3-6：机器码不上屏）。
		*
		* 未登记的原因落到 [describeSkipReason] 的兜底句，原始串只进 `data-reason`。
		*/
		const SKIP_LABEL = {
			"not-allowlisted": "未列入白名单（本版不清理）",
			absent: "不存在",
			unreadable: "不可读",
			"log-root-unresolved": "日志目录未注入（应用未提供日志目录）",
			"current-generation-absent": "当前引擎日志不在场（引擎未启动）",
			preserved: "属保留项",
			"scope-rejected": "越出白名单作用域（已拒绝）",
			vanished: "执行前已消失",
			aborted: "被执行中断跳过",
			"remove-failed": "删除失败（文件被占用、只读或权限不足）"
		};
		/**
		* 跳过/失败原因 → 人话（P3-1）。
		* @param item - 扫描或执行结果里的一项。
		* @returns 已知原因的中文短标签；未知原因给兜底句（**不回显原码**）。
		*/
		function describe(item) {
			const reason = item.reason ?? "";
			if (reason === "") return "原因未记录";
			return SKIP_LABEL[reason] ?? "原因未在本版登记（可复制日志反馈）";
		}
		/**
		* 「清除运行时缓存」设置行。
		* @returns 该设置分区内的一个功能块。
		*/
		function RuntimeCacheRow() {
			const [scan, setScan] = (0, react.useState)({});
			/**
			* 扫描状态（S3-16）：`unknown`（还没读到/宿主不可用）与 `ok`（真值）必须分开——
			* 旧实现把「读不到」渲染成「可回收：0 B（0 项）」，看起来像「确实没东西可清」的合法空态。
			*/
			const [scanState, setScanState] = (0, react.useState)("unknown");
			const [report, setReport] = (0, react.useState)(null);
			const [message, setMessage] = (0, react.useState)(null);
			const [busy, setBusy] = (0, react.useState)(false);
			const [confirming, setConfirming] = (0, react.useState)(false);
			const refresh = (0, react.useCallback)(async () => {
				try {
					const response = await fetch("/api/android/runtime-cache/scan", {
						credentials: "same-origin",
						cache: "no-store"
					});
					if (!response.ok) {
						setScanState("failed");
						setMessage({
							text: describeHttpFailure("读取运行时缓存", response.status),
							http: response.status
						});
						return;
					}
					const payload = await response.json();
					setScan(payload);
					setScanState("ok");
					setMessage(null);
				} catch {
					setScanState("unknown");
					setMessage(null);
				}
			}, [setScan]);
			(0, react.useEffect)(() => {
				refresh();
			}, [refresh]);
			const run = (0, react.useCallback)(async () => {
				setConfirming(false);
				setBusy(true);
				setMessage(null);
				try {
					const response = await fetch("/api/android/runtime-cache/execute", {
						method: "POST",
						credentials: "same-origin",
						cache: "no-store"
					});
					if (!response.ok) {
						setMessage({
							text: describeHttpFailure("清理运行时缓存", response.status),
							http: response.status
						});
						return;
					}
					const payload = await response.json().catch(() => null);
					setReport(payload);
					if (payload === null) setMessage({ text: "清理结果无法解析（响应不是预期的数据）——请重试；仍失败可复制日志反馈" });
				} catch {
					setMessage({ text: "清理请求失败（仅安卓应用内有此能力）——请确认在本机应用内操作" });
				} finally {
					setBusy(false);
					refresh();
				}
			}, [refresh]);
			const reclaimable = typeof scan.reclaimableBytes === "number" ? scan.reclaimableBytes : 0;
			const targets = Array.isArray(scan.targets) ? scan.targets : [];
			const skipped = Array.isArray(scan.skipped) ? scan.skipped : [];
			const items = report !== null && Array.isArray(report.items) ? report.items : [];
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: "dsh-dev-cache",
				"data-plugin": "dev-runtime-cache",
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						"data-scan-state": scanState,
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: scanState === "ok" ? reclaimable > 0 ? "运行时缓存可回收：" + fmtBytes(reclaimable) + "（" + String(targets.length) + " 项）" : "运行时缓存：暂无可清理项（本版白名单内没有残留）" : "运行时缓存可回收：读不到（不是 0——应用内的读取通道不可用或返回异常）" }),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn",
								disabled: busy,
								onClick: () => {
									refresh();
								},
								children: "重新扫描"
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn dsh-dev-danger",
								disabled: busy || scanState !== "ok" || reclaimable === 0,
								onClick: () => {
									setConfirming(true);
								},
								children: busy ? "清理中…" : "清理"
							})
						]
					}),
					scanState === "ok" && targets.length > 0 && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("ul", {
						className: "dsh-dev-cache-list",
						children: targets.map((item) => /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("li", { children: [
							item.label ?? item.id,
							" — ",
							fmtBytes(typeof item.bytes === "number" ? item.bytes : 0),
							typeof item.files === "number" ? "（" + String(item.files) + " 项）" : ""
						] }, item.label ?? item.id))
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "白名单制：只清理本版已知安全的引擎运行时残留（引擎日志历史代 + 应用私有数据下有名有据的缓存目录）。 当前引擎日志（壳侧鉴权链依赖它）、会话与附件、凭据、用户设置、已装插件一律不触碰； DSH_HOME/cache 按子目录分别裁定，本版未纳入任何子目录，因此整目录逐项跳过并如实列出。 未识别的路径会被显式跳过。本版不清理 pnpm store，也不触碰快照在途标志。"
					}),
					skipped.length > 0 && /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("details", {
						className: "dsh-dev-hint",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("summary", { children: [
							"跳过 ",
							skipped.length,
							" 项（未识别/保留）"
						] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("ul", {
							className: "dsh-dev-cache-list",
							children: skipped.map((item) => /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("li", { children: [
								item.label ?? item.id,
								"：",
								describe(item)
							] }, item.label ?? item.id))
						})]
					}),
					message !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						...noticeDataAttrs(message),
						children: message.text
					}),
					report !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
						className: "dsh-dev-hint",
						children: [
							"已清理 ",
							String(report.removed ?? 0),
							" 项，释放 ",
							fmtBytes(typeof report.removedBytes === "number" ? report.removedBytes : 0),
							"（扫描值 ",
							fmtBytes(typeof report.plannedBytes === "number" ? report.plannedBytes : 0),
							"）",
							typeof report.failed === "number" && report.failed > 0 ? "；" + String(report.failed) + " 项失败" : ""
						]
					}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("ul", {
						className: "dsh-dev-cache-list",
						children: items.map((item) => /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("li", {
							...item.reason === void 0 || item.reason === "" ? {} : { "data-reason": item.reason },
							...item.detail === void 0 || item.detail === "" ? {} : { "data-detail": item.detail },
							children: [
								item.label ?? item.id,
								" — ",
								item.status === "removed" ? "已删除 " + fmtBytes(typeof item.bytes === "number" ? item.bytes : 0) : item.status === "failed" ? "失败：" + describe(item) : "跳过：" + describe(item)
							]
						}, item.label ?? item.id))
					})] }),
					confirming && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: "dsh-dev-modal-overlay",
						role: "dialog",
						"aria-modal": "true",
						"aria-label": "确认清除运行时缓存",
						onClick: () => {
							setConfirming(false);
						},
						children: /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
							className: "dsh-dev-modal",
							role: "document",
							onClick: (event) => {
								event.stopPropagation();
							},
							children: [
								/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
									className: "dsh-dev-modal-title",
									children: "确认清除运行时缓存？"
								}),
								/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
									className: "dsh-dev-modal-desc",
									children: [
										"将删除以下 ",
										targets.length,
										" 项，合计 ",
										fmtBytes(reclaimable),
										"。会话、附件、凭据、 设置、已装插件与当前引擎日志不受影响；删除逐项进行，失败项会如实列出。"
									]
								}),
								/* @__PURE__ */ (0, react_jsx_runtime.jsx)("ul", {
									className: "dsh-dev-cache-list",
									children: targets.map((item) => /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("li", { children: [
										item.label ?? item.id,
										" — ",
										fmtBytes(typeof item.bytes === "number" ? item.bytes : 0)
									] }, item.label ?? item.id))
								}),
								/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
									className: "dsh-dev-modal-actions",
									children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "button",
										className: "dsh-dev-btn",
										autoFocus: true,
										onClick: () => {
											setConfirming(false);
										},
										children: "取消"
									}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "button",
										className: "dsh-dev-btn dsh-dev-danger",
										onClick: () => {
											run();
										},
										children: "清理"
									})]
								})
							]
						})
					})
				]
			});
		}
		//#endregion
		//#region src/client/dev-section/DevSection.tsx
		/**
		* Developer-options settings page (Android shell facilities): restart / shut down (both with a
		* custom confirm) / refresh UI / open console / dev debug-log toggle. Registered at the upstream
		* settings.section extension point (auto-projected by ui-settings-general's nav, zero upstream
		* changes). Bridge calls go through window.androidBridge (injected by MainActivity's
		* addJavascriptInterface).
		*
		* Restart and shut down draw a custom frontend confirm because WebView's window.confirm is
		* unreliable under the shell's auto-approving onJsAlert; "Shut down" stops the engine and falls
		* back to the init screen (shell shutdownToGuide bridge).
		*/
		/** 壳侧悬浮球开关真值回读（桥不可用/抛错 → false）。
		*  ST-02（页侧半边）：壳侧 getOverlayEnabled() = 偏好 && 悬浮窗权限 && 服务实例在场，
		*  权限缺失时偏好已回落 false —— 展示值只能以该回读为准，不得沿用上次的 UI 值。 */
		function readOverlayEnabled() {
			try {
				var _window$androidBridge, _window$androidBridge2;
				return ((_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 || (_window$androidBridge2 = _window$androidBridge.getOverlayEnabled) === null || _window$androidBridge2 === void 0 ? void 0 : _window$androidBridge2.call(_window$androidBridge)) ?? false;
			} catch {
				return false;
			}
		}
		/**
		* 破坏性/中断性操作的二次确认文案（0.14.1 批 9 / S3-14）。
		*
		* 为什么三处都要确认：同一页里「运行时缓存清理」原本有确认弹窗，而「一键清理临时工作区」
		* 与「关闭」一样是**单击即执行**的破坏性操作——同一个页面里同级破坏力却有两种确认强度，
		* 用户无法从外观预判哪一下会真的删东西（审查档 §3.3 第 14 行）。
		*/
		const CONFIRM_TEXT = {
			restart: {
				title: "重启 DeepCode？",
				desc: "将终止并自动重新启动本地引擎与页面（约数秒）。未发送的内容会保留在输入框。",
				ok: "重启"
			},
			close: {
				title: "关闭并回退到初始化界面？",
				desc: "将停止本地引擎并退出到初始化界面；引擎不会自动重启，需手动再次启动。",
				ok: "关闭"
			},
			clean: {
				title: "清理临时工作区？",
				desc: "将删除文件直达（分享进来）的临时文件；相关会话中的文件引用会失效，无法恢复。会话本身、附件、配置与凭据不受影响。",
				ok: "清理"
			}
		};
		/**
		* Render the developer-options section content column.
		* @param props - composed slot props (contract/slots.ts).
		* @returns the section element tree.
		*/
		function DevSection({ renderSlot }) {
			const [devLog, refreshDevLog] = useShellState(() => {
				try {
					var _window$androidBridge3, _window$androidBridge4;
					return ((_window$androidBridge3 = window.androidBridge) === null || _window$androidBridge3 === void 0 || (_window$androidBridge4 = _window$androidBridge3.getDevLogEnabled) === null || _window$androidBridge4 === void 0 ? void 0 : _window$androidBridge4.call(_window$androidBridge3)) ?? false;
				} catch {
					return false;
				}
			});
			const [overlayOn, refreshOverlay] = useShellState(readOverlayEnabled);
			const [overlayMsg, setOverlayMsg] = (0, react.useState)(null);
			const [restarting, setRestarting] = (0, react.useState)(false);
			/** 重启/刷新等动作的失败回执（S3-15：旧实现在桥缺席时显示「重启中…」两秒后自己变回去）。 */
			const [actionMsg, setActionMsg] = (0, react.useState)(null);
			const [allFiles] = useShellState(() => {
				try {
					var _window$androidBridge5, _window$androidBridge6;
					return ((_window$androidBridge5 = window.androidBridge) === null || _window$androidBridge5 === void 0 || (_window$androidBridge6 = _window$androidBridge5.hasAllFilesAccess) === null || _window$androidBridge6 === void 0 ? void 0 : _window$androidBridge6.call(_window$androidBridge5)) ?? false;
				} catch {
					return false;
				}
			});
			const [confirm, setConfirm] = (0, react.useState)(null);
			const [incomingBytes, setIncomingBytes] = (0, react.useState)(null);
			const [incomingMsg, setIncomingMsg] = (0, react.useState)(null);
			const [cleaning, setCleaning] = (0, react.useState)(false);
			const refreshIncoming = (0, react.useCallback)(async () => {
				try {
					const r = await fetch("/api/android/file-incoming", {
						credentials: "same-origin",
						cache: "no-store"
					});
					if (!r.ok) {
						setIncomingMsg({
							text: describeHttpFailure("读取来件占用", r.status),
							http: r.status
						});
						return;
					}
					if (r.ok) {
						const j = await r.json();
						setIncomingBytes(typeof j.bytes === "number" ? j.bytes : null);
					}
				} catch {}
			}, []);
			(0, react.useEffect)(() => {
				refreshIncoming();
			}, [refreshIncoming]);
			(0, react.useEffect)(() => {
				const onVisible = () => {
					if (document.visibilityState !== "visible") return;
					refreshIncoming();
				};
				document.addEventListener("visibilitychange", onVisible);
				window.addEventListener("focus", onVisible);
				return () => {
					document.removeEventListener("visibilitychange", onVisible);
					window.removeEventListener("focus", onVisible);
				};
			}, [refreshIncoming]);
			const cleanIncoming = (0, react.useCallback)(async () => {
				setCleaning(true);
				setIncomingMsg(null);
				try {
					const r = await fetch("/api/android/file-incoming/clean", {
						method: "POST",
						credentials: "same-origin",
						cache: "no-store"
					});
					if (!r.ok) {
						setIncomingMsg({
							text: describeHttpFailure("清理临时工作区", r.status),
							http: r.status
						});
						return;
					}
					const j = await r.json().catch(() => null);
					setIncomingMsg((j === null || j === void 0 ? void 0 : j.ok) ? { text: `已清理本工具临时项（${j.removed ?? 0} 项）——相关会话中的文件引用将失效` } : {
						text: "清理未完成——请重试；仍失败可复制日志反馈",
						...(j === null || j === void 0 ? void 0 : j.reason) === void 0 ? {} : { code: j.reason }
					});
				} catch {
					setIncomingMsg({ text: "清理请求失败（仅安卓应用内有此能力）——请确认在本机应用内操作" });
				} finally {
					setCleaning(false);
					refreshIncoming();
				}
			}, [refreshIncoming]);
			const fmtBytes = (n) => {
				if (n >= 1048576) return (n / 1024 / 1024).toFixed(1) + " MB";
				if (n >= 1024) return (n / 1024).toFixed(1) + " KB";
				return n + " B";
			};
			const askRestart = (0, react.useCallback)(() => setConfirm("restart"), []);
			const askClose = (0, react.useCallback)(() => setConfirm("close"), []);
			const cancelConfirm = (0, react.useCallback)(() => setConfirm(null), []);
			const doRestart = (0, react.useCallback)(() => {
				setConfirm(null);
				setActionMsg(null);
				let started = false;
				try {
					var _window$androidBridge7, _window$androidBridge8;
					started = ((_window$androidBridge7 = window.androidBridge) === null || _window$androidBridge7 === void 0 || (_window$androidBridge8 = _window$androidBridge7.restartEngine) === null || _window$androidBridge8 === void 0 ? void 0 : _window$androidBridge8.call(_window$androidBridge7)) === true;
				} catch {
					started = false;
				}
				if (!started) {
					setActionMsg({ text: "重启没有发起：应用与页面的连接不可用，或已在重启中——请稍等几秒；仍无效请关闭并重新打开应用" });
					return;
				}
				setRestarting(true);
				window.setTimeout(() => setRestarting(false), 2e3);
			}, []);
			const doClose = (0, react.useCallback)(() => {
				setConfirm(null);
				try {
					var _window$androidBridge9, _window$androidBridge10;
					(_window$androidBridge9 = window.androidBridge) === null || _window$androidBridge9 === void 0 || (_window$androidBridge10 = _window$androidBridge9.shutdownToGuide) === null || _window$androidBridge10 === void 0 || _window$androidBridge10.call(_window$androidBridge9);
				} catch {}
			}, []);
			const reload = (0, react.useCallback)(() => {
				setActionMsg(null);
				try {
					var _window$androidBridge11;
					if (((_window$androidBridge11 = window.androidBridge) === null || _window$androidBridge11 === void 0 ? void 0 : _window$androidBridge11.reloadWebUI) === void 0) {
						setActionMsg({ text: "刷新界面不可用：应用与页面的连接未装配——请关闭并重新打开应用" });
						return;
					}
					window.androidBridge.reloadWebUI();
				} catch {
					setActionMsg({ text: "刷新界面失败：应用与页面的连接不可用——请关闭并重新打开应用" });
				}
			}, []);
			const openConsole = (0, react.useCallback)(() => {
				setActionMsg(null);
				try {
					var _window$androidBridge12;
					if (((_window$androidBridge12 = window.androidBridge) === null || _window$androidBridge12 === void 0 ? void 0 : _window$androidBridge12.openConsole) === void 0) {
						setActionMsg({ text: "控制台不可用：应用与页面的连接未装配——请关闭并重新打开应用" });
						return;
					}
					window.androidBridge.openConsole();
				} catch {
					setActionMsg({ text: "控制台打开失败：应用与页面的连接不可用——请关闭并重新打开应用" });
				}
			}, []);
			const toggleLog = (0, react.useCallback)((enabled) => {
				try {
					var _window$androidBridge13, _window$androidBridge14;
					(_window$androidBridge13 = window.androidBridge) === null || _window$androidBridge13 === void 0 || (_window$androidBridge14 = _window$androidBridge13.setDevLogEnabled) === null || _window$androidBridge14 === void 0 || _window$androidBridge14.call(_window$androidBridge13, enabled);
				} catch {}
				refreshDevLog();
			}, [refreshDevLog]);
			const toggleOverlay = (0, react.useCallback)((enabled) => {
				setOverlayMsg(null);
				try {
					var _window$androidBridge15, _window$androidBridge16;
					const started = ((_window$androidBridge15 = window.androidBridge) === null || _window$androidBridge15 === void 0 || (_window$androidBridge16 = _window$androidBridge15.setOverlayEnabled) === null || _window$androidBridge16 === void 0 ? void 0 : _window$androidBridge16.call(_window$androidBridge15, enabled)) ?? false;
					refreshOverlay();
					if (enabled && !started) setOverlayMsg("已打开系统授权页；授予后请重新打开本开关");
					else if (enabled) setOverlayMsg("悬浮球已开启：任意界面可拖拽；点开面板实时查看工具调用，可一键停止");
					else setOverlayMsg("悬浮球已关闭");
				} catch {
					setOverlayMsg("应用内连接不可用（悬浮球仅安卓应用内支持）——请重新打开应用后重试");
				}
			}, [refreshOverlay]);
			const [configMsg, setConfigMsg] = (0, react.useState)(null);
			const exportConfig = (0, react.useCallback)(() => {
				try {
					var _window$androidBridge17, _window$androidBridge18;
					const raw = (_window$androidBridge17 = window.androidBridge) === null || _window$androidBridge17 === void 0 || (_window$androidBridge18 = _window$androidBridge17.exportConfig) === null || _window$androidBridge18 === void 0 ? void 0 : _window$androidBridge18.call(_window$androidBridge17);
					const j = JSON.parse(raw ?? "{}");
					setConfigMsg(j.ok ? `已导出到 ${j.path ?? "exports/config/settings.yaml"}` : `导出失败：${j.error ?? "未知错误"}`);
				} catch {
					setConfigMsg("导出失败：应用内连接不可用（仅安卓应用内可用）——请重新打开应用后重试");
				}
			}, []);
			const importConfig = (0, react.useCallback)(() => {
				try {
					var _window$androidBridge19, _window$androidBridge20;
					const raw = (_window$androidBridge19 = window.androidBridge) === null || _window$androidBridge19 === void 0 || (_window$androidBridge20 = _window$androidBridge19.importConfig) === null || _window$androidBridge20 === void 0 ? void 0 : _window$androidBridge20.call(_window$androidBridge19);
					const j = JSON.parse(raw ?? "{}");
					setConfigMsg(j.ok ? `已导入并生效（原配置备份为 settings.yaml.import-backup）。${j.hint ?? ""}` : `导入失败：${j.error ?? "未知错误"}`);
				} catch {
					setConfigMsg("导入失败：应用内连接不可用（仅安卓应用内可用）——请重新打开应用后重试");
				}
			}, []);
			const onKeyDown = (0, react.useCallback)((e) => {
				if (e.key === "Escape") cancelConfirm();
			}, [cancelConfirm]);
			const logPathHint = allFiles === false ? "未授予「所有文件访问」：日志将写入应用私有目录，授权后自动切换公共目录。" : "开启后按天写入 Documents/dshdata/log/dsh-<日期>.log。";
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				"data-plugin": "dev-section",
				onKeyDown,
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-note",
						children: "DeepCode 开发者选项：控制台为运行时内嵌命令行；日志默认关闭。"
					}),
					renderSlot === null || renderSlot === void 0 ? void 0 : renderSlot("settings.dev.item", {}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn",
								onClick: askRestart,
								disabled: restarting,
								children: restarting ? "重启中…" : "重启"
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn dsh-dev-danger",
								onClick: askClose,
								children: "关闭"
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn",
								onClick: reload,
								children: "刷新界面"
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn",
								onClick: openConsole,
								children: "打开控制台"
							})
						]
					}),
					actionMsg !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-warn",
						...noticeDataAttrs(actionMsg),
						children: actionMsg.text
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
						className: "dsh-dev-row dsh-dev-switch",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
							type: "checkbox",
							checked: devLog,
							onChange: (e) => toggleLog(e.target.checked)
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: "开发者调试日志" })]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
						className: "dsh-dev-row dsh-dev-switch",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
							type: "checkbox",
							checked: overlayOn,
							onChange: (e) => toggleOverlay(e.target.checked)
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: "悬浮球（任意界面可见的任务面板入口）" })]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "屏幕上的圆球：点开可看实时工具调用、可一键停止。与「手机控制」页的「虚拟屏浮窗」 （退后台显示虚拟屏画面）是两个不同的东西。"
					}),
					overlayMsg !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: overlayMsg
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: exportConfig,
							children: "导出配置"
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: importConfig,
							children: "导入配置"
						})]
					}),
					configMsg !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: configMsg
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "导出位置 Documents/dshdata/exports/config/settings.yaml；用文件管理器修改后点「导入配置」即可生效。 配置不含 API 密钥（密钥在应用私有目录，不随导出泄漏）。"
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)(RuntimeCacheRow, {}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "通知的提醒方式（含「关掉提问提醒会发生什么」）在「设置 → 通知」里。"
					}),
					incomingBytes !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: ["文件直达临时工作区占用：", fmtBytes(incomingBytes)] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn dsh-dev-danger",
							disabled: cleaning || incomingBytes === 0,
							onClick: () => {
								setConfirm("clean");
							},
							children: cleaning ? "清理中…" : "一键清理"
						})]
					}),
					incomingMsg !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						...noticeDataAttrs(incomingMsg),
						children: incomingMsg.text
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "清理会删除临时工作区内的外部文件；相关会话中的文件引用将失效（D15：纯手动清理，无自动清理）。"
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: logPathHint
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-warn",
						children: "日志包含命令与模型内容，仅用于排查，请及时清理。"
					}),
					confirm !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: "dsh-dev-modal-overlay",
						role: "dialog",
						"aria-modal": "true",
						"aria-label": CONFIRM_TEXT[confirm].title,
						onClick: cancelConfirm,
						children: /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
							className: "dsh-dev-modal",
							role: "document",
							onClick: (e) => e.stopPropagation(),
							children: [
								/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
									className: "dsh-dev-modal-title",
									children: CONFIRM_TEXT[confirm].title
								}),
								/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
									className: "dsh-dev-modal-desc",
									children: CONFIRM_TEXT[confirm].desc
								}),
								/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
									className: "dsh-dev-modal-actions",
									children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "button",
										className: "dsh-dev-btn",
										autoFocus: true,
										onClick: cancelConfirm,
										children: "取消"
									}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "button",
										className: confirm === "restart" ? "dsh-dev-btn" : "dsh-dev-btn dsh-dev-danger",
										onClick: () => {
											const which = confirm;
											setConfirm(null);
											if (which === "restart") doRestart();
											else if (which === "close") doClose();
											else cleanIncoming();
										},
										children: CONFIRM_TEXT[confirm].ok
									})]
								})
							]
						})
					})
				]
			});
		}
		//#endregion
		//#region src/client/dev-section/phone-control.tsx
		/**
		* 「手机控制」设置分区（0.14.0 用户定例：把手机控制单独开一个设置页）。
		*
		* 内容：Shizuku 特权通道（状态 + 下载/打开入口 + 视频教程）/ 开放屏幕范围 / 虚拟屏分辨率档位 /
		* 虚拟屏浮窗（退后台自动显示）/ 无障碍入口（含 Android 13+ 受限设置解锁）/ 强制销毁（连点三次确认）。
		*
		* 数据面纪律：全部经 window.androidBridge 只读回读 + 写后回读；桥不可用/抛错一律**如实报不可读**，
		* 不用安全默认值冒充事实（0.14.1 UI 审查：桥缺席时页面显示「未开启 / 0.75 / 仅虚拟屏幕」这些
		* 看起来像事实的值，改完还会自己弹回）。
		*
		* 0.14.1 Shizuku 面重做（用户 2026-09-22 定例，UI 审查 P0）：
		*  - **状态源换掉**：旧实现读的是 `vdisplayStatus()`——标题写「Shizuku 特权通道」，内容却是虚拟屏
		*    状态码与 displayId（只有虚拟屏 blocked 时才顺带透出 Shizuku 的 guidance）。现在读 `shizukuStatus()`，
		*    虚拟屏状态另起一行、名字也改对。
		*  - **两个入口**：左「下载 Shizuku」右「打开 Shizuku」，并列半行宽；**未安装时「打开 Shizuku」不可点**
		*    （旧态是「模型让用户去设置页安装、启动并授权 Shizuku」，而那一页一个入口都没有——死循环）。
		*  - **两个外链共用一条壳侧通道**：下载页与视频教程都只是跳出去，页面只传 key，URL 表在壳侧
		*    （`ExternalLinks`），页面拿不到「打开任意地址」的能力。
		*  - **授权只能由用户在 Shizuku 内完成**（被提权方不得自改授权），壳侧只负责把人送到界面；
		*    回到本页每 2 秒轮询一次状态，授权完成会自动收敛，不需要用户手动点刷新。
		*/
		const SCOPES = [
			"virtual-only",
			"real-only",
			"all"
		];
		const SCALE_OPTIONS = [
			.5,
			.75,
			1
		];
		/** 外链 key（与壳侧 `ExternalLinks` 的登记名逐字一致）。 */
		const LINK_DOWNLOAD = "shizuku-download";
		const LINK_TUTORIAL = "shizuku-tutorial";
		const STATUS_LABEL = {
			disabled: "已关闭",
			blocked: "需要准备",
			ready: "可创建",
			active: "已激活"
		};
		const A11Y_UNREADABLE = {
			readable: false,
			enabled: false,
			hint: "",
			restrictedSettingsApplies: false
		};
		const SHIZUKU_UNREADABLE = {
			readable: false,
			installed: false,
			running: false,
			granted: false,
			bound: false,
			binding: false,
			guidance: ""
		};
		const ROOT_GRANT_UNREADABLE = {
			readable: false,
			granted: false,
			consentValid: false,
			channelUid: -1,
			channelRoot: false,
			rootGranted: false,
			rootState: "unknown",
			honesty: ""
		};
		const OWNERSHIP_UNREADABLE = {
			readable: false,
			running: false,
			overdue: false,
			startedAt: 0,
			completedAt: 0,
			result: void 0
		};
		function readOwnershipState(value) {
			if (value === null || typeof value !== "object" || Array.isArray(value)) return OWNERSHIP_UNREADABLE;
			const parsed = value;
			if (typeof parsed.running !== "boolean") return OWNERSHIP_UNREADABLE;
			const count = (value) => typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : 0;
			const result = parsed.result !== null && typeof parsed.result === "object" && !Array.isArray(parsed.result) ? parsed.result : void 0;
			return {
				readable: true,
				running: parsed.running,
				overdue: parsed.overdue === true,
				startedAt: count(parsed.startedAt),
				completedAt: count(parsed.completedAt),
				operation: typeof parsed.operation === "string" ? parsed.operation : void 0,
				result
			};
		}
		/** Submitted/running/unknown results must never be rendered as completed repair. */
		function describeOwnershipRepair(state) {
			if (!state.readable || state.startedAt === 0) return void 0;
			if (state.running) return {
				ok: state.overdue ? false : void 0,
				text: state.overdue ? "特权工作结果仍不明，已暂停新派发和维护；不要重复操作。无法确认 helper 结算时请重启设备（仅重启应用不算结算）。" : state.operation !== void 0 && !state.operation.includes("ownership") ? "正在等待已有特权工作结算，尚未开始文件属主维护。" : "属主维护进行中，正在有界检查本应用数据目录；结果会自动刷新。"
			};
			const result = state.result;
			if (result === void 0) return {
				ok: false,
				text: "属主维护结果不可读，不能确认修复完成；请复制诊断日志。"
			};
			if (result.skipped === "no-root-path") return {
				ok: true,
				text: "当前没有可用 root 修复路径，未执行属主变更。"
			};
			const count = (key) => typeof result[key] === "number" && Number.isFinite(result[key]) && result[key] >= 0 ? String(result[key]) : "未知";
			const counts = "检查 " + count("checked") + " 项 / 修复 " + count("healed") + " 项 / 失败 " + count("failures") + " 项";
			const completeCounts = ["checked", "healed"].every((key) => typeof result[key] === "number" && Number.isSafeInteger(result[key]) && result[key] >= 0);
			if (result.ok === true && completeCounts && result.truncated === false && result.deadlineExceeded === false && result.remaining === 0 && result.unverifiedMutations === 0 && result.failures === 0) return {
				ok: true,
				text: "文件属主维护完成（" + counts + "）；未进行 SELinux 重标记。"
			};
			const reason = typeof result.reason === "string" ? result.reason : typeof result.code === "string" ? result.code : void 0;
			return {
				ok: false,
				text: "文件属主维护未完成（" + counts + "）：" + describeCallReason(reason)
			};
		}
		const ROOT_ACCESS_UNREADABLE = {
			readable: false,
			suExists: false,
			state: "unknown",
			uid: -1,
			granted: false,
			requesting: false,
			managerLabel: "",
			managerInstalled: false,
			guidance: ""
		};
		/** issue #262 用户指定文案（**逐字保留**，未 root 通道下的红字）。 */
		const ROOT_GRANT_NOT_ROOT_TEXT = "无法在未 root 的设备上赋予该权限";
		/**
		* 外链/拉起失败原因的中文口径在**唯一真源** `../user-copy.ts`（0.14.1 批 3 / P3-1）。
		*
		* 本文件此前自带一张 `LINK_REASON_LABEL` 局部表——与本页其它面、以及壳侧各自的局部表并存，
		* 于是同一个码在不同界面说法不同。局部表已删除：翻译只此一处，码本身只进 `data-*`。
		*/
		/**
		* Shizuku 通道状态 → 中文状态词。
		*
		* 五态与壳侧 `ShizukuTransport.status()` 的字段一一对应；顺序即真实推进顺序
		* （未安装 → 未启动 → 未授权 → 未绑定 → 就绪），用户据此知道自己在第几步。
		*/
		function shizukuStateLabel(status) {
			if (!status.readable) return "状态不可读";
			if (!status.installed) return "未安装";
			if (!status.running) return "已安装，Shizuku 未启动";
			if (!status.granted) return "已启动，尚未授权";
			if (!status.bound) return status.binding ? "已授权，通道建立中" : "已授权，通道未建立";
			return "通道就绪";
		}
		/** 壳侧 guidance 缺失时的兜底说明（每态都能说清「下一步做什么」）。 */
		function shizukuStepHint(status) {
			if (!status.readable) return "读不到 Shizuku 状态：壳侧桥未装配或解析失败。";
			if (!status.installed) return "点「下载 Shizuku」到发布页装好，再回来点「打开 Shizuku」。";
			if (!status.running) return "点「打开 Shizuku」，在应用内按提示用无线调试启动它（重启设备后需要重做一次）。";
			if (!status.granted) return "点「打开 Shizuku」，在里面允许本应用使用 Shizuku——授权只能由你亲手完成。";
			if (!status.bound) return "状态每 2 秒自动刷新，稍等即可；一直停在这里可以点「打开 Shizuku」重进一次。";
			return "特权通道已就绪，虚拟屏与特权 shell 可以用了。";
		}
		function parseRootReply(raw) {
			if (!raw) return void 0;
			try {
				const value = JSON.parse(raw);
				return value !== null && typeof value === "object" && !Array.isArray(value) ? value : void 0;
			} catch {
				return;
			}
		}
		function parseAnswer(raw) {
			if (raw === void 0 || raw === "") return void 0;
			try {
				return JSON.parse(raw);
			} catch {
				return;
			}
		}
		/** 外链/拉起类调用统一结算：成功给人话，失败给「原因 + 下一步」，绝不静默。 */
		function settleLinkCall(raw, okText, failLead) {
			const answer = parseAnswer(raw);
			if ((answer === null || answer === void 0 ? void 0 : answer.ok) === true) return {
				ok: true,
				text: okText
			};
			return {
				ok: false,
				text: failLead + "：" + describeCallReason(answer === null || answer === void 0 ? void 0 : answer.reason)
			};
		}
		/** 受限设置解锁结算（壳侧回 `{ok, message}`，message 已是人话）。 */
		function settleUnlockCall(raw) {
			const answer = parseAnswer(raw);
			if ((answer === null || answer === void 0 ? void 0 : answer.ok) === true) return {
				ok: true,
				text: answer.message ?? "已解锁受限设置，现在可以回系统页开启无障碍服务。"
			};
			return {
				ok: false,
				text: "解锁失败：" + ((answer === null || answer === void 0 ? void 0 : answer.message) ?? describeCallReason(answer === null || answer === void 0 ? void 0 : answer.reason))
			};
		}
		function readScope() {
			try {
				var _window$androidBridge, _window$androidBridge2;
				const raw = (_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 || (_window$androidBridge2 = _window$androidBridge.getScreenScope) === null || _window$androidBridge2 === void 0 ? void 0 : _window$androidBridge2.call(_window$androidBridge);
				if (SCOPES.includes(raw)) return raw;
			} catch {}
			return "virtual-only";
		}
		function readVdisplay() {
			try {
				var _window$androidBridge3, _window$androidBridge4;
				const raw = (_window$androidBridge3 = window.androidBridge) === null || _window$androidBridge3 === void 0 || (_window$androidBridge4 = _window$androidBridge3.vdisplayStatus) === null || _window$androidBridge4 === void 0 ? void 0 : _window$androidBridge4.call(_window$androidBridge3);
				const parsed = raw ? JSON.parse(raw) : void 0;
				const state = parsed === null || parsed === void 0 ? void 0 : parsed.state;
				return {
					state: state === "disabled" || state === "blocked" || state === "ready" || state === "active" ? state : "blocked",
					code: typeof (parsed === null || parsed === void 0 ? void 0 : parsed.code) === "string" ? parsed.code : "vdisplay-status-unavailable",
					guidance: typeof (parsed === null || parsed === void 0 ? void 0 : parsed.guidance) === "string" ? parsed.guidance : "虚拟屏状态暂不可读；不会把虚拟屏请求回退到真实屏幕。",
					...typeof (parsed === null || parsed === void 0 ? void 0 : parsed.displayId) === "number" ? { displayId: parsed.displayId } : {}
				};
			} catch {
				return {
					state: "blocked",
					code: "vdisplay-status-unavailable",
					guidance: "读不到虚拟屏状态：应用内桥未接好或解析失败。重新打开应用再试；读不到时不会把虚拟屏请求回退到真实屏幕。"
				};
			}
		}
		function readScale() {
			try {
				var _window$androidBridge5, _window$androidBridge6;
				const value = (_window$androidBridge5 = window.androidBridge) === null || _window$androidBridge5 === void 0 || (_window$androidBridge6 = _window$androidBridge5.getVdisplayScale) === null || _window$androidBridge6 === void 0 ? void 0 : _window$androidBridge6.call(_window$androidBridge5);
				if (typeof value === "number" && Number.isFinite(value)) return value;
			} catch {}
			return .75;
		}
		function readFloat() {
			try {
				var _window$androidBridge7, _window$androidBridge8;
				return ((_window$androidBridge7 = window.androidBridge) === null || _window$androidBridge7 === void 0 || (_window$androidBridge8 = _window$androidBridge7.getVdisplayFloatEnabled) === null || _window$androidBridge8 === void 0 ? void 0 : _window$androidBridge8.call(_window$androidBridge7)) ?? true;
			} catch {
				return true;
			}
		}
		/**
		* Shizuku 通道状态（**唯一**「装没装」的事实来源）。
		*
		* `installed` 字段决定「打开 Shizuku」是否可点：拿不到状态时按**未安装**处理（复用
		* [SHIZUKU_UNREADABLE]），于是按钮不可点而不是点了没反应——死路形态在本页被结构性排除。
		*/
		function readShizuku() {
			try {
				var _window$androidBridge9, _window$androidBridge10;
				const parsed = parseRootReply((_window$androidBridge9 = window.androidBridge) === null || _window$androidBridge9 === void 0 || (_window$androidBridge10 = _window$androidBridge9.shizukuStatus) === null || _window$androidBridge10 === void 0 ? void 0 : _window$androidBridge10.call(_window$androidBridge9));
				if (parsed === void 0 || typeof parsed.installed !== "boolean") return SHIZUKU_UNREADABLE;
				return {
					readable: true,
					installed: parsed.installed === true,
					running: parsed.running === true,
					granted: parsed.granted === true,
					bound: parsed.bound === true,
					binding: parsed.binding === true,
					guidance: typeof parsed.guidance === "string" ? parsed.guidance : ""
				};
			} catch {
				return SHIZUKU_UNREADABLE;
			}
		}
		/**
		* issue #262「AI root 权限」读面（壳侧 [RootGrant.state] 的桥接）。
		*
		* 读不到一律回落 [ROOT_GRANT_UNREADABLE]（`readable:false`）：界面据此显示「状态不可读」
		* 而不是冒充「未授权」或「可开启」——与 [readShizuku] 同纪律。
		*/
		function readRootGrant() {
			try {
				var _window$androidBridge11, _window$androidBridge12;
				const parsed = parseRootReply((_window$androidBridge11 = window.androidBridge) === null || _window$androidBridge11 === void 0 || (_window$androidBridge12 = _window$androidBridge11.rootGrantState) === null || _window$androidBridge12 === void 0 ? void 0 : _window$androidBridge12.call(_window$androidBridge11));
				if (parsed === void 0 || typeof parsed.granted !== "boolean") return ROOT_GRANT_UNREADABLE;
				return {
					readable: true,
					granted: parsed.granted === true,
					consentValid: parsed.consentValid === true,
					channelUid: typeof parsed.channelUid === "number" ? parsed.channelUid : -1,
					channelRoot: parsed.channelRoot === true,
					rootGranted: parsed.rootGranted === true,
					rootState: typeof parsed.rootState === "string" ? parsed.rootState : "unknown",
					honesty: typeof parsed.honesty === "string" ? parsed.honesty : "",
					ownership: readOwnershipState(parsed.ownership)
				};
			} catch {
				return ROOT_GRANT_UNREADABLE;
			}
		}
		/**
		* 应用级 root 授权读面（壳侧 [RootAccess.state] 的桥接）。
		*
		* 读不到一律回落 [ROOT_ACCESS_UNREADABLE]（`readable:false`）：界面据此显示「状态不可读」
		* 而不是冒充「未授权」或「已授权」——与 [readRootGrant] 同纪律。
		*/
		function readRootAccess() {
			try {
				var _window$androidBridge13, _window$androidBridge14;
				const parsed = parseRootReply((_window$androidBridge13 = window.androidBridge) === null || _window$androidBridge13 === void 0 || (_window$androidBridge14 = _window$androidBridge13.rootAccessState) === null || _window$androidBridge14 === void 0 ? void 0 : _window$androidBridge14.call(_window$androidBridge13));
				if (parsed === void 0 || typeof parsed.state !== "string") return ROOT_ACCESS_UNREADABLE;
				const manager = parsed.manager ?? {};
				return {
					readable: true,
					suExists: parsed.suExists === true,
					state: parsed.state,
					uid: typeof parsed.uid === "number" ? parsed.uid : -1,
					granted: parsed.granted === true,
					requesting: parsed.requesting === true,
					managerLabel: typeof manager.label === "string" ? manager.label : "",
					managerInstalled: manager.installed === true,
					guidance: typeof parsed.guidance === "string" ? parsed.guidance : ""
				};
			} catch {
				return ROOT_ACCESS_UNREADABLE;
			}
		}
		/** 应用级 root 授权状态 → 中文状态词（与壳侧 RootAccess 的状态常量一一对应）。 */
		function rootAccessStateLabel(status) {
			if (!status.readable) return "状态不可读";
			switch (status.state) {
				case "granted": return "已授权（uid 0）";
				case "requesting": return "正在检测授权…";
				case "denied": return "已拒绝";
				case "timeout": return "检测超时，请在管理器确认后重试";
				case "no-su": return "本机没有可用的 su";
				default: return "未检测";
			}
		}
		/** 请求按钮可否点：有 su 且没有请求在飞。 */
		function canRequestRoot(status) {
			return status.readable && status.suExists && !status.requesting;
		}
		/**
		* 开关状态 → 中文状态词（与壳侧字段一一对应，供测试直接断言）。
		*
		* 顺序即真实判据顺序：读不到 → 通道非 root（置灰）→ 未授权 → 已授权。
		*/
		function rootGrantStateLabel(status) {
			if (!status.readable) return "状态不可读";
			if (!status.channelRoot && !status.rootGranted) return "没有可用 root 通道";
			if (!status.granted) return "未授权";
			return "已授权（AI 可用 root）";
		}
		/** 开关可否点击：只有**通道身份确为 root**才可点（读不到/非 root 一律不可点）。 */
		function canToggleRootGrant(status) {
			return status.readable && (status.channelRoot || status.rootGranted);
		}
		/**
		* 非 root 通道下的引导语（issue #262 要求**按通道身份分流**，2026-09-30 对账补）：
		*  - 通道 uid == 2000（Shizuku 以 ADB 启动，设备**可能已 root**）⇒ 引导「在 Shizuku 内以 root 启动」；
		*  - 其它（无通道 / 未 root）⇒ 用户指定红字「无法在未 root 的设备上赋予该权限」（逐字）。
		*
		* 两者都置灰开关；区别只在**用户下一步该做什么**——把已 root 的设备误报成「未 root」会让人
		* 去折腾设备 root，而真正要做的是重启 Shizuku 的启动方式。
		*/
		function rootGrantChannelHint(status) {
			if (!status.readable) return {
				text: "",
				kind: "none"
			};
			if (status.channelRoot) return {
				text: "",
				kind: "none"
			};
			if (status.channelUid === 2e3) return {
				text: "Shizuku 当前以 shell（uid 2000）身份运行——请在 Shizuku 内以 root 启动它，再回到本页开启。",
				kind: "shell-identity"
			};
			return {
				text: ROOT_GRANT_NOT_ROOT_TEXT,
				kind: "not-root"
			};
		}
		/** 无障碍通道状态；读不到时如实报不可读，不冒充「未开启」（0.14.1 UI 审查 P1）。 */
		function readA11y() {
			try {
				var _window$androidBridge15, _window$androidBridge16;
				const raw = (_window$androidBridge15 = window.androidBridge) === null || _window$androidBridge15 === void 0 || (_window$androidBridge16 = _window$androidBridge15.a11yStatus) === null || _window$androidBridge16 === void 0 ? void 0 : _window$androidBridge16.call(_window$androidBridge15);
				if (typeof raw === "string" && raw.startsWith("{")) {
					const parsed = JSON.parse(raw);
					return {
						readable: true,
						enabled: parsed.enabled === true,
						hint: typeof parsed.hint === "string" ? parsed.hint : "",
						restrictedSettingsApplies: parsed.restrictedSettingsApplies === true
					};
				}
			} catch {}
			return A11Y_UNREADABLE;
		}
		/**
		* 渲染「手机控制」设置分区。
		* @returns 分区元素树。
		*/
		function PhoneControlSection(_props) {
			const [scope, refreshScope] = useShellState(readScope);
			const [vdisplay, refreshVdisplay] = useShellState(readVdisplay, { pollMs: 2e3 });
			const [shizuku, refreshShizuku] = useShellState(readShizuku, { pollMs: 2e3 });
			const [scale, refreshScale] = useShellState(readScale);
			const [floatOn, refreshFloat] = useShellState(readFloat);
			const [a11y, refreshA11y] = useShellState(readA11y, { pollMs: 3e3 });
			const [rootGrant, refreshRootGrant] = useShellState(readRootGrant, { pollMs: 2e3 });
			const [rootAccess, refreshRootAccess] = useShellState(readRootAccess, { pollMs: 2e3 });
			/** 用户尝试开启开关但 root 未授权：壳侧已弹授权框，授权一到就自动续开（见下方 effect）。 */
			const [pendingEnable, setPendingEnable] = (0, react.useState)(false);
			const [rootMsg, setRootMsg] = (0, react.useState)(null);
			const [rootOk, setRootOk] = (0, react.useState)(null);
			const [confirmStage, setConfirmStage] = (0, react.useState)(0);
			const [forceMsg, setForceMsg] = (0, react.useState)(null);
			const [shizukuMsg, setShizukuMsg] = (0, react.useState)(null);
			const [shizukuOk, setShizukuOk] = (0, react.useState)(null);
			const [a11yMsg, setA11yMsg] = (0, react.useState)(null);
			const [a11yOk, setA11yOk] = (0, react.useState)(null);
			(0, react.useEffect)(() => {
				if (confirmStage === 0) return void 0;
				const timer = window.setTimeout(() => setConfirmStage(0), 4e3);
				return () => window.clearTimeout(timer);
			}, [confirmStage]);
			const setScope = (0, react.useCallback)((next) => {
				try {
					var _window$androidBridge17, _window$androidBridge18;
					(_window$androidBridge17 = window.androidBridge) === null || _window$androidBridge17 === void 0 || (_window$androidBridge18 = _window$androidBridge17.setScreenScope) === null || _window$androidBridge18 === void 0 || _window$androidBridge18.call(_window$androidBridge17, next);
				} catch {}
				refreshScope();
			}, [refreshScope]);
			const setScale = (0, react.useCallback)((next) => {
				try {
					var _window$androidBridge19, _window$androidBridge20;
					(_window$androidBridge19 = window.androidBridge) === null || _window$androidBridge19 === void 0 || (_window$androidBridge20 = _window$androidBridge19.setVdisplayScale) === null || _window$androidBridge20 === void 0 || _window$androidBridge20.call(_window$androidBridge19, next);
				} catch {}
				refreshScale();
			}, [refreshScale]);
			const setFloat = (0, react.useCallback)((enable) => {
				try {
					var _window$androidBridge21, _window$androidBridge22;
					(_window$androidBridge21 = window.androidBridge) === null || _window$androidBridge21 === void 0 || (_window$androidBridge22 = _window$androidBridge21.setVdisplayFloatEnabled) === null || _window$androidBridge22 === void 0 || _window$androidBridge22.call(_window$androidBridge21, enable);
				} catch {}
				refreshFloat();
			}, [refreshFloat]);
			const openA11y = (0, react.useCallback)(() => {
				try {
					var _window$androidBridge23, _window$androidBridge24;
					(_window$androidBridge23 = window.androidBridge) === null || _window$androidBridge23 === void 0 || (_window$androidBridge24 = _window$androidBridge23.openA11ySettings) === null || _window$androidBridge24 === void 0 || _window$androidBridge24.call(_window$androidBridge23);
				} catch {}
			}, []);
			const unlockA11y = (0, react.useCallback)(() => {
				let raw;
				try {
					var _window$androidBridge25, _window$androidBridge26;
					raw = (_window$androidBridge25 = window.androidBridge) === null || _window$androidBridge25 === void 0 || (_window$androidBridge26 = _window$androidBridge25.unlockRestrictedSettings) === null || _window$androidBridge26 === void 0 ? void 0 : _window$androidBridge26.call(_window$androidBridge25);
				} catch {
					raw = void 0;
				}
				const settled = settleUnlockCall(raw);
				setA11yOk(settled.ok);
				setA11yMsg(settled.text);
				refreshA11y();
			}, [refreshA11y]);
			/** 外链与拉起的共同收口（两个入口共用一条通道，结算也共用）。 */
			const runShizukuAction = (0, react.useCallback)((call, okText, failLead) => {
				let raw;
				try {
					raw = call === null || call === void 0 ? void 0 : call();
				} catch {
					raw = void 0;
				}
				const settled = settleLinkCall(raw, okText, failLead);
				setShizukuOk(settled.ok);
				setShizukuMsg(settled.text);
				refreshShizuku();
			}, [refreshShizuku]);
			const downloadShizuku = (0, react.useCallback)(() => {
				var _window$androidBridge27;
				runShizukuAction(((_window$androidBridge27 = window.androidBridge) === null || _window$androidBridge27 === void 0 ? void 0 : _window$androidBridge27.openExternalLink) ? () => window.androidBridge.openExternalLink(LINK_DOWNLOAD) : void 0, "已打开 Shizuku 发布页——下载 release 版 APK 装好后回到这里。", "打开下载页失败");
			}, [runShizukuAction]);
			const tutorialShizuku = (0, react.useCallback)(() => {
				var _window$androidBridge28;
				runShizukuAction(((_window$androidBridge28 = window.androidBridge) === null || _window$androidBridge28 === void 0 ? void 0 : _window$androidBridge28.openExternalLink) ? () => window.androidBridge.openExternalLink(LINK_TUTORIAL) : void 0, "已用浏览器打开视频教程。", "打开教程失败");
			}, [runShizukuAction]);
			const openShizuku = (0, react.useCallback)(() => {
				var _window$androidBridge29;
				runShizukuAction(((_window$androidBridge29 = window.androidBridge) === null || _window$androidBridge29 === void 0 ? void 0 : _window$androidBridge29.openShizukuManager) ? () => window.androidBridge.openShizukuManager() : void 0, "已打开 Shizuku——在那里启动并授权后，回到本页状态会自动刷新。", "打开 Shizuku 失败");
			}, [runShizukuAction]);
			/**
			* 2026-09-30：**显式请求 Shizuku 授权**。
			*
			* 实测缺陷：授权请求此前只在 `ensureBound` 的后台路径自动发起，而 Shizuku 的
			* `requestPermission` 需要前台 Activity 才能把对话框落到用户眼前 ⇒ 静默失败，
			* 管理器「应用管理」列表里根本没有本应用、状态恒 denied，用户没有任何可点的授权入口。
			* 本入口在 UI 线程发起请求，对话框随即出现。
			*/
			const requestShizukuPermission = (0, react.useCallback)(() => {
				var _window$androidBridge30;
				runShizukuAction(((_window$androidBridge30 = window.androidBridge) === null || _window$androidBridge30 === void 0 ? void 0 : _window$androidBridge30.requestShizukuPermission) ? () => window.androidBridge.requestShizukuPermission() : void 0, "已发起 Shizuku 授权请求——请在弹窗上点「允许」（本页每 2 秒自动刷新）。", "请求 Shizuku 授权失败");
			}, [runShizukuAction]);
			/**
			* 「重置链接」：强制移除 Shizuku 侧 UserService 并清空绑定态。
			*
			* 与「刷新状态」同一行（都是非破坏性只读/自愈动作），结算沿用既有 [runShizukuAction] →
			* [settleLinkCall]，**不新造结算口径**：壳侧回 {ok, code/guidance}，ok=false 走失败支并把人话原因说清。
			*
			* 「持续扫描链接」= 既有的 2 秒轮询（[runShizukuAction] 内部已调 refreshShizuku 立刻回读一次，
			* 之后交给 useShellState 的 2s 轮询自然收敛）。**不新开定时器**：新增常驻轮询=新增常驻 CPU，
			* 与 T1（dsh-model-capability 每 5s 全量 describe 造成 24-26% CPU）同族，明确禁止。
			*/
			const resetShizuku = (0, react.useCallback)(() => {
				var _window$androidBridge31;
				runShizukuAction(((_window$androidBridge31 = window.androidBridge) === null || _window$androidBridge31 === void 0 ? void 0 : _window$androidBridge31.resetShizukuConnection) ? () => window.androidBridge.resetShizukuConnection() : void 0, "已重置 Shizuku 连接，正在重新建立通道（本页每 2 秒自动重扫）。", "重置 Shizuku 连接失败");
			}, [runShizukuAction]);
			/**
			* issue #262：root 授权面三个动作（开关 / 「已阅读」确认 / 免责声明）的共同收口。
			*
			* 结算口径沿用 [settleLinkCall]（壳侧回 {ok, code/guidance}），写后立刻 [refreshRootGrant] 回读——
			* 不新造结算口径、不新开定时器（2s 轮询已由 useShellState 承担）。
			*/
			const runRootAction = (0, react.useCallback)((call, okText, failLead, preRaw) => {
				let raw = preRaw;
				if (raw === void 0) try {
					raw = call === null || call === void 0 ? void 0 : call();
				} catch {
					raw = void 0;
				}
				const settled = settleLinkCall(raw, okText, failLead);
				setRootOk(settled.ok);
				setRootMsg(settled.text);
				refreshRootGrant();
				refreshRootAccess();
			}, [refreshRootGrant, refreshRootAccess]);
			const toggleRootGrant = (0, react.useCallback)((next) => {
				setPendingEnable(false);
				let raw;
				try {
					var _window$androidBridge32;
					raw = ((_window$androidBridge32 = window.androidBridge) === null || _window$androidBridge32 === void 0 ? void 0 : _window$androidBridge32.setRootGranted) ? window.androidBridge.setRootGranted(next) : void 0;
				} catch {
					raw = void 0;
				}
				const parsed = parseRootReply(raw);
				if (next && (parsed === null || parsed === void 0 ? void 0 : parsed.code) === "request-started") {
					setPendingEnable(true);
					setRootOk(true);
					setRootMsg("正在检测 root 授权。若没有弹窗，请在你使用的 Root 管理器中允许本应用；授权后会继续本次开启操作。");
					refreshRootGrant();
					refreshRootAccess();
					return;
				}
				runRootAction(void 0, next ? "已开启 AI root 权限：特权通道按 root 身份执行，请在需要时使用、用完即关。" : "已关闭 AI root 权限：特权通道已恢复整体拒绝。", next ? "开启 AI root 权限失败" : "关闭 AI root 权限失败", raw);
			}, [
				runRootAction,
				refreshRootGrant,
				refreshRootAccess
			]);
			/**
			* 检测 / 尝试获取 root 授权（壳侧后台跑一次 `su -c id`；本页 2s 轮询看到结果）。
			*
			* ★口径（2026-09-30 主人指正）：**多数 Root 管理器不再自动弹授权框**（除 Magisk 外，
			* 用户得自己打开管理器授予）✗ ⇒ 文案**不承诺"会弹窗"**，只承诺"取一次真实身份并如实回报" ✓。
			*/
			const requestRoot = (0, react.useCallback)(() => {
				var _window$androidBridge33;
				runRootAction(((_window$androidBridge33 = window.androidBridge) === null || _window$androidBridge33 === void 0 ? void 0 : _window$androidBridge33.requestRootAccess) ? () => window.androidBridge.requestRootAccess() : void 0, "已发起 root 授权检测，结果会自动刷新；检测成功不代表 AI root 开关已开启。", "检测 root 授权未通过");
				refreshRootAccess();
			}, [runRootAction, refreshRootAccess]);
			const [repairReply, setRepairReply] = (0, react.useState)();
			const [repairFailure, setRepairFailure] = (0, react.useState)();
			const polledRepair = rootGrant.ownership ?? OWNERSHIP_UNREADABLE;
			const repairState = polledRepair.readable && polledRepair.startedAt >= ((repairReply === null || repairReply === void 0 ? void 0 : repairReply.startedAt) ?? 0) ? polledRepair : repairReply ?? polledRepair;
			const repairFeedback = repairFailure === void 0 ? describeOwnershipRepair(repairState) : {
				ok: false,
				text: repairFailure
			};
			/** Native request is asynchronous and single-flight; only later native result counts as completed. */
			const repairOwnership = (0, react.useCallback)(() => {
				let raw;
				try {
					var _window$androidBridge34, _window$androidBridge35;
					raw = (_window$androidBridge34 = window.androidBridge) === null || _window$androidBridge34 === void 0 || (_window$androidBridge35 = _window$androidBridge34.repairRootOwnership) === null || _window$androidBridge35 === void 0 ? void 0 : _window$androidBridge35.call(_window$androidBridge34);
				} catch {
					raw = void 0;
				}
				const parsed = parseRootReply(raw);
				const observed = readOwnershipState(parsed);
				if ((parsed === null || parsed === void 0 ? void 0 : parsed.ok) === true && (parsed.code === "repair-started" || parsed.code === "repair-running") && observed.readable && observed.startedAt > 0 && (observed.running || observed.completedAt >= observed.startedAt && observed.result !== void 0)) {
					setRepairReply(observed);
					setRepairFailure(void 0);
				} else setRepairFailure("启动文件属主维护失败：" + describeCallReason(typeof (parsed === null || parsed === void 0 ? void 0 : parsed.reason) === "string" ? parsed.reason : void 0));
				refreshRootGrant();
			}, [refreshRootGrant]);
			/**
			* 自动续开（2026-09-30 主人定例的体验闭环）：用户开开关 → 壳侧因「root 未授权」拦下并
			* **弹出授权框** → 用户点「允许」→ 本页 2s 轮询看到 `rootAccess.granted` → 自动把开关续开。
			* 用户只需点一次开关 + 在弹窗上点一次「允许」，不必回设置页再点一次。
			*/
			(0, react.useEffect)(() => {
				if (!pendingEnable) return;
				if (!rootGrant.consentValid || [
					"denied",
					"timeout",
					"no-su"
				].includes(rootAccess.state)) {
					setPendingEnable(false);
					return;
				}
				if (rootGrant.granted) {
					setPendingEnable(false);
					return;
				}
				if (rootAccess.granted) {
					setPendingEnable(false);
					toggleRootGrant(true);
				}
			}, [
				pendingEnable,
				rootGrant.granted,
				rootGrant.consentValid,
				rootAccess.granted,
				rootAccess.state,
				toggleRootGrant
			]);
			const toggleRootConsent = (0, react.useCallback)((next) => {
				var _window$androidBridge36;
				runRootAction(((_window$androidBridge36 = window.androidBridge) === null || _window$androidBridge36 === void 0 ? void 0 : _window$androidBridge36.setRootConsent) ? () => window.androidBridge.setRootConsent(next) : void 0, next ? "已记录「已阅读」——与当前版本绑定，升级后需重新确认。" : "已撤销同意，并同时关闭了 AI root 权限。", next ? "记录「已阅读」失败" : "撤销同意失败");
			}, [runRootAction]);
			const openRootDisclaimer = (0, react.useCallback)(() => {
				var _window$androidBridge37;
				runRootAction(((_window$androidBridge37 = window.androidBridge) === null || _window$androidBridge37 === void 0 ? void 0 : _window$androidBridge37.openRootDisclaimer) ? () => window.androidBridge.openRootDisclaimer() : void 0, "已打开免责声明（APK 内置文档，离线可读）。", "打开免责声明失败");
			}, [runRootAction]);
			const tapForce = (0, react.useCallback)(() => {
				const next = confirmStage + 1;
				if (next < 3) {
					setConfirmStage(next);
					setForceMsg(null);
					return;
				}
				setConfirmStage(0);
				try {
					var _window$androidBridge38, _window$androidBridge39;
					const raw = (_window$androidBridge38 = window.androidBridge) === null || _window$androidBridge38 === void 0 || (_window$androidBridge39 = _window$androidBridge38.forceDestroyVdisplay) === null || _window$androidBridge39 === void 0 ? void 0 : _window$androidBridge39.call(_window$androidBridge38);
					const parsed = raw ? JSON.parse(raw) : void 0;
					const ok = (parsed === null || parsed === void 0 ? void 0 : parsed.ok) === true;
					setForceMsg(ok ? {
						ok: true,
						text: "已强制销毁全部虚拟屏。"
					} : {
						ok: false,
						text: "销毁失败：" + describeCallReason(parsed === null || parsed === void 0 ? void 0 : parsed.code),
						code: String((parsed === null || parsed === void 0 ? void 0 : parsed.code) ?? "unknown")
					});
				} catch {
					setForceMsg({
						ok: false,
						text: "销毁调用失败（应用内桥不可用）——请重新打开应用后重试。",
						code: "bridge-threw"
					});
				}
				refreshVdisplay();
			}, [confirmStage, refreshVdisplay]);
			const forceLabel = confirmStage === 0 ? "强制销毁虚拟屏" : "再次点击确认（" + confirmStage + "/3）";
			const forceArmed = confirmStage > 0;
			const cancelForce = (0, react.useCallback)(() => {
				setConfirmStage(0);
				setForceMsg(null);
			}, []);
			const canOpenShizuku = shizuku.readable && shizuku.installed;
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("section", {
				className: "dsh-screen-control-card",
				"aria-labelledby": "dsh-phone-control-title",
				...vdisplay.displayId === void 0 ? {} : { "data-display-id": String(vdisplay.displayId) },
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("header", {
						className: "dsh-screen-control-header",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", {
							id: "dsh-phone-control-title",
							children: "手机控制"
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: "屏幕与特权通道的授权面；模型不能自行更改这里的任何设置。" })] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
							className: "dsh-screen-control-state",
							"data-state": vdisplay.state,
							children: STATUS_LABEL[vdisplay.state]
						})]
					}),
					vdisplay.guidance !== shizuku.guidance ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: vdisplay.guidance
					}) : null,
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-screen-control-detail",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "Shizuku 特权通道" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
							"data-code": shizuku.readable ? void 0 : "shizuku-status-unreadable",
							children: shizukuStateLabel(shizuku)
						})]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: shizuku.guidance !== "" ? shizuku.guidance : shizukuStepHint(shizuku)
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row dsh-dev-split",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: downloadShizuku,
							children: "下载 Shizuku"
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							disabled: !canOpenShizuku,
							onClick: openShizuku,
							children: "打开 Shizuku"
						})]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "两个入口都会跳到应用外（系统浏览器 / Shizuku 应用）。装好并授权后回到本页即可—— 状态每 2 秒自动刷新，不需要手动操作。授权只能在 Shizuku 内由你亲手完成。"
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-link",
								onClick: tutorialShizuku,
								children: "点击查看教程"
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn",
								onClick: refreshShizuku,
								children: "刷新状态"
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: "dsh-dev-btn",
								onClick: resetShizuku,
								children: "重置链接"
							})
						]
					}),
					shizuku.granted ? null : /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							disabled: !shizuku.readable || !shizuku.running,
							onClick: requestShizukuPermission,
							children: "请求 Shizuku 授权"
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
							className: "dsh-dev-hint",
							children: "授权框需要前台界面才能弹出——后台自动请求会静默失败（管理器里会看不到本应用）。"
						})]
					}),
					shizukuMsg === null ? null : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: shizukuOk === true ? "dsh-dev-hint" : "dsh-dev-error",
						children: shizukuMsg
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("section", {
						className: "dsh-root-access-card",
						"aria-label": "Root 权限设置",
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
								className: "dsh-screen-control-header",
								children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "Root 权限" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: "先确认可用通道，再决定是否允许 AI 使用。" })] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
									className: "dsh-screen-control-state",
									"data-state": rootGrant.granted ? "active" : "disabled",
									children: rootGrant.granted ? "AI 已开启" : "AI 未开启"
								})]
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
								className: "dsh-root-step",
								children: [
									/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
										className: "dsh-screen-control-detail",
										children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "1. 应用 root 授权" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
											"data-code": rootAccess.readable ? rootAccess.state : "root-access-unreadable",
											children: rootAccessStateLabel(rootAccess)
										})]
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: "dsh-dev-hint",
										children: rootAccess.guidance || "检测应用的 su 授权；Shizuku 已以 root 启动时，也可直接使用该通道。"
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "button",
										className: "dsh-dev-btn",
										disabled: !canRequestRoot(rootAccess),
										onClick: requestRoot,
										children: rootAccess.requesting ? "正在检测…" : "检测 root 授权"
									}),
									rootAccess.managerInstalled ? /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
										className: "dsh-dev-hint",
										children: [
											"Root 管理器：",
											rootAccess.managerLabel,
											"。请在管理器中允许本应用，再回到这里检测。"
										]
									}) : null
								]
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
								className: "dsh-root-step",
								children: [
									/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
										className: "dsh-screen-control-detail",
										children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "2. AI root 权限" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
											"data-code": rootGrant.readable ? void 0 : "root-grant-unreadable",
											children: rootGrantStateLabel(rootGrant)
										})]
									}),
									!rootGrant.readable ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: "dsh-dev-error",
										children: "授权状态不可读。重新打开应用再试；读不到时不会允许 AI 使用 root。"
									}) : !rootGrant.channelRoot && !rootGrant.rootGranted ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: rootGrantChannelHint(rootGrant).kind === "shell-identity" ? "dsh-dev-hint" : "dsh-dev-error",
										"data-code": rootGrantChannelHint(rootGrant).kind === "shell-identity" ? "shizuku-shell-identity" : "not-root-channel",
										children: rootGrantChannelHint(rootGrant).text
									}) : /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
										className: "dsh-dev-hint",
										children: [
											"可用通道：",
											rootGrant.channelRoot ? "Shizuku root" : "应用 su root",
											"。应用获得授权不等于已允许 AI 使用。"
										]
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "button",
										className: "dsh-dev-link",
										onClick: openRootDisclaimer,
										children: "阅读《AI root 权限免责声明》"
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
										className: "dsh-screen-scope-row dsh-root-toggle-row",
										children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "已阅读免责声明" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: "理解 root 操作可能修改或删除系统与个人数据，并愿意承担相应风险。升级后需重新确认；撤销同意会同时关闭 AI 开关。" })] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
											type: "checkbox",
											"aria-label": "已阅读免责声明",
											checked: rootGrant.consentValid,
											disabled: !rootGrant.readable,
											onChange: (event) => toggleRootConsent(event.target.checked)
										})]
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
										className: "dsh-screen-scope-row dsh-root-toggle-row",
										children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "授权 AI 使用 root" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: rootGrant.honesty || "这是策略与知情同意开关，不是技术沙箱。关闭后 AI 的 root 执行路径会被拒绝。" })] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
											type: "checkbox",
											role: "switch",
											"aria-label": "授权 AI 使用 root",
											checked: rootGrant.granted,
											disabled: !rootGrant.readable || !rootGrant.granted && (!canToggleRootGrant(rootGrant) || !rootGrant.consentValid),
											onChange: (event) => toggleRootGrant(event.target.checked)
										})]
									}),
									rootGrant.readable && canToggleRootGrant(rootGrant) && !rootGrant.consentValid ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: "dsh-dev-hint",
										children: "请先阅读并确认免责声明，才能开启 AI root 权限。"
									}) : null,
									rootMsg === null ? null : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										role: "status",
										"aria-live": "polite",
										className: rootOk === true ? "dsh-dev-hint" : "dsh-dev-error",
										children: rootMsg
									})
								]
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("details", {
								className: "dsh-root-maintenance",
								children: [
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("summary", { children: "文件属主维护" }),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: "dsh-dev-hint",
										children: "root 写盘后应用无法读取文件时使用。仅修复本应用数据目录，不改变 AI 授权。"
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "button",
										className: "dsh-dev-btn",
										disabled: repairState.running || !rootAccess.granted && !rootGrant.channelRoot,
										onClick: repairOwnership,
										children: repairState.running ? "属主维护进行中" : "修复文件属主"
									}),
									repairFeedback === void 0 ? null : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										role: "status",
										"aria-live": "polite",
										className: repairFeedback.ok === false ? "dsh-dev-error" : "dsh-dev-hint",
										children: repairFeedback.text
									})
								]
							})
						]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
						className: "dsh-screen-scope-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "开放屏幕范围" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: "默认仅虚拟屏幕。真实屏幕、截图与控制都遵守此范围和完全访问权限。" })] }), /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("select", {
							"aria-label": "开放屏幕范围",
							value: scope,
							onChange: (event) => setScope(event.target.value),
							children: [
								/* @__PURE__ */ (0, react_jsx_runtime.jsx)("option", {
									value: "virtual-only",
									children: "仅虚拟屏幕"
								}),
								/* @__PURE__ */ (0, react_jsx_runtime.jsx)("option", {
									value: "real-only",
									children: "仅真实屏幕"
								}),
								/* @__PURE__ */ (0, react_jsx_runtime.jsx)("option", {
									value: "all",
									children: "全部开放"
								})
							]
						})]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
						className: "dsh-screen-scope-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "虚拟屏分辨率档位" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: "跟随真机比例并同比例缩放 densityDpi（下次建屏生效；默认 0.75，更省性能）。" })] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("select", {
							"aria-label": "虚拟屏分辨率档位",
							value: String(scale),
							onChange: (event) => setScale(Number(event.target.value)),
							children: SCALE_OPTIONS.map((option) => /* @__PURE__ */ (0, react_jsx_runtime.jsx)("option", {
								value: String(option),
								children: option === 1 ? "原生" : String(Math.round(option * 100)) + "%"
							}, option))
						})]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
						className: "dsh-screen-scope-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "虚拟屏浮窗（退后台自动显示）" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: "只读浮窗：应用切到后台时显示虚拟屏画面；前台只在侧栏可见。与开发者选项里的「悬浮球」不是同一个东西（那个是任务面板入口）。" })] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
							"aria-label": "虚拟屏浮窗（退后台自动显示）",
							type: "checkbox",
							checked: floatOn,
							onChange: (event) => setFloat(event.target.checked)
						})]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-screen-control-detail",
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "无障碍通道" }),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: !a11y.readable ? "状态不可读" : a11y.enabled ? "已开启（语义读取/点击/输入）" : "未开启（推荐开启）" }),
							a11y.hint !== "" ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: a11y.hint }) : null
						]
					}),
					a11y.readable && !a11y.enabled && a11y.restrictedSettingsApplies ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "Android 13 及以上对侧载应用默认开启「受限设置」：系统页里本应用的开关会是灰的。 先点「解锁受限设置」（经 Shizuku 特权通道，只影响本应用这一项），再回去开启。"
					}) : null,
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: openA11y,
							children: "去开启无障碍服务"
						}), a11y.readable && !a11y.enabled && a11y.restrictedSettingsApplies ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: unlockA11y,
							children: "解锁受限设置"
						}) : null]
					}),
					a11yMsg === null ? null : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: a11yOk === true ? "dsh-dev-hint" : "dsh-dev-error",
						children: a11yMsg
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-screen-control-detail",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", { children: "强制销毁虚拟屏" }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: "销毁全部虚拟屏与其上的任务（无视会话归属）；需连续点击三次确认，点错可取消。" })]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: forceArmed ? "dsh-dev-btn dsh-dev-danger" : "dsh-dev-btn",
							"data-stage": confirmStage,
							"data-armed": forceArmed ? "true" : "false",
							onClick: tapForce,
							children: forceLabel
						}), forceArmed ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-link",
							onClick: cancelForce,
							children: "取消"
						}) : null]
					}),
					forceMsg === null ? null : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: forceMsg.ok ? "dsh-dev-hint" : "dsh-dev-error",
						...forceMsg.code === void 0 ? {} : { "data-code": forceMsg.code },
						children: forceMsg.text
					})
				]
			});
		}
		//#endregion
		//#region src/client/dev-section/notify-settings.tsx
		/**
		* 通知设置行（0.14.1 块J FIX-4 的页面半）。
		*
		* 背景（J-1「FIX-4 名义落地、实际不可达」）：壳侧 `NotifyCenter.settingsSnapshot` /
		* `applySetting` / `onSuppressForegroundChanged` 三个入口在 `app/src/main` 全仓**零外部调用点**
		* （只有定义处互调），`AndroidBridge.kt` 的 35 个 `@JavascriptInterface` 无一涉及 notify/suppress，
		* 本目录亦 0 命中——能力在、入口无，与它要修的缺陷同形复发。本组件补上页面侧入口。
		*
		* 通道选择：桥方法（`window.androidBridge.getNotifySetting` / `setNotifySetting`）而不是 `/api` 路由。
		* 理由是**真源位置**：这些设置落在 Android 侧 `SharedPreferences("dsh-notify")`，引擎侧插件进程
		* 读不到它；桥是同一宿主内的唯一可达通道，与 `setImmersiveMode` / `setDevLogEnabled` /
		* `setOverlayEnabled` / `setVdisplayScale` 等既有设置面完全同构。因此不新增 `/api` 路由，
		* `scripts/api-route-auth-policy.json` 无需登记（该门禁只约束 `/api` 注册）。
		*
		* 数据面纪律（与 `GeneralSettings` / `DevSection` 同款）：
		*  - 全部状态经 `useShellState` 订阅（挂载读 + 可见/回前台重读），不在 `useState` 初值器里裸读桥；
		*  - **写后回读**：`setNotifySetting` 的返回带 `applied`，只有 `applied=true` 才展示为新值，
		*    否则保留壳侧回读值并如实显示失败原因（拒绝乐观置位）。
		*/
		/**
		* 分类展示名 + **关掉会怎样**（与 `NotifyCenter.Face` 的五类一一对应；顺序即 UI 顺序）。
		*
		* 为什么每行必须带后果（0.14.1 批 4 / P0-5）：旧界面是五个纯标签开关，一行解释都没有。
		* 而关掉「提问 / 授权请求」的实际后果远重于其它三类——引擎侧 `ask_user_question` 与授权请求
		* **没有超时**，用户以为「少点打扰」，实际是任务永久挂起（现象是「AI 不动了」）。
		* 壳侧已把这类的关闭语义改成「不弹窗、不响铃，但仍投递到通知栏可作答」；文案必须如实说明这一点，
		* 否则用户仍在按旧语义做决定。
		*/
		const CATEGORY_LABELS = [
			[
				"report",
				"工作汇报",
				"关闭后不再提醒；任务本身不受影响"
			],
			[
				"question",
				"提问",
				"关闭 = 不弹窗、不响铃；提问仍会出现在通知栏、可直接作答（AI 在等你的回答）"
			],
			[
				"approval",
				"授权请求",
				"关闭 = 不弹窗、不响铃；仍需你在通知栏或应用内批准，工具不会自动放行"
			],
			[
				"todo",
				"待办进度",
				"关闭后不再显示步骤进度；任务本身不受影响"
			],
			[
				"silent",
				"后台动态",
				"关闭后不再显示看门狗与引擎状态；只影响提示，不影响引擎"
			]
		];
		/** 读自检；桥缺席/不可解析返回 null（页面显示「不可用」，不伪造）。 */
		function readSelfCheck() {
			try {
				var _window$androidBridge, _window$androidBridge2;
				const raw = (_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 || (_window$androidBridge2 = _window$androidBridge.notifySelfCheck) === null || _window$androidBridge2 === void 0 ? void 0 : _window$androidBridge2.call(_window$androidBridge);
				if (raw === void 0 || raw === "") return null;
				const value = JSON.parse(raw);
				if (value === null || typeof value !== "object") return null;
				if (value.ok === false) return null;
				return value;
			} catch {
				return null;
			}
		}
		/** 解析桥返回；不可解析/桥缺席一律返回 null（调用方据此显示「不可用」，不伪造状态）。 */
		function parseSnapshot(raw) {
			if (raw === void 0 || raw === "") return null;
			try {
				const value = JSON.parse(raw);
				if (value === null || typeof value !== "object") return null;
				const snapshot = value;
				if (snapshot.ok === false) return null;
				return snapshot;
			} catch {
				return null;
			}
		}
		/** 读壳侧真源（每次调用现读；桥缺席返回 undefined）。 */
		function readSettings() {
			try {
				var _window$androidBridge3, _window$androidBridge4;
				return (_window$androidBridge3 = window.androidBridge) === null || _window$androidBridge3 === void 0 || (_window$androidBridge4 = _window$androidBridge3.getNotifySetting) === null || _window$androidBridge4 === void 0 ? void 0 : _window$androidBridge4.call(_window$androidBridge3, "");
			} catch {
				return;
			}
		}
		/**
		* 「通知」设置块：前台抑制开关 + 五类分类开关。
		* @returns 该设置分区内的一个功能块；桥不可用时只显示一行不可用说明。
		*/
		function NotifySettingsRow() {
			const [raw, refresh] = useShellState(readSettings, { pollMs: 0 });
			const [message, setMessage] = (0, react.useState)(null);
			const [selfCheck, setSelfCheck] = (0, react.useState)(null);
			const [selfCheckTried, setSelfCheckTried] = (0, react.useState)(false);
			const snapshot = parseSnapshot(raw);
			/** 写一项并**按返回读回**（applied=false 即未生效，不乐观置位）。 */
			const write = (0, react.useCallback)((key, value) => {
				let reply = null;
				try {
					var _window$androidBridge5, _window$androidBridge6;
					reply = parseSnapshot((_window$androidBridge5 = window.androidBridge) === null || _window$androidBridge5 === void 0 || (_window$androidBridge6 = _window$androidBridge5.setNotifySetting) === null || _window$androidBridge6 === void 0 ? void 0 : _window$androidBridge6.call(_window$androidBridge5, key, value));
				} catch {
					reply = null;
				}
				if (reply === null) {
					setMessage({ text: "写入失败：应用内连接不可用（仅安卓应用内可用）——请重新打开应用后重试" });
					refresh();
					return;
				}
				if (reply.applied !== true) {
					setMessage({
						text: describeNotifyWriteFailure(reply.reason),
						...reply.reason === void 0 ? {} : { code: reply.reason }
					});
					refresh();
					return;
				}
				setMessage(null);
				refresh();
			}, [refresh]);
			if (snapshot === null) return /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
				className: "dsh-dev-notify",
				"data-plugin": "dev-notify-settings",
				children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
					className: "dsh-dev-hint",
					children: "通知设置不可用（桥未装配或壳侧上下文未绑定）。"
				})
			});
			const suppress = snapshot.suppressForeground === true;
			const isDefault = suppress === (snapshot.suppressForegroundDefault === true);
			const categories = snapshot.categories ?? {};
			const channelRows = (selfCheck === null || selfCheck === void 0 ? void 0 : selfCheck.channels) ?? [];
			const degradedRows = channelRows.filter((row) => row.degraded === true);
			/** 渠道档位文本：壳侧人话优先，缺失才回退到页面侧的数值翻译（快照可能比 APK 新）。 */
			const importanceTextOf = (row) => row.importanceLabel !== void 0 && row.importanceLabel !== "" ? row.importanceLabel : describeImportance(row.importance);
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: "dsh-dev-notify",
				"data-plugin": "dev-notify-settings",
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
						className: "dsh-dev-row dsh-dev-switch",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
							type: "checkbox",
							role: "switch",
							"aria-label": "前台抑制通知",
							checked: suppress,
							onChange: (event) => {
								write("suppressForeground", event.target.checked);
							}
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: "应用在前台时不弹工作汇报（改为延后，回后台补投）" })]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
						className: "dsh-dev-hint",
						children: [
							"当前：",
							suppress ? "前台抑制开启（工作汇报延后）" : "前台照常推送",
							isDefault ? "；等于本版默认值" : "；已偏离本版默认值"
						]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "关闭抑制（默认）即「前台也发系统通知」；开启后命中的工作汇报进入待投队列， 回到后台或再次关闭抑制时补投（队列有 TTL 与容量上限）。提问与授权请求永不受此项影响。"
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: "dsh-dev-notify-cats",
						children: CATEGORY_LABELS.map(([key, label, consequence]) => /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
							className: "dsh-dev-notify-cat",
							children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
								className: "dsh-dev-row dsh-dev-switch",
								children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
									type: "checkbox",
									role: "switch",
									"aria-label": label,
									checked: categories[key] === true,
									onChange: (event) => {
										write("cat." + key, event.target.checked);
									}
								}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: label })]
							}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
								className: "dsh-dev-hint",
								children: consequence
							})]
						}, key))
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-row dsh-dev-split",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: () => {
								setSelfCheckTried(true);
								setSelfCheck(readSelfCheck());
							},
							children: "通知自检"
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: () => {
								let ok = false;
								try {
									var _window$androidBridge7, _window$androidBridge8;
									ok = ((_window$androidBridge7 = window.androidBridge) === null || _window$androidBridge7 === void 0 || (_window$androidBridge8 = _window$androidBridge7.openNotifyAppSettings) === null || _window$androidBridge8 === void 0 ? void 0 : _window$androidBridge8.call(_window$androidBridge7)) === true;
								} catch {
									ok = false;
								}
								if (!ok) setMessage({ text: "该系统没有「应用通知设置」页——请在系统设置里手动找到本应用的通知项" });
							},
							children: "系统通知设置"
						})]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: "dsh-dev-row",
						children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: "dsh-dev-btn",
							onClick: () => {
								let posted = 0;
								try {
									var _window$androidBridge9, _window$androidBridge10;
									posted = ((_window$androidBridge9 = window.androidBridge) === null || _window$androidBridge9 === void 0 || (_window$androidBridge10 = _window$androidBridge9.notifySendTest) === null || _window$androidBridge10 === void 0 ? void 0 : _window$androidBridge10.call(_window$androidBridge9)) ?? 0;
								} catch {
									posted = 0;
								}
								setMessage(posted > 0 ? { text: "已发送 " + String(posted) + " 条测试通知（五类各一条）——请到通知栏看哪几条真的到了、哪几条是静默的" } : { text: "一条也没发出去：通知权限未授予或渠道不可用——请先在上面的自检里看渠道状态，并到系统设置里允许通知" });
							},
							children: "发送测试通知"
						})
					}),
					selfCheckTried && selfCheck === null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-warn",
						children: "自检不可用（桥未装配或壳侧上下文未绑定）。"
					}),
					selfCheck !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: "dsh-dev-notify-check",
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
								className: "dsh-dev-hint",
								children: [
									"应用通知总开关：",
									selfCheck.notificationsEnabled === false ? "系统已关闭" : "已开启",
									"；",
									"通知权限：",
									selfCheck.permissionLabel ?? (selfCheck.permissionGranted === true ? "已授予" : "未授予（任务完成不会提醒）")
								]
							}),
							degradedRows.length > 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
								className: "dsh-dev-warn",
								children: [
									"系统已降级以下通知：「",
									degradedRows.map((row) => row.label ?? row.category ?? "未知渠道").join("、"),
									"」—— 应用无法调回，需在系统设置里恢复（可从右侧按钮进入）。"
								]
							}) : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
								className: "dsh-dev-hint",
								children: "系统未降级任何通知渠道。"
							}),
							channelRows.map((c) => /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
								className: "dsh-dev-row dsh-dev-check-row",
								children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [
									c.label ?? c.category ?? "未知渠道",
									"：",
									c.degraded === true ? "系统已降级" : c.exists === false ? "渠道不存在" : "正常",
									importanceTextOf(c) === "" ? "" : "（" + importanceTextOf(c) + "）"
								] }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
									type: "button",
									className: "dsh-dev-link",
									onClick: () => {
										const channelId = String(c.selected ?? "");
										let ok = false;
										try {
											var _window$androidBridge11, _window$androidBridge12;
											ok = channelId !== "" && ((_window$androidBridge11 = window.androidBridge) === null || _window$androidBridge11 === void 0 || (_window$androidBridge12 = _window$androidBridge11.openNotifyChannelSettings) === null || _window$androidBridge12 === void 0 ? void 0 : _window$androidBridge12.call(_window$androidBridge11, channelId)) === true;
										} catch {
											ok = false;
										}
										if (!ok) setMessage({ text: "无法打开该渠道的系统设置页——请在系统设置里手动查找" });
									},
									children: "打开该渠道设置"
								})]
							}, String(c.selected ?? c.category)))
						]
					}),
					message !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-warn",
						...noticeDataAttrs(message),
						children: message.text
					})
				]
			});
		}
		/**
		* 「通知」设置分区（0.14.1 批 3 / P3-5）。
		*
		* 真因：本块此前的**唯一入口**在「开发者选项」里（`index.ts` 的 `settings.section` id
		* `android-dev`）。而「关掉提问提醒 = 引擎的提问被静默丢弃、任务永久挂起」这类后果，
		* 是**每个用户**都要面对的决定，把它埋在开发者选项等于对普通用户不可达
		* （审查档 §3.3 第 14 行：入口埋在开发者选项）。
		*
		* 现在的层级：设置页一级分区「通知」（`android-notify`，order 97），与「手机控制」（98）并列；
		* 开发者选项里保留一行**指路**文案，不重复渲染同一组开关（同功能双实现是审查档 §5 的结构性根因）。
		*
		* @returns 该设置分区的内容列。
		*/
		function NotifySettingsSection(_props) {
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("section", {
				className: "dsh-screen-control-card",
				"aria-labelledby": "dsh-notify-title",
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("header", {
					className: "dsh-screen-control-header",
					children: /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("strong", {
						id: "dsh-notify-title",
						children: "通知"
					}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("small", { children: "任务汇报、向你提问与授权请求的提醒方式；模型不能自行更改这里的任何设置。" })] })
				}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)(NotifySettingsRow, {})]
			});
		}
		//#endregion
		//#region src/client/dev-section/dev-section.css.ts
		/**
		* Developer-options settings-page styles: reuse --dsw-* semantic tokens (auto light/dark), buttons in
		* a wrapping row layout; on narrow screens (mobile form) buttons become a two-column grid.
		* Injection matches mobile-settings.css.ts (style tag + data-plugin attribute; this page's root
		* selector uses [data-plugin='dev-section'] against class-hash churn).
		*/
		const DEV_SECTION_CSS = `
[data-plugin='dev-section'] {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0 12px;
}

.dsh-dev-note {
  margin: 0;
  font-size: 13px;
  line-height: 20px;
  color: var(--dsw-alias-label-secondary);
}

.dsh-dev-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

/* 并列半行宽按钮行（0.14.1 Shizuku 引导：左「下载」右「打开」）。
 * 两侧等宽平分（flex: 1 1 0），窄屏 media query 里的 calc(50% - 5px) 被这条的
 * 更高优先级覆盖——用户定例是「无论宽窄都并列半行」。
 * 高度单列抬高到 44px：这两个按钮是「跳出去办一件事」，比本页其余按钮更需要点得准。 */
.dsh-dev-split > .dsh-dev-btn {
  flex: 1 1 0;
  min-height: 44px;
  text-align: center;
}

/* 文字链（0.14.1：蓝色下划线的「点击查看教程」）。
 * 用 button 而非 a[href]：跳转由壳侧发起（外部浏览器），页面不导航；
 * button 天然可键盘聚焦、可回车触发，且不会出现「点了页面自己跳走」。
 * 颜色写显式品牌蓝，**不取 --dsw-alias-brand-primary**——该 token 深色主题下实测近白
 * （与 reference-menu.ts 同一处理），会变成白底白字。 */
.dsh-dev-link {
  min-height: 40px;
  padding: 8px 2px;
  border: none;
  background: none;
  color: #4d6bfe;
  font-size: 13px;
  line-height: 20px;
  text-decoration: underline;
  text-underline-offset: 2px;
  cursor: pointer;
}

.dsh-dev-btn {
  min-height: 36px;
  padding: 6px 14px;
  border: 1px solid var(--dsw-alias-border-l3);
  border-radius: 8px;
  background: var(--dsw-alias-button-elevated-fill);
  color: var(--dsw-alias-label-primary);
  font-size: 13px;
  line-height: 20px;
  cursor: pointer;
}

.dsh-dev-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.dsh-dev-danger {
  border-color: var(--dsw-alias-state-error-primary);
  color: var(--dsw-alias-state-error-primary);
}

.dsh-dev-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2147483000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  padding: calc(20px + var(--dsh-mobile-top-inset, 0px)) 20px 20px;
}

.dsh-dev-modal {
  width: 100%;
  max-width: 360px;
  padding: 18px 20px;
  border: 1px solid var(--dsw-alias-border-l3);
  border-radius: 12px;
  background: var(--dsw-alias-bg-layer-2);
  box-shadow: var(--dsw-elevation-prominent);
}

.dsh-dev-modal-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--dsw-alias-label-primary);
}

.dsh-dev-modal-desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 20px;
  color: var(--dsw-alias-label-secondary);
}

.dsh-dev-modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.dsh-dev-switch {
  font-size: 14px;
  color: var(--dsw-alias-label-primary);
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
}

.dsh-dev-switch input {
  width: 16px;
  height: 16px;
  margin: 0;
}

.dsh-screen-scope-row {
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px solid var(--dsw-alias-border-l2);
}
.dsh-screen-scope-row > span {
  display: grid;
  gap: 4px;
  min-width: 0;
  color: var(--dsw-alias-label-primary);
  font-size: 14px;
}
.dsh-screen-scope-row small {
  color: var(--dsw-alias-label-secondary);
  font-size: 12px;
  line-height: 18px;
}
.dsh-screen-scope-row select {
  min-height: 34px;
  max-width: min(100%, 190px);
  padding: 0 28px 0 10px;
  border: 1px solid var(--dsw-alias-border-l4);
  border-radius: 8px;
  background: var(--dsw-alias-bg-layer-2);
  color: var(--dsw-alias-label-primary);
  font: inherit;
}

.dsh-screen-control-card {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--dsw-alias-border-l3);
  border-radius: 12px;
  background: var(--dsw-alias-bg-layer-2);
}
.dsh-screen-control-header {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: flex-start;
}
.dsh-screen-control-header > span:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
  color: var(--dsw-alias-label-primary);
  font-size: 14px;
}
.dsh-screen-control-header small {
  color: var(--dsw-alias-label-secondary);
  font-size: 12px;
  line-height: 18px;
}
.dsh-screen-control-state {
  flex: 0 0 auto;
  padding: 3px 8px;
  border: 1px solid var(--dsw-alias-border-l4);
  border-radius: 999px;
  color: var(--dsw-alias-label-secondary);
  font-size: 12px;
  line-height: 18px;
}
.dsh-screen-control-state[data-state='ready'],
.dsh-screen-control-state[data-state='active'] {
  border-color: var(--dsw-alias-link);
  color: var(--dsw-alias-link);
}
.dsh-screen-control-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  color: var(--dsw-alias-label-secondary);
  font-size: 12px;
  line-height: 18px;
}
.dsh-screen-control-detail strong { color: var(--dsw-alias-label-primary); }

.dsh-root-access-card {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--dsw-alias-border-l3);
  border-radius: 12px;
  background: var(--dsw-alias-bg-layer-2);
}
.dsh-root-access-card p { margin: 0; overflow-wrap: anywhere; }
.dsh-root-access-card .dsh-screen-control-state { max-width: 45%; text-align: center; }
.dsh-root-step { display: grid; gap: 8px; min-width: 0; }
.dsh-root-step + .dsh-root-step { padding-top: 12px; border-top: 1px solid var(--dsw-alias-border-l2); }
.dsh-root-access-card .dsh-dev-btn { min-height: 44px; width: 100%; }
.dsh-root-access-card .dsh-dev-link { min-height: 44px; justify-self: start; text-align: left; }
.dsh-root-toggle-row { display: flex; align-items: center; gap: 14px; min-height: 44px; }
.dsh-root-toggle-row > span { flex: 1 1 auto; }
.dsh-root-toggle-row > input { flex: 0 0 auto; width: 24px; height: 24px; margin: 0; accent-color: #4d6bfe; }
.dsh-root-maintenance { padding-top: 8px; border-top: 1px solid var(--dsw-alias-border-l2); }
.dsh-root-maintenance summary { min-height: 44px; display: flex; align-items: center; cursor: pointer; color: var(--dsw-alias-label-primary); }
.dsh-root-maintenance[open] { display: grid; gap: 8px; }
.dsh-root-access-card button:focus-visible,
.dsh-root-access-card input:focus-visible,
.dsh-root-maintenance summary:focus-visible { outline: 2px solid #4d6bfe; outline-offset: 3px; }
@media (min-width: 640px) {
  .dsh-root-access-card .dsh-dev-btn { width: auto; justify-self: start; min-width: 160px; }
}

.dsh-dev-error {
  color: var(--dsw-alias-state-error-primary);
  font-size: 12px;
  line-height: 18px;
}

.dsh-dev-label {
  font-size: 14px;
  color: var(--dsw-alias-label-primary);
  min-width: 64px;
}

.dsh-dev-value {
  font-size: 13px;
  color: var(--dsw-alias-label-secondary);
  min-width: 44px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.dsh-dev-row input[type='range'] {
  flex: 1;
  min-width: 120px;
}

.dsh-dev-hint {
  margin: 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--dsw-alias-label-secondary);
}

.dsh-dev-warn {
  margin: 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--dsw-alias-state-error-primary);
}

/* 0.14.1 块 E：运行时缓存清理块（清单行 + 跳过明细折叠）。标签一律为
 * $DSH_HOME/$DSH_FILES_DIR 形态，绝不出现应用私有目录的绝对路径。 */
.dsh-dev-cache {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--dsw-alias-border-l3);
  border-radius: 12px;
  background: var(--dsw-alias-bg-layer-2);
}

.dsh-dev-cache-list {
  margin: 0;
  padding-left: 18px;
  max-height: 180px;
  overflow: auto;
  font-size: 12px;
  line-height: 18px;
  color: var(--dsw-alias-label-secondary);
  word-break: break-all;
}

/* 0.14.1 块J FIX-4：通知设置块（前台抑制 + 五类分类开关）。 */
.dsh-dev-notify {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--dsw-alias-border-l3);
  border-radius: 12px;
  background: var(--dsw-alias-bg-layer-2);
}

.dsh-dev-notify-cats {
  display: grid;
  gap: 4px;
}

/* 每个分类开关 + 它的「关掉会怎样」说明（批 4 / P0-5：纯标签开关会让用户按旧语义做决定）。 */
.dsh-dev-notify-cat {
  display: grid;
  gap: 2px;
  padding: 4px 0;
}

.dsh-dev-notify-cat .dsh-dev-hint {
  margin: 0 0 0 2px;
}

/* #43 深色兜底（2026-08-18 首修 / 2026-09-25 二次修）。
 *
 * 首修把这一块挂在 @media (prefers-color-scheme: dark)：本机实测三个 prefers-color-scheme
 * 查询（dark/light/no-preference）全为 false，所以那块兜底从未生效。主题真源是
 * body[data-ds-dark-theme]（壳侧另写 html[data-ds-theme-source]='dark'），上游
 * design-platform.css 自己的深色令牌表也挂这个选择器 —— 故本节改挂同一选择器。
 *
 * 上方基础规则已全部改用本代存在的自适应令牌（label-primary / label-secondary /
 * bg-layer-2 / border-l3 / button-elevated-fill / state-error-primary），亮暗两套取值由上游
 * 按主题各自定义。因此本节在正常宿主下不改变任何取值，只在整张令牌表缺席的宿主
 * （非 Android WebView、无 dsh 主题表）里兜住深色：回退值一律取深色，绝不再退回
 * #fff/#ccc 这类亮色硬编码，否则就是 #43 的白底白字复发。 */
body[data-ds-dark-theme] .dsh-dev-btn,
body[data-ds-dark-theme] .dsh-dev-modal {
  background: var(--dsw-alias-bg-layer-2, #26262b);
  border-color: var(--dsw-alias-border-l3, #3a3a42);
}
body[data-ds-dark-theme] .dsh-dev-warn,
body[data-ds-dark-theme] .dsh-dev-danger,
body[data-ds-dark-theme] .dsh-dev-error {
  border-color: var(--dsw-alias-state-error-primary, #f25a5a);
  color: var(--dsw-alias-state-error-primary, #f25a5a);
}

@media (max-width: 639px) {
  .dsh-dev-btn {
    flex: 1 1 calc(50% - 5px);
    text-align: center;
  }
}

/* 通知自检面（批 4）：每渠道一行「系统实际状态 + 直达系统设置」——此前这条信息零调用点。 */
.dsh-dev-notify-check {
  display: grid;
  gap: 4px;
  margin-top: 8px;
}

.dsh-dev-check-row {
  align-items: center;
  gap: 8px;
}
`;
		//#endregion
		//#region src/client/general-settings/GeneralSettings.tsx
		/**
		* General-settings additions for the Android shell (issue #59): the upstream
		* Settings → General section lost the Android-only immersive status-bar toggle.
		* The shell bridge exists (androidBridge.getImmersiveMode / setImmersiveMode,
		* whose truth source is the shell's ShellState.ImmersiveMode) and the row
		* registers at the upstream settings.general.item extension point (auto
		* projected into the General section nav), mirroring DevSection.
		*
		* 0.13.3 (D6 收益省略): the font-size slider (WebView textZoom, 50–200%)
		* retired — upstream ui-theme now ships a native fontSize field (12–17px
		* content font size) rendered in the Appearance section with persistence.
		* The shell's setTextZoom bridge and persistence were removed with it.
		*
		* ST-10: the value is the bridge's getImmersiveMode() (shell pref is the truth
		* source). The localStorage key (dsh.android.immersive) is only a fallback for
		* hosts without that bridge (desktop / older shells), and this page is its sole
		* writer — no injected index.html script writes it.
		*
		* ST-09: the read goes through useShellState (mount + visible/foreground
		* re-read + write-then-read-back), never a one-shot bridge read.
		*/
		const IMMERSIVE_KEY = "dsh.android.immersive";
		/**
		* Immersive initial value (ST-10): the shell bridge is the sole truth source
		* (ShellState.ImmersiveMode); the localStorage mirror is only the fallback for
		* hosts without that bridge, and the default stays true (the shell's default).
		* @returns the effective immersive flag for this render.
		*/
		function readImmersive() {
			try {
				var _window$androidBridge, _window$androidBridge2;
				const fromBridge = (_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 || (_window$androidBridge2 = _window$androidBridge.getImmersiveMode) === null || _window$androidBridge2 === void 0 ? void 0 : _window$androidBridge2.call(_window$androidBridge);
				if (typeof fromBridge === "boolean") return fromBridge;
			} catch {}
			try {
				return localStorage.getItem(IMMERSIVE_KEY) !== "0";
			} catch {
				return true;
			}
		}
		/**
		* Render the Android general-settings rows (immersive and screen scope).
		* @param props - composed slot props (contract/slots.ts).
		* @returns the section element tree.
		*/
		function GeneralSettings(_props) {
			const [immersive, refreshImmersive] = useShellState(readImmersive);
			/** 写失败回执（S3-17：本项是设置页里唯一**没有**失败反馈路径的开关）。 */
			const [notice, setNotice] = (0, react.useState)(null);
			const toggleImmersive = (0, react.useCallback)((enabled) => {
				var _window$androidBridge3;
				setNotice(null);
				try {
					localStorage.setItem(IMMERSIVE_KEY, enabled ? "1" : "0");
				} catch {}
				if (((_window$androidBridge3 = window.androidBridge) === null || _window$androidBridge3 === void 0 ? void 0 : _window$androidBridge3.setImmersiveMode) === void 0) {
					refreshImmersive();
					return;
				}
				try {
					window.androidBridge.setImmersiveMode(enabled);
				} catch {
					setNotice("设置没有生效：应用与页面的连接不可用——请重新打开应用后再试。");
					refreshImmersive();
					return;
				}
				refreshImmersive();
				if (readImmersive() !== enabled) setNotice(enabled ? "沉浸式状态栏没有开启：系统或应用未接受本次设置——可到系统设置里检查本应用的显示权限。" : "沉浸式状态栏没有关闭：系统或应用未接受本次设置——请重试，或重新打开应用。");
			}, [refreshImmersive]);
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				"data-plugin": "android-general",
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("label", {
						className: "dsh-dev-row dsh-dev-switch",
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
							type: "checkbox",
							checked: immersive,
							onChange: (e) => toggleImmersive(e.target.checked)
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { children: "沉浸式状态栏" })]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-hint",
						children: "常态隐藏系统状态栏，边缘滑动临时呼出；关闭后常驻显示。"
					}),
					notice !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: "dsh-dev-warn",
						children: notice
					})
				]
			});
		}
		//#endregion
		//#region src/client/theme-bridge.ts
		/**
		* ThemeBridge: make prefers-color-scheme follow the OS dark state on
		* WebViews whose media query does not track the system uiMode (observed on
		* vivo/Android 16: FORCE_DARK_AUTO leaves matchMedia stuck at light).
		*
		* The shell APK watches Configuration changes and pushes the dark flag via
		* window.__dshThemeBridge.setDark(dark). This module hooks matchMedia for the
		* (prefers-color-scheme: dark) query so the upstream ui-theme service
		* (default preference: system) resolves and live-updates through its own
		* listener — zero upstream changes.
		*/
		var ThemeBridge = class {
			constructor() {
				_defineProperty(this, "dark", false);
				_defineProperty(this, "listeners", /* @__PURE__ */ new Set());
				_defineProperty(this, "patched", false);
			}
			/** Install the matchMedia hook and the bridge object (idempotent). */
			install() {
				if (this.patched) return;
				this.patched = true;
				const android = window.androidBridge;
				if (window.__dshThemeBridge) return;
				if (!android || typeof android.getSystemDark !== "function") return;
				const self = this;
				const nativeMatchMedia = window.matchMedia.bind(window);
				window.matchMedia = ((query) => {
					if (!query.includes("prefers-color-scheme")) return nativeMatchMedia(query);
					const onChange = () => {
						for (const listener of self.listeners) try {
							listener();
						} catch {}
					};
					return {
						get matches() {
							return self.dark;
						},
						get media() {
							return query;
						},
						get onchange() {
							return null;
						},
						set onchange(_v) {},
						addEventListener: (type, cb) => {
							if (type !== "change" || typeof cb !== "function") return;
							self.listeners.add(cb);
							onChange();
						},
						removeEventListener: (type, cb) => {
							if (type !== "change" || typeof cb !== "function") return;
							self.listeners.delete(cb);
						},
						addListener: (cb) => {
							self.listeners.add(cb);
						},
						removeListener: (cb) => {
							self.listeners.delete(cb);
						},
						dispatchEvent: () => false
					};
				});
				const globalObj = window;
				globalObj.__dshThemeBridge = { setDark: (d) => {
					if (self.dark === d) return;
					self.dark = d;
					for (const listener of self.listeners) try {
						listener();
					} catch {}
				} };
				try {
					if (android.getSystemDark()) globalObj.__dshThemeBridge.setDark(true);
				} catch {}
			}
		};
		//#endregion
		//#region src/client/mobile/form-marker.ts
		/**
		* Mobile-form marker: the single source of truth behind every narrow-screen
		* rule this plugin injects.
		*
		* Two DOM facts are published here:
		* - `data-dsh-mobile-form` on `<html>` mirrors the `(max-width: 767px)` media
		*   query, so stylesheets re-anchored from the retired fork's `[data-mobile]`
		*   attribute keep one gate that matches the frame's own breakpoint choices
		*   (upstream's right panel turns fullscreen below 768px of frame width).
		* - `data-dsh-frame` tags the upstream frame root. The frame carries no stable
		*   hook of its own; its right column does (`data-rightbar-col`), so the tag is
		*   written from there and re-applied whenever the frame remounts.
		* - `data-dsh-modal-open` on `<html>` when any body-level modal is up, and
		*   `data-dsh-settings-dialog` on the settings panel itself. The settings
		*   overlay renders inside the sidebar subtree, so a translated (off-canvas)
		*   ancestor would carry it off-screen; the settings panel has no attribute of
		*   its own to key a stylesheet on, only its nav/content structure, so this
		*   marker is written onto the panel element it finds.
		* - `data-dsh-settings-open` on `<html>` while that settings panel is present.
		*   The narrow-form sheet must pin the drawer (transform: none) for the
		*   settings panel alone: a descendant rule cannot key on an attribute carried
		*   by the descendant, and a :has() selector would need a Chromium 105 floor
		*   this plugin does not have. Publishing the fact on the root keeps the
		*   stylesheet a plain attribute match on every supported kernel.
		*/
		/** Width at or below which the phone form applies; matches upstream's 768px fullscreen threshold. */
		const MOBILE_FORM_MAX_WIDTH = 767;
		/** The media query the marker mirrors. */
		const MOBILE_FORM_QUERY = `(max-width: 767px)`;
		/** Frame tag consumed by this plugin's narrow-form stylesheet. */
		const FRAME_TAG = "data-dsh-frame";
		/** Settings-panel tag consumed by the mobile settings stylesheet. */
		const SETTINGS_TAG = "data-dsh-settings-dialog";
		/** Root tag consumed by the narrow-form stylesheet to pin the drawer for the settings panel alone. */
		const SETTINGS_OPEN_TAG = "data-dsh-settings-open";
		/** Marks the phone form on `<html>` and tags the upstream frame root. */
		var MobileFormMarker = class {
			constructor() {
				_defineProperty(this, "media", null);
				_defineProperty(this, "observer", null);
				_defineProperty(this, "frame", null);
				_defineProperty(this, "modal", null);
				_defineProperty(this, "syncDom", () => {
					this.syncFrame();
					this.syncModal();
				});
				_defineProperty(this, "syncForm", () => {
					const matches = this.media === null ? window.innerWidth <= 767 : this.media.matches;
					document.documentElement.toggleAttribute("data-dsh-mobile-form", matches);
				});
				_defineProperty(this, "syncFrame", () => {
					var _document$querySelect, _this$frame;
					const frame = ((_document$querySelect = document.querySelector("[data-rightbar-col]")) === null || _document$querySelect === void 0 ? void 0 : _document$querySelect.parentElement) ?? null;
					if (frame === this.frame) return;
					(_this$frame = this.frame) === null || _this$frame === void 0 || _this$frame.removeAttribute(FRAME_TAG);
					this.frame = frame;
					frame === null || frame === void 0 || frame.setAttribute(FRAME_TAG, "");
				});
			}
			/** Publish both facts and keep them current. */
			attach() {
				var _this$media, _this$media$addEventL;
				this.media = typeof window.matchMedia === "function" ? window.matchMedia(MOBILE_FORM_QUERY) : null;
				(_this$media = this.media) === null || _this$media === void 0 || (_this$media$addEventL = _this$media.addEventListener) === null || _this$media$addEventL === void 0 || _this$media$addEventL.call(_this$media, "change", this.syncForm);
				this.syncForm();
				this.observer = new MutationObserver(this.syncDom);
				this.observer.observe(document.documentElement, {
					childList: true,
					subtree: true
				});
				this.syncDom();
			}
			/** Remove listeners, the observer, and both marks. */
			detach() {
				var _this$media2, _this$media2$removeEv, _this$observer, _this$frame2, _this$modal;
				(_this$media2 = this.media) === null || _this$media2 === void 0 || (_this$media2$removeEv = _this$media2.removeEventListener) === null || _this$media2$removeEv === void 0 || _this$media2$removeEv.call(_this$media2, "change", this.syncForm);
				this.media = null;
				(_this$observer = this.observer) === null || _this$observer === void 0 || _this$observer.disconnect();
				this.observer = null;
				(_this$frame2 = this.frame) === null || _this$frame2 === void 0 || _this$frame2.removeAttribute(FRAME_TAG);
				this.frame = null;
				(_this$modal = this.modal) === null || _this$modal === void 0 || _this$modal.removeAttribute(SETTINGS_TAG);
				this.modal = null;
				document.documentElement.removeAttribute("data-dsh-mobile-form");
				document.documentElement.removeAttribute("data-dsh-modal-open");
				document.documentElement.removeAttribute(SETTINGS_OPEN_TAG);
			}
			/**
			* Publish "a modal is up", tag the settings panel, and mirror that one
			* dialog on the root.
			*
			* Dialogs inside the frame's own overlay layer (this plugin's export-result
			* dialog) are not modals over the sidebar and never pin the drawer.
			*/
			syncModal() {
				const dialogs = [...document.querySelectorAll("[role='dialog'][aria-modal='true']")].filter((dialog) => dialog.closest("[data-shell-overlay]") === null);
				const settings = dialogs.find((dialog) => dialog.querySelector(":scope > nav") !== null) ?? null;
				if (settings !== this.modal) {
					var _this$modal2;
					(_this$modal2 = this.modal) === null || _this$modal2 === void 0 || _this$modal2.removeAttribute(SETTINGS_TAG);
					this.modal = settings;
					settings === null || settings === void 0 || settings.setAttribute(SETTINGS_TAG, "");
				}
				document.documentElement.toggleAttribute("data-dsh-modal-open", dialogs.length > 0);
				document.documentElement.toggleAttribute(SETTINGS_OPEN_TAG, settings !== null);
			}
		};
		//#endregion
		//#region src/client/enter-guard.ts
		/**
		* EnterGuard: mobile-form Enter-key semantics.
		*
		* On the phone soft keyboard the Enter (newline) key fires a plain keydown
		* Enter — upstream InputBar treats it as submit (keyboard.submit), and there
		* is no Shift to fall back on. This guard, on the mobile form only
		* (viewport <= MOBILE_FORM_MAX_WIDTH), intercepts a plain Enter inside the
		* composer's editable at document capture phase — before React's root
		* listener — and turns it into a line break, leaving the send button as the
		* only send channel.
		*
		* The editable is upstream's Lexical contenteditable since 0.1.5 (the
		* pre-0.1.5 composer was a textarea), and Lexical's own line break is reached
		* through the Shift+Enter gesture: the composer keymap returns false for
		* shiftKey and lets @lexical/plain-text insert the break. The guard therefore
		* re-dispatches the swallowed Enter as Shift+Enter on the same element instead
		* of writing text itself. Dropping the textarea assumption is what kept this
		* guard alive across the 0.1.5 upgrade: a textarea-only check silently turned
		* every soft-keyboard Enter back into a submit (measured 2026-09-10 on MuMu,
		* WebView 110: composer innerText was empty after Enter and the message had
		* been sent).
		*
		* Guards that must stay untouched:
		* - IME composition (isComposing / keyCode 229): the candidate-confirm Enter.
		* - Open command/reference menu ([role=listbox]): Enter picks the highlighted item.
		* - Shift+Enter (external keyboards): upstream native newline.
		* - Desktop/wide viewport: upstream behavior unchanged.
		*/
		/** The composer's own editable: upstream's Lexical host, or the pre-0.1.5 textarea. */
		const COMPOSER_EDITABLE = "[contenteditable=\"true\"], textarea";
		/**
		* 中文 IME 的候选确认键常落在 compositionend **之后**几毫秒（apk #182-3）。
		* 那段时间里 `isComposing=false` 且 keyCode 不是 229，只看这两条会把「确认候选」误判成
		* 「用户按了换行」→ 被改发 Shift+Enter，多插一个换行。上游 keymap 用 `recentlyComposing`
		* 补这一档，这里对齐同样的宽限窗。
		*/
		const COMPOSITION_GRACE_MS = 10;
		var EnterGuard = class {
			constructor() {
				_defineProperty(this, "lastCompositionEndAt", 0);
				_defineProperty(this, "onCompositionEnd", () => {
					this.lastCompositionEndAt = Date.now();
				});
				_defineProperty(this, "onKeyDown", (event) => {
					if (event.key !== "Enter" || event.shiftKey) return;
					if (event.isComposing || event.keyCode === 229) return;
					if (Date.now() - this.lastCompositionEndAt <= COMPOSITION_GRACE_MS) return;
					const target = event.target;
					if (!(target instanceof HTMLElement)) return;
					const card = target.closest("[data-composer-card]");
					if (card === null) return;
					const editable = target.closest(COMPOSER_EDITABLE);
					if (editable === null || !card.contains(editable)) return;
					if (document.querySelector("[role=\"listbox\"]") !== null) return;
					if (window.innerWidth > 767) return;
					event.stopPropagation();
					event.preventDefault();
					if (editable instanceof HTMLTextAreaElement) {
						try {
							document.execCommand("insertText", false, "\n");
						} catch {}
						return;
					}
					try {
						editable.dispatchEvent(new KeyboardEvent("keydown", {
							key: "Enter",
							code: "Enter",
							shiftKey: true,
							bubbles: true,
							cancelable: true
						}));
					} catch {}
				});
			}
			attach() {
				document.addEventListener("keydown", this.onKeyDown, { capture: true });
				document.addEventListener("compositionend", this.onCompositionEnd, { capture: true });
			}
			detach() {
				document.removeEventListener("keydown", this.onKeyDown, { capture: true });
				document.removeEventListener("compositionend", this.onCompositionEnd, { capture: true });
			}
		};
		//#endregion
		//#region src/client/keyboard-boundary.ts
		/**
		* KeyboardBoundary (issue #57): Android 16 edge-to-edge WebViews do not
		* shrink the layout viewport when the soft keyboard opens (adjustResize
		* does not resize the WebView content; visualViewport shrinks but
		* innerHeight stays 758). The frame (height: 100%, upstream ui-layout's root
		* grid) therefore extends under the keyboard, and its scrollable content leaves
		* a blank band below the composer — swiping up past the input reveals empty
		* black.
		*
		* Fix: while the IME inset is non-zero, pin the mobile frame's height to the
		* visualViewport height (the keyboard's top edge). The frame's overflow:
		* hidden then clips the blank band instead of letting it scroll into view.
		* Restored to 100% when the keyboard closes.
		*
		* The composer seat (position: sticky; bottom: 0) normally relies on
		* composer-insets.css.ts padding-bottom = --dsh-android-ime-bottom to lift
		* the input above the keyboard while the frame keeps its full height. Once
		* this class pins the frame to the keyboard top edge, that same padding
		* becomes redundant and inflates the seat past its sticky container (seat
		* height > scrollBody height makes the sticky bottom anchor inert and the
		* composer drifts to the top of the viewport). While pinned, the seat's
		* padding-bottom is therefore zeroed; it is restored on keyboard close.
		*/
		var KeyboardBoundary = class {
			constructor() {
				_defineProperty(this, "frame", null);
				_defineProperty(this, "seat", null);
				_defineProperty(this, "media", null);
				_defineProperty(this, "lastIme", 0);
				_defineProperty(this, "lastVv", 0);
				_defineProperty(this, "lastVvTop", 0);
				_defineProperty(this, "settleGeneration", 0);
				_defineProperty(this, "settleTimer", null);
				_defineProperty(this, "detached", false);
				_defineProperty(this, "onViewportChange", () => {
					const frame = document.querySelector("[data-dsh-frame]");
					if (frame === null) return;
					this.frame = frame;
					this.apply(frame);
					const generation = ++this.settleGeneration;
					const settle = () => {
						if (this.detached || generation !== this.settleGeneration) return;
						try {
							this.apply(frame);
						} catch {}
					};
					requestAnimationFrame(settle);
					this.settleTimer = window.setTimeout(() => {
						this.settleTimer = null;
						settle();
					}, 260);
				});
			}
			/** Watch visualViewport resize + scroll + the shell's IME inset variable. */
			attach() {
				var _window$visualViewpor, _window$visualViewpor2, _this$media, _this$media$addEventL;
				this.detached = false;
				(_window$visualViewpor = window.visualViewport) === null || _window$visualViewpor === void 0 || _window$visualViewpor.addEventListener("resize", this.onViewportChange);
				(_window$visualViewpor2 = window.visualViewport) === null || _window$visualViewpor2 === void 0 || _window$visualViewpor2.addEventListener("scroll", this.onViewportChange);
				this.media = typeof window.matchMedia === "function" ? window.matchMedia("(max-width: 767px)") : null;
				(_this$media = this.media) === null || _this$media === void 0 || (_this$media$addEventL = _this$media.addEventListener) === null || _this$media$addEventL === void 0 || _this$media$addEventL.call(_this$media, "change", this.onViewportChange);
				this.onViewportChange();
			}
			/** Remove listeners and restore the frame and seat styles. */
			detach() {
				var _window$visualViewpor3, _window$visualViewpor4, _this$media2, _this$media2$removeEv;
				this.detached = true;
				if (this.settleTimer !== null) {
					window.clearTimeout(this.settleTimer);
					this.settleTimer = null;
				}
				(_window$visualViewpor3 = window.visualViewport) === null || _window$visualViewpor3 === void 0 || _window$visualViewpor3.removeEventListener("resize", this.onViewportChange);
				(_window$visualViewpor4 = window.visualViewport) === null || _window$visualViewpor4 === void 0 || _window$visualViewpor4.removeEventListener("scroll", this.onViewportChange);
				(_this$media2 = this.media) === null || _this$media2 === void 0 || (_this$media2$removeEv = _this$media2.removeEventListener) === null || _this$media2$removeEv === void 0 || _this$media2$removeEv.call(_this$media2, "change", this.onViewportChange);
				this.restore();
			}
			/** 按当前 IME inset 与可视视口高度决定钉住还是还原（可重复调用，幂等）。 */
			apply(frame) {
				const rootStyle = getComputedStyle(document.documentElement);
				const ime = Number.parseFloat(rootStyle.getPropertyValue("--dsh-android-ime-bottom")) || 0;
				const vv = window.visualViewport;
				const vvHeight = vv === null ? 0 : Math.round(vv.height);
				const rawTop = vv === null ? 0 : Number(vv.offsetTop);
				const vvTop = Number.isFinite(rawTop) ? Math.max(0, Math.round(rawTop)) : 0;
				if (ime > 0 && vvHeight > 0 && (ime !== this.lastIme || vvHeight !== this.lastVv || vvTop !== this.lastVvTop)) {
					this.lastIme = ime;
					this.lastVv = vvHeight;
					this.lastVvTop = vvTop;
					frame.style.height = `${vvHeight + vvTop}px`;
					const seat = document.querySelector("[data-composer-seat]");
					if (seat !== null) {
						this.seat = seat;
						seat.style.paddingBottom = "0px";
					}
				} else if (ime === 0 && (this.lastIme !== 0 || frame.style.height !== "")) this.restore();
			}
			/** Restore the natural frame height and seat padding. */
			restore() {
				if (this.frame !== null) this.frame.style.height = "";
				this.frame = null;
				if (this.seat !== null) this.seat.style.paddingBottom = "";
				this.seat = null;
				this.lastIme = 0;
				this.lastVv = 0;
				this.lastVvTop = 0;
			}
		};
		//#endregion
		//#region src/client/export-result.ts
		/** Host-owned channel: the plugin writes, the dialog component reads. */
		var ExportResultChannel = class {
			constructor() {
				_defineProperty(this, "listeners", /* @__PURE__ */ new Set());
				_defineProperty(this, "snapshot", {
					open: false,
					ok: true,
					title: "",
					detail: ""
				});
				_defineProperty(this, "getSnapshot", () => this.snapshot);
				_defineProperty(this, "subscribe", (listener) => {
					this.listeners.add(listener);
					return () => {
						this.listeners.delete(listener);
					};
				});
			}
			/**
			* Open the dialog with one outcome; a second result supersedes a still-open first.
			* @param payload - the outcome to show.
			*/
			show(payload) {
				this.snapshot = {
					open: true,
					ok: payload.ok,
					title: payload.title,
					detail: payload.detail
				};
				this.publish();
			}
			/** Fold the dialog; the last result stays recorded. */
			close() {
				this.snapshot = {
					...this.snapshot,
					open: false
				};
				this.publish();
			}
			publish() {
				for (const listener of [...this.listeners]) listener();
			}
		};
		/**
		* Report one user-facing outcome through the shell-overlay dialog.
		*
		* The dialog entry subscribes to the `dsh:export-result` DOM event, which the
		* shell's export bridge and this plugin's own native-action failures both use:
		* one surface, one dismissal, no second dialog implementation.
		* @param payload - the outcome to show.
		*/
		function reportUserFacingResult(payload) {
			window.dispatchEvent(new CustomEvent("dsh:export-result", { detail: payload }));
		}
		//#endregion
		//#region src/client/mobile/mobile-form.css.ts
		/**
		* The phone form (<768px) over the upstream frame.
		*
		* Upstream keeps ownership of the frame, the columns, and both sidebars; this
		* sheet only re-shapes them for a phone:
		* - the left sidebar becomes an off-canvas drawer (its collapsed rail steps
		*   aside with it; the header's leading toggle is the entry, and the rail's own
		*   toggle keeps working from inside the drawer);
		* - the centre column spans the whole frame and pads only under the system inset;
		* - the right column keeps its zero-width track so the upstream panel (already
		*   fullscreen below 768px of frame width) hangs over the centre as a
		*   slide-over, with the system insets respected;
		* - the desktop drag handles are touch noise and step aside.
		*
		* The frame's track widths are inline styles, so the grid override must be
		* `!important`. Children are placed explicitly: with the sidebar out of flow,
		* auto-placement would otherwise drop the centre column into the first track.
		*/
		const MOBILE_FORM_CSS = `
:root {
  --dsh-mobile-top-inset: max(env(safe-area-inset-top, 0px), var(--dsh-android-system-top, 0px));
}

@media (max-width: 767px) {
  [data-dsh-frame] {
    grid-template-columns: 0 minmax(0, 1fr) 0 !important;
  }

  [data-dsh-frame] > [class*='sidebarCol'] {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    width: min(288px, 82vw);
    z-index: 30;
    background: var(--dsw-specific-sidebar-fill);
    transform: translateX(-100%);
    transition: transform var(--ds-transition-duration-slow, 200ms) var(--ds-ease-in-out, ease);
    /* 顶部安全区（2026-09-11 真机反馈）：关闭沉浸式状态栏时抽屉全高贴顶，上游侧栏的品牌行
       （logo）被状态栏盖住；这里与右栏/中栏用同一个 inset 变量（沉浸式打开时为 0，不影响布局）。 */
    padding-top: var(--dsh-mobile-top-inset, 0px);
    /* 底部安全区（apk #153 / PR #157）：抽屉全高贴底时，底部用户栏里的设置入口与系统手势条
       重叠，点击被拦截。与 composer-insets 的底部 inset 取值保持一致。 */
    padding-bottom: max(env(safe-area-inset-bottom, 0px), var(--dsh-android-system-bottom, 0px));
  }

  [data-dsh-frame]:not([data-sidebar-collapsed]) > [class*='sidebarCol'] {
    transform: none;
  }

  /* The settings overlay renders inside the sidebar subtree: a translated
     (off-canvas) ancestor would carry it off-screen (a transformed ancestor
     becomes the containing block of its fixed-position descendants). The
     drawer therefore stays on screen while the settings panel is up.

     Only the settings panel may pin it. Keying this on the coarse
     data-dsh-modal-open attribute flattened the drawer for every body-level
     aria-modal dialog (session rename, permission risk confirmation, image
     lightbox). data-dsh-settings-open is the settings-only fact published on
     the root by form-marker.ts, so this stays a plain ancestor match with no
     :has() requirement. */
  html[data-dsh-settings-open] [data-dsh-frame] > [class*='sidebarCol'] {
    transform: none;
  }

  /* Both in-flow columns take the explicit first row. The frame's track widths
     are inline styles and upstream declares a single 100% row; a right column
     left on auto-placement lands in an implicit second row (0px tall) and the
     frame's overflow:hidden clips the whole right panel -- toggle, tabs and
     corner expand key included -- out of the viewport. The centre column joins
     it so no second row is materialised at all. */
  [data-dsh-frame] > [class*='centerCol'] {
    grid-column: 1 / -1;
    grid-row: 1;
    /* 0.14.2 P4: the self-drawn 44px top bar is gone (the drawer toggle now sits in
       upstream's own header row), so the centre column no longer reserves a band
       above the header -- only the system/safe-area top inset. */
    padding-top: var(--dsh-mobile-top-inset, 0px);
  }

  [data-dsh-frame] > [class*='rightbarCol'] {
    grid-column: 3;
    grid-row: 1;
  }

  /* Anchored on upstream's own attribute: CSS attribute-substring matching is
     case-sensitive and the real class is widthHandle (capital H), so a
     [class*=handle] selector matches nothing and the desktop drag handles
     stayed live on a phone. */
  [data-dsh-frame] [data-width-handle] {
    display: none;
  }

  [data-sidebar-right-panel='fullscreen'] {
    box-sizing: border-box;
    padding-top: var(--dsh-mobile-top-inset, 0px);
    padding-bottom: env(safe-area-inset-bottom, 0px);
  }
}

@media (prefers-reduced-motion: reduce) {
  [data-dsh-frame] > [class*='sidebarCol'] {
    transition: none;
  }
}
`;
		//#endregion
		//#region \0dsh-css:/home/agentuser/DeepSeek 2/dsh-upgrade-021/plugin/dsh-client-ui-responsive/src/client/mobile/MobileChrome.module.css.mjs
		const css$3 = "._5KYWqa_root{pointer-events:none;z-index:25;position:absolute;inset:0}._5KYWqa_toggle,._5KYWqa_mask{display:none}html[data-dsh-mobile-form] ._5KYWqa_toggle{width:28px;height:28px;color:var(--dsw-alias-label-primary);cursor:pointer;touch-action:manipulation;background:0 0;border:none;border-radius:8px;flex:none;justify-content:center;align-items:center;margin-inline-end:8px;padding:0;display:inline-flex;position:relative}html[data-dsh-mobile-form] ._5KYWqa_toggle:active{background:var(--dsw-alias-button-floating-fill)}@media (width<=767px){._5KYWqa_mask{opacity:0;pointer-events:none;transition:opacity var(--ds-transition-duration-slow,.2s) var(--ds-ease-in-out,ease);z-index:25;background:#00000073;display:block;position:absolute;inset:0}._5KYWqa_mask[data-open]{opacity:1;pointer-events:auto}}@media (prefers-reduced-motion:reduce){._5KYWqa_mask{transition:none}}html[data-dsh-mobile-form] ._5KYWqa_badge{background:var(--dsw-alias-brand-primary,#3d6df0);pointer-events:none;border-radius:50%;width:8px;height:8px;position:absolute;top:6px;right:6px}html[data-dsh-mobile-form] ._5KYWqa_pendingHint{color:var(--dsw-alias-label-secondary);white-space:nowrap;align-self:center;margin-left:6px;font-size:12px;line-height:1}";
		const tagId$3 = "@dsh-android/dsh-client-ui-responsive/MobileChrome.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$3) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@dsh-android/dsh-client-ui-responsive";
			tag.dataset.pluginCss = tagId$3;
			tag.textContent = css$3;
			document.head.appendChild(tag);
		}
		var MobileChrome_module_css_default = {
			"badge": "_5KYWqa_badge",
			"mask": "_5KYWqa_mask",
			"pendingHint": "_5KYWqa_pendingHint",
			"root": "_5KYWqa_root",
			"toggle": "_5KYWqa_toggle"
		};
		//#endregion
		//#region src/client/mobile/MobileChrome.tsx
		/**
		* Mobile chrome: the drawer mask (0.14.2 P4).
		*
		* Registered into the frame's shell.overlay seat. It used to own the drawer toggle
		* too, inside a self-drawn 44px band ([data-dsh-mobile-topbar]) above the header;
		* the user reported that band as wasted vertical space ("这个顶部的额头太大了（标题上方留空）
		* 挤占屏幕空间"), so the toggle moved into upstream's own header row
		* (conversation.header.leading, see SidebarToggle.tsx) and this entry keeps only the
		* mask that covers the frame while the drawer is open.
		*
		* The open state is mirrored from the frame's own data-sidebar-collapsed attribute
		* rather than owned here: the drawer keeps its own toggle inside, and the marker may
		* also flip the attribute through rotation. Reading it keeps the mask honest without
		* a second source of truth.
		*/
		/** The frame root, tagged by the form marker; the right column identifies it before the tag lands. */
		function frameElement$1() {
			var _document$querySelect;
			return document.querySelector("[data-dsh-frame]") ?? ((_document$querySelect = document.querySelector("[data-rightbar-col]")) === null || _document$querySelect === void 0 ? void 0 : _document$querySelect.parentElement) ?? null;
		}
		/**
		* Mirror whether the left sidebar is expanded.
		* @returns true while the frame renders the sidebar opened.
		*/
		function useSidebarOpen$1() {
			const [open, setOpen] = (0, react.useState)(false);
			(0, react.useEffect)(() => {
				let frame = null;
				let frameObserver = null;
				const sync = () => {
					setOpen(frame !== null && !frame.hasAttribute("data-sidebar-collapsed"));
				};
				const bind = () => {
					frame = frameElement$1();
					if (frame === null) return false;
					frameObserver = new MutationObserver(sync);
					frameObserver.observe(frame, {
						attributes: true,
						attributeFilter: ["data-sidebar-collapsed"]
					});
					sync();
					return true;
				};
				if (bind()) return () => {
					frameObserver === null || frameObserver === void 0 || frameObserver.disconnect();
				};
				const waitObserver = new MutationObserver(() => {
					if (bind()) waitObserver.disconnect();
				});
				waitObserver.observe(document.documentElement, {
					childList: true,
					subtree: true
				});
				return () => {
					waitObserver.disconnect();
					frameObserver === null || frameObserver === void 0 || frameObserver.disconnect();
				};
			}, []);
			return open;
		}
		/**
		* The drawer mask for the phone form.
		* @param props - the injected drawer toggle (used to close on a tap beside the drawer).
		* @returns the mask, which is inert outside the phone form.
		*/
		function MobileChrome({ toggleSidebar }) {
			const open = useSidebarOpen$1();
			return /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
				className: MobileChrome_module_css_default.root,
				children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
					className: MobileChrome_module_css_default.mask,
					"data-open": open || void 0,
					"data-dsh-mobile-mask": "",
					onClick: () => {
						toggleSidebar();
					}
				})
			});
		}
		//#endregion
		//#region src/client/mobile/session-marker.ts
		/** 发布当前会话 id 的 DOM 属性名（注入层与 e2e 断言共用）。 */
		const SESSION_ID_ATTRIBUTE = "data-dsh-session-id";
		var SessionMarker = class {
			constructor(sessions) {
				this.sessions = sessions;
				_defineProperty(this, "unsubscribe", void 0);
				_defineProperty(this, "attached", false);
			}
			/** 开始跟踪当前会话（幂等）。 */
			attach() {
				if (this.attached) return;
				this.attached = true;
				this.sync();
				try {
					var _this$sessions, _this$sessions$subscr;
					this.unsubscribe = (_this$sessions = this.sessions) === null || _this$sessions === void 0 || (_this$sessions = _this$sessions.list) === null || _this$sessions === void 0 || (_this$sessions$subscr = _this$sessions.subscribe) === null || _this$sessions$subscr === void 0 ? void 0 : _this$sessions$subscr.call(_this$sessions, () => {
						this.sync();
					});
				} catch {
					this.unsubscribe = void 0;
				}
			}
			/** 停止跟踪并移除标记。 */
			detach() {
				try {
					var _this$unsubscribe;
					(_this$unsubscribe = this.unsubscribe) === null || _this$unsubscribe === void 0 || _this$unsubscribe.call(this);
				} catch {}
				this.unsubscribe = void 0;
				this.attached = false;
				try {
					document.documentElement.removeAttribute(SESSION_ID_ATTRIBUTE);
				} catch {}
			}
			/** 同步一次：当前会话 id → 属性；无会话则移除。 */
			sync() {
				try {
					var _this$sessions2, _this$sessions2$getSn;
					const current = (_this$sessions2 = this.sessions) === null || _this$sessions2 === void 0 || (_this$sessions2 = _this$sessions2.list) === null || _this$sessions2 === void 0 || (_this$sessions2$getSn = _this$sessions2.getSnapshot) === null || _this$sessions2$getSn === void 0 || (_this$sessions2$getSn = _this$sessions2$getSn.call(_this$sessions2)) === null || _this$sessions2$getSn === void 0 ? void 0 : _this$sessions2$getSn.current;
					const root = document.documentElement;
					if (current === void 0 || current === null || String(current) === "") root.removeAttribute(SESSION_ID_ATTRIBUTE);
					else root.setAttribute(SESSION_ID_ATTRIBUTE, String(current));
				} catch {}
			}
		};
		//#endregion
		//#region src/client/mobile/browser-auto-place.ts
		/**
		* 侧栏收起时「有待展开的浏览器标签」的标记属性（S3-19）。
		*
		* 唯一真源在这里（写方），读方是 `MobileChrome` 的侧栏开关徽标——两处必须同字面量，
		* 所以只在这个模块里定义一次并导出。
		*/
		const BROWSER_PENDING_ATTR = "data-dsh-browser-pending";
		//#endregion
		//#region src/client/mobile/SidebarToggle.tsx
		/**
		* The drawer toggle, seated in upstream's own header row (0.14.2 P4).
		*
		* It replaced a self-drawn 44px band ([data-dsh-mobile-topbar]) that sat above the
		* header: the user reported that band as wasted vertical space ("这个顶部的额头太大了
		* （标题上方留空）挤占屏幕空间"). Upstream already reserves an empty global-navigation
		* seat beside the Session title (conversation.header.leading, kind single, scope root),
		* so the control moves there and the band is removed.
		*
		* The open state is mirrored from the frame's own data-sidebar-collapsed attribute
		* rather than owned here: the drawer keeps its own toggle inside, the marker can flip
		* the attribute on rotation, and reading it keeps aria-expanded honest without a
		* second source of truth.
		*/
		/** Copy (the Android layer's product strings are Chinese; see DevSection/ExportResultDialog). */
		const TOGGLE_OPEN = "打开导航";
		const TOGGLE_CLOSE = "关闭导航";
		/** The frame root, tagged by the form marker; the right column identifies it before the tag lands. */
		function frameElement() {
			var _document$querySelect;
			return document.querySelector("[data-dsh-frame]") ?? ((_document$querySelect = document.querySelector("[data-rightbar-col]")) === null || _document$querySelect === void 0 ? void 0 : _document$querySelect.parentElement) ?? null;
		}
		/**
		* Mirror whether the left sidebar is expanded.
		* @returns true while the frame renders the sidebar opened.
		*/
		function useSidebarOpen() {
			const [open, setOpen] = (0, react.useState)(false);
			(0, react.useEffect)(() => {
				let frame = null;
				let frameObserver = null;
				const sync = () => {
					setOpen(frame !== null && !frame.hasAttribute("data-sidebar-collapsed"));
				};
				const bind = () => {
					frame = frameElement();
					if (frame === null) return false;
					frameObserver = new MutationObserver(sync);
					frameObserver.observe(frame, {
						attributes: true,
						attributeFilter: ["data-sidebar-collapsed"]
					});
					sync();
					return true;
				};
				if (bind()) return () => {
					frameObserver === null || frameObserver === void 0 || frameObserver.disconnect();
				};
				const waitObserver = new MutationObserver(() => {
					if (bind()) waitObserver.disconnect();
				});
				waitObserver.observe(document.documentElement, {
					childList: true,
					subtree: true
				});
				return () => {
					waitObserver.disconnect();
					frameObserver === null || frameObserver === void 0 || frameObserver.disconnect();
				};
			}, []);
			return open;
		}
		/**
		* Observe the cross-module signal "the model opened a browser tab while the sidebar
		* is collapsed" (S3-19).
		*
		* The defect: with the sidebar collapsed, a model-opened browser tab was completely
		* silent for the user - browser-auto-place only recorded the intent and waited for a
		* manual expand, with nothing on screen saying something was waiting. This turns it
		* into a badge on the sidebar toggle: the marker sits on the very button the user has
		* to press, so opening it reveals the tab.
		* @returns true while a browser tab is waiting for the drawer.
		*/
		function useBrowserPending() {
			const [pending, setPending] = (0, react.useState)(false);
			(0, react.useEffect)(() => {
				const sync = () => {
					setPending(document.documentElement.hasAttribute(BROWSER_PENDING_ATTR));
				};
				sync();
				const observer = new MutationObserver(sync);
				observer.observe(document.documentElement, {
					attributes: true,
					attributeFilter: [BROWSER_PENDING_ATTR]
				});
				return () => {
					observer.disconnect();
				};
			}, []);
			return pending;
		}
		/**
		* The drawer toggle for the header's leading seat.
		* @param props - runtime share and the injected toggle.
		* @returns the toggle button, with the pending-browser badge when one is waiting.
		*/
		function SidebarToggle({ toggleSidebar }) {
			const open = useSidebarOpen();
			const browserPending = useBrowserPending();
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)(react_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("button", {
				type: "button",
				className: MobileChrome_module_css_default.toggle,
				"data-dsh-sidebar-toggle": "",
				"aria-label": browserPending && !open ? "打开导航（有一个浏览器标签在等你展开侧栏）" : open ? TOGGLE_CLOSE : TOGGLE_OPEN,
				"aria-expanded": open,
				onClick: () => {
					toggleSidebar();
				},
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("svg", {
					width: "18",
					height: "18",
					viewBox: "0 0 18 18",
					"aria-hidden": "true",
					children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("path", {
						d: "M2.5 4.5h13M2.5 9h13M2.5 13.5h13",
						fill: "none",
						stroke: "currentColor",
						strokeWidth: "1.6",
						strokeLinecap: "round"
					})
				}), browserPending && !open && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
					className: MobileChrome_module_css_default.badge,
					"data-dsh-browser-badge": "pending",
					"aria-hidden": "true"
				})]
			}), browserPending && !open && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
				className: MobileChrome_module_css_default.pendingHint,
				"data-dsh-browser-pending-hint": "",
				children: "有浏览器标签待展开"
			})] });
		}
		//#endregion
		//#region src/client/mobile/vendor-chrome-hide.css.ts
		/**
		* Vendor-chrome hiding for the Android header (0.14.2 P5).
		*
		* The user reported two controls in the Session header as useless and asked for
		* them to go away (2026-09-26, verbatim): 「那个文件夹按钮完全无用，绿点也无用，这俩隐藏掉」.
		*
		* - The folder button is this plugin's own contribution, so it is retired at its
		*   registration site (`index.ts`), not here.
		* - The green dot belongs to the vendored `dsh-undo-savepoint` package
		*   (`dsh-mobile-apk/vendor/`, a pinned third-party copy). Editing vendor source
		*   would put this change into the mirror surface for no benefit, so it is
		*   hidden with one CSS override keyed on the vendor's own published attribute.
		*
		* The attribute selector is the vendor's own `data-undo-header` (published by
		* `vendor/dsh-undo-savepoint/lib/client.js`), never a hashed CSS-Module class
		* name: hashed names are unreachable from another package and would silently
		* stop matching on the next vendor bump.
		*
		* Scoped to no width query: this plugin only ever runs inside the Android shell,
		* and the dot is unwanted in both portrait and landscape. The 767px phone form
		* does NOT cover the landscape device (1600px wide), so a width-gated rule would
		* leave the dot visible there.
		*/
		const VENDOR_CHROME_HIDE_CSS = `
/* The undo-savepoint status dot: no action, no readable state, removed by user
   request. Hidden rather than unmounted because the vendor owns the element. */
[data-undo-header] .u_dot {
  display: none !important;
}
`;
		//#endregion
		//#region src/client/mobile/address.ts
		/** The scheme and type every file address opens with. */
		const FILE_ADDRESS_PREFIX = "dsh-resource://file/";
		/** Whether a decoded first segment is a Windows drive (`C:`). */
		function isDriveSegment(segment) {
			return segment !== void 0 && /^[A-Za-z]:$/.test(segment);
		}
		/**
		* Read a file address back into its parts.
		* @param address - a candidate address.
		* @returns the parts, or `undefined` when the string is not a file address in a known scope or a segment is malformed.
		*/
		function parseFileAddress(address) {
			try {
				if (!address.startsWith(FILE_ADDRESS_PREFIX)) return void 0;
				const end = address.search(/[?#]/);
				const [scope, ...rest] = address.slice(20, end === -1 ? void 0 : end).split("/");
				if (scope === "session") {
					const [id, ...segments] = rest;
					if (id === void 0 || id === "" || segments.length === 0) return void 0;
					return {
						scope,
						sessionId: decodeURIComponent(id),
						path: segments.map(decodeURIComponent).join("/")
					};
				}
				if (scope === "absolute") {
					const unc = rest[0] === "" && rest.length > 1;
					const segments = (unc ? rest.slice(1) : rest).map(decodeURIComponent);
					if (segments.length === 0 || segments[0] === "") return void 0;
					if (unc) return {
						scope,
						path: `//${segments.join("/")}`
					};
					return {
						scope,
						path: isDriveSegment(segments[0]) ? segments.join("/") : `/${segments.join("/")}`
					};
				}
				return;
			} catch {
				return;
			}
		}
		/**
		* Resolve a parsed address to the absolute device path the shell can open.
		* @param parsed - the parsed address.
		* @param sessionRoot - the Session's workspace directory, from its summary.
		* @returns the absolute path, or `undefined` when a relative path has no known root.
		*/
		function resolveAbsolutePath(parsed, sessionRoot) {
			if (parsed.scope === "absolute") return parsed.path;
			if (parsed.path.startsWith("/")) return parsed.path;
			if (sessionRoot === void 0 || sessionRoot === "") return void 0;
			const root = sessionRoot.replace(/\/+$/, "");
			return parsed.path === "" ? root : `${root}/${parsed.path}`;
		}
		//#endregion
		//#region src/client/mobile/external-open-paths.ts
		/** This type's identity: the registry id and the key its body registers under. */
		const EXTERNAL_OPEN_ID = "@dsh-android/client-ui-responsive/open-with";
		/** This type's kind discriminator. */
		const EXTERNAL_OPEN_KIND = "open-with";
		/** The address family this type claims. */
		const FILE_ADDRESS_PATTERN = "dsh-resource://file/**";
		/**
		* Suffixes whose content is not text and has no preview renderer, so the phone
		* answer is "hand it to another application": archives, Android/iOS packages,
		* disk images, installers, databases, machine code, fonts.
		*/
		const EXTERNAL_ONLY_EXTENSIONS = [
			"7z",
			"a",
			"aab",
			"aar",
			"apk",
			"apks",
			"bin",
			"bz2",
			"cab",
			"class",
			"dat",
			"db",
			"deb",
			"dex",
			"dll",
			"dmg",
			"dylib",
			"exe",
			"gz",
			"img",
			"iso",
			"jar",
			"lz4",
			"lzma",
			"msi",
			"msix",
			"o",
			"pak",
			"rar",
			"rpm",
			"so",
			"sqlite",
			"sqlite3",
			"tar",
			"tgz",
			"ttf",
			"otf",
			"wasm",
			"xapk",
			"xz",
			"zip",
			"zst"
		];
		/**
		* The lowercase suffix of a path, without its dot.
		* @param path - decoded file path.
		* @returns the suffix, or an empty string when the name has none.
		*/
		function extensionOf(path) {
			const name = path.replace(/\\/g, "/").split("/").pop() ?? "";
			const dot = name.lastIndexOf(".");
			return dot <= 0 ? "" : name.slice(dot + 1).toLowerCase();
		}
		/**
		* Whether a path's content has no preview and belongs to another application.
		* @param path - decoded file path.
		* @returns true for the curated suffix list.
		*/
		function isExternalOnlyPath(path) {
			return EXTERNAL_ONLY_EXTENSIONS.includes(extensionOf(path));
		}
		/**
		* The decoded last segment of an address, used as the tab's chip title.
		* @param address - a `dsh-resource://file/…` address.
		* @returns the decoded name, or the address when it has no segment.
		*/
		function basenameOf(address) {
			const name = address.slice(address.lastIndexOf("/") + 1);
			if (name === "") return address;
			try {
				return decodeURIComponent(name);
			} catch {
				return name;
			}
		}
		/**
		* The type's registry definition.
		* @param claimedByAnother - asks the registry whether a builtin or extension type already welcomes the address.
		* @returns the definition to register.
		*/
		function externalOpenDefinition(claimedByAnother) {
			return {
				id: EXTERNAL_OPEN_ID,
				kind: EXTERNAL_OPEN_KIND,
				patterns: [FILE_ADDRESS_PATTERN],
				priority: "extension",
				canOpen: (address) => {
					const parsed = parseFileAddress(address);
					if (parsed === void 0) return false;
					if (parsed.scope === "absolute") return true;
					if (!isExternalOnlyPath(parsed.path)) return false;
					return !claimedByAnother(address);
				},
				title: basenameOf
			};
		}
		//#endregion
		//#region src/client/mobile/open-path.ts
		/**
		* Whether the running host can raise the native chooser.
		* @returns true when the shell injected the method.
		*/
		function chooserAvailable() {
			var _window$androidBridge;
			return typeof ((_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 ? void 0 : _window$androidBridge.openPathChooser) === "function";
		}
		/**
		* Ask the shell to open a path through the system chooser.
		* @param path - absolute device path.
		* @param mode - `folder` targets file managers on the directory, `view` on the file.
		* @returns the shell's outcome; `{ ok: false, reason: 'unavailable' }` without a shell.
		*/
		function openPathChooser(path, mode = "view") {
			const bridge = window.androidBridge;
			if (typeof (bridge === null || bridge === void 0 ? void 0 : bridge.openPathChooser) !== "function") return {
				ok: false,
				reason: "unavailable"
			};
			try {
				const raw = bridge.openPathChooser(path, mode);
				if (typeof raw !== "string" || raw === "") return {
					ok: false,
					reason: "empty-answer"
				};
				const answer = JSON.parse(raw);
				if (answer.ok === true) return { ok: true };
				return {
					ok: false,
					reason: typeof answer.reason === "string" ? answer.reason : "refused"
				};
			} catch (error) {
				return {
					ok: false,
					reason: error instanceof Error ? error.message : "bridge-error"
				};
			}
		}
		//#endregion
		//#region src/client/mobile/session-cwd.ts
		/**
		* Read one Session's workspace directory.
		* @param state - the runtime's session-list snapshot.
		* @param sessionId - the Session whose summary is read.
		* @returns the directory, or `undefined` when the summary lacks one.
		*/
		function sessionCwd(state, sessionId) {
			var _byId$sessionId;
			const byId = state.byId;
			const cwd = byId === null || byId === void 0 || (_byId$sessionId = byId[sessionId]) === null || _byId$sessionId === void 0 ? void 0 : _byId$sessionId.cwd;
			return typeof cwd === "string" && cwd !== "" ? cwd : void 0;
		}
		//#endregion
		//#region \0dsh-css:/home/agentuser/DeepSeek 2/dsh-upgrade-021/plugin/dsh-client-ui-responsive/src/client/mobile/ExternalOpen.module.css.mjs
		const css$2 = ".pSQwbW_card{flex-direction:column;gap:8px;min-width:0;padding:16px;display:flex}.pSQwbW_name{font-size:var(--dsh-content-font-size,14px);color:var(--dsw-alias-label-primary);overflow-wrap:anywhere;margin:0}.pSQwbW_path{font-family:var(--dsw-font-markdown-code-font-family,ui-monospace, SFMono-Regular, Menlo, monospace);font-size:var(--dsh-content-font-size-secondary,13px);color:var(--dsw-alias-label-secondary);overflow-wrap:anywhere;margin:0}.pSQwbW_hint{font-size:var(--dsh-content-font-size-secondary,13px);color:var(--dsw-alias-label-tertiary);margin:0}.pSQwbW_actions{flex-wrap:wrap;gap:8px;margin-top:4px;display:flex}.pSQwbW_primary,.pSQwbW_secondary{border:1px solid var(--dsw-alias-border-l1);font-size:var(--dsh-content-font-size-secondary,13px);cursor:pointer;touch-action:manipulation;border-radius:10px;flex:none;padding:8px 14px}.pSQwbW_primary{color:var(--dsw-alias-label-primary);background:var(--dsw-alias-interactive-bg-hover)}.pSQwbW_secondary{color:var(--dsw-alias-label-primary);background:0 0}.pSQwbW_failure{font-size:var(--dsh-content-font-size-secondary,13px);color:var(--dsw-alias-state-error-primary);margin:4px 0 0}";
		const tagId$2 = "@dsh-android/dsh-client-ui-responsive/ExternalOpen.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$2) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@dsh-android/dsh-client-ui-responsive";
			tag.dataset.pluginCss = tagId$2;
			tag.textContent = css$2;
			document.head.appendChild(tag);
		}
		var ExternalOpen_module_css_default = {
			"actions": "pSQwbW_actions",
			"card": "pSQwbW_card",
			"failure": "pSQwbW_failure",
			"hint": "pSQwbW_hint",
			"name": "pSQwbW_name",
			"path": "pSQwbW_path",
			"primary": "pSQwbW_primary",
			"secondary": "pSQwbW_secondary"
		};
		//#endregion
		//#region src/client/mobile/external-open.tsx
		/**
		* "Open with" tab body: what the right Sidebar shows for a file no preview can
		* render (archives, packages, binaries, installers). The type's claims live in
		* `external-open-paths.ts`; this file is only the card and its two gestures.
		*
		* The body reads no file content: it names the file and hands the absolute
		* device path to the shell's native chooser, so opening a 200 MB archive costs
		* nothing.
		*/
		/** The directory holding a path (the chooser's `folder` target). */
		function parentDirectory(path) {
			const cut = path.replace(/\/+$/, "").lastIndexOf("/");
			return cut <= 0 ? path : path.slice(0, cut);
		}
		/**
		* The tab body: name the file, then hand it to the system chooser.
		* @param props - the session-scoped tab share (runtime hooks + the tab reader).
		* @returns the card, or an explanation when this host cannot open paths.
		*/
		function ExternalOpenTab({ sessionId, useSessions, useTabInfo }) {
			const info = useTabInfo();
			const cwd = useSessions((state) => sessionCwd(state, sessionId));
			const address = info.tab.navigation.address;
			const parsed = parseFileAddress(address);
			const absolute = parsed === void 0 ? void 0 : resolveAbsolutePath(parsed, cwd);
			const [failure, setFailure] = (0, react.useState)(null);
			const hand = (target, mode) => {
				const result = openPathChooser(target, mode);
				if (result.ok) {
					setFailure(null);
					return;
				}
				const next = result.reason === "no-handler" ? {
					title: "没有可用的文件管理器",
					detail: "设备上没有能打开该路径的应用，可先安装 MT 管理器。"
				} : {
					title: "打开失败",
					detail: `调用系统选择器失败（${result.reason ?? "unknown"}）。`
				};
				setFailure(next);
				reportUserFacingResult({
					ok: false,
					...next
				});
			};
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: ExternalOpen_module_css_default.card,
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: ExternalOpen_module_css_default.name,
						children: basenameOf(address)
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: ExternalOpen_module_css_default.path,
						children: absolute ?? address
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: ExternalOpen_module_css_default.hint,
						children: "该格式没有内置预览，可交给设备上的应用打开。"
					}),
					chooserAvailable() && absolute !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: ExternalOpen_module_css_default.actions,
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: ExternalOpen_module_css_default.primary,
							onClick: () => {
								hand(parentDirectory(absolute), "folder");
							},
							children: "打开所在文件夹"
						}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: ExternalOpen_module_css_default.secondary,
							onClick: () => {
								hand(absolute, "view");
							},
							children: "用其它应用打开"
						})]
					}) : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: ExternalOpen_module_css_default.hint,
						children: absolute === void 0 ? "无法确定该文件的设备路径。" : "当前环境不支持调用系统应用（请在安卓应用内打开）。"
					}),
					failure !== null && /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("p", {
						className: ExternalOpen_module_css_default.failure,
						children: [
							failure.title,
							"：",
							failure.detail
						]
					})
				]
			});
		}
		//#endregion
		//#region src/client/mobile/settings-document.ts
		/**
		* Mobile takeover of the upstream "open configuration file" settings action (apk #152).
		*
		* Upstream renders that action while the settings dialog is open and, on click, calls
		* `settings.openSettingsDocument()`: the Host materializes the provider-owned document and
		* hands it to a native desktop text editor (mac/win/linux). Android has no such opener, so the
		* click always ended in the localized 「无法打开配置文件」 error.
		*
		* The shell can open any path the app is allowed to read through its system chooser
		* (`androidBridge.openPathChooser`), and the settings document path is a fixed app-private
		* location the shell can report (`androidBridge.settingsPath`). This handler claims the click
		* while the settings dialog is up and routes it there; when either bridge is missing, or the
		* chooser refuses, the event is left alone so upstream behavior (and its error message) stays.
		*/
		/** Upstream action labels this handler claims (zh / en dictionaries). */
		const ACTION_LABELS = ["打开配置文件", "Open configuration file"];
		/** Read the settings document path from the shell bridge; empty when unavailable. */
		function settingsPath() {
			const bridge = window.androidBridge;
			if (typeof (bridge === null || bridge === void 0 ? void 0 : bridge.settingsPath) !== "function") return "";
			try {
				return bridge.settingsPath() || "";
			} catch {
				return "";
			}
		}
		/**
		* apk #168 的关键一步：优先用**壳侧导出的副本**路径。
		*
		* 活动配置在私有 `$DSH_HOME`，而选择器白名单（与 FileProvider 映射）刻意不包括 `.dsh`——
		* 那里有 `.credentials.yaml` 等凭据，放宽等于把凭据交给系统选择器。所以壳侧先把 settings.yaml
		* 复制到已放行的 `Documents/dshdata/exports/config/`，页面打开的是这份副本（UI 文案已说明）。
		* 副本拿不到时才退回旧路径（私有路径会被白名单拒绝，届时仍走上游错误路径）。
		*/
		/**
		* 选择器要打开的路径，并标明它是不是**导出副本**（S3-20）。
		*
		* 缺陷现场：这个动作实际打开的是壳侧导出的副本，而界面上一个字都没说——用户以为改的是真源，
		* 改完发现「不生效」。把「是不是副本」作为返回值交给调用方，由它给用户可见说明。
		*/
		function settingsPathForChooser() {
			const bridge = window.androidBridge;
			if (typeof (bridge === null || bridge === void 0 ? void 0 : bridge.exportSettingsDocument) === "function") try {
				const exported = bridge.exportSettingsDocument() || "";
				if (exported !== "") return {
					path: exported,
					isCopy: true
				};
			} catch {}
			return {
				path: settingsPath(),
				isCopy: false
			};
		}
		/** Whether the clicked element is the upstream open-configuration-file action. */
		function isSettingsDocumentAction(target) {
			if (!(target instanceof Element)) return false;
			const button = target.closest("button");
			if (button === null) return false;
			if (button.closest("[data-dsh-settings-dialog]") === null) return false;
			const label = (button.textContent ?? "").trim();
			return ACTION_LABELS.includes(label);
		}
		/** Claims the upstream action and opens the settings document through the shell chooser. */
		var SettingsDocumentAction = class {
			constructor() {
				_defineProperty(this, "onClick", (event) => {
					if (!chooserAvailable()) return;
					if (!isSettingsDocumentAction(event.target)) return;
					const target = settingsPathForChooser();
					if (target.path === "") return;
					if (!openPathChooser(target.path, "view").ok) return;
					event.preventDefault();
					event.stopPropagation();
					if (target.isCopy) reportUserFacingResult({
						ok: true,
						title: "已打开配置文件的副本",
						detail: "这是导出副本（Documents/dshdata/exports/config/settings.yaml），在它上面修改不会直接生效；改完请回到「设置 → 开发者选项 → 导入配置」。"
					});
				});
			}
			attach() {
				document.addEventListener("click", this.onClick, true);
			}
			detach() {
				document.removeEventListener("click", this.onClick, true);
			}
		};
		//#endregion
		//#region src/client/mobile/reference-menu.ts
		/**
		* Mobile reference-menu enhancer (apk #163 / 多选 chrome 审计 apk #169)。
		*
		* Upstream's `@` menu gives a directory row two verbs: the row body settles the pick (the folder
		* itself becomes an atomic reference and the menu closes) while only the ~14px trailing chevron
		* (or Tab) drills into it. That is fine with a mouse and keyboard; on a phone the chevron is a
		* poor target, so tapping a folder row referenced the folder and the user never reached the
		* files inside — reported as "the @ feature is unusable" (#150 / #144 / #163).
		*
		* The row-body behavior itself is fixed one layer down, in the engine tree: patch
		* `reference-drill-F6` makes the mobile form's directory rows settle into the folder. This
		* enhancer therefore owns only the multi-select chrome.
		*
		* ## 0.13.8（本版按审计 #169 的六条逐条修）
		*
		* 1. **勾选态落地**：状态挂在**行元素**上（`data-dsh-ref-on`），视觉 100% 由 CSS 从该属性派生；
		*    没有任何 JS「视觉同步」步骤，因此不存在「集合已选中而方框未勾」的失配窗口。
		* 2. **不再有无界 rAF/DOM 抖动**：`renderBar()` 幂等——节点只建一次，之后只改文本；
		*    绝不 `innerHTML=''` 重建（旧实现在选中期间每帧重建 → 触发 observer → 再重建）。
		* 3. **不再静默丢弃选择**：多选插入逐个进行，某个候选找不到时**保留剩余选择**并在底部条
		*    如实报告「已插入 k 项 / m 项未找到（可能已下钻目录）」，不再无声清空。
		* 4. **移动形态门**：非移动形态（宽视口 / 桌面模式）直接不注入——审计指出旧实现会在宽视口
		*    装一套手机专用 chrome，而此时 F6 不生效，形成未验证的第三种行为。
		* 5. **稳定身份**：多选键是「标签 + 同标签内序号」（`data-dsh-ref-key`），不是裸显示文本——
		*    同名文件/同名会话不再互相塌缩；已存在的键优先保留，列表变化时不打散已选项。
		* 6. **按行去抖 + 关菜单即清态**：去抖按「目标行」而非全局时间戳（400ms 内点第二行不再被吞）；
		*    菜单关闭时清空集合与底部条，重开不会出现幻影「已选 N 项」。
		*/
		const ROW_SELECTOR = "[data-trigger-menu] [role=\"option\"]";
		const MENU_SELECTOR$1 = "[data-trigger-menu]";
		const CHECK_ATTR = "data-dsh-ref-check";
		/** 选中标记（**视觉状态的唯一来源**：属性是状态，样式是后果，由 CSS 消费）。 */
		const ON_ATTR = "data-dsh-ref-on";
		/** 多选键（稳定身份：标签 + 同标签内序号），挂在行上。 */
		const KEY_ATTR = "data-dsh-ref-key";
		const BAR_ATTR = "data-dsh-ref-bar";
		const COUNT_ATTR = "data-dsh-ref-bar-count";
		const ADD_ATTR = "data-dsh-ref-add";
		/** Gesture kinds one tap can arrive as; only the first of an interaction acts. */
		const GESTURES = [
			"pointerdown",
			"mousedown",
			"click"
		];
		/** 同一行的一次点按（pointerdown + mousedown + click）折叠成一个动作的时间窗。 */
		const ROW_DEBOUNCE_MS = 400;
		/** 移动形态门（与 form-marker.ts 的 767px 单一来源一致）。 */
		const MOBILE_QUERY = "(max-width: 767px)";
		/** 品牌蓝。**不取 `--dsw-alias-brand-primary`**：该 token 在深色主题下实测解析为
		*  rgb(249,250,251)（近白），当底色配白字就是「白底白字不可见」。 */
		const BRAND = "#4d6bfe";
		/**
		* 勾选框与底部条样式。
		*
		* - 勾选框用 `<span>` + CSS 画（原生 input 在深色主题里是浏览器默认方块，与上游行样式不融）；
		* - 选中态由 `[data-dsh-ref-on]` 属性派生 —— 属性是状态，样式是后果，中间没有 JS 同步步骤；
		* - 底部条的关键样式在 JS 里内联 `!important`（上游 button 默认样式会盖过注入样式表）。
		*/
		const REFERENCE_BAR_CSS = `
[data-dsh-ref-check] {
  flex: none;
  width: 18px;
  height: 18px;
  margin: 0 10px 0 2px;
  align-self: center;
  border-radius: 5px;
  border: 1.5px solid #8b909a;
  background: transparent;
  box-sizing: border-box;
  position: relative;
  transition: background-color .12s ease, border-color .12s ease;
}
[data-dsh-ref-on] [data-dsh-ref-check] {
  background: ${BRAND};
  border-color: ${BRAND};
}
[data-dsh-ref-on] [data-dsh-ref-check]::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 1.5px;
  width: 4px;
  height: 8px;
  border: solid #ffffff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
[data-dsh-ref-bar] {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-top: 1px solid var(--dsw-alias-border-l1, #e5e5e5);
  background: var(--dsw-alias-bg-layer-2, #ffffff);
}
[data-dsh-ref-bar-count] {
  font-size: 13px;
  color: var(--dsw-alias-label-secondary, #5f6368);
}
[data-dsh-ref-add] {
  padding: 6px 14px;
  border-radius: 8px;
  border: none;
  background: ${BRAND};
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}
[data-dsh-ref-add]:active { filter: brightness(0.92); }
@media (prefers-color-scheme: dark) {
  [data-dsh-ref-check] { border-color: #6b7075; }
  [data-dsh-ref-bar] {
    border-top-color: var(--dsw-alias-border-l1, #2a2b30);
    background: var(--dsw-alias-bg-layer-2, #17181c);
  }
  [data-dsh-ref-bar-count] { color: var(--dsw-alias-label-secondary, #9aa0a6); }
}
`;
		/** Read a row's candidate label (upstream renders it in the name span; fall back to text). */
		function rowLabel(row) {
			const name = row.querySelector("[class*=\"itemName\"]");
			return (((name === null || name === void 0 ? void 0 : name.textContent) ?? row.textContent) || "").trim();
		}
		/** The composer's editable host (upstream Lexical root). */
		function composerEditable() {
			return document.querySelector("[data-composer-card] [contenteditable=\"true\"], [data-composer-card] textarea");
		}
		/** 是否移动形态（#169-4）：以页面标记或 767px 视口为准，宽视口不注入手机专用 chrome。 */
		function isMobileForm() {
			if (document.documentElement.hasAttribute("data-dsh-mobile-form")) return true;
			return typeof window.matchMedia === "function" && window.matchMedia(MOBILE_QUERY).matches;
		}
		/** Multi-select state plus the mobile-only row behavior for the reference menu. */
		var ReferenceMenuEnhancer = class {
			constructor() {
				_defineProperty(this, "checked", /* @__PURE__ */ new Set());
				_defineProperty(this, "observer", null);
				_defineProperty(this, "lastGesture", /* @__PURE__ */ new WeakMap());
				_defineProperty(this, "scheduled", false);
				_defineProperty(this, "onGesture", (event) => {
					if (!isMobileForm()) return;
					const target = event.target;
					if (!(target instanceof Element)) return;
					const row = target.closest(ROW_SELECTOR);
					if (row === null) return;
					if (target.closest("[" + CHECK_ATTR + "]") === null) return;
					event.preventDefault();
					event.stopImmediatePropagation();
					event.stopPropagation();
					const now = Date.now();
					if (now - (this.lastGesture.get(row) ?? 0) < ROW_DEBOUNCE_MS) return;
					this.lastGesture.set(row, now);
					this.toggle(row);
				});
				_defineProperty(this, "onMenuClick", (event) => {
					const target = event.target;
					if (!(target instanceof Element) || target.closest("[" + BAR_ATTR + "]") === null) return;
					if (target.closest("[" + ADD_ATTR + "]") === null) return;
					event.preventDefault();
					event.stopPropagation();
					this.addSelected();
				});
				_defineProperty(this, "lastReport", "");
			}
			attach() {
				for (const kind of GESTURES) document.addEventListener(kind, this.onGesture, true);
				document.addEventListener("click", this.onMenuClick, true);
				this.observer = new MutationObserver(() => {
					this.schedule();
				});
				this.observer.observe(document.body, {
					childList: true,
					subtree: true
				});
				this.schedule();
			}
			detach() {
				var _this$observer;
				for (const kind of GESTURES) document.removeEventListener(kind, this.onGesture, true);
				document.removeEventListener("click", this.onMenuClick, true);
				(_this$observer = this.observer) === null || _this$observer === void 0 || _this$observer.disconnect();
				this.observer = null;
				this.clearState();
			}
			/** Coalesce DOM churn into one enhance pass per frame. */
			schedule() {
				if (this.scheduled) return;
				this.scheduled = true;
				requestAnimationFrame(() => {
					this.scheduled = false;
					this.enhance();
				});
			}
			/** 清空多选状态与底部条（菜单关闭 / 卸载时；#169-6 的幻影残留防线）。 */
			clearState() {
				var _document$querySelect;
				this.checked.clear();
				document.querySelectorAll("[" + ON_ATTR + "]").forEach((el) => {
					el.removeAttribute(ON_ATTR);
				});
				(_document$querySelect = document.querySelector("[" + BAR_ATTR + "]")) === null || _document$querySelect === void 0 || _document$querySelect.remove();
			}
			/**
			* Ensure every row carries a checkbox + a stable key, re-apply the checked mark, refresh the bar.
			* 菜单不在场时清态（避免关掉菜单后重开还看到「已选 N 项」）。
			* 非移动形态直接不注入（#169-4）。
			*/
			enhance() {
				if (!isMobileForm()) {
					if (this.checked.size > 0) this.clearState();
					return;
				}
				if (document.querySelector(MENU_SELECTOR$1) === null) {
					if (this.checked.size > 0 || document.querySelector("[" + BAR_ATTR + "]") !== null) this.clearState();
					return;
				}
				const seen = /* @__PURE__ */ new Map();
				for (const row of document.querySelectorAll(ROW_SELECTOR)) {
					const label = rowLabel(row);
					const occurrence = (seen.get(label) ?? 0) + 1;
					seen.set(label, occurrence);
					const candidate = label + "#" + String(occurrence);
					const current = row.getAttribute(KEY_ATTR);
					const key = current !== null && this.checked.has(current) ? current : candidate;
					if (current !== key) row.setAttribute(KEY_ATTR, key);
					let box = row.querySelector("[" + CHECK_ATTR + "]");
					if (box === null) {
						box = document.createElement("span");
						box.setAttribute(CHECK_ATTR, "");
						box.setAttribute("role", "checkbox");
						box.setAttribute("aria-label", label);
						row.insertBefore(box, row.firstChild);
					}
					const on = this.checked.has(key);
					if (on) row.setAttribute(ON_ATTR, "");
					else row.removeAttribute(ON_ATTR);
					box.setAttribute("aria-checked", on ? "true" : "false");
				}
				this.renderBar();
			}
			/** Toggle one row：状态落在行元素上，随后由 CSS 呈现（无二次同步步骤）。 */
			toggle(row) {
				var _row$querySelector;
				const key = row.getAttribute(KEY_ATTR);
				if (key === null) return;
				const on = !row.hasAttribute(ON_ATTR);
				if (on) {
					row.setAttribute(ON_ATTR, "");
					this.checked.add(key);
				} else {
					row.removeAttribute(ON_ATTR);
					this.checked.delete(key);
				}
				(_row$querySelector = row.querySelector("[" + CHECK_ATTR + "]")) === null || _row$querySelector === void 0 || _row$querySelector.setAttribute("aria-checked", on ? "true" : "false");
				this.renderBar();
			}
			/**
			* 底部条关键样式内联写入：`style.setProperty(..., 'important')` 优先级高于任何样式表规则
			* （含上游对 `button` 的默认样式——真机实测过一次「白底白字」正是这个原因）。
			* 颜色不取 `--dsw-alias-brand-primary`（深色下近白），用显式品牌蓝。
			*/
			applyBarStyles(bar, count, add) {
				const set = (el, prop, value) => {
					el.style.setProperty(prop, value, "important");
				};
				const dark = typeof window.matchMedia === "function" && window.matchMedia("(prefers-color-scheme: dark)").matches;
				set(bar, "display", "flex");
				set(bar, "gap", "8px");
				set(bar, "align-items", "center");
				set(bar, "justify-content", "space-between");
				set(bar, "padding", "8px 12px");
				set(bar, "border-top", "1px solid var(--dsw-alias-border-l1, " + (dark ? "#2a2b30" : "#e5e5e5") + ")");
				set(bar, "background", "var(--dsw-alias-bg-layer-2, " + (dark ? "#17181c" : "#ffffff") + ")");
				set(count, "font-size", "13px");
				set(count, "color", "var(--dsw-alias-label-secondary, " + (dark ? "#9aa0a6" : "#5f6368") + ")");
				set(add, "background-color", BRAND);
				set(add, "background-image", "none");
				set(add, "color", "#ffffff");
				set(add, "border", "none");
				set(add, "border-radius", "8px");
				set(add, "padding", "6px 14px");
				set(add, "font-size", "13px");
				set(add, "font-weight", "500");
				set(add, "line-height", "1.4");
				set(add, "appearance", "none");
			}
			/**
			* 底部条渲染（**幂等**，#169-2）：节点只建一次，之后只更新文本；绝不 `innerHTML=''` 重建
			* ——旧实现每帧重建子节点会触发 MutationObserver → schedule() → 再重建，选中期间持续抖动。
			*/
			renderBar() {
				const menu = document.querySelector(MENU_SELECTOR$1);
				const existing = document.querySelector("[" + BAR_ATTR + "]");
				if (menu === null || this.checked.size === 0) {
					existing === null || existing === void 0 || existing.remove();
					return;
				}
				let bar = existing;
				if (bar === null) {
					bar = document.createElement("div");
					bar.setAttribute(BAR_ATTR, "");
					const count = document.createElement("span");
					count.setAttribute(COUNT_ATTR, "");
					const add = document.createElement("button");
					add.type = "button";
					add.setAttribute(ADD_ATTR, "");
					bar.append(count, add);
					this.applyBarStyles(bar, count, add);
					menu.appendChild(bar);
				}
				const count = bar.querySelector("[" + COUNT_ATTR + "]");
				const add = bar.querySelector("[" + ADD_ATTR + "]");
				if (count !== null) count.textContent = this.statusText("已选 " + String(this.checked.size) + " 项");
				if (add !== null) add.textContent = "添加 " + String(this.checked.size) + " 项";
			}
			statusText(base) {
				return this.lastReport === "" ? base : base + " · " + this.lastReport;
			}
			/** Insert every checked candidate through upstream's settle-pick, one reference at a time. */
			async addSelected() {
				const keys = [...this.checked];
				let inserted = 0;
				this.lastReport = "";
				for (const key of keys) {
					if (!await this.pickByKey(key)) break;
					inserted++;
					this.checked.delete(key);
				}
				if (this.checked.size === 0) {
					this.clearState();
					return;
				}
				this.lastReport = "已插入 " + String(inserted) + " 项，" + String(this.checked.size) + " 项未找到（可能已下钻目录）";
				for (const row of document.querySelectorAll(ROW_SELECTOR)) {
					const key = row.getAttribute(KEY_ATTR);
					if (key === null) continue;
					if (this.checked.has(key)) row.setAttribute(ON_ATTR, "");
					else row.removeAttribute(ON_ATTR);
				}
				this.renderBar();
			}
			/**
			* Drive one upstream pick for `key`: focus the composer, (re)open the menu with `@` when it
			* closed, then settle the matching row. Upstream owns the reference it inserts; a row that never
			* appears ends the sequence (reported by the caller) rather than inventing text upstream would
			* not have produced.
			*/
			async pickByKey(key) {
				const editable = composerEditable();
				if (editable === null) return false;
				editable.focus();
				if (document.querySelector(MENU_SELECTOR$1) === null) {
					document.execCommand("insertText", false, "@");
					if (!await this.waitFor(() => this.findRow(key) !== null)) return false;
				}
				const row = this.findRow(key);
				if (row === null) return false;
				row.dispatchEvent(new MouseEvent("mousedown", {
					bubbles: true,
					cancelable: true
				}));
				await this.waitFor(() => document.querySelector(MENU_SELECTOR$1) === null || this.findRow(key) === null, 600);
				return true;
			}
			/** The row whose stable key matches（#169-5：不再按显示文本取第一个同名行）。 */
			findRow(key) {
				for (const row of document.querySelectorAll(ROW_SELECTOR)) if (row.getAttribute(KEY_ATTR) === key) return row;
				return null;
			}
			/** Poll one predicate for up to `timeout` ms (menu open/close is not observable otherwise). */
			waitFor(predicate, timeout = 1500) {
				return new Promise((resolve) => {
					const started = Date.now();
					const tick = () => {
						if (predicate()) {
							resolve(true);
							return;
						}
						if (Date.now() - started > timeout) {
							resolve(false);
							return;
						}
						setTimeout(tick, 60);
					};
					tick();
				});
			}
		};
		//#endregion
		//#region src/client/mobile/back-stack.ts
		/** The phone-form marker (`mobile/form-marker.ts`); the drawer is a layer only on phones. */
		const MOBILE_FORM_ATTR$2 = "data-dsh-mobile-form";
		/** The frame root, tagged by the form marker; identified before the tag lands by its right column. */
		const FRAME_SELECTOR$1 = "[data-dsh-frame]";
		const RIGHT_COL_SELECTOR = "[data-rightbar-col]";
		/** Frame attribute: present while the left sidebar is collapsed (drawer closed). */
		const SIDEBAR_COLLAPSED_ATTR$1 = "data-sidebar-collapsed";
		/** Every upstream modal surface (settings panel, ui-primitives Modal, image lightbox). */
		const DIALOG_SELECTOR = "[role=\"dialog\"][aria-modal=\"true\"]";
		/** Accessible names of the trajectory "Event details" side panel (zh + en dictionaries). */
		const TRAJECTORY_LABELS = ["Event details", "事件详情"];
		/**
		* Right column in its fullscreen presentation **and actually shown**.
		*
		* The panel element keeps `data-sidebar-right-panel="fullscreen"` while the column is collapsed
		* (upstream derives the attribute from the mode alone, and the mode is remembered); what marks the
		* column as presented is `data-sidebar-right-open`, written only while expanded, with
		* `aria-hidden` following it (`SidebarRight.tsx:298-303`). Measured on the MuMu x86_64 build: a
		* collapsed panel carries `aria-hidden="true"` and no open attribute, yet keeps the fullscreen
		* attribute. Counting it as a layer produced a phantom layer that consumed every back press
		* forever (IX-BG-12 red: four presses, depth stuck at 1, the activity never finished).
		*/
		const RIGHT_FULLSCREEN_SELECTOR = "[data-sidebar-right-panel=\"fullscreen\"][data-sidebar-right-open]:not([aria-hidden=\"true\"])";
		const MENU_SELECTOR = "[data-trigger-menu]";
		/**
		* This plugin's own attachment-source popup (attachment-picker-menu.ts).
		*
		* D8: the menu is mounted on document.body and removed on close, so its presence is exactly
		* "open". It was not a layer, and the shell's back callback treats "no layer" as FINISH_ACTIVITY
		* (BackGate.kt:56-60), so pressing system back with the menu open exited the app instead of
		* closing it.
		*/
		const ATTACHMENT_MENU_SELECTOR = "[data-dsh-attachment-picker-menu]";
		/** The drilled-listing breadcrumb header (a `nav`; the candidate list is a `div[role=listbox]`). */
		const MENU_DRILL_SELECTOR = "[data-trigger-menu] nav";
		/** Hashed CSS-module close controls still carry the class token (`[class*=ledger]` precedent). */
		const CLOSE_CLASS_HINT = "[class*=\"close\"]";
		/**
		* One detail level of a global main panel: the plugin manager's package, item,
		* and row pages each carry one of these stable attributes
		* (ui-plugin-manager PluginManagerPage.tsx), and each draws its own crumb.
		*/
		const PANEL_DETAIL_SELECTORS$1 = [
			"[data-plugin-detail]",
			"[data-plugin-item-detail]",
			"[data-plugin-row-detail]"
		];
		/** The crumb button inside one of those levels (class token survives CSS-module hashing). */
		const CRUMB_BUTTON_HINT = "button[class*=\"crumb\"]";
		/** This plugin's back control, mounted by mobile/main-panel-back.ts at the list root. */
		const PANEL_BACK_SELECTOR = "[data-dsh-main-panel-back]";
		/** Attributes any layer's presence is derived from; the filter keeps the observer cheap. */
		const OBSERVED_ATTRIBUTES = [
			MOBILE_FORM_ATTR$2,
			SIDEBAR_COLLAPSED_ATTR$1,
			"role",
			"aria-modal",
			"aria-label",
			"aria-hidden",
			"data-sidebar-right-panel",
			"data-sidebar-right-open",
			"data-trigger-menu",
			"data-dsh-attachment-picker-menu",
			"data-plugin-panel",
			"data-dsh-main-panel-back"
		];
		/** Dispatch one pointerdown, degrading to MouseEvent where PointerEvent is absent. */
		function dispatchPointerDown(target) {
			if (typeof PointerEvent === "function") {
				target.dispatchEvent(new PointerEvent("pointerdown", {
					bubbles: true,
					cancelable: true
				}));
				return;
			}
			target.dispatchEvent(new MouseEvent("pointerdown", {
				bubbles: true,
				cancelable: true
			}));
		}
		/** Dispatch one mouse gesture (bubbling + cancelable: React handlers and `preventDefault` both rely on it). */
		function dispatchMouse(target, type) {
			target.dispatchEvent(new MouseEvent(type, {
				bubbles: true,
				cancelable: true
			}));
		}
		/**
		* Close a modal surface through its own dismissal path.
		* @param dialog - the `[role=dialog][aria-modal]` element.
		* @returns whether a control was found and triggered.
		*/
		function closeDialog(dialog) {
			var _dialog$parentElement;
			const mask = ((_dialog$parentElement = dialog.parentElement) === null || _dialog$parentElement === void 0 ? void 0 : _dialog$parentElement.querySelector(":scope > [aria-hidden=\"true\"]")) ?? dialog.querySelector(":scope > [aria-hidden=\"true\"]");
			if (mask !== null && mask !== void 0) {
				dispatchMouse(mask, "mousedown");
				dispatchMouse(mask, "click");
				return true;
			}
			const close = dialog.querySelector(`button${CLOSE_CLASS_HINT}`) ?? dialog.querySelector(CLOSE_CLASS_HINT);
			if (close !== null) {
				dispatchMouse(close, "click");
				return true;
			}
			const backdrop = dialog.parentElement;
			if (backdrop !== null) {
				dispatchMouse(backdrop, "click");
				return true;
			}
			return false;
		}
		/**
		* Close the trajectory "Event details" side panel through its own close button.
		* @param aside - the panel element.
		* @returns whether the close control was found and triggered.
		*/
		function closeTrajectoryDetails(aside) {
			const close = aside.querySelector(`button${CLOSE_CLASS_HINT}`) ?? aside.querySelector(CLOSE_CLASS_HINT);
			if (close === null) return false;
			dispatchMouse(close, "click");
			return true;
		}
		/**
		* Leave the right column's fullscreen presentation.
		* @param panel - the `[data-sidebar-right-panel=fullscreen]` element.
		* @returns whether a control was found and triggered.
		*/
		function closeRightFullscreen(panel) {
			const target = panel.querySelector("[data-sidebar-right-mode]") ?? panel.querySelector("[data-sidebar-right-toggle]");
			if (target === null) return false;
			dispatchMouse(target, "click");
			return true;
		}
		/**
		* Leave one level of an upstream global main panel.
		*
		* The plugin manager owns a depth hierarchy (list -> package -> row) and each
		* detail level draws its own crumb control, so a press pops exactly one level
		* through that control rather than writing the page's store. The list root
		* draws no control of its own, so the fallback is this plugin's back button
		* (mobile/main-panel-back.ts), which is the control that layer closes through.
		* @returns whether a control was found and triggered.
		*/
		function closeMainPanel(leave) {
			const details = PANEL_DETAIL_SELECTORS$1.flatMap((selector) => [...document.querySelectorAll(selector)]);
			const detail = details[details.length - 1];
			const crumb = (detail === null || detail === void 0 ? void 0 : detail.querySelector(CRUMB_BUTTON_HINT)) ?? null;
			if (crumb !== null) {
				dispatchMouse(crumb, "click");
				return true;
			}
			const back = document.querySelector(PANEL_BACK_SELECTOR);
			if (back !== null) {
				dispatchMouse(back, "click");
				return true;
			}
			leave();
			return true;
		}
		/**
		* Dismiss a trigger menu (slash / `@`) through the menu's own dismissal path.
		* @param menu - the `[data-trigger-menu]` element.
		* @returns whether the dismissal was dispatched.
		*/
		function closeMenu(menu) {
			if (menu.contains(document.body)) return false;
			dispatchPointerDown(document.body);
			return true;
		}
		/**
		* Pop one drill-down level of an `@` menu listing.
		* @param nav - the breadcrumb header inside the menu.
		* @returns whether an ancestor step was found and triggered.
		*/
		function popMenuDrill(nav) {
			const ancestors = [...nav.querySelectorAll("button")].filter((button) => button.getAttribute("aria-current") === null && !button.disabled);
			const parent = ancestors[ancestors.length - 1];
			if (parent === void 0) return false;
			dispatchMouse(parent, "mousedown");
			return true;
		}
		/**
		* The page-side layer stack behind the shell's system-back callback.
		*
		* `attach` publishes `window.__dshBack` and starts observing; `detach` removes
		* the observer, the globals, and the shell's cached availability (a hot unload
		* must not leave the shell believing a layer is still up).
		*/
		var BackStackSignal = class {
			/** @param options - the drawer toggle callback. */
			constructor(options) {
				_defineProperty(this, "options", void 0);
				_defineProperty(this, "observer", null);
				_defineProperty(this, "layers", []);
				_defineProperty(this, "seq", 0);
				_defineProperty(this, "depth", -1);
				_defineProperty(this, "kinds", []);
				_defineProperty(this, "uplinked", false);
				_defineProperty(this, "available", false);
				_defineProperty(this, "attached", false);
				this.options = options;
			}
			/** Publish the back entry and keep the stack current. */
			attach() {
				if (this.attached) return;
				this.attached = true;
				window.__dshBack = () => this.popTop();
				this.observer = new MutationObserver(() => {
					this.sync();
				});
				this.observer.observe(document.documentElement, {
					childList: true,
					subtree: true,
					attributes: true,
					attributeFilter: OBSERVED_ATTRIBUTES
				});
				this.sync();
			}
			/** Stop observing and remove every trace of the signal. */
			detach() {
				var _this$observer;
				if (!this.attached) return;
				this.attached = false;
				(_this$observer = this.observer) === null || _this$observer === void 0 || _this$observer.disconnect();
				this.observer = null;
				this.layers = [];
				delete window.__dshBack;
				delete window.__dshBackDepth;
				delete window.__dshBackKinds;
				this.uplinked = false;
				this.publish();
			}
			/** The layer kinds currently stacked, bottom to top (device-side assertions read the global). */
			currentKinds() {
				return this.kinds;
			}
			/**
			* Re-reconcile now.
			*
			* The main-panel layer fact lives in the layout service, not in the DOM, so a panel
			* switch is also pushed from its subscription; a DOM change that leaves the same panel
			* selected must not be the only trigger.
			*/
			refresh() {
				if (this.attached) this.sync();
			}
			/**
			* Pop the topmost layer through its own control.
			* @returns whether a layer existed (the shell consumes the press either way);
			*   the stack itself only shrinks when the layer's anchor leaves the DOM.
			*/
			popTop() {
				this.sync();
				const top = this.layers[this.layers.length - 1];
				if (top === void 0) return false;
				try {
					return top.close();
				} catch {
					return false;
				}
			}
			/** Reconcile the observed layers with the stack, keeping open order. */
			sync() {
				const detected = this.detect();
				const next = [];
				for (const detection of detected) {
					const existing = this.layers.find((layer) => layer.id === detection.id);
					next.push(existing === void 0 ? {
						...detection,
						seq: ++this.seq
					} : {
						...existing,
						close: detection.close
					});
				}
				next.sort((a, b) => a.seq - b.seq);
				this.layers = next;
				this.publish();
			}
			/** Every layer currently in the DOM, in a fixed detection order. */
			detect() {
				const found = [];
				const drawer = this.detectDrawer();
				if (drawer !== null) found.push(drawer);
				const mainPanel = this.detectMainPanel();
				if (mainPanel !== null) found.push(mainPanel);
				for (const dialog of this.detectDialogs()) found.push(dialog);
				const trajectory = this.detectTrajectoryDetails();
				if (trajectory !== null) found.push(trajectory);
				const fullscreen = this.detectRightFullscreen();
				if (fullscreen !== null) found.push(fullscreen);
				const menu = this.detectMenu();
				if (menu !== null) found.push(menu);
				const drill = this.detectMenuDrill();
				if (drill !== null) found.push(drill);
				const attachmentMenu = this.detectAttachmentMenu();
				if (attachmentMenu !== null) found.push(attachmentMenu);
				return found;
			}
			/** The drawer: the phone form's expanded left sidebar. */
			detectDrawer() {
				var _document$querySelect;
				if (!document.documentElement.hasAttribute(MOBILE_FORM_ATTR$2)) return null;
				const frame = document.querySelector(FRAME_SELECTOR$1) ?? ((_document$querySelect = document.querySelector(RIGHT_COL_SELECTOR)) === null || _document$querySelect === void 0 ? void 0 : _document$querySelect.parentElement) ?? null;
				if (frame === null || frame.hasAttribute(SIDEBAR_COLLAPSED_ATTR$1)) return null;
				return {
					id: "drawer",
					kind: "drawer",
					close: () => {
						this.options.toggleSidebar();
						return true;
					}
				};
			}
			/**
			* The presented global main panel (plugin manager, task manager).
			*
			* It is not a dialog, so nothing else in this stack sees it: without this
			* layer a back press finished the activity from inside the page. The stack
			* position is above the drawer (the panel covers it) and below dialogs and
			* menus, so a surface opened over the panel still closes first.
			*/
			detectMainPanel() {
				if (this.options.activePanelId() === null) return null;
				return {
					id: "main-panel",
					kind: "main-panel",
					close: () => closeMainPanel(() => {
						this.options.leaveMainPanel();
					})
				};
			}
			/** Every modal surface, in document order (settings panel, Modal, lightbox). */
			detectDialogs() {
				return [...document.querySelectorAll(DIALOG_SELECTOR)].map((dialog, index) => ({
					id: `dialog:${String(index)}`,
					kind: "dialog",
					close: () => closeDialog(dialog)
				}));
			}
			/** The trajectory inspector side panel. */
			detectTrajectoryDetails() {
				for (const aside of document.querySelectorAll("aside[aria-label]")) {
					if (!TRAJECTORY_LABELS.includes(aside.getAttribute("aria-label") ?? "")) continue;
					return {
						id: "trajectory-details",
						kind: "trajectory-details",
						close: () => closeTrajectoryDetails(aside)
					};
				}
				return null;
			}
			/** The right column's fullscreen presentation. */
			detectRightFullscreen() {
				const panel = document.querySelector(RIGHT_FULLSCREEN_SELECTOR);
				if (panel === null) return null;
				return {
					id: "right-fullscreen",
					kind: "right-fullscreen",
					close: () => closeRightFullscreen(panel)
				};
			}
			/** An open command/reference menu. */
			detectMenu() {
				const menu = document.querySelector(MENU_SELECTOR);
				if (menu === null) return null;
				return {
					id: "menu",
					kind: "menu",
					close: () => closeMenu(menu)
				};
			}
			/**
			* This plugin's own attachment-source menu.
			*
			* Distinct from the upstream trigger menus: it is our node, mounted on document.body, and the
			* enhancer already closes it on the same outside-pointerdown gesture upstream menus use - so the
			* shared dismissal path below is the right control.
			*/
			detectAttachmentMenu() {
				const menu = document.querySelector(ATTACHMENT_MENU_SELECTOR);
				if (menu === null) return null;
				return {
					id: "attachment-menu",
					kind: "attachment-menu",
					close: () => closeMenu(menu)
				};
			}
			/** A menu listing descended into a directory (its breadcrumb header is up). */
			detectMenuDrill() {
				const nav = document.querySelector(MENU_DRILL_SELECTOR);
				if (nav === null) return null;
				return {
					id: "menu-drill",
					kind: "menu-drill",
					close: () => popMenuDrill(nav)
				};
			}
			/** Publish the observability globals and the shell uplink (only on change). */
			publish() {
				const kinds = this.layers.map((layer) => layer.kind);
				if (kinds.length !== this.depth || kinds.some((kind, index) => kind !== this.kinds[index])) {
					this.depth = kinds.length;
					this.kinds = kinds;
					window.__dshBackDepth = kinds.length;
					window.__dshBackKinds = kinds;
				}
				const available = kinds.length > 0;
				if (this.uplinked && available === this.available) return;
				this.uplinked = true;
				this.available = available;
				try {
					var _window$dshBackBridge, _window$dshBackBridge2;
					(_window$dshBackBridge = window.dshBackBridge) === null || _window$dshBackBridge === void 0 || (_window$dshBackBridge2 = _window$dshBackBridge.setAvailable) === null || _window$dshBackBridge2 === void 0 || _window$dshBackBridge2.call(_window$dshBackBridge, available);
				} catch {}
			}
		};
		//#endregion
		//#region src/client/mobile/main-panel-back.css.ts
		/**
		* Skin for the injected main-panel back control (mobile/main-panel-back.ts).
		*
		* The page head (PluginManagerPage `.pageHead`) is a `space-between` flex row of
		* [title block, toolbar]. Our button becomes its first child, so the row needs three
		* adjustments on the phone:
		* - `justify-content: flex-start` stops the three items being spread across the row;
		* - the title block takes the remaining width (`flex: 1`, `min-width: 0`) so it
		*   truncates instead of pushing the toolbar past the edge;
		* - the toolbar stops being the `space-between` end and is pinned right by the title
		*   block's growth, which keeps the refresh button and "add plugin" at their own gap.
		*
		* AT 360px the row is the tightest case the phone form serves: the head measured
		* refresh [196,224] and add [240,336], i.e. a 16px gap with 24px of right padding.
		* The back control is therefore `flex: none` and sized to its content rather than
		* taking a share of the row, and the title block absorbs the squeeze (it wraps).
		*/
		const MAIN_PANEL_BACK_CSS = `
@media (max-width: 767px) {
  html[data-dsh-mobile-form] [data-plugin-panel] > header[data-window-drag] {
    justify-content: flex-start;
    gap: 10px;
  }

  html[data-dsh-mobile-form] [data-plugin-panel] > header[data-window-drag] > div:first-of-type {
    flex: 1 1 auto;
    min-width: 0;
  }

  html[data-dsh-mobile-form] [data-dsh-main-panel-back] {
    display: inline-flex;
    flex: none;
    align-items: center;
    gap: 2px;
    height: 28px;
    /* Aligns with the head's first text line rather than the row's top edge. */
    margin-top: 1px;
    padding: 0 8px 0 2px;
    border: 0;
    border-radius: var(--dsw-radius-md, 6px);
    background: transparent;
    color: var(--dsw-alias-label-tertiary);
    font-size: 13px;
    line-height: 28px;
    white-space: nowrap;
    cursor: pointer;
  }

  html[data-dsh-mobile-form] [data-dsh-main-panel-back]:hover,
  html[data-dsh-mobile-form] [data-dsh-main-panel-back]:focus-visible {
    background: var(--dsw-alias-interactive-bg-hover);
    color: var(--dsw-alias-label-primary);
  }
}
`;
		//#endregion
		//#region src/client/mobile/main-panel-back.ts
		/**
		* Visible back control for upstream's global main panels (0.14.2 FX1-C).
		*
		* The defect (user, 2026-09-27): "这个左侧菜单栏里的新增选项没有返回键，没有深度层级，
		* 点进去就出不来" — the sidebar's "插件" row switches the centre column to upstream's
		* plugin-manager main panel, and that page's list root draws a head row (title,
		* refresh, "add plugin") with **no** way back. Upstream is a read-only checkout here,
		* so the control is injected from this package instead of patching the page.
		*
		* Where the page already has its own control, this module stays out of the way: the
		* package/item/row detail levels each render a crumb button in their `DetailTop`
		* (PluginManagerPage.tsx), which pops one level. Only the list root lacks one, so only
		* the list root gets our button. That keeps each level's own control authoritative and
		* stops a second back affordance from appearing on a level that has one.
		*
		* The page is React-owned: it re-renders on every poll, install, and navigation, and
		* React may drop an externally-inserted node at any commit. Mounting is therefore a
		* reconcile loop over a MutationObserver (the attachment-picker-menu precedent), never
		* a one-shot insert: a button React removed is re-inserted on the next batch, and a
		* button whose level gained its own crumb is removed again.
		*/
		const PANEL_SELECTOR = "[data-plugin-panel]";
		/** The page head row; `data-window-drag` separates it from the intro/status siblings. */
		const HEAD_SELECTOR = ":scope > header[data-window-drag]";
		/** Our control. Read by back-stack.ts as the list level's own closure. */
		const MAIN_PANEL_BACK_ATTR = "data-dsh-main-panel-back";
		/**
		* One detail level of the plugin manager (package, ledger item, configurable row).
		*
		* Shared with back-stack.ts so the two cannot drift: this module hides its button while
		* one is present, and the stack pops that level through the crumb instead.
		*/
		const PANEL_DETAIL_SELECTORS = [
			"[data-plugin-detail]",
			"[data-plugin-item-detail]",
			"[data-plugin-row-detail]"
		];
		/** The phone form gate (mobile/form-marker.ts); the control is a phone affordance. */
		const MOBILE_FORM_ATTR$1 = "data-dsh-mobile-form";
		/** Back to the Conversation. */
		const BACK_LABEL = "返回会话";
		/** A chevron pointing left; drawn here so no icon package is pulled into this plugin. */
		function chevronLeft() {
			const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
			svg.setAttribute("viewBox", "0 0 18 18");
			svg.setAttribute("width", "18");
			svg.setAttribute("height", "18");
			svg.setAttribute("fill", "none");
			svg.setAttribute("aria-hidden", "true");
			const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
			path.setAttribute("d", "M11 4 6 9l5 5");
			path.setAttribute("stroke", "currentColor");
			path.setAttribute("stroke-width", "1.6");
			path.setAttribute("stroke-linecap", "round");
			path.setAttribute("stroke-linejoin", "round");
			svg.appendChild(path);
			return svg;
		}
		/** Whether the plugin-manager page is presenting one of its detail levels. */
		function detailLevelPresent(panel) {
			return PANEL_DETAIL_SELECTORS.some((selector) => panel.querySelector(selector) !== null);
		}
		/**
		* Mounts the list root's back control into the plugin-manager page head.
		*
		* `attach` starts the reconcile loop; `detach` removes the observer and every control
		* this instance mounted, so a hot unload leaves no orphan button behind.
		*/
		var MainPanelBackMount = class {
			/** @param leave - return to the Conversation (upstream `ctx.layout.selectPanel(null)`). */
			constructor(leave) {
				_defineProperty(this, "observer", null);
				_defineProperty(this, "scheduled", false);
				_defineProperty(this, "attached", false);
				_defineProperty(this, "mounted", []);
				_defineProperty(this, "leave", void 0);
				this.leave = leave;
			}
			/** Start reconciling; safe to call twice. */
			attach() {
				if (this.attached) return;
				this.attached = true;
				try {
					this.observer = new MutationObserver(() => {
						this.schedule();
					});
					this.observer.observe(document.documentElement, {
						childList: true,
						subtree: true,
						attributes: true,
						attributeFilter: [MOBILE_FORM_ATTR$1]
					});
					this.sync();
				} catch (error) {
					console.warn("[dsh-main-panel-back] attach failed; the page keeps its own controls", error);
				}
			}
			/** Stop reconciling and remove this instance's controls. */
			detach() {
				var _this$observer;
				if (!this.attached) return;
				this.attached = false;
				(_this$observer = this.observer) === null || _this$observer === void 0 || _this$observer.disconnect();
				this.observer = null;
				this.scheduled = false;
				for (const button of this.mounted) button.remove();
				this.mounted.length = 0;
			}
			/**
			* Whether a tracked button or its page left the DOM.
			*
			* False while nothing is mounted keeps an idle enhancer from running a document-wide
			* query on every unrelated render batch (attachment-picker-menu's measure).
			*/
			dirty() {
				for (const button of this.mounted) if (!button.isConnected || button.parentElement === null) return true;
				return false;
			}
			/** Whether a mutated node is, or contains, the page we mount into. */
			carriesPanel(node) {
				if (!(node instanceof Element)) return false;
				return node.matches(PANEL_SELECTOR) || node.querySelector(PANEL_SELECTOR) !== null;
			}
			schedule() {
				if (this.scheduled) return;
				this.scheduled = true;
				queueMicrotask(() => {
					this.scheduled = false;
					if (this.attached) this.sync();
				});
			}
			/**
			* Reconcile our controls with the live page.
			*
			* Cheap-exit first: with nothing mounted and no panel in the mutated batch this pass
			* does no document-wide query at all.
			*/
			sync() {
				if (this.mounted.length === 0 && document.querySelector(PANEL_SELECTOR) === null) return;
				try {
					for (let index = this.mounted.length - 1; index >= 0; index -= 1) if (!this.mounted[index].isConnected) this.mounted.splice(index, 1);
					const mobileForm = document.documentElement.hasAttribute(MOBILE_FORM_ATTR$1);
					const panel = document.querySelector(PANEL_SELECTOR);
					const head = (panel === null || panel === void 0 ? void 0 : panel.querySelector(HEAD_SELECTOR)) ?? null;
					if (!(mobileForm && head !== null && panel !== null && !detailLevelPresent(panel))) {
						for (const button of this.mounted) button.remove();
						this.mounted.length = 0;
						return;
					}
					for (const button of this.mounted) if (button.parentElement !== head) head.insertBefore(button, head.firstChild);
					if (this.mounted.length === 0) {
						const button = this.createButton();
						head.insertBefore(button, head.firstChild);
						this.mounted.push(button);
					}
				} catch (error) {
					console.warn("[dsh-main-panel-back] sync failed; the page keeps its own controls", error);
				}
			}
			createButton() {
				const button = document.createElement("button");
				button.type = "button";
				button.className = "dsh-main-panel-back";
				button.setAttribute(MAIN_PANEL_BACK_ATTR, "");
				button.setAttribute("aria-label", BACK_LABEL);
				button.appendChild(chevronLeft());
				const text = document.createElement("span");
				text.textContent = "返回";
				button.appendChild(text);
				button.addEventListener("click", (event) => {
					event.preventDefault();
					this.leave();
				});
				return button;
			}
		};
		//#endregion
		//#region src/client/mobile/panel-nav-drawer.ts
		/**
		* Move the phone drawer aside when a global main panel is selected (0.14.2 FX1-C).
		*
		* The panel the drawer navigates to is drawn in the centre column, but on the phone
		* form the sidebar is a `position: fixed` off-canvas overlay 289px wide (mobile-form.css.ts).
		* Selecting a panel therefore left the drawer covering the panel it had just opened -
		* including that panel's own back control, which the user then could not see or tap
		* ("点进去就出不来" / "你关闭键呢").
		*
		* Upstream cannot do this itself: `selectPanel` only writes `panelInfo.activePanelId`,
		* and the docked desktop sidebar has no reason to close. Closing only on the
		* Conversation -> panel transition (rather than on every notification) keeps a user who
		* re-opens the drawer while a panel is presented in control: that drawer stays open.
		*/
		/** Phone-form marker (mobile/form-marker.ts); the drawer is an overlay only on phones. */
		const MOBILE_FORM_ATTR = "data-dsh-mobile-form";
		/** Frame root tag written by the form marker. */
		const FRAME_SELECTOR = "[data-dsh-frame]";
		/** Frame attribute: present while the left drawer is collapsed. */
		const SIDEBAR_COLLAPSED_ATTR = "data-sidebar-collapsed";
		/**
		* Collapses the drawer once, when a panel selection replaces the Conversation.
		*
		* Not a subscription owner: the caller feeds it the layout service's notifications,
		* so this class holds no listener of its own and cannot leak one.
		*/
		var PanelNavDrawer = class {
			/** @param deps - frame facts and the drawer action. */
			constructor(deps) {
				_defineProperty(this, "deps", void 0);
				_defineProperty(this, "lastPanelId", void 0);
				this.deps = deps;
				this.lastPanelId = deps.activePanelId();
			}
			/**
			* Reconcile against the current panel selection.
			* @returns whether the drawer was collapsed by this call.
			*/
			sync() {
				const panelId = this.deps.activePanelId();
				const opened = panelId !== null && this.lastPanelId === null;
				this.lastPanelId = panelId;
				if (!opened) return false;
				if (!document.documentElement.hasAttribute(MOBILE_FORM_ATTR)) return false;
				const frame = this.deps.frame() ?? document.querySelector(FRAME_SELECTOR);
				if (frame === null || frame.hasAttribute(SIDEBAR_COLLAPSED_ATTR)) return false;
				this.deps.collapseDrawer();
				return true;
			}
		};
		//#endregion
		//#region src/client/mobile/upstream-browser/browser/BrowserFrame.ts
		/**
		* Create idle navigation state without a page target.
		* @returns state before any page has been requested.
		*/
		function emptyBrowserFrame() {
			return {
				target: void 0,
				address: "empty",
				loading: false,
				canGoBack: false,
				canGoForward: false,
				error: void 0,
				sandboxEnabled: void 0
			};
		}
		//#endregion
		//#region src/client/mobile/upstream-browser/browser/BrowserPersistence.ts
		/**
		* Read the selected address from a saved navigation record.
		* @param state - saved navigation.
		* @returns its last selected address, if any.
		*/
		function currentBrowserTarget(state) {
			return state === void 0 || state.index < 0 ? void 0 : state.entries[state.index];
		}
		/**
		* Checkpoint an observed address without serializing native history.
		* @param target - current address.
		* @param revision - navigation generation.
		* @returns an address-only checkpoint.
		*/
		function browserAddressCheckpoint(target, revision) {
			return {
				entries: [target],
				index: 0,
				request: {
					target,
					revision
				},
				navigation: {
					status: "known",
					revision
				},
				failure: void 0
			};
		}
		//#endregion
		//#region \0dsh-css:/home/agentuser/DeepSeek 2/dsh-upgrade-021/plugin/dsh-client-ui-responsive/src/client/mobile/upstream-browser/view/Browser.module.css.mjs
		const css$1 = ".Fi7DmW_root{height:100%;min-height:0;color:var(--dsw-alias-label-primary);background:var(--dsw-alias-bg-base);flex-direction:column;flex:auto;display:flex}.Fi7DmW_toolbar{box-sizing:border-box;border-bottom:.5px solid var(--dsw-alias-border-l3);flex:none;align-items:center;gap:4px;height:38px;padding:5px 6px;display:flex}.Fi7DmW_tool{width:28px;height:28px;color:var(--dsw-alias-label-secondary);border-radius:var(--dsw-radius-sm);cursor:pointer;background:0 0;border:0;flex:none;justify-content:center;align-items:center;padding:0;display:inline-flex}.Fi7DmW_tool:hover:not(:disabled){color:var(--dsw-alias-label-primary);background:var(--dsw-alias-interactive-bg-hover)}.Fi7DmW_tool:disabled{color:var(--dsw-alias-label-quaternary);cursor:default}.Fi7DmW_sandboxOff{color:var(--dsw-alias-state-error-primary);background:color-mix(in srgb, var(--dsw-alias-state-error-primary) 10%, transparent)}.Fi7DmW_addressBox{flex:auto;min-width:0;position:relative}.Fi7DmW_address{box-sizing:border-box;width:100%;height:28px;color:var(--dsw-alias-label-primary);font:var(--dsw-font-xxs-12);background:var(--dsw-alias-bg-layer-1);border:.5px solid var(--dsw-alias-border-l2);border-radius:var(--dsw-radius-sm);padding:0 34px 0 9px}.Fi7DmW_addressGo{visibility:hidden;opacity:0;position:absolute;top:0;right:0}.Fi7DmW_addressBox:focus-within .Fi7DmW_addressGo{visibility:visible;opacity:1}.Fi7DmW_addressUnknown{--dsh-scrollbar-thumb:var(--dsw-alias-scrollbar-bg-l2);--dsh-scrollbar-thumb-hover:var(--dsw-alias-scrollbar-hover-l2);color:var(--dsw-alias-label-tertiary);background:var(--dsw-alias-bg-layer-2)}.Fi7DmW_addressUnknown:focus{color:var(--dsw-alias-label-primary);background:var(--dsw-alias-bg-layer-1)}.Fi7DmW_addressChanged{color:var(--dsw-alias-label-tertiary);font:var(--dsw-font-xxxs-11);pointer-events:none;background:var(--dsw-alias-bg-layer-2);padding-left:8px;line-height:16px;position:absolute;top:6px;right:8px}.Fi7DmW_addressBox:focus-within .Fi7DmW_addressChanged{visibility:hidden;opacity:0}.Fi7DmW_address:focus{outline:1px solid var(--dsw-alias-state-business-primary);outline-offset:-1px}.Fi7DmW_frame{background:var(--dsw-alias-bg-base);border:0;flex:auto;width:100%;min-height:0;display:flex}.Fi7DmW_content{flex:auto;min-width:0;min-height:0;display:flex;position:relative}.Fi7DmW_viewport{flex:auto;min-width:0;min-height:0;display:flex}.Fi7DmW_placeholder{pointer-events:none;display:flex;position:absolute;inset:0}.Fi7DmW_restore{text-align:center;flex-direction:column;align-items:center;gap:10px;padding:24px;display:flex;position:absolute;inset:0;overflow:auto}.Fi7DmW_restoreLabel{color:var(--dsw-alias-label-tertiary);font:var(--dsw-font-xxs-12);margin:auto 0 0}.Fi7DmW_restoreTitle{overflow-wrap:anywhere;max-width:100%;font:var(--dsw-font-xs-13);margin:0;font-weight:500}.Fi7DmW_restoreUrl{overflow-wrap:anywhere;max-width:100%;color:var(--dsw-alias-label-secondary);font:var(--dsw-font-xxs-12);margin:0}.Fi7DmW_restore>:last-child{flex-shrink:0;margin-bottom:auto}.Fi7DmW_webview{-webkit-app-region:no-drag;border:0;flex:auto;width:100%;min-width:0;height:100%;min-height:0;display:flex}html:has([data-dockkit-pointer]) .Fi7DmW_webview{pointer-events:none}.Fi7DmW_start{min-height:0;color:var(--dsw-alias-label-tertiary);font:var(--dsw-font-xs-13);text-align:center;flex:auto;justify-content:center;align-items:center;padding:24px;display:flex}.Fi7DmW_failure{color:var(--dsw-alias-state-error-primary);font:var(--dsw-font-xxxs-11);background:color-mix(in srgb, var(--dsw-alias-state-error-primary) 8%, transparent);flex:none;padding:6px 12px}.Fi7DmW_sandboxWarning{color:var(--dsw-alias-label-secondary);font:var(--dsw-font-xxxs-11);background:var(--dsw-alias-state-warn-tertiary);flex:none;padding:6px 12px}.Fi7DmW_limit{color:var(--dsw-alias-label-tertiary);font:var(--dsw-font-xxxs-11);text-overflow:ellipsis;white-space:nowrap;border-top:.5px solid var(--dsw-alias-border-l3);flex:none;margin:0;padding:4px 10px;overflow:hidden}.Fi7DmW_titleIcon{flex:none;margin-right:4px}";
		const tagId$1 = "@dsh-android/dsh-client-ui-responsive/Browser.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$1) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@dsh-android/dsh-client-ui-responsive";
			tag.dataset.pluginCss = tagId$1;
			tag.textContent = css$1;
			document.head.appendChild(tag);
		}
		var Browser_module_css_default = {
			"address": "Fi7DmW_address",
			"addressBox": "Fi7DmW_addressBox",
			"addressChanged": "Fi7DmW_addressChanged",
			"addressGo": "Fi7DmW_addressGo",
			"addressUnknown": "Fi7DmW_addressUnknown",
			"content": "Fi7DmW_content",
			"failure": "Fi7DmW_failure",
			"frame": "Fi7DmW_frame",
			"limit": "Fi7DmW_limit",
			"placeholder": "Fi7DmW_placeholder",
			"restore": "Fi7DmW_restore",
			"restoreLabel": "Fi7DmW_restoreLabel",
			"restoreTitle": "Fi7DmW_restoreTitle",
			"restoreUrl": "Fi7DmW_restoreUrl",
			"root": "Fi7DmW_root",
			"sandboxOff": "Fi7DmW_sandboxOff",
			"sandboxWarning": "Fi7DmW_sandboxWarning",
			"start": "Fi7DmW_start",
			"titleIcon": "Fi7DmW_titleIcon",
			"tool": "Fi7DmW_tool",
			"toolbar": "Fi7DmW_toolbar",
			"viewport": "Fi7DmW_viewport",
			"webview": "Fi7DmW_webview"
		};
		//#endregion
		//#region src/client/mobile/upstream-browser/view/BrowserBody.tsx
		/** Common browser chrome; a presentation adapter attaches the page inside its content container. */
		const EMPTY_FRAME = emptyBrowserFrame();
		function SandboxPolicyIcon({ sandboxed }) {
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("svg", {
				width: "15",
				height: "15",
				viewBox: "0 0 16 16",
				fill: "none",
				"aria-hidden": true,
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("path", {
					d: _deepseek_ai_dsh_client_ui_primitives.SHIELD_OUTLINE_PATH,
					stroke: "currentColor",
					strokeWidth: _deepseek_ai_dsh_client_ui_primitives.ICON_REGULAR_STROKE,
					strokeLinejoin: "round"
				}), sandboxed ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("path", {
					d: "M12.1654 5.7552L8.9447 9.41475C8.73044 9.65816 8.53628 9.8804 8.35774 10.0423C8.1713 10.2114 7.94235 10.3717 7.64016 10.4254C7.48207 10.4535 7.32 10.4552 7.16151 10.4294C6.85843 10.3801 6.62728 10.2223 6.43836 10.0559C6.25752 9.89653 6.06037 9.67732 5.84264 9.43705L4.72925 8.20897L5.63557 7.38707L6.74897 8.61594C6.98603 8.87755 7.12974 9.03533 7.24673 9.13839C7.31033 9.19443 7.34485 9.21476 7.35823 9.22122C7.38068 9.22484 7.40352 9.22515 7.42593 9.22122C7.40522 9.22502 7.42893 9.23294 7.53583 9.136C7.65132 9.03126 7.79316 8.87139 8.02643 8.60638L11.2479 4.94763L12.1654 5.7552Z",
					fill: "currentColor"
				}) : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("path", {
					d: "M10.6074 4.40278L8.00975 6.99973L10.6074 9.59739L9.59736 10.6074L6.9997 8.00978L4.40274 10.6074L3.3927 9.59739L5.98966 6.99973L3.3927 4.40278L4.40274 3.39273L6.9997 5.98969L9.59736 3.39273L10.6074 4.40278Z",
					fill: "currentColor",
					transform: "translate(1.2 0.8)"
				})]
			});
		}
		function useBrowserDraft(url, revision) {
			const [edit, setEdit] = (0, react.useState)();
			return [(edit === null || edit === void 0 ? void 0 : edit.revision) === revision ? edit.value : url ?? "", (value) => {
				setEdit({
					revision,
					value
				});
			}];
		}
		/** Render provider-neutral navigation state and optional controls. */
		function BrowserBody(props) {
			var _tab$navigation$param, _tab$refreshShortcut, _tab$refreshShortcut2;
			const { mount, loadUrl, restore, goBack, goForward, reload, setSandbox, useBrowserState, useStore, useTabInfo, t } = props;
			const { tab } = useTabInfo();
			(0, react.useEffect)(() => tab.actions.bindCommands({ refresh: () => {
				reload(tab.id);
			} }), [
				tab.actions,
				tab.id,
				reload
			]);
			const saved = useStore((state) => state.byTab[tab.id]);
			const initial = (0, react.useRef)(saved);
			const initialUrl = (0, react.useRef)((_tab$navigation$param = tab.navigation.params) === null || _tab$navigation$param === void 0 ? void 0 : _tab$navigation$param.url);
			const viewportId = (0, react.useId)();
			const [mountEpoch, setMountEpoch] = (0, react.useState)(0);
			const state = useBrowserState(tab.id);
			const frame = (state === null || state === void 0 ? void 0 : state.frame) ?? EMPTY_FRAME;
			const restoreTarget = state === void 0 ? currentBrowserTarget(initial.current) : state.restoreTarget;
			const target = frame.target ?? restoreTarget;
			const [draft, setDraft] = useBrowserDraft((target === null || target === void 0 ? void 0 : target.url) ?? initialUrl.current, (state === null || state === void 0 ? void 0 : state.addressRevision) ?? 0);
			(0, react.useLayoutEffect)(() => {
				const hide = mount({
					tabId: tab.id,
					signal: tab.signal,
					viewportId,
					applicationOrigin: window.location.origin,
					initial: initial.current,
					initialUrl: initialUrl.current,
					openTab: (url) => {
						tab.actions.openTab("browser", {
							params: { url },
							revealIfOpened: false
						});
					}
				});
				setMountEpoch((value) => value + 1);
				return hide;
			}, [
				mount,
				tab.id,
				tab.signal,
				tab.actions,
				viewportId,
				props.actions
			]);
			const unknown = frame.address === "unknown";
			const externalUrl = unknown ? void 0 : target === null || target === void 0 ? void 0 : target.url;
			const sandboxed = frame.sandboxEnabled;
			const failure = state === null || state === void 0 ? void 0 : state.addressFailure;
			const error = frame.error;
			const submit = (event) => {
				event.preventDefault();
				loadUrl(tab.id, draft);
			};
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: Browser_module_css_default.root,
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("form", {
						className: Browser_module_css_default.toolbar,
						onSubmit: submit,
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: Browser_module_css_default.tool,
								"aria-label": t("back"),
								title: t("back"),
								disabled: !frame.canGoBack,
								onClick: () => {
									goBack(tab.id);
								},
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.IconChevronLeftOutlineRegular, {})
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: Browser_module_css_default.tool,
								"aria-label": t("forward"),
								title: t("forward"),
								disabled: !frame.canGoForward,
								onClick: () => {
									goForward(tab.id);
								},
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.IconChevronRightOutlineRegular, {})
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.Tooltip, {
								label: t("reload"),
								shortcutKeys: (_tab$refreshShortcut = tab.refreshShortcut) === null || _tab$refreshShortcut === void 0 ? void 0 : _tab$refreshShortcut.keys,
								side: "bottom",
								delayMs: 500,
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
									type: "button",
									className: Browser_module_css_default.tool,
									"aria-label": t("reload"),
									"aria-keyshortcuts": (_tab$refreshShortcut2 = tab.refreshShortcut) === null || _tab$refreshShortcut2 === void 0 ? void 0 : _tab$refreshShortcut2.aria,
									disabled: target === void 0 || mountEpoch === 0,
									onClick: () => {
										reload(tab.id);
									},
									children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.IconRefreshOutlineRegular, {})
								})
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
								className: Browser_module_css_default.addressBox,
								children: [
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
										className: [Browser_module_css_default.address, unknown ? Browser_module_css_default.addressUnknown : ""].join(" "),
										value: draft,
										"aria-label": t("address.placeholder"),
										placeholder: t("address.placeholder"),
										spellCheck: false,
										onChange: (event) => {
											setDraft(event.currentTarget.value);
										}
									}),
									unknown && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
										className: Browser_module_css_default.addressChanged,
										children: t("address.changed")
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
										type: "submit",
										className: [Browser_module_css_default.tool, Browser_module_css_default.addressGo].join(" "),
										"aria-label": t("go"),
										title: t("go"),
										children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.IconLinkOutlineRegular, {})
									})
								]
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: Browser_module_css_default.tool,
								"aria-label": t("external"),
								title: t("external"),
								disabled: externalUrl === void 0,
								onClick: externalUrl === void 0 ? void 0 : () => {
									window.open(externalUrl, "_blank", "noopener,noreferrer");
								},
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.IconRightUpOutlineRegular, { size: 14 })
							}),
							sandboxed !== void 0 && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: [Browser_module_css_default.tool, sandboxed ? "" : Browser_module_css_default.sandboxOff].join(" "),
								"aria-label": t(sandboxed ? "sandbox.disable" : "sandbox.enable"),
								title: t(sandboxed ? "sandbox.disable" : "sandbox.enable"),
								"aria-pressed": !sandboxed,
								onClick: () => {
									setSandbox(tab.id, !sandboxed);
								},
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(SandboxPolicyIcon, { sandboxed })
							})
						]
					}),
					sandboxed === false && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: Browser_module_css_default.sandboxWarning,
						role: "status",
						children: t("sandbox.warning")
					}),
					error !== void 0 && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: Browser_module_css_default.failure,
						role: "status",
						children: error.code !== void 0 && error.description !== void 0 ? t("load.failed.detail", {
							code: String(error.code),
							description: error.description
						}) : t("load.failed")
					}),
					failure !== void 0 && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: Browser_module_css_default.failure,
						role: "alert",
						children: t(`error.${failure}`)
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: Browser_module_css_default.content,
						"aria-busy": frame.loading,
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
								id: viewportId,
								className: Browser_module_css_default.viewport,
								"aria-label": t("type.label")
							}),
							restoreTarget !== void 0 && /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("section", {
								className: Browser_module_css_default.restore,
								"aria-label": t("restore.previous"),
								children: [
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: Browser_module_css_default.restoreLabel,
										children: t("restore.previous")
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: Browser_module_css_default.restoreTitle,
										children: restoreTarget.title
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
										className: Browser_module_css_default.restoreUrl,
										children: restoreTarget.url
									}),
									/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.Button, {
										variant: "primary",
										size: "sm",
										disabled: mountEpoch === 0,
										onClick: () => {
											restore(tab.id);
										},
										children: t("restore.action")
									})
								]
							}),
							restoreTarget === void 0 && (target === void 0 || frame.loading) && error === void 0 && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
								className: Browser_module_css_default.placeholder,
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
									className: Browser_module_css_default.start,
									children: t(target === void 0 ? "start" : "loading")
								})
							})
						]
					}),
					unknown && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", {
						className: Browser_module_css_default.limit,
						children: t("address.unknown")
					})
				]
			});
		}
		//#endregion
		//#region \0dsh-css:/home/agentuser/DeepSeek 2/dsh-upgrade-021/plugin/dsh-client-ui-responsive/src/client/mobile/BrowserTab.module.css.mjs
		const css = ".dC0C3q_root{min-width:0;height:100%;min-height:0;color:var(--dsw-alias-label-primary);background:var(--dsw-alias-bg-base);flex-direction:column;display:flex}.dC0C3q_official{flex:auto;min-width:0;min-height:0;display:flex;overflow:hidden}.dC0C3q_controls{box-sizing:border-box;border-top:.5px solid var(--dsw-alias-border-l3);flex-wrap:wrap;flex:none;align-items:center;gap:6px;min-height:40px;padding:6px;display:flex}.dC0C3q_mode,.dC0C3q_apply,.dC0C3q_status button{min-height:28px;color:var(--dsw-alias-label-primary);font:var(--dsw-font-xxs-12);background:var(--dsw-alias-bg-layer-1);border:.5px solid var(--dsw-alias-border-l2);border-radius:var(--dsw-radius-sm);cursor:pointer;flex:none;padding:0 9px}.dC0C3q_mode[aria-pressed=true]{color:var(--dsw-alias-state-business-primary);border-color:var(--dsw-alias-state-business-primary)}.dC0C3q_mode:disabled,.dC0C3q_apply:disabled,.dC0C3q_resolution:disabled{opacity:.5;cursor:default}.dC0C3q_resolution{box-sizing:border-box;width:110px;min-width:80px;height:28px;color:var(--dsw-alias-label-primary);font:var(--dsw-font-xxs-12);background:var(--dsw-alias-bg-layer-1);border:.5px solid var(--dsw-alias-border-l2);border-radius:var(--dsw-radius-sm);flex:90px;padding:0 9px}.dC0C3q_resolution[aria-invalid=true]{border-color:var(--dsw-alias-state-error-primary)}.dC0C3q_status{overflow-wrap:anywhere;color:var(--dsw-alias-state-error-primary);font:var(--dsw-font-xxxs-11);background:color-mix(in srgb, var(--dsw-alias-state-error-primary) 8%, transparent);flex-wrap:wrap;flex:none;align-items:center;gap:6px;padding:6px 10px;display:flex}";
		const tagId = "@dsh-android/dsh-client-ui-responsive/BrowserTab.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@dsh-android/dsh-client-ui-responsive";
			tag.dataset.pluginCss = tagId;
			tag.textContent = css;
			document.head.appendChild(tag);
		}
		var BrowserTab_module_css_default = {
			"apply": "dC0C3q_apply",
			"controls": "dC0C3q_controls",
			"mode": "dC0C3q_mode",
			"official": "dC0C3q_official",
			"resolution": "dC0C3q_resolution",
			"root": "dC0C3q_root",
			"status": "dC0C3q_status"
		};
		//#endregion
		//#region src/client/mobile/browser-tab.tsx
		/** Official BrowserBody/BrowserTitle chrome with an Android-native page provider. */
		/** Own dispatch id; existing Android browser layout records keep their occurrence ids. */
		const BROWSER_TAB_ID = "android-browser";
		/** Take the official builtin kind through the registry's extension band. */
		const BROWSER_TAB_KIND = "browser";
		/** Guide-less resolver for pre-0.2 Android layouts, without rewriting any layout data. */
		const LEGACY_BROWSER_TAB_KIND = "android-browser";
		/** Legacy needs a distinct implementation id because registry ids are globally unique. */
		const LEGACY_BROWSER_TAB_ID = "android-browser.legacy";
		/**
		* Contribute the official guide artwork and one Browser entry, not a replacement workspace tree.
		* @param t - locale-live private Browser dictionary.
		* @returns extension-band browser type.
		*/
		function browserTabDefinition(t) {
			return {
				id: BROWSER_TAB_ID,
				kind: BROWSER_TAB_KIND,
				priority: "extension",
				multiple: true,
				keepMounted: true,
				title: () => t("type.label"),
				guide: [{
					id: "new",
					commandId: "browser.new",
					order: 30,
					title: () => t("guide.title"),
					description: () => t("guide.description"),
					icon: _deepseek_ai_dsh_client_ui_primitives.GuideArtworkBrowser
				}]
			};
		}
		/** @param t - locale-live copy. @returns guide-less compatibility resolver. */
		function legacyBrowserTabDefinition(t) {
			return {
				...browserTabDefinition(t),
				id: LEGACY_BROWSER_TAB_ID,
				kind: LEGACY_BROWSER_TAB_KIND,
				guide: []
			};
		}
		function parseResolution(value) {
			const match = /^\s*(\d{2,4})\s*[x×*]\s*(\d{2,4})\s*$/i.exec(value);
			if (match === null) return void 0;
			const width = Number(match[1]);
			const height = Number(match[2]);
			return width >= 240 && width <= 3840 && height >= 240 && height <= 3840 ? {
				width,
				height
			} : void 0;
		}
		/** Reuse the official address/start/restore UI; native controls occupy their own non-stage row. */
		function BrowserTab(props) {
			const { useTabInfo, useNativeBrowserState, setBrowserVisible, setBrowserIdentity, setBrowserViewport, refreshBrowserStatus, t } = props;
			const { tab } = useTabInfo();
			const state = useNativeBrowserState(tab.id);
			const current = state !== void 0 && state.viewportWidth > 0 && state.viewportHeight > 0 ? state.viewportWidth + "x" + state.viewportHeight : "390x844";
			const [edit, setEdit] = (0, react.useState)();
			const [invalid, setInvalid] = (0, react.useState)(false);
			const value = (edit === null || edit === void 0 ? void 0 : edit.current) === current ? edit.value : current;
			const ready = (state === null || state === void 0 ? void 0 : state.available) === true && state.nativeTabId !== void 0;
			const desktop = (state === null || state === void 0 ? void 0 : state.identityId) === "linux-desktop";
			const mobile = (state === null || state === void 0 ? void 0 : state.identityId) === "android-real";
			(0, react.useLayoutEffect)(() => {
				setBrowserVisible(tab.id, tab.visible);
				return () => {
					setBrowserVisible(tab.id, false);
				};
			}, [
				setBrowserVisible,
				tab.id,
				tab.visible
			]);
			const submit = (event) => {
				event.preventDefault();
				const resolution = parseResolution(value);
				setInvalid(resolution === void 0);
				if (resolution !== void 0) setBrowserViewport(tab.id, resolution.width, resolution.height);
			};
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: BrowserTab_module_css_default.root,
				children: [
					(state === null || state === void 0 ? void 0 : state.available) === false && /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: BrowserTab_module_css_default.status,
						role: "status",
						children: [t("native.unavailable", { reason: state.reason }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							onClick: () => {
								refreshBrowserStatus(tab.id);
							},
							children: t("native.retry")
						})]
					}),
					(state === null || state === void 0 ? void 0 : state.available) === true && state.reason !== "" && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: BrowserTab_module_css_default.status,
						role: "status",
						children: t("native.operation.failed", { reason: state.reason })
					}),
					(state === null || state === void 0 ? void 0 : state.available) === true && state.profileAvailable === false && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: BrowserTab_module_css_default.status,
						role: "status",
						children: t("native.profile.unavailable", { reason: state.profileReason })
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: BrowserTab_module_css_default.official,
						children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(BrowserBody, { ...props })
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("form", {
						className: BrowserTab_module_css_default.controls,
						onSubmit: submit,
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: BrowserTab_module_css_default.mode,
								disabled: !ready,
								"aria-pressed": desktop,
								onClick: () => {
									setBrowserIdentity(tab.id, true);
								},
								children: t("native.desktop")
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: BrowserTab_module_css_default.mode,
								disabled: !ready,
								"aria-pressed": mobile,
								onClick: () => {
									setBrowserIdentity(tab.id, false);
								},
								children: t("native.mobile")
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("input", {
								className: BrowserTab_module_css_default.resolution,
								value,
								"aria-label": t("native.viewport"),
								"aria-invalid": invalid,
								disabled: !ready,
								spellCheck: false,
								onChange: (event) => {
									setEdit({
										current,
										value: event.currentTarget.value
									});
									setInvalid(false);
								}
							}),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "submit",
								className: BrowserTab_module_css_default.apply,
								disabled: !ready,
								children: t("native.viewport.apply")
							})
						]
					}),
					invalid && /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: BrowserTab_module_css_default.status,
						role: "alert",
						children: t("native.viewport.invalid")
					})
				]
			});
		}
		/** Add native profile/status/close actions without fabricating a public toolbar slot. */
		function BrowserTabMenu({ tab, dismiss, useNativeBrowserState, setBrowserIdentity, refreshBrowserStatus, closeBrowserTab, t }) {
			const state = useNativeBrowserState(tab.id);
			if (tab.kind !== "browser" && tab.kind !== "android-browser") return null;
			const ready = (state === null || state === void 0 ? void 0 : state.available) === true && state.nativeTabId !== void 0;
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)(react_jsx_runtime.Fragment, { children: [
				/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.MenuItemButton, {
					separatorBefore: true,
					disabled: !ready,
					onSelect: () => {
						dismiss();
						setBrowserIdentity(tab.id, true);
					},
					children: t("native.desktop")
				}),
				/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.MenuItemButton, {
					disabled: !ready,
					onSelect: () => {
						dismiss();
						setBrowserIdentity(tab.id, false);
					},
					children: t("native.mobile")
				}),
				/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.MenuItemButton, {
					onSelect: () => {
						dismiss();
						refreshBrowserStatus(tab.id);
					},
					children: t("native.retry")
				}),
				/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.MenuItemButton, {
					danger: true,
					onSelect: () => {
						dismiss();
						closeBrowserTab(tab.id);
					},
					children: t("native.close")
				})
			] });
		}
		/**
		* Parse one address-bar value into the fixed protocol allowlist.
		* @param input - user or typed-open input.
		* @param applicationOrigin - current DSH document origin, blocked for HTTPS.
		* @returns a canonical target or the refusal reason.
		*/
		function parseBrowserAddress(input, applicationOrigin) {
			const trimmed = input.trim();
			if (trimmed === "") return {
				ok: false,
				reason: "empty"
			};
			if (trimmed.length > 16384) return {
				ok: false,
				reason: "invalid"
			};
			const candidate = /^[A-Za-z][A-Za-z\d+.-]*:(?!\d+(?:[/?#]|$))/u.test(trimmed) ? trimmed : `https://${trimmed}`;
			let url;
			try {
				url = new URL(candidate);
			} catch {
				return {
					ok: false,
					reason: "invalid"
				};
			}
			if (url.username !== "" || url.password !== "") return {
				ok: false,
				reason: "credentials"
			};
			if (url.protocol === "https:" || url.protocol === "http:") {
				if (applicationOrigin !== void 0 && applicationOrigin !== "null") try {
					if (url.origin === new URL(applicationOrigin).origin) return {
						ok: false,
						reason: "application-origin"
					};
				} catch {}
				return {
					ok: true,
					target: {
						kind: url.protocol === "https:" ? "https" : "http",
						url: url.href,
						title: url.hostname
					}
				};
			}
			return {
				ok: false,
				reason: "protocol"
			};
		}
		//#endregion
		//#region src/client/mobile/native-browser-bridge.ts
		function record(value) {
			return value !== null && typeof value === "object" && !Array.isArray(value) ? value : void 0;
		}
		function text(value, limit = 2048) {
			return typeof value === "string" && value.length <= limit ? value : void 0;
		}
		function identity(value) {
			const parsed = text(value, 256);
			return parsed !== void 0 && parsed !== "" ? parsed : void 0;
		}
		function nonnegative(value) {
			return typeof value === "number" && Number.isSafeInteger(value) && value >= 0 ? value : 0;
		}
		/** @param session - requesting Session. @param reason - wire refusal. @returns explicit unavailable state. */
		function unavailableNativeBrowser(session, reason) {
			return {
				ok: false,
				available: false,
				session,
				reason,
				profileAvailable: false,
				profileReason: reason,
				tabs: [],
				authoritativeTabs: false,
				tabId: void 0
			};
		}
		/**
		* Decode shell JSON, rejecting malformed ids, duplicate tabs and foreign Sessions.
		* @param raw - synchronous shell reply.
		* @param session - Session captured by the caller before the operation.
		* @returns validated native state, or an explicit unavailable reason.
		*/
		function parseNativeBrowserSnapshot(raw, session) {
			if (raw === void 0 || raw === "") return unavailableNativeBrowser(session, "browser-command-unavailable");
			if (raw.length > 4194304) return unavailableNativeBrowser(session, "browser-reply-too-large");
			let decoded;
			try {
				decoded = JSON.parse(raw);
			} catch (_invalidJson) {
				return unavailableNativeBrowser(session, "browser-reply-invalid-json");
			}
			const value = record(decoded);
			if (value === void 0) return unavailableNativeBrowser(session, "browser-reply-invalid");
			if ((text(value.session) ?? text(value.ownerSessionId)) !== session) return unavailableNativeBrowser(session, "browser-reply-session-mismatch");
			const profile = record(value.profile);
			const explicitProfile = typeof value.profileAvailable === "boolean" ? value.profileAvailable : typeof (profile === null || profile === void 0 ? void 0 : profile.available) === "boolean" ? profile.available : void 0;
			const profileAvailable = explicitProfile === true;
			const profileReason = text(value.profileReason) ?? text(profile === null || profile === void 0 ? void 0 : profile.reason) ?? (profileAvailable ? "" : explicitProfile === false ? "browser-profile-unavailable" : "browser-profile-state-missing");
			const reason = text(value.reason) || (value.ok === true && value.available === true ? "" : "browser-unavailable");
			const tabId = identity(value.tabId);
			const rawTabs = Array.isArray(value.tabs) ? value.tabs : tabId === void 0 ? [] : [value];
			if (rawTabs.length > 256) return unavailableNativeBrowser(session, "browser-reply-too-many-tabs");
			const tabs = [];
			const ids = /* @__PURE__ */ new Set();
			const uiIds = /* @__PURE__ */ new Set();
			for (const item of rawTabs) {
				const tab = record(item);
				const id = identity(tab === null || tab === void 0 ? void 0 : tab.tabId);
				if (tab === void 0 || id === void 0 || ids.has(id)) return unavailableNativeBrowser(session, "browser-reply-invalid-tab");
				const tabOwner = text(tab.session) ?? text(tab.ownerSessionId);
				if (tabOwner !== void 0 && tabOwner !== session) return unavailableNativeBrowser(session, "browser-reply-session-mismatch");
				const uiTabId = identity(tab.uiTabId);
				if (uiTabId !== void 0 && uiIds.has(uiTabId)) return unavailableNativeBrowser(session, "browser-reply-duplicate-ui-tab");
				if (tab.uiTabId !== void 0 && tab.uiTabId !== null && uiTabId === void 0) return unavailableNativeBrowser(session, "browser-reply-invalid-tab");
				const url = text(tab.url, 16384);
				const title = text(tab.title, 1024);
				if (url === void 0 || title === void 0) return unavailableNativeBrowser(session, "browser-reply-invalid-tab");
				const tabProfile = record(tab.profile);
				const explicitProfile = typeof tab.profileAvailable === "boolean" ? tab.profileAvailable : typeof (tabProfile === null || tabProfile === void 0 ? void 0 : tabProfile.available) === "boolean" ? tabProfile.available : profileAvailable;
				tabs.push({
					tabId: id,
					uiTabId,
					url,
					title,
					pageGeneration: nonnegative(tab.pageGeneration),
					loadState: text(tab.loadState, 128) ?? "unknown",
					canGoBack: tab.canGoBack === true,
					canGoForward: tab.canGoForward === true,
					identityId: text(tab.identityId, 256) ?? "",
					viewportWidth: nonnegative(tab.viewportWidth),
					viewportHeight: nonnegative(tab.viewportHeight),
					profileAvailable: explicitProfile,
					profileReason: text(tab.profileReason) ?? text(tabProfile === null || tabProfile === void 0 ? void 0 : tabProfile.reason) ?? profileReason,
					reason: text(tab.reason) ?? ""
				});
				ids.add(id);
				if (uiTabId !== void 0) uiIds.add(uiTabId);
			}
			return {
				ok: value.ok === true,
				available: value.available === true,
				session,
				reason,
				profileAvailable,
				profileReason,
				tabs,
				authoritativeTabs: Array.isArray(value.tabs),
				tabId
			};
		}
		/**
		* Execute only the declared trusted command with captured Session and occurrence ids.
		* @param session - owning Session.
		* @param action - narrow operation.
		* @param options - native/GUI ids and optional validated address.
		* @returns decoded reply; bridge failures are unavailable state, not optimistic UI success.
		*/
		function nativeBrowserCommand(session, action, options = {}) {
			if (session === "") return unavailableNativeBrowser(session, "browser-session-missing");
			try {
				var _window$androidBridge, _window$androidBridge2;
				return parseNativeBrowserSnapshot((_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 || (_window$androidBridge2 = _window$androidBridge.browserHostCommand) === null || _window$androidBridge2 === void 0 ? void 0 : _window$androidBridge2.call(_window$androidBridge, JSON.stringify({
					action,
					session,
					...options
				})), session);
			} catch (_bridgeFailure) {
				return unavailableNativeBrowser(session, "browser-command-failed");
			}
		}
		//#endregion
		//#region src/client/mobile/native-browser-presentation.ts
		/** Native presentation never opens or closes a page; detach is targeted hide only. */
		var NativeBrowserPresentation = class {
			constructor(options) {
				this.options = options;
				_defineProperty(this, "release", void 0);
				_defineProperty(this, "publish", void 0);
			}
			/** Republish after a framework-visible tab or native navigation change. */
			refresh() {
				var _this$publish;
				(_this$publish = this.publish) === null || _this$publish === void 0 || _this$publish.call(this);
			}
			mount(viewportId) {
				var _this$release, _window$visualViewpor, _window$visualViewpor2;
				(_this$release = this.release) === null || _this$release === void 0 || _this$release.call(this);
				const stage = document.getElementById(viewportId);
				if (stage === null) return () => {};
				let queued = 0;
				let active = true;
				let last = "";
				const send = (visible) => {
					const tabId = this.options.nativeTabId();
					if (tabId === void 0) return;
					const rect = stage.getBoundingClientRect();
					let left = Math.max(0, rect.left);
					let top = Math.max(0, rect.top);
					let right = Math.min(window.innerWidth, rect.right);
					let bottom = Math.min(window.innerHeight, rect.bottom);
					let displayed = stage.isConnected && !document.hidden;
					for (let element = stage; element !== null; element = element.parentElement) {
						const style = window.getComputedStyle(element);
						if (element.hidden || style.display === "none" || style.visibility === "hidden" || Number(style.opacity) === 0) displayed = false;
						const clip = element.getBoundingClientRect();
						if (/hidden|clip|scroll|auto/.test(style.overflowX)) {
							left = Math.max(left, clip.left);
							right = Math.min(right, clip.right);
						}
						if (/hidden|clip|scroll|auto/.test(style.overflowY)) {
							top = Math.max(top, clip.top);
							bottom = Math.min(bottom, clip.bottom);
						}
					}
					const overlay = Array.from(document.querySelectorAll("[role=\"menu\"], [role=\"dialog\"], [aria-modal=\"true\"], [role=\"listbox\"]")).some((element) => {
						const bounds = element.getBoundingClientRect();
						const style = window.getComputedStyle(element);
						return bounds.width > 0 && bounds.height > 0 && style.display !== "none" && style.visibility !== "hidden" && Number(style.opacity) !== 0;
					});
					const payload = JSON.stringify({
						session: this.options.session,
						tabId,
						uiTabId: this.options.uiTabId,
						left,
						top,
						width: Math.max(0, right - left),
						height: Math.max(0, bottom - top),
						viewportWidth: window.innerWidth,
						viewportHeight: window.innerHeight,
						visible: visible && displayed && !overlay && document.querySelector("[data-dockkit-pointer]") === null && right - left > 1 && bottom - top > 1
					});
					if (payload === last) return;
					try {
						var _window$androidBridge;
						if (((_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 ? void 0 : _window$androidBridge.browserHostBounds) === void 0) return;
						window.androidBridge.browserHostBounds(payload);
						last = payload;
					} catch (_bridgeUnavailable) {}
				};
				const publish = () => {
					if (active) send(this.options.visible() && this.options.ready());
				};
				const schedule = () => {
					if (queued !== 0 || !active) return;
					queued = window.requestAnimationFrame(() => {
						queued = 0;
						publish();
					});
				};
				const resize = typeof ResizeObserver === "undefined" ? void 0 : new ResizeObserver(schedule);
				const mutation = typeof MutationObserver === "undefined" ? void 0 : new MutationObserver(schedule);
				resize === null || resize === void 0 || resize.observe(stage);
				mutation === null || mutation === void 0 || mutation.observe(document.documentElement, {
					childList: true,
					subtree: true,
					attributes: true,
					attributeFilter: [
						"style",
						"class",
						"hidden",
						"open",
						"aria-hidden",
						"data-sidebar-right-open",
						"data-dockkit-pointer"
					]
				});
				window.addEventListener("resize", schedule);
				window.addEventListener("scroll", schedule, true);
				(_window$visualViewpor = window.visualViewport) === null || _window$visualViewpor === void 0 || _window$visualViewpor.addEventListener("resize", schedule);
				(_window$visualViewpor2 = window.visualViewport) === null || _window$visualViewpor2 === void 0 || _window$visualViewpor2.addEventListener("scroll", schedule);
				document.addEventListener("visibilitychange", schedule);
				this.publish = publish;
				publish();
				const release = () => {
					var _window$visualViewpor3, _window$visualViewpor4;
					if (!active) return;
					active = false;
					if (queued !== 0) window.cancelAnimationFrame(queued);
					resize === null || resize === void 0 || resize.disconnect();
					mutation === null || mutation === void 0 || mutation.disconnect();
					window.removeEventListener("resize", schedule);
					window.removeEventListener("scroll", schedule, true);
					(_window$visualViewpor3 = window.visualViewport) === null || _window$visualViewpor3 === void 0 || _window$visualViewpor3.removeEventListener("resize", schedule);
					(_window$visualViewpor4 = window.visualViewport) === null || _window$visualViewpor4 === void 0 || _window$visualViewpor4.removeEventListener("scroll", schedule);
					document.removeEventListener("visibilitychange", schedule);
					send(false);
					if (this.release === release) {
						this.release = void 0;
						this.publish = void 0;
					}
				};
				this.release = release;
				return release;
			}
			/** Release observers and hide only this Session's native tab. */
			detach() {
				var _this$release2;
				(_this$release2 = this.release) === null || _this$release2 === void 0 || _this$release2.call(this);
			}
		};
		//#endregion
		//#region src/client/mobile/native-browser-adapter.ts
		/** Session/GUI-occurrence ownership over the Android native browser authority. */
		/** Native tabs outlive DOM mounts and plugin HMR; explicit layout close owns destruction. */
		var NativeBrowserSession = class {
			constructor(session) {
				this.session = session;
				_defineProperty(this, "snapshot", void 0);
				_defineProperty(this, "bindings", /* @__PURE__ */ new Map());
				_defineProperty(this, "visible", /* @__PURE__ */ new Map());
				_defineProperty(this, "controls", /* @__PURE__ */ new Map());
				_defineProperty(this, "frames", /* @__PURE__ */ new Map());
				_defineProperty(this, "listeners", /* @__PURE__ */ new Set());
				_defineProperty(this, "disposed", false);
				_defineProperty(
					this,
					/** Framework keyed source, stable before the BrowserBody commits its container. */
					"controlSource",
					(key) => {
						let source = this.controls.get(key);
						if (source === void 0) {
							source = (0, _deepseek_ai_dsh_client_store.createSnapshotStore)(this.controlState(key));
							this.controls.set(key, source);
						}
						return source;
					}
				);
				_defineProperty(
					this,
					/** Assemble the official controller's native navigation and targeted presentation. */
					"createPage",
					(options) => {
						var _this$frames$get, _options$initial;
						(_this$frames$get = this.frames.get(options.tabId)) === null || _this$frames$get === void 0 || _this$frames$get.detach();
						const frame = new NativeBrowserFrame(this, options);
						this.frames.set(options.tabId, frame);
						this.refreshStatus(options.tabId, (_options$initial = options.initial) === null || _options$initial === void 0 ? void 0 : _options$initial.nativeTabId);
						frame.synchronize();
						return {
							frame,
							presentation: frame.presentation
						};
					}
				);
				this.snapshot = unavailableNativeBrowser(session, "browser-command-unavailable");
			}
			/** Read native authority for reconciliation; failed polls cannot authorize removals. */
			getSnapshot() {
				return this.snapshot;
			}
			/** Refresh only this Session; there is no global-focus status fallback. */
			refresh() {
				if (!this.disposed) this.accept(nativeBrowserCommand(this.session, "tabs"));
				return this.snapshot;
			}
			/** Recover a binding only from native ownership or a validated retained checkpoint. */
			refreshStatus(uiTabId, retainedNativeId) {
				if (this.disposed) return;
				const id = this.nativeTabId(uiTabId);
				const result = nativeBrowserCommand(this.session, "status", {
					uiTabId,
					...id === void 0 ? {} : { tabId: id }
				});
				this.accept(result, uiTabId);
				if (this.nativeTabId(uiTabId) === void 0 && retainedNativeId !== void 0) {
					const saved = this.snapshot.tabs.find((tab) => tab.tabId === retainedNativeId);
					if (saved !== void 0 && (saved.uiTabId === void 0 || saved.uiTabId === uiTabId)) this.claim(uiTabId, saved.tabId);
				}
			}
			/** Bind a native tab; absentPreviousUi is accepted only after complete layout-inventory absence. */
			claim(uiTabId, nativeTabId, absentPreviousUi) {
				const native = this.snapshot.tabs.find((tab) => tab.tabId === nativeTabId);
				if (!this.snapshot.ok || !this.snapshot.available || native === void 0 || native.uiTabId !== void 0 && native.uiTabId !== uiTabId && native.uiTabId !== absentPreviousUi || [...this.bindings].some(([ui, id]) => ui !== uiTabId && ui !== absentPreviousUi && id === nativeTabId)) return false;
				const result = nativeBrowserCommand(this.session, "select", {
					tabId: nativeTabId,
					uiTabId
				});
				this.accept(result, uiTabId, nativeTabId);
				return result.ok && this.nativeTabId(uiTabId) === nativeTabId;
			}
			/** Read only this occurrence's validated native id. */
			nativeTabId(uiTabId) {
				return this.bindings.get(uiTabId);
			}
			/** Read only this occurrence's native navigation. */
			nativeTab(uiTabId) {
				const id = this.nativeTabId(uiTabId);
				return id === void 0 ? void 0 : this.snapshot.tabs.find((tab) => tab.tabId === id);
			}
			/** Native select changes ownership; visibility is delivered separately through bounds. */
			setVisible(uiTabId, visible) {
				var _this$frames$get2;
				const changed = this.visible.get(uiTabId) !== visible;
				this.visible.set(uiTabId, visible);
				if (visible && changed && this.nativeTabId(uiTabId) !== void 0) this.command(uiTabId, "select");
				(_this$frames$get2 = this.frames.get(uiTabId)) === null || _this$frames$get2 === void 0 || _this$frames$get2.presentation.refresh();
			}
			/** Visibility comes from the actual useTabInfo tab occurrence. */
			isVisible(uiTabId) {
				return this.visible.get(uiTabId) === true;
			}
			/** Execute one targeted navigation; opening an empty GUI occurrence creates its own native tab. */
			command(uiTabId, action, url) {
				if (this.disposed) return;
				const tabId = this.nativeTabId(uiTabId);
				if (tabId === void 0 && action !== "open" && action !== "status") return;
				const result = nativeBrowserCommand(this.session, action, {
					uiTabId,
					...tabId === void 0 ? {} : { tabId },
					...url === void 0 ? {} : { url }
				});
				this.accept(result, uiTabId, action === "open" ? result.tabId : tabId);
			}
			/** Explicit UI close only; cleanup failure keeps the Sidebar record through its close handler. */
			closeUi(uiTabId) {
				if (this.disposed) throw new Error("browser-adapter-disposed");
				const previousId = this.nativeTabId(uiTabId);
				if (previousId !== void 0 && this.snapshot.ok && this.snapshot.available && this.snapshot.authoritativeTabs && !this.snapshot.tabs.some((tab) => tab.tabId === previousId)) {
					this.bindings.delete(uiTabId);
					this.publish();
					return;
				}
				this.refreshStatus(uiTabId);
				if (!this.snapshot.ok || !this.snapshot.available) {
					this.refresh();
					if (!this.snapshot.ok || !this.snapshot.available || !this.snapshot.authoritativeTabs) throw new Error(this.snapshot.reason || "browser-close-status-unavailable");
				}
				const tabId = this.nativeTabId(uiTabId);
				if (tabId === void 0) return;
				if (this.snapshot.authoritativeTabs && !this.snapshot.tabs.some((tab) => tab.tabId === tabId)) {
					this.bindings.delete(uiTabId);
					this.publish();
					return;
				}
				const result = nativeBrowserCommand(this.session, "close", {
					tabId,
					uiTabId
				});
				this.accept(result);
				if (!result.ok) throw new Error(result.reason || "browser-close-failed");
				this.bindings.delete(uiTabId);
				this.publish();
			}
			/** Apply the shell's profile, never a UI-only UA label or optimistic readiness flag. */
			setIdentity(uiTabId, desktop) {
				this.setting(uiTabId, "identity", {
					profile: desktop ? "linux-desktop" : "android-real",
					preset: "custom",
					width: desktop ? 1280 : 390,
					height: desktop ? 720 : 844,
					route: "S2"
				});
			}
			/** Keep native viewport dimensions separate from the viewport's presentation rectangle. */
			setViewport(uiTabId, width, height) {
				if (!Number.isInteger(width) || !Number.isInteger(height) || width < 240 || width > 3840 || height < 240 || height > 3840) return;
				this.setting(uiTabId, "viewport", {
					id: "custom",
					width,
					height,
					route: "S2"
				});
			}
			/** Subscribe outside components; renderer sees only the derived inject observables. */
			subscribe(listener) {
				this.listeners.add(listener);
				return () => {
					this.listeners.delete(listener);
				};
			}
			/** Publish a local integration refusal without treating a failed operation as native tab absence. */
			reportFailure(reason) {
				this.accept(unavailableNativeBrowser(this.session, reason));
			}
			/** Stop this provider and hide its attachments; no native tabs or workspace are closed. */
			dispose() {
				if (this.disposed) return;
				this.disposed = true;
				for (const frame of this.frames.values()) frame.detach();
				this.frames.clear();
				this.listeners.clear();
				this.controls.clear();
				this.visible.clear();
			}
			setting(uiTabId, kind, value) {
				if (this.disposed) return;
				const tabId = this.nativeTabId(uiTabId);
				if (tabId === void 0) return;
				const payload = JSON.stringify({
					session: this.session,
					tabId,
					uiTabId,
					...value
				});
				let result;
				try {
					var _window$androidBridge, _window$androidBridge2, _window$androidBridge3, _window$androidBridge4;
					result = parseNativeBrowserSnapshot(kind === "identity" ? (_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 || (_window$androidBridge2 = _window$androidBridge.browserHostIdentity) === null || _window$androidBridge2 === void 0 ? void 0 : _window$androidBridge2.call(_window$androidBridge, payload) : (_window$androidBridge3 = window.androidBridge) === null || _window$androidBridge3 === void 0 || (_window$androidBridge4 = _window$androidBridge3.browserHostViewport) === null || _window$androidBridge4 === void 0 ? void 0 : _window$androidBridge4.call(_window$androidBridge3, payload), this.session);
				} catch (_bridgeFailure) {
					result = unavailableNativeBrowser(this.session, "browser-setting-failed");
				}
				this.accept(result, uiTabId, tabId);
			}
			accept(result, uiTabId, expectedId) {
				if (this.disposed) return;
				if (result.ok && result.available) {
					const tabs = result.authoritativeTabs ? result.tabs : [...this.snapshot.tabs.filter((old) => !result.tabs.some((tab) => tab.tabId === old.tabId)), ...result.tabs];
					this.snapshot = {
						...result,
						tabs
					};
					for (const tab of tabs) if (tab.uiTabId !== void 0) this.bindings.set(tab.uiTabId, tab.tabId);
					const owned = uiTabId === void 0 ? void 0 : tabs.find((tab) => tab.uiTabId === uiTabId);
					const addressed = expectedId === void 0 ? void 0 : tabs.find((tab) => tab.tabId === expectedId);
					if (uiTabId !== void 0 && owned !== void 0) this.bindings.set(uiTabId, owned.tabId);
					else if (uiTabId !== void 0 && addressed !== void 0 && (addressed.uiTabId === void 0 || addressed.uiTabId === uiTabId) && ![...this.bindings].some(([ui, id]) => ui !== uiTabId && id === addressed.tabId)) this.bindings.set(uiTabId, addressed.tabId);
					for (const [ui, id] of this.bindings) {
						const tab = tabs.find((item) => item.tabId === id);
						if ((tab === null || tab === void 0 ? void 0 : tab.uiTabId) !== void 0 && tab.uiTabId !== ui) this.bindings.delete(ui);
					}
				} else this.snapshot = {
					...result,
					tabs: this.snapshot.tabs,
					authoritativeTabs: false
				};
				this.publish();
			}
			controlState(uiTabId) {
				const tab = this.nativeTab(uiTabId);
				return {
					nativeTabId: this.nativeTabId(uiTabId),
					available: this.snapshot.available,
					reason: this.snapshot.reason || (tab === null || tab === void 0 ? void 0 : tab.reason) || "",
					profileAvailable: this.snapshot.ok ? (tab === null || tab === void 0 ? void 0 : tab.profileAvailable) ?? this.snapshot.profileAvailable : this.snapshot.profileAvailable,
					profileReason: this.snapshot.ok ? (tab === null || tab === void 0 ? void 0 : tab.profileReason) ?? this.snapshot.profileReason : this.snapshot.profileReason,
					identityId: (tab === null || tab === void 0 ? void 0 : tab.identityId) ?? "",
					viewportWidth: (tab === null || tab === void 0 ? void 0 : tab.viewportWidth) ?? 0,
					viewportHeight: (tab === null || tab === void 0 ? void 0 : tab.viewportHeight) ?? 0
				};
			}
			publish() {
				for (const [uiTabId, source] of this.controls) {
					const next = this.controlState(uiTabId);
					if (JSON.stringify(next) !== JSON.stringify(source.getSnapshot())) source.set(next);
				}
				for (const listener of [...this.listeners]) listener();
			}
		};
		/** Native navigation observable consumed unchanged by the vendored BrowserController. */
		var NativeBrowserFrame = class {
			constructor(owner, options) {
				this.owner = owner;
				this.options = options;
				_defineProperty(this, "source", (0, _deepseek_ai_dsh_client_store.createSnapshotStore)(emptyBrowserFrame()));
				_defineProperty(this, "unsubscribe", void 0);
				_defineProperty(this, "disposed", false);
				_defineProperty(this, "checkpoint", "");
				_defineProperty(this, "presentation", void 0);
				_defineProperty(this, "getSnapshot", () => this.source.getSnapshot());
				_defineProperty(this, "subscribe", (listener) => this.source.subscribe(listener));
				this.presentation = new NativeBrowserPresentation({
					session: owner.session,
					uiTabId: options.tabId,
					nativeTabId: () => owner.nativeTabId(options.tabId),
					visible: () => owner.isVisible(options.tabId),
					ready: () => {
						const snapshot = owner.getSnapshot();
						const tab = owner.nativeTab(options.tabId);
						return snapshot.ok && snapshot.available && (tab === null || tab === void 0 ? void 0 : tab.profileAvailable) === true && this.source.getSnapshot().target !== void 0;
					}
				});
				this.unsubscribe = owner.subscribe(() => {
					this.synchronize();
				});
			}
			loadUrl(target) {
				if (!this.disposed) this.owner.command(this.options.tabId, "open", target.url);
			}
			goBack() {
				if (!this.disposed && this.getSnapshot().canGoBack) this.owner.command(this.options.tabId, "back");
			}
			goForward() {
				if (!this.disposed && this.getSnapshot().canGoForward) this.owner.command(this.options.tabId, "forward");
			}
			reload() {
				if (!this.disposed) this.owner.command(this.options.tabId, "reload");
			}
			/** Copy only observed native navigation into the official observable/checkpoint. */
			synchronize() {
				if (this.disposed) return;
				const snapshot = this.owner.getSnapshot();
				const native = this.owner.nativeTab(this.options.tabId);
				const parsed = native === void 0 || native.url === "" || native.url === "about:blank" ? void 0 : parseBrowserAddress(native.url, window.location.origin);
				const target = (parsed === null || parsed === void 0 ? void 0 : parsed.ok) === true ? {
					...parsed.target,
					title: (native === null || native === void 0 ? void 0 : native.title) || parsed.target.title
				} : void 0;
				const reason = !snapshot.ok || !snapshot.available ? snapshot.reason : (native === null || native === void 0 ? void 0 : native.profileAvailable) === false ? native.profileReason || "browser-profile-unavailable" : (native === null || native === void 0 ? void 0 : native.loadState) === "error" || (native === null || native === void 0 ? void 0 : native.loadState) === "failed" ? native.reason || "browser-load-failed" : void 0;
				const next = {
					target,
					address: target === void 0 ? "empty" : "observed",
					loading: (native === null || native === void 0 ? void 0 : native.loadState) === "loading" || (native === null || native === void 0 ? void 0 : native.loadState) === "navigating",
					canGoBack: (native === null || native === void 0 ? void 0 : native.canGoBack) === true && snapshot.ok,
					canGoForward: (native === null || native === void 0 ? void 0 : native.canGoForward) === true && snapshot.ok,
					error: reason === void 0 || reason === "" ? void 0 : {
						code: void 0,
						description: reason
					},
					sandboxEnabled: void 0
				};
				if (JSON.stringify(next) !== JSON.stringify(this.source.getSnapshot())) this.source.set(next);
				if (target !== void 0 && native !== void 0) {
					const saved = {
						...browserAddressCheckpoint(target, native.pageGeneration),
						nativeTabId: native.tabId
					};
					const encoded = JSON.stringify(saved);
					if (encoded !== this.checkpoint) {
						this.checkpoint = encoded;
						this.options.persist(saved);
					}
				}
				this.presentation.refresh();
			}
			/** Replacement, HMR and detach release presentation only; explicit close is owned by Sidebar cleanup. */
			detach() {
				if (this.disposed) return;
				this.disposed = true;
				this.unsubscribe();
				this.presentation.detach();
			}
			dispose() {
				this.detach();
				return Promise.resolve();
			}
		};
		//#endregion
		//#region src/client/mobile/native-browser-auto-place.ts
		/** One workspace per Session; HMR disposes attachments, never native workspace contents. */
		var NativeBrowserPlacement = class {
			constructor(options) {
				this.options = options;
				_defineProperty(this, "workspaces", /* @__PURE__ */ new Map());
				_defineProperty(this, "pending", /* @__PURE__ */ new Map());
				_defineProperty(this, "timer", void 0);
				_defineProperty(this, "disposed", false);
			}
			/** @param session - owning Session. @returns stable native adapter for that Session. */
			for(session) {
				if (this.disposed) throw new Error("browser-placement-disposed");
				let workspace = this.workspaces.get(session);
				if (workspace === void 0) {
					workspace = new NativeBrowserSession(session);
					this.workspaces.set(session, workspace);
				}
				return workspace;
			}
			/** Start discovery without expanding another Session or a collapsed column. */
			attach() {
				if (this.disposed || this.timer !== void 0) return () => {};
				this.tick();
				this.timer = window.setInterval(() => {
					this.tick();
				}, 1e3);
				return () => {
					this.dispose();
				};
			}
			/** Hide attachments and stop discovery; explicit close remains a separate operation. */
			dispose() {
				if (this.disposed) return;
				this.disposed = true;
				if (this.timer !== void 0) window.clearInterval(this.timer);
				for (const workspace of this.workspaces.values()) workspace.dispose();
				this.workspaces.clear();
				this.pending.clear();
				document.documentElement.removeAttribute(BROWSER_PENDING_ATTR);
			}
			tick() {
				if (this.disposed) return;
				const mounted = this.options.mounted();
				const ids = /* @__PURE__ */ new Set([
					...this.options.sessions(),
					...this.options.tabs().map((tab) => tab.sessionId),
					...this.workspaces.keys()
				]);
				if (mounted !== void 0) ids.add(mounted);
				let mountedPending = false;
				for (const session of ids) {
					const workspace = this.for(session);
					try {
						const snapshot = workspace.refresh();
						if (!snapshot.ok || !snapshot.available || !snapshot.authoritativeTabs) continue;
						let records = this.options.tabs().filter((tab) => tab.sessionId === session && (tab.kind === this.options.kind || tab.kind === this.options.legacyKind));
						for (const record of records) {
							const nativeId = workspace.nativeTabId(record.tabId);
							if (nativeId !== void 0 && !snapshot.tabs.some((tab) => tab.tabId === nativeId)) this.options.close(session, record.tabId);
						}
						records = this.options.tabs().filter((tab) => tab.sessionId === session && (tab.kind === this.options.kind || tab.kind === this.options.legacyKind));
						let pending = this.pending.get(session);
						if (pending === void 0) {
							pending = /* @__PURE__ */ new Map();
							this.pending.set(session, pending);
						}
						for (const native of snapshot.tabs) {
							if (records.some((record) => workspace.nativeTabId(record.tabId) === native.tabId || record.tabId === native.uiTabId)) {
								pending.delete(native.tabId);
								continue;
							}
							if (native.uiTabId !== void 0 && this.options.tabs().some((tab) => tab.sessionId === session && tab.tabId === native.uiTabId)) continue;
							const queued = pending.get(native.tabId);
							const queuedRecord = queued === void 0 ? void 0 : records.find((record) => record.tabId === queued);
							if (queuedRecord !== void 0) {
								if (workspace.claim(queuedRecord.tabId, native.tabId, native.uiTabId)) pending.delete(native.tabId);
								continue;
							}
							const legacy = records.find((record) => record.kind === this.options.legacyKind && workspace.nativeTabId(record.tabId) === void 0);
							if (legacy !== void 0) {
								if (!workspace.claim(legacy.tabId, native.tabId, native.uiTabId)) pending.set(native.tabId, legacy.tabId);
								continue;
							}
							if (session !== mounted || !this.options.expanded()) {
								if (session === mounted) mountedPending = true;
								continue;
							}
							const before = new Set(this.options.tabs().map((tab) => tab.tabId));
							this.options.open(session);
							const created = this.options.tabs().find((tab) => tab.sessionId === session && tab.kind === this.options.kind && !before.has(tab.tabId));
							if (created !== void 0) {
								pending.set(native.tabId, created.tabId);
								if (workspace.claim(created.tabId, native.tabId, native.uiTabId)) pending.delete(native.tabId);
								records = this.options.tabs().filter((tab) => tab.sessionId === session && (tab.kind === this.options.kind || tab.kind === this.options.legacyKind));
							}
						}
						for (const nativeId of pending.keys()) if (!snapshot.tabs.some((tab) => tab.tabId === nativeId)) pending.delete(nativeId);
					} catch (_sidebarOperationRefused) {
						workspace.reportFailure("browser-sidebar-operation-refused");
						if (session === mounted) mountedPending = true;
					}
				}
				if (mountedPending) document.documentElement.setAttribute(BROWSER_PENDING_ATTR, "true");
				else document.documentElement.removeAttribute(BROWSER_PENDING_ATTR);
			}
		};
		//#endregion
		//#region src/client/mobile/upstream-browser/browser/BrowserController.ts
		/** Carrier-independent tab commands and renderer-facing state. */
		/** Owns input validation and page lifetime without inspecting the carrier type. */
		var BrowserController = class {
			/** @param options - identity, persistence, page factory and source-tab navigation. */
			constructor(options) {
				this.options = options;
				_defineProperty(this, "page", void 0);
				_defineProperty(this, "store", void 0);
				_defineProperty(this, "unsubscribe", void 0);
				_defineProperty(this, "actions", void 0);
				_defineProperty(this, "checkpoint", void 0);
				_defineProperty(this, "started", false);
				_defineProperty(this, "disposed", false);
				_defineProperty(this, "disposal", void 0);
				_defineProperty(this, "abort", () => {
					this.dispose();
				});
				_defineProperty(
					this,
					/** @returns immutable state for the common toolbar. */
					"getSnapshot",
					() => this.store.getSnapshot()
				);
				_defineProperty(
					this,
					/** @param listener - state invalidation. @returns unsubscribe callback. */
					"subscribe",
					(listener) => this.store.subscribe(listener)
				);
				this.actions = options.actions;
				this.checkpoint = options.initial;
				this.page = options.createPage({
					tabId: options.tabId,
					initial: options.initial,
					persist: (state) => {
						if (this.disposed) return;
						this.checkpoint = state;
						this.actions.replace(options.tabId, state);
					},
					openRequested: (value) => {
						if (this.disposed) return;
						const result = parseBrowserAddress(value, options.applicationOrigin);
						if (!result.ok) {
							this.addressFailed(result.reason);
							return;
						}
						options.openTab(result.target.url);
					}
				});
				this.store = (0, _deepseek_ai_dsh_client_store.createSnapshotStore)({
					frame: this.page.frame.getSnapshot(),
					restoreTarget: currentBrowserTarget(this.checkpoint),
					addressFailure: void 0,
					addressRevision: 0
				});
				this.unsubscribe = this.page.frame.subscribe(() => {
					var _frame$target, _current$frame$target;
					if (this.disposed) return;
					const current = this.store.getSnapshot();
					const frame = this.page.frame.getSnapshot();
					const changed = ((_frame$target = frame.target) === null || _frame$target === void 0 ? void 0 : _frame$target.url) !== ((_current$frame$target = current.frame.target) === null || _current$frame$target === void 0 ? void 0 : _current$frame$target.url);
					this.store.set({
						frame,
						restoreTarget: frame.target === void 0 ? currentBrowserTarget(this.checkpoint) : void 0,
						addressFailure: changed ? void 0 : current.addressFailure,
						addressRevision: current.addressRevision + Number(changed)
					});
				});
				options.signal.addEventListener("abort", this.abort, { once: true });
			}
			/**
			* Attach the page without transferring ownership of its tab occurrence.
			* @param viewportId - mounted content container.
			* @returns physical attachment cleanup only.
			*/
			mount(viewportId) {
				this.publishSaved();
				return this.page.presentation.mount(viewportId);
			}
			/**
			* Consume initial navigation once; a saved checkpoint alone never starts a page.
			* @param initialUrl - explicit typed-open address, or absence.
			*/
			start(initialUrl) {
				if (this.started || this.disposed) return;
				this.started = true;
				if (initialUrl !== void 0) this.loadUrl(initialUrl);
			}
			/** Load the saved address only after an explicit restore action. */
			restore() {
				const target = this.store.getSnapshot().restoreTarget;
				if (target !== void 0) this.loadUrl(target.url);
			}
			/**
			* Validate an address before navigation, publishing invalid input for correction.
			* @param value - address-bar or typed-open input.
			*/
			loadUrl(value) {
				if (this.disposed) return;
				const parsed = parseBrowserAddress(value, this.options.applicationOrigin);
				if (!parsed.ok) {
					this.addressFailed(parsed.reason);
					return;
				}
				this.command(() => {
					this.page.frame.loadUrl(parsed.target);
				});
			}
			/** Delegate Back to the page's navigation provider. */
			goBack() {
				this.command(() => {
					this.page.frame.goBack();
				});
			}
			/** Delegate Forward to the page's navigation provider. */
			goForward() {
				this.command(() => {
					this.page.frame.goForward();
				});
			}
			/** Restore a saved address, or reload the already requested page. */
			reload() {
				if (this.store.getSnapshot().restoreTarget !== void 0) this.restore();
				else this.command(() => {
					this.page.frame.reload();
				});
			}
			/**
			* Apply the optional embedding-sandbox control; unsupported providers remain unchanged.
			* @param enabled - whether to enforce the provider's embedding sandbox.
			*/
			setSandbox(enabled) {
				const sandbox = this.page.frame.sandbox;
				if (sandbox !== void 0) this.command(() => {
					sandbox.setEnabled(enabled);
				});
			}
			/**
			* Redirect future checkpoint writes to a replacement Session binding.
			* @param actions - replacement persistence writer.
			*/
			rebind(actions) {
				this.actions = actions;
			}
			/**
			* Release the page and detach occurrence and state listeners.
			* @returns after page teardown; repeated callers join the same disposal.
			*/
			dispose() {
				if (this.disposal !== void 0) return this.disposal;
				this.disposed = true;
				this.options.signal.removeEventListener("abort", this.abort);
				this.unsubscribe();
				this.disposal = this.page.frame.dispose();
				return this.disposal;
			}
			publishSaved() {
				if (this.checkpoint !== void 0) this.actions.replace(this.options.tabId, this.checkpoint);
			}
			addressFailed(reason) {
				this.store.set({
					...this.store.getSnapshot(),
					addressFailure: reason
				});
			}
			command(run) {
				if (this.disposed) return;
				const current = this.store.getSnapshot();
				this.store.set({
					...current,
					addressFailure: void 0,
					addressRevision: current.addressRevision + 1
				});
				run();
			}
		};
		/**
		* Own tab-occurrence controllers behind Session-scoped callbacks.
		* @param actions - persisted view-state writer.
		* @param createPage - composition-selected provider.
		* @param isTabOpen - authoritative layout membership, independent of mounted bodies and plugin lifetime.
		* @returns tab callbacks.
		*/
		function createBrowserControllers(actions, createPage, isTabOpen) {
			let currentActions = actions;
			const controllers = /* @__PURE__ */ new Map();
			const controller = (id) => {
				var _controllers$get;
				return (_controllers$get = controllers.get(id)) === null || _controllers$get === void 0 ? void 0 : _controllers$get.controller;
			};
			return {
				keyedHooks: { browserState: (key) => controller(key) },
				mount(request) {
					const { tabId, signal } = request;
					if (signal.aborted) return () => {};
					let held = controllers.get(tabId);
					if ((held === null || held === void 0 ? void 0 : held.signal) !== signal) {
						if (held !== void 0) {
							held.signal.removeEventListener("abort", held.forget);
							held.controller.dispose();
						}
						const created = new BrowserController({
							...request,
							actions: currentActions,
							createPage
						});
						const forget = () => {
							controllers.delete(tabId);
							if (!isTabOpen(tabId)) currentActions.forget(tabId);
						};
						held = {
							signal,
							controller: created,
							forget
						};
						controllers.set(tabId, held);
						signal.addEventListener("abort", forget, { once: true });
					}
					const hide = held.controller.mount(request.viewportId);
					held.controller.start(request.initialUrl);
					return hide;
				},
				dispose: async () => {
					const pending = [...controllers.values()].map(({ signal, controller, forget }) => {
						signal.removeEventListener("abort", forget);
						return controller.dispose();
					});
					controllers.clear();
					await Promise.all(pending);
				},
				rebind: (actions) => {
					currentActions = actions;
					for (const { controller } of controllers.values()) controller.rebind(actions);
				},
				loadUrl: (id, value) => {
					var _controller;
					(_controller = controller(id)) === null || _controller === void 0 || _controller.loadUrl(value);
				},
				restore: (id) => {
					var _controller2;
					(_controller2 = controller(id)) === null || _controller2 === void 0 || _controller2.restore();
				},
				goBack: (id) => {
					var _controller3;
					(_controller3 = controller(id)) === null || _controller3 === void 0 || _controller3.goBack();
				},
				goForward: (id) => {
					var _controller4;
					(_controller4 = controller(id)) === null || _controller4 === void 0 || _controller4.goForward();
				},
				reload: (id) => {
					var _controller5;
					(_controller5 = controller(id)) === null || _controller5 === void 0 || _controller5.reload();
				},
				setSandbox: (id, enabled) => {
					var _controller6;
					(_controller6 = controller(id)) === null || _controller6 === void 0 || _controller6.setSandbox(enabled);
				}
			};
		}
		//#endregion
		//#region src/client/mobile/upstream-browser/browser/store.ts
		/** Persisted Browser tab snapshots shared by the body and title slots. */
		/**
		* Declare the Session-scoped Browser persistence store.
		* @returns a fresh store handle for Slot registration.
		*/
		function createBrowserStore() {
			return (0, _deepseek_ai_dsh_client_store.defineStore)({
				init: () => ({ byTab: {} }),
				persist: "dsh.android-sidebar-browser.v1",
				actions: {
					replace: (draft, tabId, state) => {
						draft.byTab[tabId] = state;
					},
					forget: (draft, tabId) => {
						const byTab = {};
						for (const [id, state] of Object.entries(draft.byTab)) if (id !== tabId) byTab[id] = state;
						draft.byTab = byTab;
					}
				}
			});
		}
		//#endregion
		//#region src/client/mobile/upstream-browser/view/BrowserTitle.tsx
		/** Browser icon and current host name. */
		function BrowserTitle({ useTabInfo, useStore }) {
			const { tab } = useTabInfo();
			const entry = useStore((state) => currentBrowserTarget(state.byTab[tab.id]));
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)(react_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.IconGlobeOutlineRegular, { className: Browser_module_css_default.titleIcon }), (entry === null || entry === void 0 ? void 0 : entry.title) ?? tab.title] });
		}
		//#endregion
		//#region src/client/mobile/upstream-browser/locales.ts
		/** Locale-owned Browser tab copy. */
		const zh = {
			"native.desktop": "PC",
			"native.mobile": "手机",
			"native.viewport": "浏览器视口分辨率",
			"native.viewport.apply": "应用分辨率",
			"native.viewport.invalid": "请输入 240–4096 范围的宽 × 高。",
			"native.unavailable": "Android 浏览器不可用：{reason}",
			"native.profile.unavailable": "浏览器身份档不可用：{reason}",
			"native.close": "关闭原生浏览器标签页",
			"native.retry": "重新读取浏览器状态",
			"native.operation.failed": "浏览器操作未完成：{reason}",
			"type.label": "浏览器",
			"guide.title": "浏览器",
			"guide.description": "浏览网页",
			"shortcut.noSession": "请先打开一个会话",
			"address.placeholder": "输入 HTTP(S) 地址",
			"address.changed": "URL 已变化",
			back: "后退",
			forward: "前进",
			reload: "刷新",
			go: "前往",
			external: "在系统浏览器中打开",
			"sandbox.disable": "关闭沙箱限制",
			"sandbox.enable": "恢复沙箱限制",
			"sandbox.warning": "沙箱限制已关闭；页面可以导航顶层应用，并使用下载、模态对话框与输入锁定。",
			start: "输入 HTTP(S) 地址开始浏览",
			loading: "正在打开…",
			"restore.previous": "上次打开",
			"restore.action": "恢复页面",
			"error.empty": "请输入地址。",
			"error.invalid": "这个地址无效或过长。",
			"error.protocol": "只支持 HTTP 和 HTTPS 地址；本地文件请使用文档预览。",
			"error.credentials": "地址不能包含用户名或密码。",
			"error.application-origin": "不能在嵌入浏览器中打开 DSH 应用自身。",
			"load.failed": "页面加载失败；请刷新重试或在系统浏览器中打开。",
			"load.failed.detail": "页面加载失败 ({code}): {description}",
			"address.unknown": "页面已跳转；当前载体无法读取新的 URL。"
		};
		/** English dictionary with the same keys. */
		const en = {
			"native.desktop": "PC",
			"native.mobile": "Mobile",
			"native.viewport": "Browser viewport resolution",
			"native.viewport.apply": "Apply resolution",
			"native.viewport.invalid": "Enter width × height within 240–4096.",
			"native.unavailable": "Android browser unavailable: {reason}",
			"native.profile.unavailable": "Browser profile unavailable: {reason}",
			"native.close": "Close native browser tab",
			"native.retry": "Refresh browser status",
			"native.operation.failed": "Browser operation did not complete: {reason}",
			"type.label": "Browser",
			"guide.title": "Browser",
			"guide.description": "Browse web pages",
			"shortcut.noSession": "Open a session first",
			"address.placeholder": "Enter an HTTP(S) address",
			"address.changed": "URL changed",
			back: "Back",
			forward: "Forward",
			reload: "Reload",
			go: "Go",
			external: "Open in system browser",
			"sandbox.disable": "Disable sandbox restrictions",
			"sandbox.enable": "Restore sandbox restrictions",
			"sandbox.warning": "Sandbox restrictions are disabled; the page can navigate the top-level app and use downloads, modal dialogs, and input locks.",
			start: "Enter an HTTP(S) address to start browsing",
			loading: "Opening…",
			"restore.previous": "Previously opened",
			"restore.action": "Restore page",
			"error.empty": "Enter an address.",
			"error.invalid": "That address is invalid or too long.",
			"error.protocol": "Only HTTP and HTTPS addresses are supported; use Document Preview for local files.",
			"error.credentials": "Addresses cannot contain a username or password.",
			"error.application-origin": "The embedded browser cannot open the DSH application itself.",
			"load.failed": "The page could not load; reload or open it in the system browser.",
			"load.failed.detail": "Page load failed ({code}): {description}",
			"address.unknown": "The page navigated; this carrier cannot read its new URL."
		};
		//#endregion
		//#region src/client/mobile/incoming-draft.ts
		/**
		* External-open attachment-draft consumer.
		*
		* The queue returns opaque file metadata only. The trusted shell bridge supplies the temporary
		* workspace cwd (never a source-file path); the normal client Session controller creates a blank
		* locally addressable Session there before the source is claimed and attached through the existing
		* composer/upload flow.
		*/
		function itemOf(value) {
			if (value === null || typeof value !== "object") return void 0;
			const item = value;
			if (typeof item.entryId !== "string" || item.entryId === "") return void 0;
			if (item.sessionId !== void 0 && (typeof item.sessionId !== "string" || item.sessionId === "")) return void 0;
			if (item.state !== "received" && item.state !== "session-created") return void 0;
			if (typeof item.name !== "string" || item.name === "") return void 0;
			if (typeof item.bytes !== "number" || !Number.isFinite(item.bytes) || item.bytes < 0) return void 0;
			return item;
		}
		function claimOf(value) {
			if (value === null || typeof value !== "object") return void 0;
			const claim = value;
			return claim.ok === true && typeof claim.ticket === "string" && claim.ticket !== "" && typeof claim.name === "string" && claim.name !== "" ? claim : void 0;
		}
		function waitTurn() {
			return new Promise((resolve) => {
				window.setTimeout(resolve, 100);
			});
		}
		/** Drives one process-local external attachment flow. */
		var IncomingDraftConsumer = class IncomingDraftConsumer {
			/** @param fetchImpl authenticated same-origin fetch. @param runtime session/conversation bridge. */
			constructor(fetchImpl, runtime) {
				this.fetchImpl = fetchImpl;
				this.runtime = runtime;
				_defineProperty(this, "busy", false);
				_defineProperty(this, "hydrating", /* @__PURE__ */ new Set());
				_defineProperty(this, "createdSessions", /* @__PURE__ */ new Map());
				_defineProperty(this, "attempts", /* @__PURE__ */ new Map());
				_defineProperty(this, "exhausted", /* @__PURE__ */ new Set());
				_defineProperty(this, "workspaceWarned", /* @__PURE__ */ new Set());
			}
			/** Fetch eligible queue metadata and hydrate each blank-session attachment once. */
			async poll() {
				if (this.busy) return;
				this.busy = true;
				document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "fetching");
				try {
					const response = await this.fetchImpl("/api/android/file-incoming", {
						credentials: "same-origin",
						cache: "no-store"
					});
					if (!response.ok) return;
					const payload = await response.json().catch(() => null);
					if (!Array.isArray(payload === null || payload === void 0 ? void 0 : payload.items)) return;
					document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "items:" + String(payload.items.length));
					for (const raw of payload.items) {
						const item = itemOf(raw);
						if (item !== void 0) await this.hydrate(item);
					}
				} catch (error) {
					console.warn("[dsh-mobile] incoming draft queue retry deferred:", String(error));
				} finally {
					this.busy = false;
				}
			}
			/**
			* review C10：尝试次数封顶。旧实现每次轮询无条件重试；createSession 失败时每次都会新建
			* workspace/session（用户侧表现：反复多出临时会话）。封顶后停止自动补建并提示一次，
			* 用户重新分享文件即可得到新条目（进程内新记录不受影响）。
			*/
			exhaustedFor(item) {
				const attempts = (this.attempts.get(item.entryId) ?? 0) + 1;
				this.attempts.set(item.entryId, attempts);
				if (attempts <= IncomingDraftConsumer.MAX_HYDRATE_ATTEMPTS) return false;
				document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "exhausted");
				if (!this.exhausted.has(item.entryId)) {
					this.exhausted.add(item.entryId);
					console.warn("[dsh-mobile] incoming draft hydration exhausted; a fresh share is required:", item.name);
					const sessionId = item.sessionId ?? this.createdSessions.get(item.entryId);
					const scope = sessionId === void 0 ? void 0 : this.runtime.sessionScope(sessionId);
					if (scope !== void 0) this.runtime.notify(scope, "外部附件草稿多次载入失败，已停止自动重试；请重新使用系统打开方式分享文件。");
				}
				return true;
			}
			/**
			* 「拿不到临时工作区路径」的可见回执（S3-21；同一 entryId 只提示一次）。
			*
			* 为什么会拿不到：壳侧的临时工作区尚未建立（首启/引擎重启中），或壳侧未装配该 bridge 方法。
			* 旧实现直接 return —— 用户的分享就此消失，既没有提示也没有下一步。
			* @param item - 被卡住的来件条目。
			*/
			notifyWorkspaceUnavailable(item) {
				if (this.workspaceWarned.has(item.entryId)) return;
				this.workspaceWarned.add(item.entryId);
				document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "workspace-unavailable");
				reportUserFacingResult({
					ok: false,
					title: "分享进来的文件还没放进来",
					detail: "应用正在准备临时工作区（或壳侧连接未就绪），所以这次分享还没有落地。请稍后重新分享一次，或改用应用内的附件按钮添加文件。"
				});
			}
			async hydrate(item) {
				if (this.hydrating.has(item.entryId)) return;
				if (this.exhaustedFor(item)) return;
				this.hydrating.add(item.entryId);
				document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "hydrate:" + item.state);
				try {
					const locallyCreated = this.createdSessions.get(item.entryId);
					let sessionId = item.sessionId ?? locallyCreated;
					if (sessionId !== void 0) {
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "session-refresh");
						try {
							if (item.sessionId !== void 0) await this.runtime.refreshSessions();
						} catch {
							sessionId = void 0;
						}
						if (sessionId !== void 0) {
							try {
								this.runtime.openSession(sessionId);
							} catch {}
							document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "session-opened");
						}
					}
					if (sessionId === void 0) {
						var _window$androidBridge, _window$androidBridge2;
						const cwd = ((_window$androidBridge = window.androidBridge) === null || _window$androidBridge === void 0 || (_window$androidBridge2 = _window$androidBridge.incomingWorkspacePath) === null || _window$androidBridge2 === void 0 ? void 0 : _window$androidBridge2.call(_window$androidBridge)) ?? "";
						if (cwd === "") {
							this.notifyWorkspaceUnavailable(item);
							return;
						}
						try {
							document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "session-create-call");
							sessionId = await this.runtime.createSession(cwd);
							this.createdSessions.set(item.entryId, sessionId);
							try {
								this.runtime.openSession(sessionId);
							} catch {}
							document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "session-opened");
						} catch (error) {
							console.warn("[dsh-mobile] incoming session creation deferred:", String(error));
							return;
						}
					}
					let scope = this.runtime.sessionScope(sessionId);
					for (let attempt = 0; scope === void 0 && attempt < 100; attempt += 1) {
						await waitTurn();
						scope = this.runtime.sessionScope(sessionId);
					}
					if (scope === void 0) return;
					let ticket = "";
					try {
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "claim");
						const claimResponse = await this.fetchImpl("/api/android/file-incoming/claim", {
							method: "POST",
							credentials: "same-origin",
							headers: { "content-type": "application/json" },
							body: JSON.stringify({
								entryId: item.entryId,
								sessionId
							})
						});
						const claim = claimResponse.ok ? claimOf(await claimResponse.json().catch(() => null)) : void 0;
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "claim:" + String(claimResponse.status));
						if (claim === void 0) return;
						ticket = claim.ticket;
						const content = await this.fetchImpl("/api/android/file-incoming/content?ticket=" + encodeURIComponent(ticket), {
							credentials: "same-origin",
							cache: "no-store"
						});
						if (!content.ok) throw new Error("incoming content HTTP " + content.status);
						const file = new File([await content.blob()], claim.name, { type: "application/octet-stream" });
						const attached = this.runtime.attachGenericFile(sessionId, file);
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "attached:" + String(attached));
						await this.finish(ticket, attached ? "draft-ready" : "removed");
						ticket = "";
						if (!attached) this.runtime.notify(scope, "外部附件草稿当前无法加入编辑框，请重新使用系统打开方式。");
					} catch {
						if (ticket !== "") await this.finish(ticket, "removed");
						this.runtime.notify(scope, "外部附件草稿载入失败；文件没有自动发送，请重新使用系统打开方式。");
					}
				} finally {
					this.hydrating.delete(item.entryId);
				}
			}
			async finish(ticket, outcome) {
				try {
					await this.fetchImpl("/api/android/file-incoming/complete", {
						method: "POST",
						credentials: "same-origin",
						headers: { "content-type": "application/json" },
						body: JSON.stringify({
							ticket,
							outcome
						})
					});
				} catch {}
			}
		};
		_defineProperty(IncomingDraftConsumer, "MAX_HYDRATE_ATTEMPTS", 5);
		//#endregion
		//#region src/client/index.ts
		/** Required services: composition, copy/theme faces, the runtime sessions, and the frame's panel actions. */
		const inject = [
			"slots",
			"theme",
			"sessions",
			"workspaces",
			"uiWorkspace",
			"layout",
			"conversation"
		];
		/** Append one stylesheet and return its disposer. */
		function injectStyle(id, css) {
			const style = document.createElement("style");
			style.setAttribute("data-plugin", id);
			style.textContent = css;
			document.head.appendChild(style);
			return () => {
				style.remove();
			};
		}
		/**
		* Client plugin body: the Android adaptation layer over the upstream frame.
		* @param ctx - client root context.
		*/
		function apply(ctx) {
			ctx.effect(() => injectStyle("snapshot-panels", SNAPSHOT_PANELS_CSS), "ui-responsive: snapshot panel paint");
			ctx.effect(() => {
				const observer = new SnapshotPanelsObserver();
				observer.attach();
				return () => {
					observer.detach();
				};
			}, "ui-responsive: snapshot panel ancestor ownership");
			ctx.effect(() => injectStyle("mobile-form", MOBILE_FORM_CSS), "ui-responsive: mobile form styles");
			ctx.effect(() => {
				const marker = new MobileFormMarker();
				marker.attach();
				return () => {
					marker.detach();
				};
			}, "ui-responsive: mobile form marker");
			ctx.effect(() => injectStyle("mobile-settings", MOBILE_SETTINGS_CSS), "ui-responsive: mobile settings styles");
			ctx.effect(() => injectStyle("composer-row", COMPOSER_ROW_CSS), "ui-responsive: composer row narrow fix");
			ctx.effect(() => injectStyle("composer-insets", COMPOSER_INSETS_CSS), "ui-responsive: composer insets adaptation");
			ctx.effect(() => injectStyle("composer-menu", COMPOSER_MENU_CSS), "ui-responsive: composer menu scroll fix");
			ctx.effect(() => {
				const guard = new ComposerPopupGuard();
				guard.attach();
				return () => {
					guard.detach();
				};
			}, "ui-responsive: composer popup geometry guard");
			ctx.effect(() => injectStyle("attachment-picker-menu", ATTACHMENT_PICKER_MENU_CSS), "ui-responsive: attachment picker source menu styles");
			ctx.effect(() => {
				const picker = new AttachmentPickerMenuEnhancer();
				picker.attach();
				return () => {
					picker.detach();
				};
			}, "ui-responsive: paperclip attachment/image source menu");
			ctx.effect(() => {
				const disposeStyle = injectStyle("trajectory-details", TRAJECTORY_DETAILS_CSS);
				const observer = new TrajectoryPanelsObserver(document.querySelector("[class*=\"ledger\"]"));
				observer.attach();
				return () => {
					observer.detach();
					disposeStyle();
				};
			}, "ui-responsive: trajectory details full-viewport overlay + :has() fallback");
			ctx.effect(() => {
				const guard = new EnterGuard();
				guard.attach();
				return () => {
					guard.detach();
				};
			}, "ui-responsive: mobile enter guard");
			ctx.effect(() => {
				const boundary = new KeyboardBoundary();
				boundary.attach();
				return () => {
					boundary.detach();
				};
			}, "ui-responsive: mobile keyboard boundary");
			ctx.effect(() => {
				new ThemeBridge().install();
				return () => {};
			}, "ui-responsive: theme bridge");
			ctx.effect(() => injectStyle("dev-section", DEV_SECTION_CSS), "ui-responsive: dev section styles");
			// 本机改造 2026-10-06：以下四个入口只服务安卓宿主 APK——window.androidBridge
			// 由 MainActivity 注入，纯浏览器访问时它们没有任何可用能力，只会往设置导航里
			// 多塞三个点不动的死入口（开发者选项 / 通知 / 手机控制）和一条 Android 显示。
			// 故仅在宿主桥存在时注册；手机形态的 CSS 重排不受影响。
			if (window.androidBridge !== void 0) {
			ctx.slots.inject("settings.section", () => ctx.slots.register({
				name: "settings.section",
				id: "android-dev",
				order: 99,
				label: () => "开发者选项",
				children: { "settings.dev.item": {
					kind: "list",
					scope: "root"
				} }
			}, DevSection));
			ctx.slots.inject("settings.section", () => ctx.slots.register({
				name: "settings.section",
				id: "android-notify",
				order: 97,
				label: () => "通知"
			}, NotifySettingsSection));
			ctx.slots.inject("settings.section", () => ctx.slots.register({
				name: "settings.section",
				id: "android-phone-control",
				order: 98,
				label: () => "手机控制"
			}, PhoneControlSection));
			ctx.slots.inject("settings.general.item", () => ctx.slots.register({
				name: "settings.general.item",
				id: "android-general",
				order: 90,
				label: () => "Android 显示"
			}, GeneralSettings));
			}
			ctx.slots.inject("shell.overlay", () => ctx.slots.register({
				name: "shell.overlay",
				id: "mobile-chrome",
				inject: () => ({ toggleSidebar: () => {
					ctx.layout.toggleSidebar();
				} })
			}, MobileChrome));
			ctx.slots.inject("conversation.header.leading", () => ctx.slots.register({
				name: "conversation.header.leading",
				inject: () => ({ toggleSidebar: () => {
					ctx.layout.toggleSidebar();
				} })
			}, SidebarToggle));
			ctx.effect(() => injectStyle("vendor-chrome-hide", VENDOR_CHROME_HIDE_CSS), "ui-responsive: hide vendor header chrome");
			const exportChannel = new ExportResultChannel();
			ctx.slots.inject("shell.overlay", () => ctx.slots.register({
				name: "shell.overlay",
				id: "export-result",
				inject: () => ({
					hooks: { exportResult: exportChannel },
					close: () => {
						exportChannel.close();
					}
				})
			}, ExportResultDialog));
			// 本机改造 2026-10-06：隐藏上游「导出 Session」弹窗是给安卓壳的自研导出让路的，
			// 浏览器里壳侧导出不存在，藏掉只会让上游导出功能凭空消失。故仅在宿主桥存在时生效。
			if (window.androidBridge !== void 0) ctx.effect(() => injectStyle("session-log-dialog", SESSION_LOG_DIALOG_HIDE_CSS), "ui-responsive: hide upstream session-log dialog");
			if (window.androidBridge !== void 0) ctx.effect(() => {
				const observer = new SessionLogDialogObserver();
				observer.attach();
				return () => {
					observer.detach();
				};
			}, "ui-responsive: session-log dialog :has() fallback");
			ctx.effect(() => {
				const tabs = ctx.get("sidebarRightTabs");
				if (tabs === void 0) return () => {};
				let ranking = false;
				const claimedByAnother = (address) => {
					if (ranking) return false;
					ranking = true;
					try {
						return tabs.candidates(address).some((definition) => definition.id !== "@dsh-android/client-ui-responsive/open-with" && definition.priority !== "fallback");
					} finally {
						ranking = false;
					}
				};
				return tabs.register(externalOpenDefinition(claimedByAnother));
			}, "ui-responsive: open-with tab type");
			ctx.slots.inject("sidebar.right.pane.tab", () => ctx.slots.register({
				name: "sidebar.right.pane.tab",
				key: EXTERNAL_OPEN_ID
			}, ExternalOpenTab));
			ctx.effect(() => {
				const marker = new SessionMarker(ctx.sessions);
				marker.attach();
				return () => {
					marker.detach();
				};
			}, "ui-responsive: session id marker for tool-row file links");
			// 本机改造 2026-10-06：原生浏览器工作台（BrowserHost）只有安卓壳有。它把上游
			// 「浏览器」标签的 kind 抢了过去，浏览器访问时就整页变成 "Android 浏览器不可用：
			// browser-command-unavailable"。故整块注入（标签类型 + 面板槽 + 原生 placement）
			// 都只在宿主桥存在时注册，浏览器里让回上游 ui-sidebar-browser。
			// 注意：这是单语句 if，末尾不要再补右括号。
			if (window.androidBridge !== void 0) ctx.inject([
				"locale",
				"sidebarRight",
				"sidebarRightTabs"
			], (scope) => {
				const namespace = "androidSidebarBrowser";
				const t = scope.locale.bind(namespace);
				const store = createBrowserStore();
				const openTabs = scope.sidebarRight.openTabs;
				const placement = new NativeBrowserPlacement({
					sessions: () => scope.sessions.list.getSnapshot().ids,
					tabs: () => openTabs.getSnapshot(),
					mounted: () => scope.sidebarRight.mounted.getSnapshot(),
					expanded: () => scope.sidebarRight.isExpanded(),
					open: (session) => {
						scope.sidebarRight.openTabIn(session, BROWSER_TAB_KIND, { revealIfOpened: false });
					},
					close: (session, tabId) => {
						scope.sidebarRight.closeIn(session, tabId);
					},
					kind: BROWSER_TAB_KIND,
					legacyKind: LEGACY_BROWSER_TAB_KIND
				});
				const controllers = /* @__PURE__ */ new Map();
				scope.effect(() => scope.locale.register(namespace, {
					zh,
					en
				}), "ui-responsive.browser.copy");
				scope.effect(() => scope.sidebarRightTabs.register(browserTabDefinition(t)), "ui-responsive.browser.type");
				scope.effect(() => scope.sidebarRightTabs.register(legacyBrowserTabDefinition(t)), "ui-responsive.browser.legacy-type");
				for (const kind of [BROWSER_TAB_KIND, LEGACY_BROWSER_TAB_KIND]) scope.effect(() => scope.sidebarRight.registerCloseHandler(kind, (session, tab) => {
					placement.for(session).closeUi(tab.id);
				}), "ui-responsive.browser.explicit-close");
				scope.effect(() => async () => {
					placement.dispose();
					await Promise.all([...controllers.values()].map((controller) => controller.dispose()));
					controllers.clear();
				}, "ui-responsive.browser.frames");
				for (const key of [BROWSER_TAB_ID, LEGACY_BROWSER_TAB_ID]) {
					scope.effect(() => scope.slots.inject("sidebar.right.pane.tab", () => scope.slots.register({
						name: "sidebar.right.pane.tab",
						key,
						locale: namespace,
						store,
						inject: (session, actions) => {
							const existing = controllers.get(session);
							if (existing !== void 0) {
								existing.rebind(actions);
								return existing;
							}
							const native = placement.for(session);
							const neutral = createBrowserControllers(actions, native.createPage, (tabId) => openTabs.getSnapshot().some((tab) => tab.sessionId === session && tab.tabId === tabId));
							const controller = {
								...neutral,
								keyedHooks: {
									...neutral.keyedHooks,
									nativeBrowserState: native.controlSource
								},
								setBrowserVisible: (tabId, visible) => {
									native.setVisible(tabId, visible);
								},
								setBrowserIdentity: (tabId, desktop) => {
									native.setIdentity(tabId, desktop);
								},
								setBrowserViewport: (tabId, width, height) => {
									native.setViewport(tabId, width, height);
								},
								refreshBrowserStatus: (tabId) => {
									native.refreshStatus(tabId);
								},
								closeBrowserTab: (tabId) => {
									scope.sidebarRight.closeIn(session, tabId);
								}
							};
							controllers.set(session, controller);
							return controller;
						}
					}, BrowserTab)), "ui-responsive.browser.body");
					scope.effect(() => scope.slots.inject("sidebar.right.pane.tab.title", () => scope.slots.register({
						name: "sidebar.right.pane.tab.title",
						key,
						store
					}, BrowserTitle)), "ui-responsive.browser.title");
				}
				scope.effect(() => scope.slots.inject("sidebar.right.tab.menu.item", () => scope.slots.register({
					name: "sidebar.right.tab.menu.item",
					id: "android-browser.native-controls",
					locale: namespace,
					inject: (session) => {
						const native = placement.for(session);
						return {
							keyedHooks: { nativeBrowserState: native.controlSource },
							setBrowserVisible: (tabId, visible) => {
								native.setVisible(tabId, visible);
							},
							setBrowserIdentity: (tabId, desktop) => {
								native.setIdentity(tabId, desktop);
							},
							setBrowserViewport: (tabId, width, height) => {
								native.setViewport(tabId, width, height);
							},
							refreshBrowserStatus: (tabId) => {
								native.refreshStatus(tabId);
							},
							closeBrowserTab: (tabId) => {
								scope.sidebarRight.closeIn(session, tabId);
							}
						};
					}
				}, BrowserTabMenu)), "ui-responsive.browser.menu");
				scope.effect(() => placement.attach(), "ui-responsive.browser.native-tab-placement");
			});
			ctx.effect(() => {
				const enhancer = new ReferenceMenuEnhancer();
				enhancer.attach();
				return () => {
					enhancer.detach();
				};
			}, "ui-responsive: reference menu enhancer");
			ctx.effect(() => injectStyle("reference-bar", REFERENCE_BAR_CSS), "ui-responsive: reference menu bar styles");
			ctx.effect(() => {
				const action = new SettingsDocumentAction();
				action.attach();
				return () => {
					action.detach();
				};
			}, "ui-responsive: settings document action takeover");
			ctx.effect(() => {
				const backStack = new BackStackSignal({
					toggleSidebar: () => {
						ctx.layout.toggleSidebar();
					},
					activePanelId: () => ctx.layout.panelInfo.getSnapshot().activePanelId,
					leaveMainPanel: () => {
						ctx.layout.selectPanel(null);
					}
				});
				backStack.attach();
				const drawer = new PanelNavDrawer({
					activePanelId: () => ctx.layout.panelInfo.getSnapshot().activePanelId,
					collapseDrawer: () => {
						ctx.layout.toggleSidebar();
					},
					frame: () => document.querySelector("[data-dsh-frame]")
				});
				const unsubscribe = ctx.layout.panelInfo.subscribe(() => {
					backStack.refresh();
					drawer.sync();
				});
				return () => {
					unsubscribe();
					backStack.detach();
				};
			}, "ui-responsive: back-stack signal (page layers → shell back gate)");
			ctx.effect(() => {
				const disposeStyle = injectStyle("main-panel-back", MAIN_PANEL_BACK_CSS);
				const mount = new MainPanelBackMount(() => {
					ctx.layout.selectPanel(null);
				});
				mount.attach();
				return () => {
					mount.detach();
					disposeStyle();
				};
			}, "ui-responsive: injected back control for upstream main panels");
			ctx.effect(() => {
				const onResult = (event) => {
					const payload = event.detail;
					if (payload === null || typeof payload !== "object") return;
					if (typeof payload.ok !== "boolean" || typeof payload.title !== "string" || typeof payload.detail !== "string") return;
					exportChannel.show(payload);
				};
				const bridge = (payload) => {
					reportUserFacingResult(payload);
				};
				window.__dshExportResult = bridge;
				window.addEventListener("dsh:export-result", onResult);
				return () => {
					window.removeEventListener("dsh:export-result", onResult);
					delete window.__dshExportResult;
				};
			}, "ui-responsive: export result dialog bridge");
			// 本机改造 2026-10-06：来件队列是安卓宿主 APK 的能力（/api/android/file-incoming
			// 由壳侧提供），浏览器里该接口不存在，这个每 4 秒一次的轮询只会一直 404。
			// 故仅在宿主桥存在时启动消费者。
			if (window.androidBridge !== void 0) ctx.effect(() => {
				document.documentElement.setAttribute("data-dsh-incoming-draft-consumer", "active");
				const sessions = () => ctx.sessions;
				const workspaces = () => ctx.get("workspaces");
				const uiWorkspace = () => ctx.get("uiWorkspace");
				const conversation = () => ctx.conversation;
				const consumer = new IncomingDraftConsumer((path, init) => fetch(path, init), {
					refreshSessions: () => sessions().refresh(),
					createSession: async (cwd) => {
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "workspace-create");
						const workspace = await workspaces().create({ path: cwd });
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "workspace-created");
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "workspace-connect");
						const sessionId = await uiWorkspace().connectWorkspace(workspace.workspaceId);
						document.documentElement.setAttribute("data-dsh-incoming-draft-poll", "workspace-connected");
						return String(sessionId);
					},
					openSession: (sessionId) => {
						sessions().open(sessionId);
					},
					sessionScope: (sessionId) => sessions().scope(sessionId),
					attachGenericFile: (sessionId, file) => {
						try {
							return conversation().addFiles(sessionId, [file]);
						} catch {
							return false;
						}
					},
					notify: (scope, text) => {
						try {
							conversation().input.for(scope).notify("error", text);
						} catch {}
					}
				});
				const poll = () => {
					consumer.poll();
				};
				const timer = window.setInterval(() => {
					if (document.visibilityState === "visible") poll();
				}, 4e3);
				const onVisible = () => {
					if (document.visibilityState === "visible") poll();
				};
				document.addEventListener("visibilitychange", onVisible);
				window.addEventListener("focus", onVisible);
				poll();
				return () => {
					window.clearInterval(timer);
					document.removeEventListener("visibilitychange", onVisible);
					window.removeEventListener("focus", onVisible);
					document.documentElement.removeAttribute("data-dsh-incoming-draft-consumer");
				};
			}, "ui-responsive: blank-session external attachment drafts");
			/**
			* 壳侧 → 页面的**通知落点**通道（0.14.1 批 4 / P0-1）。
			*
			* 为什么需要它：通知点击此前只是把应用拉到前台（壳侧一直在写 `dsh.notify.*` extras 而全仓没有
			* 读取者，`MainActivity` 连 `onNewIntent` 都没有）——整族通知是单向公告板。会话视图与切换能力
			* 只在页面里，故落点必须由页面执行：壳侧把会话 id 送进来，这里调会话服务的 `open(id)`。
			*
			* 契约（壳侧 `MainActivity.deliverNotifyRoute` 依此判成败）：**同步返回 boolean**
			*   true  = 已切到该会话；false = 没找到（会话可能已被删除），壳侧据此给用户可见提示。
			* 不把异常抛出去（抛出去会在桥层被吞成 undefined，壳侧就分不清「失败」与「未实现」）。
			*/
			ctx.effect(() => {
				const open = (sessionId) => openSessionForNotify(ctx.sessions, sessionId);
				window.__dshOpenSession = open;
				return () => {
					delete window.__dshOpenSession;
				};
			}, "ui-responsive: notification landing (openSession)");
		}
		//#endregion
		exports.MOBILE_FORM_MAX_WIDTH = MOBILE_FORM_MAX_WIDTH;
		exports.apply = apply;
		exports.inject = inject;
		return module.exports;
	}
});

//# sourceMappingURL=client.js.map

window.__ModuleLoader__.load({
  id: "@dsh-android/deepseek-harness-app-plugin",
  factory: (require) => {
    var module = { exports: {} };
    var exports = module.exports;
    Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });

    const CSS = `
/* Model trigger: give it the free width next to the upload control, but keep a real minimum so a
   narrow flex row can never collapse the label to an icon-sized zero. */
html[data-dsh-mobile-form] [data-composer-card] [class*="_root"]:has(> button[aria-label*="选择模型"]),
html[data-dsh-mobile-form] [data-composer-card] [class*="_root"]:has(> button[aria-label*="Select model"]) {
  flex: 1 1 auto !important;
  min-width: 120px !important;
  max-width: none !important;
}
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="选择模型"],
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="Select model"] {
  width: auto !important;
  max-width: 132px !important;
  margin-left: -8px !important;
  font-size: 12px !important;
  justify-content: flex-start !important;
}
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="选择模型"] span[class*="_triggerEffort"],
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="Select model"] span[class*="_triggerEffort"],
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="选择模型"] svg[class*="_chevron"],
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="Select model"] svg[class*="_chevron"] {
  display: none !important;
}
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="选择模型"] span[class*="_triggerLabel"],
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="Select model"] span[class*="_triggerLabel"] {
  display: none !important;
}
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="选择模型"]::after,
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="Select model"]::after {
  content: var(--dsh-mobile-model, "");
  white-space: nowrap;
  font: inherit;
  color: inherit;
}
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="选择模型"] span,
html[data-dsh-mobile-form] [data-composer-card] button[aria-label*="Select model"] span {
  overflow: visible !important;
  text-overflow: clip !important;
  white-space: nowrap !important;
}
/* Stats dock: two short readings must sit side by side, never overlap. */
html[data-dsh-mobile-form] [data-slot="conversation.composer.dock"] {
  flex-wrap: nowrap !important;
  justify-content: flex-start !important;
  gap: 10px !important;
  max-width: 100% !important;
}
html[data-dsh-mobile-form] [data-composer-stat] {
  flex: 0 0 auto !important;
  max-width: 45vw !important;
}
html[data-dsh-mobile-form] [data-composer-stat] span[class*="_label"] {
  display: none !important;
}
/* The label text is owned by React and is rewritten on every stream tick; render our reading
   through a root-level CSS variable instead of touching React's nodes. */
html[data-dsh-mobile-form] [data-composer-stat="activity"] button::after,
html[data-dsh-mobile-form] [data-composer-stat="activity"] span[class*="_pill"]::after {
  content: var(--dsh-mobile-speed, "");
  white-space: nowrap;
  font: inherit;
  color: inherit;
}
html[data-dsh-mobile-form] [data-composer-stat="usage"] button::after,
html[data-dsh-mobile-form] [data-composer-stat="usage"] span[class*="_pill"]::after {
  content: var(--dsh-mobile-usage, "");
  white-space: nowrap;
  font: inherit;
  color: inherit;
}
html[data-dsh-mobile-form] [data-composer-stat] svg {
  display: block !important;
  flex: none !important;
}
/* While a turn runs upstream hides the standardControls group (which owns the model trigger)
   and shows an activity control instead. On the phone the model name must stay visible, so keep
   the standard group in the row and let the separate stop button remain the running affordance. */
html[data-dsh-mobile-form] [data-composer-card] [class*="_standardControls"][hidden] {
  display: flex !important;
}
html[data-dsh-mobile-form] [data-composer-card] [class*="_activityExpanded"] {
  display: none !important;
}
/* The reference multi-select enhancer also lands on the plus menu, where the squares are noise.
   Desktop has no squares; hide the marker while keeping the enhancer's selection behaviour. */
html[data-dsh-mobile-form] [data-dsh-ref-check] {
  display: none !important;
}
/* The workspace folder button in the header overlaps the background-task chip on a phone.
   It is an open-in-app control, not part of the conversation; hide it in the mobile header and
   give the remaining header groups a real gap instead of letting their edges touch. */
html[data-dsh-mobile-form] header [data-open-target],
html[data-dsh-mobile-form] header [data-open-path],
html[data-dsh-mobile-form] header [aria-label*="工作区"],
html[data-dsh-mobile-form] header [aria-label*="workspace"],
html[data-dsh-mobile-form] header [aria-label*="文件夹"],
html[data-dsh-mobile-form] header [aria-label*="folder"] {
  display: none !important;
}
html[data-dsh-mobile-form] header [data-conversation-header-actions],
html[data-dsh-mobile-form] header [data-conversation-header-leading],
html[data-dsh-mobile-form] header [class*="_headerActions"],
html[data-dsh-mobile-form] header [class*="_headerUtilities"] {
  gap: 8px !important;
}
/* The background-job chip can be wider than its header seat, pushing into the "..." menu.
   Keep it in its lane and truncate the label instead of overlapping the controls. */
html[data-dsh-mobile-form] header button[aria-label*="后台任务"],
html[data-dsh-mobile-form] header button[aria-label*="background job"] {
  max-width: 40vw !important;
  overflow: hidden !important;
}
`;

    function apply(ctx) {
      if (typeof document === "undefined") return;
      // This bundle is shared by the desktop Harness app and the Android WebView. The phone
      // tweaks (compact stats, model label, file actions) must never run in the desktop window,
      // where they would hide the native speed/identity indicators. Require an Android-only
      // signal; a generic native bridge name is not enough because desktop shells can expose one.
      const androidShell = /DSHAndroidShell/i.test(navigator.userAgent) ||
        new URLSearchParams(location.search).get("dsh_android") === "1" ||
        /;\s*wv\)/i.test(navigator.userAgent);
      if (!androidShell) {
        document.documentElement.removeAttribute("data-dsh-android-shell");
        document.documentElement.removeAttribute("data-dsh-mobile-tweaks");
        window.__dshMobileTweaks = false;
        for (const name of ["--dsh-mobile-speed", "--dsh-mobile-usage", "--dsh-mobile-model"]) {
          document.documentElement.style.removeProperty(name);
        }
        document.querySelectorAll('style[data-plugin="@dsh-android/dsh-client-ui-responsive-bundle"]')
          .forEach((node) => node.remove());
        return;
      }
      document.documentElement.setAttribute("data-dsh-android-shell", "1");
      document.documentElement.setAttribute("data-dsh-mobile-tweaks", "1");
      window.__dshMobileTweaks = true;
      try {
        if (ctx.uiWorkspace && typeof ctx.uiWorkspace.openSession === "function") {
          window.__dshOpenSession = (sessionId) => ctx.uiWorkspace.openSession(String(sessionId));
        }
      } catch (e) {}

      const style = document.createElement("style");
      style.dataset.plugin = "@dsh-android/dsh-client-ui-responsive-bundle";
      style.textContent = CSS;
      document.head.appendChild(style);

      const setVar = (name, value) => {
        const rootStyle = document.documentElement.style;
        const quoted = JSON.stringify(value);
        if ((rootStyle.getPropertyValue(name) || "") !== quoted) rootStyle.setProperty(name, quoted);
      };

      const compactActivity = () => {
        const root = document.querySelector('[data-composer-stat="activity"]');
        if (root === null) return;
        const button = root.querySelector("button");
        const label = root.querySelector('span[class*="_label"]');
        const aria = (button === null ? root.getAttribute("aria-label") : button.getAttribute("aria-label")) || "";
        const parts = aria.split("·").map((part) => part.trim()).filter((part) => part !== "");
        let speed = parts.find((part) => /tok/i.test(part));
        if (speed === undefined && label !== null) {
          // Fallback for a node React has not finished decorating: the speed is the text after
          // the separator span. Read it without touching React's nodes.
          const separator = label.querySelector('span[class*="_sep"]');
          if (separator !== null) {
            let text = "";
            let node = separator.nextSibling;
            while (node !== null) {
              text += node.textContent || "";
              node = node.nextSibling;
            }
            speed = text.trim();
          }
        }
        if (speed !== undefined && speed !== "") {
          if (root.style.display !== "") root.style.display = "";
          setVar("--dsh-mobile-speed", speed);
          return;
        }
        // No speed available: hide the turns/steps-only pill rather than showing the counts.
        if (root.style.display !== "none") root.style.display = "none";
        setVar("--dsh-mobile-speed", "");
      };

      const compactUsage = () => {
        const root = document.querySelector('[data-composer-stat="usage"]');
        if (root === null) return;
        const button = root.querySelector("button");
        const label = root.querySelector('span[class*="_label"]');
        const aria = (button === null ? root.getAttribute("aria-label") : button.getAttribute("aria-label")) || "";
        const text = aria || (label === null ? "" : label.textContent || "");
        const tokens = text.split("·")[0].trim();
        if (tokens === "") return;
        const percent = text.match(/(\d+(?:\.\d+)?%)/);
        const wanted = percent === null ? tokens : tokens + " · " + percent[1];
        if (root.style.display !== "") root.style.display = "";
        setVar("--dsh-mobile-usage", wanted);
      };

      const compactStats = () => {
        if (document.visibilityState === "hidden") return;
        compactActivity();
        compactUsage();
        const model = document.querySelector('[data-composer-card] button[aria-label*="选择模型"], [data-composer-card] button[aria-label*="Select model"]');
        const modelLabel = model === null ? null : model.querySelector('span[class*="_triggerLabel"]');
        const modelText = modelLabel === null ? "" : (modelLabel.textContent || "").trim();
        if (modelText !== "") setVar("--dsh-mobile-model", modelText);
      };
      window.__dshCompactStats = compactStats;

      let scheduled = false;
      const schedule = () => {
        if (scheduled) return;
        scheduled = true;
        // Stream ticks arrive many times per second; refresh the two readings at a calm rate.
        window.setTimeout(() => {
          scheduled = false;
          compactStats();
        }, 700);
      };

      const observe = () => {
        const dock = document.querySelector('[data-slot="conversation.composer.dock"]') || document.querySelector("[data-composer-seat]");
        if (dock === null) {
          window.setTimeout(observe, 800);
          return;
        }
        compactStats();
        try {
          new MutationObserver(schedule).observe(dock, { subtree: true, childList: true, characterData: true });
        } catch (e) {}
      };

      observe();
      try {
        new MutationObserver(schedule).observe(document.body, { subtree: true, childList: true, characterData: true });
      } catch (e) {}
      window.setInterval(compactStats, 2000);
      document.addEventListener("visibilitychange", () => {
        if (document.visibilityState !== "hidden") compactStats();
      });
      installFileActions();
      installImageUploadDefault();
      installNativeVoiceBridge();
    }

    /**
     * Use the Android speech recognizer for the composer microphone. The experimental DSH voice
     * plugin records PCM and sends it to a host transcription backend; on a phone that path can
     * be unavailable. The native recognizer is a local/system fallback and inserts the transcript
     * into the same Lexical composer.
     */
    function installNativeVoiceBridge() {
      if (window.__dshNativeVoiceBridge) return;
      window.__dshNativeVoiceBridge = true;
      window.__dshInsertSpeech = (text) => {
        const editor = document.querySelector('[data-composer-input="true"]');
        if (editor === null || text === undefined || text === null) return false;
        editor.focus();
        try {
          const selection = window.getSelection();
          const range = document.createRange();
          range.selectNodeContents(editor);
          range.collapse(false);
          selection.removeAllRanges();
          selection.addRange(range);
          document.execCommand("insertText", false, String(text));
          editor.dispatchEvent(new InputEvent("input", {
            bubbles: true,
            inputType: "insertText",
            data: String(text),
          }));
          return true;
        } catch (e) {
          return false;
        }
      };
      document.addEventListener("click", (event) => {
        const target = event.target instanceof Element ? event.target : null;
        if (target === null) return;
        const trigger = target.closest(
          "[aria-label*='开始录音'], [aria-label*='语音'], [aria-label*='voice'], [aria-label*='microphone']",
        );
        if (trigger === null) return;
        try {
          if (typeof window.DshShellBridge.startSpeechInput !== "function") return;
          event.preventDefault();
          event.stopImmediatePropagation();
          window.DshShellBridge.startSpeechInput();
        } catch (e) {}
      }, true);
    }

    /**
     * On the phone the attachment trigger opens a two-row menu whose entries both end in the same
     * Android file chooser. Choose the image entry immediately so the phone gets the image picker
     * without an extra menu tap. The menu is React-owned; a MutationObserver clicks its image row
     * as soon as it mounts.
     */
    function installImageUploadDefault() {
      if (window.__dshImageUploadDefault) return;
      window.__dshImageUploadDefault = true;
      let pending = false;
      const isImageItem = (node) => {
        const text = ((node.innerText || "") + " " + (node.getAttribute("aria-label") || "")).trim();
        return /上传图片|上传照片|upload image|upload photo/i.test(text);
      };
      document.addEventListener("click", (event) => {
        const target = event.target instanceof Element ? event.target : null;
        if (target === null) return;
        if (target.closest(".dsh-attachment-picker-trigger, [aria-label*='上传附件'], [aria-label*='Upload attachment']")) {
          pending = true;
        }
      }, true);
      try {
        new MutationObserver(() => {
          if (!pending) return;
          const item = [...document.querySelectorAll("button, [role='menuitem']")]
            .find((node) => node.offsetWidth > 0 && isImageItem(node));
          if (item === undefined) return;
          pending = false;
          item.click();
        }).observe(document.body, { subtree: true, childList: true });
      } catch (e) {}
    }

    /**
     * Workspace file panel: add a per-row "..." menu with download/delete.
     *
     * The shipped workspaceFiles API is read-only, so these two actions go through the small
     * host-side /dsh-file-ops route. Paths are reconstructed from the panel's absolute root plus
     * the nested row labels; the host route confines them to its configured workspace roots.
     */
    function installFileActions() {
      if (window.__dshFileActions) return;
      window.__dshFileActions = true;

      const style = document.createElement("style");
      style.textContent = `
        [class*="Woyo-G_row"] { position: relative !important; padding-right: 26px !important; }
        .dsh-file-more {
          position: absolute; right: 4px; top: 50%; transform: translateY(-50%);
          width: 22px; height: 22px; line-height: 20px; text-align: center;
          border-radius: 6px; color: var(--dsw-alias-label-secondary, #999);
          font: 16px/20px sans-serif; cursor: pointer; z-index: 2;
        }
        .dsh-file-more:hover { background: var(--dsw-alias-interactive-bg-hover, rgba(127,127,127,.2)); }
        .dsh-file-menu {
          position: fixed; z-index: 2147483000; min-width: 128px; padding: 4px;
          border: 1px solid var(--dsw-alias-border-l1, rgba(127,127,127,.3));
          border-radius: 10px; background: var(--dsw-alias-bg-layer-2, #1c1c1e);
          box-shadow: 0 8px 30px rgba(0,0,0,.35); color: var(--dsw-alias-label-primary, #eee);
          font: 13px/1.4 sans-serif;
        }
        .dsh-file-menu button {
          display: block; width: 100%; padding: 7px 10px; border: 0; border-radius: 7px;
          background: transparent; color: inherit; text-align: left; font: inherit; cursor: pointer;
        }
        .dsh-file-menu button:hover { background: var(--dsw-alias-interactive-bg-hover, rgba(127,127,127,.2)); }
        .dsh-file-menu button.danger { color: var(--dsw-alias-label-danger, #f2555a); }
      `;
      style.dataset.plugin = "@dsh-android/dsh-file-actions";
      document.head.appendChild(style);

      let openMenu = null;
      const closeMenu = () => {
        if (openMenu !== null) {
          openMenu.remove();
          openMenu = null;
        }
      };
      document.addEventListener("pointerdown", (event) => {
        if (openMenu !== null && !openMenu.contains(event.target)) closeMenu();
      }, true);
      window.addEventListener("scroll", closeMenu, true);

      const label = (row) => {
        const name = row.querySelector('[class*="Woyo-G_name"]');
        return (name === null ? row.innerText : name.textContent || "").trim();
      };
      const rootPath = () => {
        const path = document.querySelector('[class*="Woyo-G_path"]');
        return path === null ? "" : (path.textContent || "").trim().replace(/[\\/]+$/, "");
      };
      const absolutePath = (row) => {
        const root = rootPath();
        if (root === "") return "";
        const parts = [label(row)];
        let li = row.closest("li");
        while (li !== null) {
          const parentLi = li.parentElement === null ? null : li.parentElement.closest("li");
          if (parentLi === null) break;
          const parentRow = parentLi.querySelector(':scope > [class*="Woyo-G_row"]');
          if (parentRow !== null) parts.unshift(label(parentRow));
          li = parentLi;
        }
        return root + "\\" + parts.join("\\");
      };
      const refreshFiles = () => {
        const header = document.querySelector('[class*="Woyo-G_header"]');
        const button = header === null ? null : [...header.querySelectorAll("button")].find((item) => {
          const text = (item.getAttribute("aria-label") || "") + (item.getAttribute("title") || "");
          return /刷新|refresh/i.test(text);
        });
        if (button !== null) button.click();
        else window.setTimeout(() => window.location.reload(), 300);
      };

      const showMenu = (row, filePath) => {
        closeMenu();
        const menu = document.createElement("div");
        menu.className = "dsh-file-menu";
        const download = document.createElement("button");
        download.textContent = "下载";
        download.addEventListener("click", (event) => {
          event.preventDefault();
          event.stopPropagation();
          closeMenu();
          const anchor = document.createElement("a");
          anchor.href = "/dsh-file-ops?action=download&path=" + encodeURIComponent(filePath);
          anchor.download = filePath.split(/[\\/]/).pop() || "download";
          document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();
        });
        const remove = document.createElement("button");
        remove.className = "danger";
        remove.textContent = "删除";
        remove.addEventListener("click", async (event) => {
          event.preventDefault();
          event.stopPropagation();
          closeMenu();
          if (!window.confirm("删除这个文件/文件夹？\\n" + filePath)) return;
          try {
            const response = await fetch("/dsh-file-ops?action=delete", {
              method: "POST",
              headers: { "content-type": "application/json" },
              body: JSON.stringify({ path: filePath }),
            });
            if (!response.ok) throw new Error((await response.json().catch(() => ({}))).error || response.status);
            window.setTimeout(refreshFiles, 350);
          } catch (error) {
            window.alert("删除失败：" + (error instanceof Error ? error.message : String(error)));
          }
        });
        menu.append(download, remove);
        document.body.appendChild(menu);
        const rect = row.getBoundingClientRect();
        const width = 140;
        const left = Math.max(8, Math.min(rect.right - width, window.innerWidth - width - 8));
        const top = Math.min(rect.bottom + 4, window.innerHeight - 90);
        menu.style.left = left + "px";
        menu.style.top = top + "px";
        openMenu = menu;
      };

      const decorate = () => {
        if (document.visibilityState === "hidden") return;
        if (rootPath() === "") return;
        document.querySelectorAll('[class*="Woyo-G_row"]').forEach((row) => {
          if (row.dataset.dshFileActions === "1") return;
          row.dataset.dshFileActions = "1";
          const more = document.createElement("span");
          more.className = "dsh-file-more";
          more.textContent = "⋯";
          more.title = "文件操作";
          more.setAttribute("role", "button");
          more.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
            const filePath = absolutePath(row);
            if (filePath !== "") showMenu(row, filePath);
          });
          row.appendChild(more);
        });
      };

      decorate();
      document.addEventListener("visibilitychange", () => {
        if (document.visibilityState !== "hidden") decorate();
      });
      try {
        new MutationObserver(decorate).observe(document.body, { subtree: true, childList: true });
      } catch (e) {}
      window.setInterval(decorate, 1500);
    }

    const inject = [];
    exports.apply = apply;
    exports.inject = inject;
    return module.exports;
  }
});


window.__ModuleLoader__.load({
	id: "@wxg-prc-cpg/browser-skill-dsh-plugin",
	factory: (require) => {
		var module = { exports: {} };
		var exports = module.exports;
		Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
		//#region \0rolldown/runtime.js
		var __create = Object.create;
		var __defProp = Object.defineProperty;
		var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
		var __getOwnPropNames = Object.getOwnPropertyNames;
		var __getProtoOf = Object.getPrototypeOf;
		var __hasOwnProp = Object.prototype.hasOwnProperty;
		var __copyProps = (to, from, except, desc) => {
			if (from && typeof from === "object" || typeof from === "function") for (var keys = __getOwnPropNames(from), i = 0, n = keys.length, key; i < n; i++) {
				key = keys[i];
				if (!__hasOwnProp.call(to, key) && key !== except) __defProp(to, key, {
					get: ((k) => from[k]).bind(null, key),
					enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable
				});
			}
			return to;
		};
		var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(isNodeMode || !mod || !mod.__esModule || !__hasOwnProp.call(mod, "default") ? __defProp(target, "default", {
			value: mod,
			enumerable: true
		}) : target, mod));
		//#endregion
		let react = require("react");
		react = __toESM(react, 1);
		let _deepseek_ai_dsh_client_ui_primitives = require("@deepseek-ai/dsh-client-ui-primitives");
		let react_jsx_runtime = require("react/jsx-runtime");
		let react_dom = require("react-dom");
		//#region \0bsk-css:/home/runner/work/BrowserSkill/BrowserSkill/packages/dsh-plugin-browserskill/src/client/bsk-tokens.nomodule.css.mjs
		const css$4 = ".bsk-obs{color-scheme:light dark;--background:oklch(99% .005 50);--foreground:oklch(14% .03 50);--card:oklch(99.5% .003 50/.82);--card-foreground:oklch(14% .03 50);--primary:oklch(62% .22 45);--primary-foreground:oklch(99% .01 50);--secondary:oklch(96% .04 65);--secondary-foreground:oklch(25% .08 65);--muted:oklch(96% .01 50);--muted-foreground:oklch(55% .04 50);--accent:oklch(96% .04 65);--accent-foreground:oklch(25% .08 65);--destructive:oklch(60% .25 25);--destructive-foreground:oklch(99% .01 50);--border:oklch(90% .025 60);--input:oklch(90% .025 60);--ring:oklch(62% .22 45);--radius:.75rem}@media (prefers-color-scheme:dark){.bsk-obs{--background:oklch(20% .02 50);--foreground:oklch(98% .01 50);--card:oklch(24% .02 50/.82);--card-foreground:oklch(98% .01 50);--primary:oklch(70% .18 45);--primary-foreground:oklch(14% .03 50);--secondary:oklch(32% .03 60);--secondary-foreground:oklch(98% .01 50);--muted:oklch(30% .02 50);--muted-foreground:oklch(75% .03 50);--accent:oklch(32% .03 60);--accent-foreground:oklch(98% .01 50);--destructive:oklch(45% .2 25);--destructive-foreground:oklch(98% .01 50);--border:oklch(35% .02 60);--input:oklch(35% .02 60);--ring:oklch(70% .18 45)}}";
		const tagId$4 = "@wxg-prc-cpg/browser-skill-dsh-plugin/bsk-tokens.nomodule.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$4) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@wxg-prc-cpg/browser-skill-dsh-plugin";
			tag.dataset.pluginCss = tagId$4;
			tag.textContent = css$4;
			document.head.appendChild(tag);
		}
		//#endregion
		//#region \0bsk-css:/home/runner/work/BrowserSkill/BrowserSkill/packages/dsh-plugin-browserskill/src/client/bsk-ui.nomodule.css.mjs
		const css$3 = "/*! tailwindcss v4.3.3 | MIT License | https://tailwindcss.com */\n@layer properties{@supports (((-webkit-hyphens:none)) and (not (margin-trim:inline))) or ((-moz-orient:inline) and (not (color:rgb(from red r g b)))){.bsk-obs *,.bsk-obs :before,.bsk-obs :after,.bsk-obs ::backdrop{--tw-rotate-x:initial;--tw-rotate-y:initial;--tw-rotate-z:initial;--tw-skew-x:initial;--tw-skew-y:initial;--tw-border-style:solid;--tw-leading:initial;--tw-font-weight:initial;--tw-tracking:initial;--tw-shadow:0 0 #0000;--tw-shadow-color:initial;--tw-shadow-alpha:100%;--tw-inset-shadow:0 0 #0000;--tw-inset-shadow-color:initial;--tw-inset-shadow-alpha:100%;--tw-ring-color:initial;--tw-ring-shadow:0 0 #0000;--tw-inset-ring-color:initial;--tw-inset-ring-shadow:0 0 #0000;--tw-ring-inset:initial;--tw-ring-offset-width:0px;--tw-ring-offset-color:#fff;--tw-ring-offset-shadow:0 0 #0000;--tw-outline-style:solid;--tw-blur:initial;--tw-brightness:initial;--tw-contrast:initial;--tw-grayscale:initial;--tw-hue-rotate:initial;--tw-invert:initial;--tw-opacity:initial;--tw-saturate:initial;--tw-sepia:initial;--tw-drop-shadow:initial;--tw-drop-shadow-color:initial;--tw-drop-shadow-alpha:100%;--tw-drop-shadow-size:initial}}}@layer theme{.bsk-obs,.bsk-obs :host{--font-sans:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", \"Noto Sans\", Arial, sans-serif, \"Apple Color Emoji\", \"Segoe UI Emoji\", \"Segoe UI Symbol\", \"Noto Color Emoji\";--font-mono:ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace;--color-red-500:oklch(63.7% .237 25.331);--color-amber-500:oklch(76.9% .188 70.08);--color-emerald-500:oklch(69.6% .17 162.48);--spacing:.25rem;--text-xs:.75rem;--text-xs--line-height:calc(1 / .75);--text-sm:.875rem;--text-sm--line-height:calc(1.25 / .875);--font-weight-medium:500;--font-weight-semibold:600;--tracking-tight:-.025em;--leading-relaxed:1.625;--default-transition-duration:.15s;--default-transition-timing-function:cubic-bezier(.4, 0, .2, 1);--default-font-family:var(--font-sans);--default-mono-font-family:var(--font-mono)}}@layer base{.bsk-obs *,.bsk-obs :after,.bsk-obs :before,.bsk-obs ::backdrop,.bsk-obs ::file-selector-button{box-sizing:border-box;border:0 solid;margin:0;padding:0}.bsk-obs,.bsk-obs :host{-webkit-text-size-adjust:100%;tab-size:4;line-height:1.5;font-family:var(--default-font-family,-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", \"Noto Sans\", Arial, sans-serif, \"Apple Color Emoji\", \"Segoe UI Emoji\", \"Segoe UI Symbol\", \"Noto Color Emoji\");font-feature-settings:var(--default-font-feature-settings,normal);font-variation-settings:var(--default-font-variation-settings,normal);-webkit-tap-highlight-color:transparent}.bsk-obs hr{height:0;color:inherit;border-top-width:1px}.bsk-obs abbr:where([title]){text-decoration:underline dotted}.bsk-obs h1,.bsk-obs h2,.bsk-obs h3,.bsk-obs h4,.bsk-obs h5,.bsk-obs h6{font-size:inherit;font-weight:inherit}.bsk-obs a{color:inherit;-webkit-text-decoration:inherit;text-decoration:inherit}.bsk-obs b,.bsk-obs strong{font-weight:bolder}.bsk-obs code,.bsk-obs kbd,.bsk-obs samp,.bsk-obs pre{font-family:var(--default-mono-font-family,ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace);font-feature-settings:var(--default-mono-font-feature-settings,normal);font-variation-settings:var(--default-mono-font-variation-settings,normal);font-size:1em}.bsk-obs small{font-size:80%}.bsk-obs sub,.bsk-obs sup{vertical-align:baseline;font-size:75%;line-height:0;position:relative}.bsk-obs sub{bottom:-.25em}.bsk-obs sup{top:-.5em}.bsk-obs table{text-indent:0;border-color:inherit;border-collapse:collapse}.bsk-obs :-moz-focusring:where(:not(iframe)){outline:auto}.bsk-obs progress{vertical-align:baseline}.bsk-obs summary{display:list-item}.bsk-obs ol,.bsk-obs ul,.bsk-obs menu{list-style:none}.bsk-obs img,.bsk-obs svg,.bsk-obs video,.bsk-obs canvas,.bsk-obs audio,.bsk-obs iframe,.bsk-obs embed,.bsk-obs object{vertical-align:middle;display:block}.bsk-obs img,.bsk-obs video{max-width:100%;height:auto}.bsk-obs button,.bsk-obs input,.bsk-obs select,.bsk-obs optgroup,.bsk-obs textarea,.bsk-obs ::file-selector-button{font:inherit;font-feature-settings:inherit;font-variation-settings:inherit;letter-spacing:inherit;color:inherit;opacity:1;background-color:#0000;border-radius:0}.bsk-obs :where(select:is([multiple],[size])) optgroup{font-weight:bolder}.bsk-obs :where(select:is([multiple],[size])) optgroup option{padding-inline-start:20px}.bsk-obs ::file-selector-button{margin-inline-end:4px}.bsk-obs ::placeholder{opacity:1}@supports (not ((-webkit-appearance:-apple-pay-button))) or (contain-intrinsic-size:1px){.bsk-obs ::placeholder{color:currentColor;@supports (color:color-mix(in lab, red, red)){color:color-mix(in oklab, currentcolor 50%, transparent)}}}.bsk-obs textarea{resize:vertical}.bsk-obs ::-webkit-search-decoration{-webkit-appearance:none}.bsk-obs ::-webkit-date-and-time-value{min-height:1lh;text-align:inherit}.bsk-obs ::-webkit-datetime-edit{display:inline-flex}.bsk-obs ::-webkit-datetime-edit-fields-wrapper{padding:0}.bsk-obs ::-webkit-datetime-edit,.bsk-obs ::-webkit-datetime-edit-year-field,.bsk-obs ::-webkit-datetime-edit-month-field,.bsk-obs ::-webkit-datetime-edit-day-field,.bsk-obs ::-webkit-datetime-edit-hour-field,.bsk-obs ::-webkit-datetime-edit-minute-field,.bsk-obs ::-webkit-datetime-edit-second-field,.bsk-obs ::-webkit-datetime-edit-millisecond-field,.bsk-obs ::-webkit-datetime-edit-meridiem-field{padding-block:0}.bsk-obs ::-webkit-calendar-picker-indicator{line-height:1}.bsk-obs :-moz-ui-invalid{box-shadow:none}.bsk-obs button,.bsk-obs input:where([type=button],[type=reset],[type=submit]),.bsk-obs ::file-selector-button{appearance:button}.bsk-obs ::-webkit-inner-spin-button,.bsk-obs ::-webkit-outer-spin-button{height:auto}.bsk-obs [hidden]:where(:not([hidden=until-found])){display:none!important}}@layer components;@layer utilities{.bsk-obs .collapse{visibility:collapse}.bsk-obs .invisible{visibility:hidden}.bsk-obs .visible{visibility:visible}.bsk-obs .absolute{position:absolute}.bsk-obs .fixed{position:fixed}.bsk-obs .relative{position:relative}.bsk-obs .static{position:static}.bsk-obs .isolate{isolation:isolate}.bsk-obs .col-start-2{grid-column-start:2}.bsk-obs .row-span-2{grid-row:span 2/span 2}.bsk-obs .row-start-1{grid-row-start:1}.bsk-obs .container{width:100%;@media (width>=40rem){max-width:40rem}@media (width>=48rem){max-width:48rem}@media (width>=64rem){max-width:64rem}@media (width>=80rem){max-width:80rem}@media (width>=96rem){max-width:96rem}}.bsk-obs .block{display:block}.bsk-obs .flex{display:flex}.bsk-obs .grid{display:grid}.bsk-obs .hidden{display:none}.bsk-obs .inline{display:inline}.bsk-obs .inline-flex{display:inline-flex}.bsk-obs .table{display:table}.bsk-obs .size-2{width:calc(var(--spacing) * 2);height:calc(var(--spacing) * 2)}.bsk-obs .size-9{width:calc(var(--spacing) * 9);height:calc(var(--spacing) * 9)}.bsk-obs .h-8{height:calc(var(--spacing) * 8)}.bsk-obs .h-9{height:calc(var(--spacing) * 9)}.bsk-obs .h-10{height:calc(var(--spacing) * 10)}.bsk-obs .w-fit{width:fit-content}.bsk-obs .w-full{width:100%}.bsk-obs .min-w-0{min-width:0}.bsk-obs .shrink-0{flex-shrink:0}.bsk-obs .transform{transform:var(--tw-rotate-x,) var(--tw-rotate-y,) var(--tw-rotate-z,) var(--tw-skew-x,) var(--tw-skew-y,)}.bsk-obs .resize{resize:both}.bsk-obs .auto-rows-min{grid-auto-rows:min-content}.bsk-obs .grid-rows-\\[auto_auto\\]{grid-template-rows:auto auto}.bsk-obs .flex-col{flex-direction:column}.bsk-obs .items-center{align-items:center}.bsk-obs .items-start{align-items:flex-start}.bsk-obs .justify-center{justify-content:center}.bsk-obs .gap-1{gap:var(--spacing)}.bsk-obs .gap-1\\.5{gap:calc(var(--spacing) * 1.5)}.bsk-obs .gap-2{gap:calc(var(--spacing) * 2)}.bsk-obs .gap-5{gap:calc(var(--spacing) * 5)}.bsk-obs .self-start{align-self:flex-start}.bsk-obs .justify-self-end{justify-self:flex-end}.bsk-obs .overflow-hidden{overflow:hidden}.bsk-obs .rounded-2xl{border-radius:calc(var(--radius) + 8px)}.bsk-obs .rounded-full{border-radius:3.40282e38px}.bsk-obs .rounded-xl{border-radius:calc(var(--radius) + 4px)}.bsk-obs .border{border-style:var(--tw-border-style);border-width:1px}.bsk-obs .border-input{border-color:var(--input)}.bsk-obs .border-transparent{border-color:#0000}.bsk-obs .bg-amber-500{background-color:var(--color-amber-500)}.bsk-obs .bg-background{background-color:var(--background)}.bsk-obs .bg-background\\/70{background-color:var(--background);@supports (color:color-mix(in lab, red, red)){background-color:color-mix(in oklab, var(--background) 70%, transparent)}}.bsk-obs .bg-card{background-color:var(--card)}.bsk-obs .bg-destructive{background-color:var(--destructive)}.bsk-obs .bg-emerald-500{background-color:var(--color-emerald-500)}.bsk-obs .bg-muted-foreground\\/40{background-color:var(--muted-foreground);@supports (color:color-mix(in lab, red, red)){background-color:color-mix(in oklab, var(--muted-foreground) 40%, transparent)}}.bsk-obs .bg-primary{background-color:var(--primary)}.bsk-obs .bg-red-500{background-color:var(--color-red-500)}.bsk-obs .bg-secondary{background-color:var(--secondary)}.bsk-obs .px-2{padding-inline:calc(var(--spacing) * 2)}.bsk-obs .px-3{padding-inline:calc(var(--spacing) * 3)}.bsk-obs .px-4{padding-inline:calc(var(--spacing) * 4)}.bsk-obs .px-5{padding-inline:calc(var(--spacing) * 5)}.bsk-obs .px-6{padding-inline:calc(var(--spacing) * 6)}.bsk-obs .py-0\\.5{padding-block:calc(var(--spacing) * .5)}.bsk-obs .py-1{padding-block:var(--spacing)}.bsk-obs .py-2{padding-block:calc(var(--spacing) * 2)}.bsk-obs .py-5{padding-block:calc(var(--spacing) * 5)}.bsk-obs .text-sm{font-size:var(--text-sm);line-height:var(--tw-leading,var(--text-sm--line-height))}.bsk-obs .text-xs{font-size:var(--text-xs);line-height:var(--tw-leading,var(--text-xs--line-height))}.bsk-obs .leading-none{--tw-leading:1;line-height:1}.bsk-obs .leading-relaxed{--tw-leading:var(--leading-relaxed);line-height:var(--leading-relaxed)}.bsk-obs .font-medium{--tw-font-weight:var(--font-weight-medium);font-weight:var(--font-weight-medium)}.bsk-obs .font-semibold{--tw-font-weight:var(--font-weight-semibold);font-weight:var(--font-weight-semibold)}.bsk-obs .tracking-tight{--tw-tracking:var(--tracking-tight);letter-spacing:var(--tracking-tight)}.bsk-obs .whitespace-nowrap{white-space:nowrap}.bsk-obs .text-card-foreground{color:var(--card-foreground)}.bsk-obs .text-destructive-foreground{color:var(--destructive-foreground)}.bsk-obs .text-foreground{color:var(--foreground)}.bsk-obs .text-muted-foreground{color:var(--muted-foreground)}.bsk-obs .text-primary{color:var(--primary)}.bsk-obs .text-primary-foreground{color:var(--primary-foreground)}.bsk-obs .text-secondary-foreground{color:var(--secondary-foreground)}.bsk-obs .underline-offset-4{text-underline-offset:4px}.bsk-obs .shadow-sm{--tw-shadow:0 1px 3px 0 var(--tw-shadow-color,#0000001a), 0 1px 2px -1px var(--tw-shadow-color,#0000001a);box-shadow:var(--tw-inset-shadow), var(--tw-inset-ring-shadow), var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow)}.bsk-obs .shadow-xs{--tw-shadow:0 1px 2px 0 var(--tw-shadow-color,#0000000d);box-shadow:var(--tw-inset-shadow), var(--tw-inset-ring-shadow), var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow)}.bsk-obs .ring{--tw-ring-shadow:var(--tw-ring-inset,) 0 0 0 calc(1px + var(--tw-ring-offset-width)) var(--tw-ring-color,currentcolor);box-shadow:var(--tw-inset-shadow), var(--tw-inset-ring-shadow), var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow)}.bsk-obs .ring-2{--tw-ring-shadow:var(--tw-ring-inset,) 0 0 0 calc(2px + var(--tw-ring-offset-width)) var(--tw-ring-color,currentcolor);box-shadow:var(--tw-inset-shadow), var(--tw-inset-ring-shadow), var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow)}.bsk-obs .ring-background{--tw-ring-color:var(--background)}.bsk-obs .outline{outline-style:var(--tw-outline-style);outline-width:1px}.bsk-obs .blur{--tw-blur:blur(8px);filter:var(--tw-blur,) var(--tw-brightness,) var(--tw-contrast,) var(--tw-grayscale,) var(--tw-hue-rotate,) var(--tw-invert,) var(--tw-saturate,) var(--tw-sepia,) var(--tw-drop-shadow,)}.bsk-obs .filter{filter:var(--tw-blur,) var(--tw-brightness,) var(--tw-contrast,) var(--tw-grayscale,) var(--tw-hue-rotate,) var(--tw-invert,) var(--tw-saturate,) var(--tw-sepia,) var(--tw-drop-shadow,)}.bsk-obs .transition-\\[background\\,border-color\\,box-shadow\\]{transition-property:background,border-color,box-shadow;transition-timing-function:var(--tw-ease,var(--default-transition-timing-function));transition-duration:var(--tw-duration,var(--default-transition-duration))}.bsk-obs .transition-all{transition-property:all;transition-timing-function:var(--tw-ease,var(--default-transition-timing-function));transition-duration:var(--tw-duration,var(--default-transition-duration))}.bsk-obs .transition-colors{transition-property:color,background-color,border-color,outline-color,text-decoration-color,fill,stroke,--tw-gradient-from,--tw-gradient-via,--tw-gradient-to;transition-timing-function:var(--tw-ease,var(--default-transition-timing-function));transition-duration:var(--tw-duration,var(--default-transition-duration))}.bsk-obs .outline-none{--tw-outline-style:none;outline-style:none}.bsk-obs .select-none{-webkit-user-select:none;user-select:none}.bsk-obs .\\[visual\\:screenshot\\]{visual:screenshot}.bsk-obs .peer-disabled\\:cursor-not-allowed:is(:where(.peer):disabled~*){cursor:not-allowed}.bsk-obs .peer-disabled\\:opacity-50:is(:where(.peer):disabled~*){opacity:.5}.bsk-obs .placeholder\\:text-muted-foreground::placeholder{color:var(--muted-foreground)}@media (hover:hover){.bsk-obs .hover\\:bg-accent:hover{background-color:var(--accent)}.bsk-obs .hover\\:bg-primary\\/90:hover{background-color:var(--primary)}@supports (color:color-mix(in lab, red, red)){.bsk-obs .hover\\:bg-primary\\/90:hover{background-color:color-mix(in oklab, var(--primary) 90%, transparent)}}.bsk-obs .hover\\:bg-secondary\\/80:hover{background-color:var(--secondary)}@supports (color:color-mix(in lab, red, red)){.bsk-obs .hover\\:bg-secondary\\/80:hover{background-color:color-mix(in oklab, var(--secondary) 80%, transparent)}}.bsk-obs .hover\\:text-accent-foreground:hover{color:var(--accent-foreground)}.bsk-obs .hover\\:underline:hover{text-decoration-line:underline}}.bsk-obs .focus-visible\\:border-ring:focus-visible{border-color:var(--ring)}.bsk-obs .focus-visible\\:ring-\\[3px\\]:focus-visible{--tw-ring-shadow:var(--tw-ring-inset,) 0 0 0 calc(3px + var(--tw-ring-offset-width)) var(--tw-ring-color,currentcolor);box-shadow:var(--tw-inset-shadow), var(--tw-inset-ring-shadow), var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow)}.bsk-obs .focus-visible\\:ring-ring\\/50:focus-visible{--tw-ring-color:var(--ring);@supports (color:color-mix(in lab, red, red)){--tw-ring-color:color-mix(in oklab, var(--ring) 50%, transparent)}}.bsk-obs .disabled\\:pointer-events-none:disabled{pointer-events:none}.bsk-obs .disabled\\:cursor-not-allowed:disabled{cursor:not-allowed}.bsk-obs .disabled\\:opacity-50:disabled{opacity:.5}.bsk-obs .has-data-\\[slot\\=card-action\\]\\:grid-cols-\\[1fr_auto\\]:has([data-slot=card-action]){grid-template-columns:1fr auto}}@property --tw-rotate-x{syntax:\"*\";inherits:false}@property --tw-rotate-y{syntax:\"*\";inherits:false}@property --tw-rotate-z{syntax:\"*\";inherits:false}@property --tw-skew-x{syntax:\"*\";inherits:false}@property --tw-skew-y{syntax:\"*\";inherits:false}@property --tw-border-style{syntax:\"*\";inherits:false;initial-value:solid}@property --tw-leading{syntax:\"*\";inherits:false}@property --tw-font-weight{syntax:\"*\";inherits:false}@property --tw-tracking{syntax:\"*\";inherits:false}@property --tw-shadow{syntax:\"*\";inherits:false;initial-value:0 0 #0000}@property --tw-shadow-color{syntax:\"*\";inherits:false}@property --tw-shadow-alpha{syntax:\"<percentage>\";inherits:false;initial-value:100%}@property --tw-inset-shadow{syntax:\"*\";inherits:false;initial-value:0 0 #0000}@property --tw-inset-shadow-color{syntax:\"*\";inherits:false}@property --tw-inset-shadow-alpha{syntax:\"<percentage>\";inherits:false;initial-value:100%}@property --tw-ring-color{syntax:\"*\";inherits:false}@property --tw-ring-shadow{syntax:\"*\";inherits:false;initial-value:0 0 #0000}@property --tw-inset-ring-color{syntax:\"*\";inherits:false}@property --tw-inset-ring-shadow{syntax:\"*\";inherits:false;initial-value:0 0 #0000}@property --tw-ring-inset{syntax:\"*\";inherits:false}@property --tw-ring-offset-width{syntax:\"<length>\";inherits:false;initial-value:0}@property --tw-ring-offset-color{syntax:\"*\";inherits:false;initial-value:#fff}@property --tw-ring-offset-shadow{syntax:\"*\";inherits:false;initial-value:0 0 #0000}@property --tw-outline-style{syntax:\"*\";inherits:false;initial-value:solid}@property --tw-blur{syntax:\"*\";inherits:false}@property --tw-brightness{syntax:\"*\";inherits:false}@property --tw-contrast{syntax:\"*\";inherits:false}@property --tw-grayscale{syntax:\"*\";inherits:false}@property --tw-hue-rotate{syntax:\"*\";inherits:false}@property --tw-invert{syntax:\"*\";inherits:false}@property --tw-opacity{syntax:\"*\";inherits:false}@property --tw-saturate{syntax:\"*\";inherits:false}@property --tw-sepia{syntax:\"*\";inherits:false}@property --tw-drop-shadow{syntax:\"*\";inherits:false}@property --tw-drop-shadow-color{syntax:\"*\";inherits:false}@property --tw-drop-shadow-alpha{syntax:\"<percentage>\";inherits:false;initial-value:100%}@property --tw-drop-shadow-size{syntax:\"*\";inherits:false}";
		const tagId$3 = "@wxg-prc-cpg/browser-skill-dsh-plugin/bsk-ui.nomodule.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$3) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@wxg-prc-cpg/browser-skill-dsh-plugin";
			tag.dataset.pluginCss = tagId$3;
			tag.textContent = css$3;
			document.head.appendChild(tag);
		}
		//#endregion
		//#region \0bsk-css:/home/runner/work/BrowserSkill/BrowserSkill/packages/dsh-plugin-browserskill/src/client/BrowserInspectToolView.module.css.mjs
		const css$2 = ".lEWnQW_card{flex-direction:column;display:flex}.lEWnQW_summary{text-overflow:ellipsis;white-space:nowrap;min-width:0;color:var(--dsw-alias-label-secondary);margin-left:6px;overflow:hidden}.lEWnQW_card[data-state=error] .lEWnQW_summary{color:var(--dsw-alias-label-error)}.lEWnQW_body{flex-direction:column;gap:8px;padding:4px 0 4px 22px;display:flex}.lEWnQW_image-wrap{display:flex}.lEWnQW_inspect-button{font:inherit;color:var(--dsw-alias-label-tertiary);cursor:pointer;background:0 0;border:none;align-self:flex-start;padding:0;font-size:12px}.lEWnQW_inspect-button:hover{color:var(--dsw-alias-label-secondary)}.lEWnQW_inspect-button:focus-visible{outline:2px solid var(--dsw-alias-border-focus);outline-offset:2px;border-radius:2px}@media (prefers-reduced-motion:reduce){.lEWnQW_inspect-button{transition:none}}";
		const tagId$2 = "@wxg-prc-cpg/browser-skill-dsh-plugin/BrowserInspectToolView.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$2) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@wxg-prc-cpg/browser-skill-dsh-plugin";
			tag.dataset.pluginCss = tagId$2;
			tag.textContent = css$2;
			document.head.appendChild(tag);
		}
		var BrowserInspectToolView_module_css_default = {
			"summary": "lEWnQW_summary",
			"inspect-button": "lEWnQW_inspect-button",
			"card": "lEWnQW_card",
			"body": "lEWnQW_body",
			"image-wrap": "lEWnQW_image-wrap"
		};
		//#endregion
		//#region \0bsk-css:/home/runner/work/BrowserSkill/BrowserSkill/packages/dsh-plugin-browserskill/src/client/ScreenshotImage.module.css.mjs
		const css$1 = ".NPjwaq_frame{place-items:center;max-width:100%;display:grid}.NPjwaq_status,.NPjwaq_retry{overflow-wrap:anywhere;text-align:center;max-width:100%}.NPjwaq_status{font-size:12px}.NPjwaq_thumbnail,.NPjwaq_retry,.NPjwaq_preview button{font:inherit;color:var(--dsw-alias-label-primary);background:var(--dsw-alias-bg-base);border:1px solid var(--dsw-alias-border-l2);cursor:pointer;border-radius:8px}.NPjwaq_thumbnail{width:100%;height:100%;padding:0;display:block;overflow:hidden}.NPjwaq_thumbnail img{width:100%;height:100%;display:block}.NPjwaq_retry{padding:6px 8px}.NPjwaq_preview button{padding:6px 10px}.NPjwaq_thumbnail:focus-visible,.NPjwaq_retry:focus-visible,.NPjwaq_preview button:focus-visible{outline:2px solid var(--dsw-alias-brand-primary);outline-offset:2px}.NPjwaq_preview{box-sizing:border-box;max-width:calc(100vw - 32px);max-height:calc(100vh - 32px);color:var(--dsw-alias-label-primary);background:var(--dsw-alias-bg-base);border:1px solid var(--dsw-alias-border-l2);border-radius:12px;padding:16px}.NPjwaq_preview::backdrop{background:var(--dsw-alias-bg-mask-photo)}.NPjwaq_preview form{justify-content:flex-end;margin:0 0 12px;display:flex}.NPjwaq_preview img{width:auto;max-width:100%;height:auto;max-height:calc(100vh - 112px);display:block}";
		const tagId$1 = "@wxg-prc-cpg/browser-skill-dsh-plugin/ScreenshotImage.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId$1) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@wxg-prc-cpg/browser-skill-dsh-plugin";
			tag.dataset.pluginCss = tagId$1;
			tag.textContent = css$1;
			document.head.appendChild(tag);
		}
		var ScreenshotImage_module_css_default = {
			"status": "NPjwaq_status",
			"frame": "NPjwaq_frame",
			"retry": "NPjwaq_retry",
			"thumbnail": "NPjwaq_thumbnail",
			"preview": "NPjwaq_preview"
		};
		//#endregion
		//#region src/client/ScreenshotImage.tsx
		const MIN_EDGE = 60;
		const MAX_EDGE = 240;
		/** Plugin-owned presentation: DSH's attachment client no longer exports image components. */
		function ScreenshotImage({ attachment, load }) {
			const [image, setImage] = (0, react.useState)({ status: "loading" });
			const [attempt, setAttempt] = (0, react.useState)(0);
			const [open, setOpen] = (0, react.useState)(false);
			const label = attachment.name ?? "screenshot";
			const naturalRatio = attachment.width / attachment.height;
			const ratio = Math.min(4, Math.max(.25, naturalRatio));
			const width = Math.max(MIN_EDGE * Math.max(1, ratio), Math.min(MAX_EDGE, MAX_EDGE * ratio, attachment.width, attachment.height * ratio));
			(0, react.useEffect)(() => {
				let active = true;
				let url;
				setImage({ status: "loading" });
				setOpen(false);
				load(attachment).then((loaded) => {
					if (!active) URL.revokeObjectURL(loaded);
					else {
						url = loaded;
						setImage({
							status: "ready",
							url: loaded
						});
					}
				}, () => {
					if (active) setImage({ status: "error" });
				});
				return () => {
					active = false;
					if (url !== void 0) URL.revokeObjectURL(url);
				};
			}, [
				attachment,
				load,
				attempt
			]);
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)(react_jsx_runtime.Fragment, { children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
				className: ScreenshotImage_module_css_default.frame,
				style: {
					width,
					aspectRatio: String(ratio)
				},
				children: image.status === "loading" ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
					role: "status",
					className: ScreenshotImage_module_css_default.status,
					children: "Loading…"
				}) : image.status === "error" ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
					type: "button",
					className: ScreenshotImage_module_css_default.retry,
					"aria-label": "Load failed — retry",
					title: "Load failed — retry",
					onClick: () => setAttempt((value) => value + 1),
					children: "Retry"
				}) : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
					type: "button",
					className: ScreenshotImage_module_css_default.thumbnail,
					title: "Open the original screenshot",
					"aria-label": `Open screenshot ${label}`,
					onClick: () => setOpen(true),
					children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("img", {
						src: image.url,
						alt: label,
						width: attachment.width,
						height: attachment.height,
						style: {
							objectFit: naturalRatio === ratio ? "scale-down" : "cover",
							objectPosition: naturalRatio < .25 ? "center top" : naturalRatio > 4 ? "left center" : "center"
						},
						onError: () => setImage({ status: "error" })
					})
				})
			}), open && image.status === "ready" && /* @__PURE__ */ (0, react_jsx_runtime.jsx)(ScreenshotPreview, {
				src: image.url,
				label,
				onClose: () => setOpen(false)
			})] });
		}
		function ScreenshotPreview({ src, label, onClose }) {
			const ref = (0, react.useRef)(null);
			(0, react.useEffect)(() => {
				const dialog = ref.current;
				dialog.showModal();
				return () => dialog.close();
			}, []);
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("dialog", {
				ref,
				className: ScreenshotImage_module_css_default.preview,
				"aria-label": "Screenshot preview",
				onClose: () => {
					if (!ref.current?.open) onClose();
				},
				onClick: (event) => {
					if (event.target !== event.currentTarget) return;
					const { left, right, top, bottom } = event.currentTarget.getBoundingClientRect();
					if (event.clientX < left || event.clientX > right || event.clientY < top || event.clientY > bottom) event.currentTarget.close();
				},
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("form", {
					method: "dialog",
					children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
						type: "submit",
						autoFocus: true,
						children: "Close preview"
					})
				}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("img", {
					src,
					alt: label
				})]
			});
		}
		//#endregion
		//#region src/client/BrowserInspectToolView.tsx
		const TERMINAL_LABELS = {
			signal: (signal) => `Signal ${signal}`,
			exitCode: (exitCode) => `Exit code ${exitCode}`,
			running: "Running",
			failed: "Failed",
			done: "Done",
			copy: "Copy",
			copied: "Copied",
			noOutput: "No output",
			noExitCode: "No exit code",
			collapseAria: "Collapse output",
			collapse: "Collapse",
			expandAria: (hidden) => `Expand the remaining ${hidden} output lines`,
			expand: (hidden) => `… ${hidden} more lines`
		};
		function firstLine(text) {
			const newline = text.indexOf("\n");
			return newline === -1 ? text : text.slice(0, newline);
		}
		/** Rebuild the command line from the logged arguments (mirrors the host presenter). */
		const INSPECT_COMMANDS = {
			observe: "observe",
			snapshot: "snapshot",
			html: "get-html",
			screenshot: "screenshot",
			console: "console",
			network: "network"
		};
		function titleOf(action) {
			return action === "html" ? "HTML" : `${action.slice(0, 1).toUpperCase()}${action.slice(1)}`;
		}
		function appendNumber(parts, flag, value) {
			if (typeof value === "number" && Number.isFinite(value)) parts.push(flag, String(value));
		}
		function commandOf(argsRaw, callId) {
			try {
				const args = JSON.parse(argsRaw);
				const action = typeof args.action === "string" ? args.action : "inspect";
				const parts = [
					"bsk",
					INSPECT_COMMANDS[action] ?? action,
					"--session"
				];
				parts.push(typeof args.session === "string" && args.session !== "" ? args.session : "(current)");
				if (action === "observe" && typeof args.cursor === "string") parts.push("--cursor", args.cursor);
				if (action === "observe" || action === "snapshot") {
					appendNumber(parts, "--max-depth", args.maxDepth);
					appendNumber(parts, "--max-tokens", args.maxTokens);
				}
				if (action === "html" || action === "console" || action === "network") appendNumber(parts, "--tab-id", args.tabId);
				if ((action === "html" || action === "screenshot") && typeof args.ref === "string") {
					if (args.ref !== "") parts.push("--ref", args.ref);
				}
				if (action === "html") appendNumber(parts, "--max-bytes", args.maxBytes);
				if (action === "console" || action === "network") {
					appendNumber(parts, "--since", args.since);
					appendNumber(parts, "--limit", args.limit);
					appendNumber(parts, "--max-text-chars", args.maxTextChars);
				}
				if (action === "console" && args.includeStack === true) parts.push("--include-stack");
				return {
					command: parts.join(" "),
					title: titleOf(action)
				};
			} catch {
				return {
					command: argsRaw === "" ? `browser_inspect (${callId})` : `browser_inspect ${firstLine(argsRaw)}`,
					title: "Inspect"
				};
			}
		}
		/** Derive the display model from the frozen block only. */
		function viewModelOf(block) {
			const settled = "kind" in block;
			const { command, title } = commandOf((settled ? block.call?.argsRaw : "argsRaw" in block ? block.argsRaw : "") ?? "", block.callId);
			if (!settled) return {
				state: "running",
				command,
				output: null,
				image: null,
				summary: command,
				title
			};
			const textBlock = block.content.find((item) => item.type === "text");
			const imageBlock = block.content.find((item) => item.type === "image");
			const output = textBlock !== void 0 && textBlock.type === "text" ? textBlock.text : null;
			const image = imageBlock !== void 0 && imageBlock.type === "image" ? imageBlock.attachment : null;
			return {
				state: block.isError ? "error" : "ok",
				command,
				output,
				image,
				summary: output !== null ? firstLine(output) : command,
				title
			};
		}
		function dotState(state) {
			switch (state) {
				case "running": return "ongoing";
				case "error": return "error";
				default: return "done";
			}
		}
		/**
		* Render one browser_inspect call: a disclosure row over a terminal block,
		* plus the screenshot image when that action returns an image attachment.
		*/
		function BrowserInspectToolView({ block, cwd, inspect, loadImage }) {
			const model = viewModelOf(block);
			const [expanded, setExpanded] = (0, react.useState)(false);
			const expandable = model.state === "running" || model.output !== null || model.image !== null;
			const open = expanded && expandable;
			return /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
				className: BrowserInspectToolView_module_css_default.card,
				"data-tool": "browser_inspect",
				"data-state": model.state,
				children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.DisclosureRow, {
					icon: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.StateDot, { state: dotState(model.state) }),
					title: model.title,
					open,
					expandable,
					onToggle: () => setExpanded((value) => !value),
					expandOnRowClick: true,
					previewChevron: true,
					collapsedContent: /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
						className: BrowserInspectToolView_module_css_default.summary,
						children: model.summary
					}),
					children: /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: BrowserInspectToolView_module_css_default.body,
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)(_deepseek_ai_dsh_client_ui_primitives.TerminalBlock, {
								command: model.command,
								cwd: cwd ?? void 0,
								output: model.output ?? void 0,
								running: model.state === "running",
								labels: TERMINAL_LABELS
							}),
							model.image !== null ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
								className: BrowserInspectToolView_module_css_default["image-wrap"],
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(ScreenshotImage, {
									attachment: model.image,
									load: loadImage
								}, model.image.attachmentId)
							}) : null,
							inspect !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: BrowserInspectToolView_module_css_default["inspect-button"],
								onClick: inspect,
								children: "Inspect"
							}) : null
						]
					})
				})
			});
		}
		//#endregion
		//#region ../../node_modules/.pnpm/clsx@2.1.1/node_modules/clsx/dist/clsx.mjs
		function r(e) {
			var t, f, n = "";
			if ("string" == typeof e || "number" == typeof e) n += e;
			else if ("object" == typeof e) if (Array.isArray(e)) {
				var o = e.length;
				for (t = 0; t < o; t++) e[t] && (f = r(e[t])) && (n && (n += " "), n += f);
			} else for (f in e) e[f] && (n && (n += " "), n += f);
			return n;
		}
		function clsx() {
			for (var e, t, f = 0, n = "", o = arguments.length; f < o; f++) (e = arguments[f]) && (t = r(e)) && (n && (n += " "), n += t);
			return n;
		}
		//#endregion
		//#region ../../node_modules/.pnpm/tailwind-merge@3.6.0/node_modules/tailwind-merge/dist/bundle-mjs.mjs
		/**
		* Concatenates two arrays faster than the array spread operator.
		*/
		const concatArrays = (array1, array2) => {
			const combinedArray = new Array(array1.length + array2.length);
			for (let i = 0; i < array1.length; i++) combinedArray[i] = array1[i];
			for (let i = 0; i < array2.length; i++) combinedArray[array1.length + i] = array2[i];
			return combinedArray;
		};
		const createClassValidatorObject = (classGroupId, validator) => ({
			classGroupId,
			validator
		});
		const createClassPartObject = (nextPart = /* @__PURE__ */ new Map(), validators = null, classGroupId) => ({
			nextPart,
			validators,
			classGroupId
		});
		const CLASS_PART_SEPARATOR = "-";
		const EMPTY_CONFLICTS = [];
		const ARBITRARY_PROPERTY_PREFIX = "arbitrary..";
		const createClassGroupUtils = (config) => {
			const classMap = createClassMap(config);
			const { conflictingClassGroups, conflictingClassGroupModifiers } = config;
			const getClassGroupId = (className) => {
				if (className.startsWith("[") && className.endsWith("]")) return getGroupIdForArbitraryProperty(className);
				const classParts = className.split(CLASS_PART_SEPARATOR);
				const startIndex = classParts[0] === "" && classParts.length > 1 ? 1 : 0;
				return getGroupRecursive(classParts, startIndex, classMap);
			};
			const getConflictingClassGroupIds = (classGroupId, hasPostfixModifier) => {
				if (hasPostfixModifier) {
					const modifierConflicts = conflictingClassGroupModifiers[classGroupId];
					const baseConflicts = conflictingClassGroups[classGroupId];
					if (modifierConflicts) {
						if (baseConflicts) return concatArrays(baseConflicts, modifierConflicts);
						return modifierConflicts;
					}
					return baseConflicts || EMPTY_CONFLICTS;
				}
				return conflictingClassGroups[classGroupId] || EMPTY_CONFLICTS;
			};
			return {
				getClassGroupId,
				getConflictingClassGroupIds
			};
		};
		const getGroupRecursive = (classParts, startIndex, classPartObject) => {
			if (classParts.length - startIndex === 0) return classPartObject.classGroupId;
			const currentClassPart = classParts[startIndex];
			const nextClassPartObject = classPartObject.nextPart.get(currentClassPart);
			if (nextClassPartObject) {
				const result = getGroupRecursive(classParts, startIndex + 1, nextClassPartObject);
				if (result) return result;
			}
			const validators = classPartObject.validators;
			if (validators === null) return;
			const classRest = startIndex === 0 ? classParts.join(CLASS_PART_SEPARATOR) : classParts.slice(startIndex).join(CLASS_PART_SEPARATOR);
			const validatorsLength = validators.length;
			for (let i = 0; i < validatorsLength; i++) {
				const validatorObj = validators[i];
				if (validatorObj.validator(classRest)) return validatorObj.classGroupId;
			}
		};
		/**
		* Get the class group ID for an arbitrary property.
		*
		* @param className - The class name to get the group ID for. Is expected to be string starting with `[` and ending with `]`.
		*/
		const getGroupIdForArbitraryProperty = (className) => className.slice(1, -1).indexOf(":") === -1 ? void 0 : (() => {
			const content = className.slice(1, -1);
			const colonIndex = content.indexOf(":");
			const property = content.slice(0, colonIndex);
			return property ? ARBITRARY_PROPERTY_PREFIX + property : void 0;
		})();
		/**
		* Exported for testing only
		*/
		const createClassMap = (config) => {
			const { theme, classGroups } = config;
			return processClassGroups(classGroups, theme);
		};
		const processClassGroups = (classGroups, theme) => {
			const classMap = createClassPartObject();
			for (const classGroupId in classGroups) {
				const group = classGroups[classGroupId];
				processClassesRecursively(group, classMap, classGroupId, theme);
			}
			return classMap;
		};
		const processClassesRecursively = (classGroup, classPartObject, classGroupId, theme) => {
			const len = classGroup.length;
			for (let i = 0; i < len; i++) {
				const classDefinition = classGroup[i];
				processClassDefinition(classDefinition, classPartObject, classGroupId, theme);
			}
		};
		const processClassDefinition = (classDefinition, classPartObject, classGroupId, theme) => {
			if (typeof classDefinition === "string") {
				processStringDefinition(classDefinition, classPartObject, classGroupId);
				return;
			}
			if (typeof classDefinition === "function") {
				processFunctionDefinition(classDefinition, classPartObject, classGroupId, theme);
				return;
			}
			processObjectDefinition(classDefinition, classPartObject, classGroupId, theme);
		};
		const processStringDefinition = (classDefinition, classPartObject, classGroupId) => {
			const classPartObjectToEdit = classDefinition === "" ? classPartObject : getPart(classPartObject, classDefinition);
			classPartObjectToEdit.classGroupId = classGroupId;
		};
		const processFunctionDefinition = (classDefinition, classPartObject, classGroupId, theme) => {
			if (isThemeGetter(classDefinition)) {
				processClassesRecursively(classDefinition(theme), classPartObject, classGroupId, theme);
				return;
			}
			if (classPartObject.validators === null) classPartObject.validators = [];
			classPartObject.validators.push(createClassValidatorObject(classGroupId, classDefinition));
		};
		const processObjectDefinition = (classDefinition, classPartObject, classGroupId, theme) => {
			const entries = Object.entries(classDefinition);
			const len = entries.length;
			for (let i = 0; i < len; i++) {
				const [key, value] = entries[i];
				processClassesRecursively(value, getPart(classPartObject, key), classGroupId, theme);
			}
		};
		const getPart = (classPartObject, path) => {
			let current = classPartObject;
			const parts = path.split(CLASS_PART_SEPARATOR);
			const len = parts.length;
			for (let i = 0; i < len; i++) {
				const part = parts[i];
				let next = current.nextPart.get(part);
				if (!next) {
					next = createClassPartObject();
					current.nextPart.set(part, next);
				}
				current = next;
			}
			return current;
		};
		const isThemeGetter = (func) => "isThemeGetter" in func && func.isThemeGetter === true;
		const createLruCache = (maxCacheSize) => {
			if (maxCacheSize < 1) return {
				get: () => void 0,
				set: () => {}
			};
			let cacheSize = 0;
			let cache = Object.create(null);
			let previousCache = Object.create(null);
			const update = (key, value) => {
				cache[key] = value;
				cacheSize++;
				if (cacheSize > maxCacheSize) {
					cacheSize = 0;
					previousCache = cache;
					cache = Object.create(null);
				}
			};
			return {
				get(key) {
					let value = cache[key];
					if (value !== void 0) return value;
					if ((value = previousCache[key]) !== void 0) {
						update(key, value);
						return value;
					}
				},
				set(key, value) {
					if (key in cache) cache[key] = value;
					else update(key, value);
				}
			};
		};
		const IMPORTANT_MODIFIER = "!";
		const MODIFIER_SEPARATOR = ":";
		const EMPTY_MODIFIERS = [];
		const createResultObject = (modifiers, hasImportantModifier, baseClassName, maybePostfixModifierPosition, isExternal) => ({
			modifiers,
			hasImportantModifier,
			baseClassName,
			maybePostfixModifierPosition,
			isExternal
		});
		const createParseClassName = (config) => {
			const { prefix, experimentalParseClassName } = config;
			/**
			* Parse class name into parts.
			*
			* Inspired by `splitAtTopLevelOnly` used in Tailwind CSS
			* @see https://github.com/tailwindlabs/tailwindcss/blob/v3.2.2/src/util/splitAtTopLevelOnly.js
			*/
			let parseClassName = (className) => {
				const modifiers = [];
				let bracketDepth = 0;
				let parenDepth = 0;
				let modifierStart = 0;
				let postfixModifierPosition;
				const len = className.length;
				for (let index = 0; index < len; index++) {
					const currentCharacter = className[index];
					if (bracketDepth === 0 && parenDepth === 0) {
						if (currentCharacter === MODIFIER_SEPARATOR) {
							modifiers.push(className.slice(modifierStart, index));
							modifierStart = index + 1;
							continue;
						}
						if (currentCharacter === "/") {
							postfixModifierPosition = index;
							continue;
						}
					}
					if (currentCharacter === "[") bracketDepth++;
					else if (currentCharacter === "]") bracketDepth--;
					else if (currentCharacter === "(") parenDepth++;
					else if (currentCharacter === ")") parenDepth--;
				}
				const baseClassNameWithImportantModifier = modifiers.length === 0 ? className : className.slice(modifierStart);
				let baseClassName = baseClassNameWithImportantModifier;
				let hasImportantModifier = false;
				if (baseClassNameWithImportantModifier.endsWith(IMPORTANT_MODIFIER)) {
					baseClassName = baseClassNameWithImportantModifier.slice(0, -1);
					hasImportantModifier = true;
				} else if (baseClassNameWithImportantModifier.startsWith(IMPORTANT_MODIFIER)) {
					baseClassName = baseClassNameWithImportantModifier.slice(1);
					hasImportantModifier = true;
				}
				const maybePostfixModifierPosition = postfixModifierPosition && postfixModifierPosition > modifierStart ? postfixModifierPosition - modifierStart : void 0;
				return createResultObject(modifiers, hasImportantModifier, baseClassName, maybePostfixModifierPosition);
			};
			if (prefix) {
				const fullPrefix = prefix + MODIFIER_SEPARATOR;
				const parseClassNameOriginal = parseClassName;
				parseClassName = (className) => className.startsWith(fullPrefix) ? parseClassNameOriginal(className.slice(fullPrefix.length)) : createResultObject(EMPTY_MODIFIERS, false, className, void 0, true);
			}
			if (experimentalParseClassName) {
				const parseClassNameOriginal = parseClassName;
				parseClassName = (className) => experimentalParseClassName({
					className,
					parseClassName: parseClassNameOriginal
				});
			}
			return parseClassName;
		};
		/**
		* Sorts modifiers according to following schema:
		* - Predefined modifiers are sorted alphabetically
		* - When an arbitrary variant appears, it must be preserved which modifiers are before and after it
		*/
		const createSortModifiers = (config) => {
			const modifierWeights = /* @__PURE__ */ new Map();
			config.orderSensitiveModifiers.forEach((mod, index) => {
				modifierWeights.set(mod, 1e6 + index);
			});
			return (modifiers) => {
				const result = [];
				let currentSegment = [];
				for (let i = 0; i < modifiers.length; i++) {
					const modifier = modifiers[i];
					const isArbitrary = modifier[0] === "[";
					const isOrderSensitive = modifierWeights.has(modifier);
					if (isArbitrary || isOrderSensitive) {
						if (currentSegment.length > 0) {
							currentSegment.sort();
							result.push(...currentSegment);
							currentSegment = [];
						}
						result.push(modifier);
					} else currentSegment.push(modifier);
				}
				if (currentSegment.length > 0) {
					currentSegment.sort();
					result.push(...currentSegment);
				}
				return result;
			};
		};
		const createConfigUtils = (config) => ({
			cache: createLruCache(config.cacheSize),
			parseClassName: createParseClassName(config),
			sortModifiers: createSortModifiers(config),
			postfixLookupClassGroupIds: createPostfixLookupClassGroupIds(config),
			...createClassGroupUtils(config)
		});
		const createPostfixLookupClassGroupIds = (config) => {
			const lookup = Object.create(null);
			const classGroupIds = config.postfixLookupClassGroups;
			if (classGroupIds) for (let i = 0; i < classGroupIds.length; i++) lookup[classGroupIds[i]] = true;
			return lookup;
		};
		const SPLIT_CLASSES_REGEX = /\s+/;
		const mergeClassList = (classList, configUtils) => {
			const { parseClassName, getClassGroupId, getConflictingClassGroupIds, sortModifiers, postfixLookupClassGroupIds } = configUtils;
			/**
			* Set of classGroupIds in following format:
			* `{importantModifier}{variantModifiers}{classGroupId}`
			* @example 'float'
			* @example 'hover:focus:bg-color'
			* @example 'md:!pr'
			*/
			const classGroupsInConflict = [];
			const classNames = classList.trim().split(SPLIT_CLASSES_REGEX);
			let result = "";
			for (let index = classNames.length - 1; index >= 0; index -= 1) {
				const originalClassName = classNames[index];
				const { isExternal, modifiers, hasImportantModifier, baseClassName, maybePostfixModifierPosition } = parseClassName(originalClassName);
				if (isExternal) {
					result = originalClassName + (result.length > 0 ? " " + result : result);
					continue;
				}
				let hasPostfixModifier = !!maybePostfixModifierPosition;
				let classGroupId;
				if (hasPostfixModifier) {
					classGroupId = getClassGroupId(baseClassName.substring(0, maybePostfixModifierPosition));
					const classGroupIdWithPostfix = classGroupId && postfixLookupClassGroupIds[classGroupId] ? getClassGroupId(baseClassName) : void 0;
					if (classGroupIdWithPostfix && classGroupIdWithPostfix !== classGroupId) {
						classGroupId = classGroupIdWithPostfix;
						hasPostfixModifier = false;
					}
				} else classGroupId = getClassGroupId(baseClassName);
				if (!classGroupId) {
					if (!hasPostfixModifier) {
						result = originalClassName + (result.length > 0 ? " " + result : result);
						continue;
					}
					classGroupId = getClassGroupId(baseClassName);
					if (!classGroupId) {
						result = originalClassName + (result.length > 0 ? " " + result : result);
						continue;
					}
					hasPostfixModifier = false;
				}
				const variantModifier = modifiers.length === 0 ? "" : modifiers.length === 1 ? modifiers[0] : sortModifiers(modifiers).join(":");
				const modifierId = hasImportantModifier ? variantModifier + IMPORTANT_MODIFIER : variantModifier;
				const classId = modifierId + classGroupId;
				if (classGroupsInConflict.indexOf(classId) > -1) continue;
				classGroupsInConflict.push(classId);
				const conflictGroups = getConflictingClassGroupIds(classGroupId, hasPostfixModifier);
				for (let i = 0; i < conflictGroups.length; ++i) {
					const group = conflictGroups[i];
					classGroupsInConflict.push(modifierId + group);
				}
				result = originalClassName + (result.length > 0 ? " " + result : result);
			}
			return result;
		};
		/**
		* The code in this file is copied from https://github.com/lukeed/clsx and modified to suit the needs of tailwind-merge better.
		*
		* Specifically:
		* - Runtime code from https://github.com/lukeed/clsx/blob/v1.2.1/src/index.js
		* - TypeScript types from https://github.com/lukeed/clsx/blob/v1.2.1/clsx.d.ts
		*
		* Original code has MIT license: Copyright (c) Luke Edwards <luke.edwards05@gmail.com> (lukeed.com)
		*/
		const twJoin = (...classLists) => {
			let index = 0;
			let argument;
			let resolvedValue;
			let string = "";
			while (index < classLists.length) if (argument = classLists[index++]) {
				if (resolvedValue = toValue(argument)) {
					string && (string += " ");
					string += resolvedValue;
				}
			}
			return string;
		};
		const toValue = (mix) => {
			if (typeof mix === "string") return mix;
			let resolvedValue;
			let string = "";
			for (let k = 0; k < mix.length; k++) if (mix[k]) {
				if (resolvedValue = toValue(mix[k])) {
					string && (string += " ");
					string += resolvedValue;
				}
			}
			return string;
		};
		const createTailwindMerge = (createConfigFirst, ...createConfigRest) => {
			let configUtils;
			let cacheGet;
			let cacheSet;
			let functionToCall;
			const initTailwindMerge = (classList) => {
				const config = createConfigRest.reduce((previousConfig, createConfigCurrent) => createConfigCurrent(previousConfig), createConfigFirst());
				configUtils = createConfigUtils(config);
				cacheGet = configUtils.cache.get;
				cacheSet = configUtils.cache.set;
				functionToCall = tailwindMerge;
				return tailwindMerge(classList);
			};
			const tailwindMerge = (classList) => {
				const cachedResult = cacheGet(classList);
				if (cachedResult) return cachedResult;
				const result = mergeClassList(classList, configUtils);
				cacheSet(classList, result);
				return result;
			};
			functionToCall = initTailwindMerge;
			return (...args) => functionToCall(twJoin(...args));
		};
		const fallbackThemeArr = [];
		const fromTheme = (key) => {
			const themeGetter = (theme) => theme[key] || fallbackThemeArr;
			themeGetter.isThemeGetter = true;
			return themeGetter;
		};
		const arbitraryValueRegex = /^\[(?:(\w[\w-]*):)?(.+)\]$/i;
		const arbitraryVariableRegex = /^\((?:(\w[\w-]*):)?(.+)\)$/i;
		const fractionRegex = /^\d+(?:\.\d+)?\/\d+(?:\.\d+)?$/;
		const tshirtUnitRegex = /^(\d+(\.\d+)?)?(xs|sm|md|lg|xl)$/;
		const lengthUnitRegex = /\d+(%|px|r?em|[sdl]?v([hwib]|min|max)|pt|pc|in|cm|mm|cap|ch|ex|r?lh|cq(w|h|i|b|min|max))|\b(calc|min|max|clamp)\(.+\)|^0$/;
		const colorFunctionRegex = /^(rgba?|hsla?|hwb|(ok)?(lab|lch)|color-mix)\(.+\)$/;
		const shadowRegex = /^(inset_)?-?((\d+)?\.?(\d+)[a-z]+|0)_-?((\d+)?\.?(\d+)[a-z]+|0)/;
		const imageRegex = /^(url|image|image-set|cross-fade|element|(repeating-)?(linear|radial|conic)-gradient)\(.+\)$/;
		const isFraction = (value) => fractionRegex.test(value);
		const isNumber = (value) => !!value && !Number.isNaN(Number(value));
		const isInteger = (value) => !!value && Number.isInteger(Number(value));
		const isPercent = (value) => value.endsWith("%") && isNumber(value.slice(0, -1));
		const isTshirtSize = (value) => tshirtUnitRegex.test(value);
		const isAny = () => true;
		const isLengthOnly = (value) => lengthUnitRegex.test(value) && !colorFunctionRegex.test(value);
		const isNever = () => false;
		const isShadow = (value) => shadowRegex.test(value);
		const isImage = (value) => imageRegex.test(value);
		const isAnyNonArbitrary = (value) => !isArbitraryValue(value) && !isArbitraryVariable(value);
		const isNamedContainerQuery = (value) => value.startsWith("@container") && (value[10] === "/" && value[11] !== void 0 || value[11] === "s" && value[16] !== void 0 && value.startsWith("-size/", 10) || value[11] === "n" && value[18] !== void 0 && value.startsWith("-normal/", 10));
		const isArbitrarySize = (value) => getIsArbitraryValue(value, isLabelSize, isNever);
		const isArbitraryValue = (value) => arbitraryValueRegex.test(value);
		const isArbitraryLength = (value) => getIsArbitraryValue(value, isLabelLength, isLengthOnly);
		const isArbitraryNumber = (value) => getIsArbitraryValue(value, isLabelNumber, isNumber);
		const isArbitraryWeight = (value) => getIsArbitraryValue(value, isLabelWeight, isAny);
		const isArbitraryFamilyName = (value) => getIsArbitraryValue(value, isLabelFamilyName, isNever);
		const isArbitraryPosition = (value) => getIsArbitraryValue(value, isLabelPosition, isNever);
		const isArbitraryImage = (value) => getIsArbitraryValue(value, isLabelImage, isImage);
		const isArbitraryShadow = (value) => getIsArbitraryValue(value, isLabelShadow, isShadow);
		const isArbitraryVariable = (value) => arbitraryVariableRegex.test(value);
		const isArbitraryVariableLength = (value) => getIsArbitraryVariable(value, isLabelLength);
		const isArbitraryVariableFamilyName = (value) => getIsArbitraryVariable(value, isLabelFamilyName);
		const isArbitraryVariablePosition = (value) => getIsArbitraryVariable(value, isLabelPosition);
		const isArbitraryVariableSize = (value) => getIsArbitraryVariable(value, isLabelSize);
		const isArbitraryVariableImage = (value) => getIsArbitraryVariable(value, isLabelImage);
		const isArbitraryVariableShadow = (value) => getIsArbitraryVariable(value, isLabelShadow, true);
		const isArbitraryVariableWeight = (value) => getIsArbitraryVariable(value, isLabelWeight, true);
		const getIsArbitraryValue = (value, testLabel, testValue) => {
			const result = arbitraryValueRegex.exec(value);
			if (result) {
				if (result[1]) return testLabel(result[1]);
				return testValue(result[2]);
			}
			return false;
		};
		const getIsArbitraryVariable = (value, testLabel, shouldMatchNoLabel = false) => {
			const result = arbitraryVariableRegex.exec(value);
			if (result) {
				if (result[1]) return testLabel(result[1]);
				return shouldMatchNoLabel;
			}
			return false;
		};
		const isLabelPosition = (label) => label === "position" || label === "percentage";
		const isLabelImage = (label) => label === "image" || label === "url";
		const isLabelSize = (label) => label === "length" || label === "size" || label === "bg-size";
		const isLabelLength = (label) => label === "length";
		const isLabelNumber = (label) => label === "number";
		const isLabelFamilyName = (label) => label === "family-name";
		const isLabelWeight = (label) => label === "number" || label === "weight";
		const isLabelShadow = (label) => label === "shadow";
		const getDefaultConfig = () => {
			/**
			* Theme getters for theme variable namespaces
			* @see https://tailwindcss.com/docs/theme#theme-variable-namespaces
			*/
			const themeColor = fromTheme("color");
			const themeFont = fromTheme("font");
			const themeText = fromTheme("text");
			const themeFontWeight = fromTheme("font-weight");
			const themeTracking = fromTheme("tracking");
			const themeLeading = fromTheme("leading");
			const themeBreakpoint = fromTheme("breakpoint");
			const themeContainer = fromTheme("container");
			const themeSpacing = fromTheme("spacing");
			const themeRadius = fromTheme("radius");
			const themeShadow = fromTheme("shadow");
			const themeInsetShadow = fromTheme("inset-shadow");
			const themeTextShadow = fromTheme("text-shadow");
			const themeDropShadow = fromTheme("drop-shadow");
			const themeBlur = fromTheme("blur");
			const themePerspective = fromTheme("perspective");
			const themeAspect = fromTheme("aspect");
			const themeEase = fromTheme("ease");
			const themeAnimate = fromTheme("animate");
			/**
			* Helpers to avoid repeating the same scales
			*
			* We use functions that create a new array every time they're called instead of static arrays.
			* This ensures that users who modify any scale by mutating the array (e.g. with `array.push(element)`) don't accidentally mutate arrays in other parts of the config.
			*/
			const scaleBreak = () => [
				"auto",
				"avoid",
				"all",
				"avoid-page",
				"page",
				"left",
				"right",
				"column"
			];
			const scalePosition = () => [
				"center",
				"top",
				"bottom",
				"left",
				"right",
				"top-left",
				"left-top",
				"top-right",
				"right-top",
				"bottom-right",
				"right-bottom",
				"bottom-left",
				"left-bottom"
			];
			const scalePositionWithArbitrary = () => [
				...scalePosition(),
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleOverflow = () => [
				"auto",
				"hidden",
				"clip",
				"visible",
				"scroll"
			];
			const scaleOverscroll = () => [
				"auto",
				"contain",
				"none"
			];
			const scaleUnambiguousSpacing = () => [
				isArbitraryVariable,
				isArbitraryValue,
				themeSpacing
			];
			const scaleInset = () => [
				isFraction,
				"full",
				"auto",
				...scaleUnambiguousSpacing()
			];
			const scaleGridTemplateColsRows = () => [
				isInteger,
				"none",
				"subgrid",
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleGridColRowStartAndEnd = () => [
				"auto",
				{ span: [
					"full",
					isInteger,
					isArbitraryVariable,
					isArbitraryValue
				] },
				isInteger,
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleGridColRowStartOrEnd = () => [
				isInteger,
				"auto",
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleGridAutoColsRows = () => [
				"auto",
				"min",
				"max",
				"fr",
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleAlignPrimaryAxis = () => [
				"start",
				"end",
				"center",
				"between",
				"around",
				"evenly",
				"stretch",
				"baseline",
				"center-safe",
				"end-safe"
			];
			const scaleAlignSecondaryAxis = () => [
				"start",
				"end",
				"center",
				"stretch",
				"center-safe",
				"end-safe"
			];
			const scaleMargin = () => ["auto", ...scaleUnambiguousSpacing()];
			const scaleSizing = () => [
				isFraction,
				"auto",
				"full",
				"dvw",
				"dvh",
				"lvw",
				"lvh",
				"svw",
				"svh",
				"min",
				"max",
				"fit",
				...scaleUnambiguousSpacing()
			];
			const scaleSizingInline = () => [
				isFraction,
				"screen",
				"full",
				"dvw",
				"lvw",
				"svw",
				"min",
				"max",
				"fit",
				...scaleUnambiguousSpacing()
			];
			const scaleSizingBlock = () => [
				isFraction,
				"screen",
				"full",
				"lh",
				"dvh",
				"lvh",
				"svh",
				"min",
				"max",
				"fit",
				...scaleUnambiguousSpacing()
			];
			const scaleColor = () => [
				themeColor,
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleBgPosition = () => [
				...scalePosition(),
				isArbitraryVariablePosition,
				isArbitraryPosition,
				{ position: [isArbitraryVariable, isArbitraryValue] }
			];
			const scaleBgRepeat = () => ["no-repeat", { repeat: [
				"",
				"x",
				"y",
				"space",
				"round"
			] }];
			const scaleBgSize = () => [
				"auto",
				"cover",
				"contain",
				isArbitraryVariableSize,
				isArbitrarySize,
				{ size: [isArbitraryVariable, isArbitraryValue] }
			];
			const scaleGradientStopPosition = () => [
				isPercent,
				isArbitraryVariableLength,
				isArbitraryLength
			];
			const scaleRadius = () => [
				"",
				"none",
				"full",
				themeRadius,
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleBorderWidth = () => [
				"",
				isNumber,
				isArbitraryVariableLength,
				isArbitraryLength
			];
			const scaleLineStyle = () => [
				"solid",
				"dashed",
				"dotted",
				"double"
			];
			const scaleBlendMode = () => [
				"normal",
				"multiply",
				"screen",
				"overlay",
				"darken",
				"lighten",
				"color-dodge",
				"color-burn",
				"hard-light",
				"soft-light",
				"difference",
				"exclusion",
				"hue",
				"saturation",
				"color",
				"luminosity"
			];
			const scaleMaskImagePosition = () => [
				isNumber,
				isPercent,
				isArbitraryVariablePosition,
				isArbitraryPosition
			];
			const scaleBlur = () => [
				"",
				"none",
				themeBlur,
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleRotate = () => [
				"none",
				isNumber,
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleScale = () => [
				"none",
				isNumber,
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleSkew = () => [
				isNumber,
				isArbitraryVariable,
				isArbitraryValue
			];
			const scaleTranslate = () => [
				isFraction,
				"full",
				...scaleUnambiguousSpacing()
			];
			return {
				cacheSize: 500,
				theme: {
					animate: [
						"spin",
						"ping",
						"pulse",
						"bounce"
					],
					aspect: ["video"],
					blur: [isTshirtSize],
					breakpoint: [isTshirtSize],
					color: [isAny],
					container: [isTshirtSize],
					"drop-shadow": [isTshirtSize],
					ease: [
						"in",
						"out",
						"in-out"
					],
					font: [isAnyNonArbitrary],
					"font-weight": [
						"thin",
						"extralight",
						"light",
						"normal",
						"medium",
						"semibold",
						"bold",
						"extrabold",
						"black"
					],
					"inset-shadow": [isTshirtSize],
					leading: [
						"none",
						"tight",
						"snug",
						"normal",
						"relaxed",
						"loose"
					],
					perspective: [
						"dramatic",
						"near",
						"normal",
						"midrange",
						"distant",
						"none"
					],
					radius: [isTshirtSize],
					shadow: [isTshirtSize],
					spacing: ["px", isNumber],
					text: [isTshirtSize],
					"text-shadow": [isTshirtSize],
					tracking: [
						"tighter",
						"tight",
						"normal",
						"wide",
						"wider",
						"widest"
					]
				},
				classGroups: {
					/**
					* Aspect Ratio
					* @see https://tailwindcss.com/docs/aspect-ratio
					*/
					aspect: [{ aspect: [
						"auto",
						"square",
						isFraction,
						isArbitraryValue,
						isArbitraryVariable,
						themeAspect
					] }],
					/**
					* Container
					* @see https://tailwindcss.com/docs/container
					* @deprecated since Tailwind CSS v4.0.0
					*/
					container: ["container"],
					/**
					* Container Type
					* @see https://tailwindcss.com/docs/responsive-design#container-queries
					*/
					"container-type": [{ "@container": [
						"",
						"normal",
						"size",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Container Name
					* @see https://tailwindcss.com/docs/responsive-design#named-containers
					*/
					"container-named": [isNamedContainerQuery],
					/**
					* Columns
					* @see https://tailwindcss.com/docs/columns
					*/
					columns: [{ columns: [
						isNumber,
						isArbitraryValue,
						isArbitraryVariable,
						themeContainer
					] }],
					/**
					* Break After
					* @see https://tailwindcss.com/docs/break-after
					*/
					"break-after": [{ "break-after": scaleBreak() }],
					/**
					* Break Before
					* @see https://tailwindcss.com/docs/break-before
					*/
					"break-before": [{ "break-before": scaleBreak() }],
					/**
					* Break Inside
					* @see https://tailwindcss.com/docs/break-inside
					*/
					"break-inside": [{ "break-inside": [
						"auto",
						"avoid",
						"avoid-page",
						"avoid-column"
					] }],
					/**
					* Box Decoration Break
					* @see https://tailwindcss.com/docs/box-decoration-break
					*/
					"box-decoration": [{ "box-decoration": ["slice", "clone"] }],
					/**
					* Box Sizing
					* @see https://tailwindcss.com/docs/box-sizing
					*/
					box: [{ box: ["border", "content"] }],
					/**
					* Display
					* @see https://tailwindcss.com/docs/display
					*/
					display: [
						"block",
						"inline-block",
						"inline",
						"flex",
						"inline-flex",
						"table",
						"inline-table",
						"table-caption",
						"table-cell",
						"table-column",
						"table-column-group",
						"table-footer-group",
						"table-header-group",
						"table-row-group",
						"table-row",
						"flow-root",
						"grid",
						"inline-grid",
						"contents",
						"list-item",
						"hidden"
					],
					/**
					* Screen Reader Only
					* @see https://tailwindcss.com/docs/display#screen-reader-only
					*/
					sr: ["sr-only", "not-sr-only"],
					/**
					* Floats
					* @see https://tailwindcss.com/docs/float
					*/
					float: [{ float: [
						"right",
						"left",
						"none",
						"start",
						"end"
					] }],
					/**
					* Clear
					* @see https://tailwindcss.com/docs/clear
					*/
					clear: [{ clear: [
						"left",
						"right",
						"both",
						"none",
						"start",
						"end"
					] }],
					/**
					* Isolation
					* @see https://tailwindcss.com/docs/isolation
					*/
					isolation: ["isolate", "isolation-auto"],
					/**
					* Object Fit
					* @see https://tailwindcss.com/docs/object-fit
					*/
					"object-fit": [{ object: [
						"contain",
						"cover",
						"fill",
						"none",
						"scale-down"
					] }],
					/**
					* Object Position
					* @see https://tailwindcss.com/docs/object-position
					*/
					"object-position": [{ object: scalePositionWithArbitrary() }],
					/**
					* Overflow
					* @see https://tailwindcss.com/docs/overflow
					*/
					overflow: [{ overflow: scaleOverflow() }],
					/**
					* Overflow X
					* @see https://tailwindcss.com/docs/overflow
					*/
					"overflow-x": [{ "overflow-x": scaleOverflow() }],
					/**
					* Overflow Y
					* @see https://tailwindcss.com/docs/overflow
					*/
					"overflow-y": [{ "overflow-y": scaleOverflow() }],
					/**
					* Overscroll Behavior
					* @see https://tailwindcss.com/docs/overscroll-behavior
					*/
					overscroll: [{ overscroll: scaleOverscroll() }],
					/**
					* Overscroll Behavior X
					* @see https://tailwindcss.com/docs/overscroll-behavior
					*/
					"overscroll-x": [{ "overscroll-x": scaleOverscroll() }],
					/**
					* Overscroll Behavior Y
					* @see https://tailwindcss.com/docs/overscroll-behavior
					*/
					"overscroll-y": [{ "overscroll-y": scaleOverscroll() }],
					/**
					* Position
					* @see https://tailwindcss.com/docs/position
					*/
					position: [
						"static",
						"fixed",
						"absolute",
						"relative",
						"sticky"
					],
					/**
					* Inset
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					inset: [{ inset: scaleInset() }],
					/**
					* Inset Inline
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					"inset-x": [{ "inset-x": scaleInset() }],
					/**
					* Inset Block
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					"inset-y": [{ "inset-y": scaleInset() }],
					/**
					* Inset Inline Start
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					* @todo class group will be renamed to `inset-s` in next major release
					*/
					start: [{
						"inset-s": scaleInset(),
						/**
						* @deprecated since Tailwind CSS v4.2.0 in favor of `inset-s-*` utilities.
						* @see https://github.com/tailwindlabs/tailwindcss/pull/19613
						*/
						start: scaleInset()
					}],
					/**
					* Inset Inline End
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					* @todo class group will be renamed to `inset-e` in next major release
					*/
					end: [{
						"inset-e": scaleInset(),
						/**
						* @deprecated since Tailwind CSS v4.2.0 in favor of `inset-e-*` utilities.
						* @see https://github.com/tailwindlabs/tailwindcss/pull/19613
						*/
						end: scaleInset()
					}],
					/**
					* Inset Block Start
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					"inset-bs": [{ "inset-bs": scaleInset() }],
					/**
					* Inset Block End
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					"inset-be": [{ "inset-be": scaleInset() }],
					/**
					* Top
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					top: [{ top: scaleInset() }],
					/**
					* Right
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					right: [{ right: scaleInset() }],
					/**
					* Bottom
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					bottom: [{ bottom: scaleInset() }],
					/**
					* Left
					* @see https://tailwindcss.com/docs/top-right-bottom-left
					*/
					left: [{ left: scaleInset() }],
					/**
					* Visibility
					* @see https://tailwindcss.com/docs/visibility
					*/
					visibility: [
						"visible",
						"invisible",
						"collapse"
					],
					/**
					* Z-Index
					* @see https://tailwindcss.com/docs/z-index
					*/
					z: [{ z: [
						isInteger,
						"auto",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Flex Basis
					* @see https://tailwindcss.com/docs/flex-basis
					*/
					basis: [{ basis: [
						isFraction,
						"full",
						"auto",
						themeContainer,
						...scaleUnambiguousSpacing()
					] }],
					/**
					* Flex Direction
					* @see https://tailwindcss.com/docs/flex-direction
					*/
					"flex-direction": [{ flex: [
						"row",
						"row-reverse",
						"col",
						"col-reverse"
					] }],
					/**
					* Flex Wrap
					* @see https://tailwindcss.com/docs/flex-wrap
					*/
					"flex-wrap": [{ flex: [
						"nowrap",
						"wrap",
						"wrap-reverse"
					] }],
					/**
					* Flex
					* @see https://tailwindcss.com/docs/flex
					*/
					flex: [{ flex: [
						isNumber,
						isFraction,
						"auto",
						"initial",
						"none",
						isArbitraryValue
					] }],
					/**
					* Flex Grow
					* @see https://tailwindcss.com/docs/flex-grow
					*/
					grow: [{ grow: [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Flex Shrink
					* @see https://tailwindcss.com/docs/flex-shrink
					*/
					shrink: [{ shrink: [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Order
					* @see https://tailwindcss.com/docs/order
					*/
					order: [{ order: [
						isInteger,
						"first",
						"last",
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Grid Template Columns
					* @see https://tailwindcss.com/docs/grid-template-columns
					*/
					"grid-cols": [{ "grid-cols": scaleGridTemplateColsRows() }],
					/**
					* Grid Column Start / End
					* @see https://tailwindcss.com/docs/grid-column
					*/
					"col-start-end": [{ col: scaleGridColRowStartAndEnd() }],
					/**
					* Grid Column Start
					* @see https://tailwindcss.com/docs/grid-column
					*/
					"col-start": [{ "col-start": scaleGridColRowStartOrEnd() }],
					/**
					* Grid Column End
					* @see https://tailwindcss.com/docs/grid-column
					*/
					"col-end": [{ "col-end": scaleGridColRowStartOrEnd() }],
					/**
					* Grid Template Rows
					* @see https://tailwindcss.com/docs/grid-template-rows
					*/
					"grid-rows": [{ "grid-rows": scaleGridTemplateColsRows() }],
					/**
					* Grid Row Start / End
					* @see https://tailwindcss.com/docs/grid-row
					*/
					"row-start-end": [{ row: scaleGridColRowStartAndEnd() }],
					/**
					* Grid Row Start
					* @see https://tailwindcss.com/docs/grid-row
					*/
					"row-start": [{ "row-start": scaleGridColRowStartOrEnd() }],
					/**
					* Grid Row End
					* @see https://tailwindcss.com/docs/grid-row
					*/
					"row-end": [{ "row-end": scaleGridColRowStartOrEnd() }],
					/**
					* Grid Auto Flow
					* @see https://tailwindcss.com/docs/grid-auto-flow
					*/
					"grid-flow": [{ "grid-flow": [
						"row",
						"col",
						"dense",
						"row-dense",
						"col-dense"
					] }],
					/**
					* Grid Auto Columns
					* @see https://tailwindcss.com/docs/grid-auto-columns
					*/
					"auto-cols": [{ "auto-cols": scaleGridAutoColsRows() }],
					/**
					* Grid Auto Rows
					* @see https://tailwindcss.com/docs/grid-auto-rows
					*/
					"auto-rows": [{ "auto-rows": scaleGridAutoColsRows() }],
					/**
					* Gap
					* @see https://tailwindcss.com/docs/gap
					*/
					gap: [{ gap: scaleUnambiguousSpacing() }],
					/**
					* Gap X
					* @see https://tailwindcss.com/docs/gap
					*/
					"gap-x": [{ "gap-x": scaleUnambiguousSpacing() }],
					/**
					* Gap Y
					* @see https://tailwindcss.com/docs/gap
					*/
					"gap-y": [{ "gap-y": scaleUnambiguousSpacing() }],
					/**
					* Justify Content
					* @see https://tailwindcss.com/docs/justify-content
					*/
					"justify-content": [{ justify: [...scaleAlignPrimaryAxis(), "normal"] }],
					/**
					* Justify Items
					* @see https://tailwindcss.com/docs/justify-items
					*/
					"justify-items": [{ "justify-items": [...scaleAlignSecondaryAxis(), "normal"] }],
					/**
					* Justify Self
					* @see https://tailwindcss.com/docs/justify-self
					*/
					"justify-self": [{ "justify-self": ["auto", ...scaleAlignSecondaryAxis()] }],
					/**
					* Align Content
					* @see https://tailwindcss.com/docs/align-content
					*/
					"align-content": [{ content: ["normal", ...scaleAlignPrimaryAxis()] }],
					/**
					* Align Items
					* @see https://tailwindcss.com/docs/align-items
					*/
					"align-items": [{ items: [...scaleAlignSecondaryAxis(), { baseline: ["", "last"] }] }],
					/**
					* Align Self
					* @see https://tailwindcss.com/docs/align-self
					*/
					"align-self": [{ self: [
						"auto",
						...scaleAlignSecondaryAxis(),
						{ baseline: ["", "last"] }
					] }],
					/**
					* Place Content
					* @see https://tailwindcss.com/docs/place-content
					*/
					"place-content": [{ "place-content": scaleAlignPrimaryAxis() }],
					/**
					* Place Items
					* @see https://tailwindcss.com/docs/place-items
					*/
					"place-items": [{ "place-items": [...scaleAlignSecondaryAxis(), "baseline"] }],
					/**
					* Place Self
					* @see https://tailwindcss.com/docs/place-self
					*/
					"place-self": [{ "place-self": ["auto", ...scaleAlignSecondaryAxis()] }],
					/**
					* Padding
					* @see https://tailwindcss.com/docs/padding
					*/
					p: [{ p: scaleUnambiguousSpacing() }],
					/**
					* Padding Inline
					* @see https://tailwindcss.com/docs/padding
					*/
					px: [{ px: scaleUnambiguousSpacing() }],
					/**
					* Padding Block
					* @see https://tailwindcss.com/docs/padding
					*/
					py: [{ py: scaleUnambiguousSpacing() }],
					/**
					* Padding Inline Start
					* @see https://tailwindcss.com/docs/padding
					*/
					ps: [{ ps: scaleUnambiguousSpacing() }],
					/**
					* Padding Inline End
					* @see https://tailwindcss.com/docs/padding
					*/
					pe: [{ pe: scaleUnambiguousSpacing() }],
					/**
					* Padding Block Start
					* @see https://tailwindcss.com/docs/padding
					*/
					pbs: [{ pbs: scaleUnambiguousSpacing() }],
					/**
					* Padding Block End
					* @see https://tailwindcss.com/docs/padding
					*/
					pbe: [{ pbe: scaleUnambiguousSpacing() }],
					/**
					* Padding Top
					* @see https://tailwindcss.com/docs/padding
					*/
					pt: [{ pt: scaleUnambiguousSpacing() }],
					/**
					* Padding Right
					* @see https://tailwindcss.com/docs/padding
					*/
					pr: [{ pr: scaleUnambiguousSpacing() }],
					/**
					* Padding Bottom
					* @see https://tailwindcss.com/docs/padding
					*/
					pb: [{ pb: scaleUnambiguousSpacing() }],
					/**
					* Padding Left
					* @see https://tailwindcss.com/docs/padding
					*/
					pl: [{ pl: scaleUnambiguousSpacing() }],
					/**
					* Margin
					* @see https://tailwindcss.com/docs/margin
					*/
					m: [{ m: scaleMargin() }],
					/**
					* Margin Inline
					* @see https://tailwindcss.com/docs/margin
					*/
					mx: [{ mx: scaleMargin() }],
					/**
					* Margin Block
					* @see https://tailwindcss.com/docs/margin
					*/
					my: [{ my: scaleMargin() }],
					/**
					* Margin Inline Start
					* @see https://tailwindcss.com/docs/margin
					*/
					ms: [{ ms: scaleMargin() }],
					/**
					* Margin Inline End
					* @see https://tailwindcss.com/docs/margin
					*/
					me: [{ me: scaleMargin() }],
					/**
					* Margin Block Start
					* @see https://tailwindcss.com/docs/margin
					*/
					mbs: [{ mbs: scaleMargin() }],
					/**
					* Margin Block End
					* @see https://tailwindcss.com/docs/margin
					*/
					mbe: [{ mbe: scaleMargin() }],
					/**
					* Margin Top
					* @see https://tailwindcss.com/docs/margin
					*/
					mt: [{ mt: scaleMargin() }],
					/**
					* Margin Right
					* @see https://tailwindcss.com/docs/margin
					*/
					mr: [{ mr: scaleMargin() }],
					/**
					* Margin Bottom
					* @see https://tailwindcss.com/docs/margin
					*/
					mb: [{ mb: scaleMargin() }],
					/**
					* Margin Left
					* @see https://tailwindcss.com/docs/margin
					*/
					ml: [{ ml: scaleMargin() }],
					/**
					* Space Between X
					* @see https://tailwindcss.com/docs/margin#adding-space-between-children
					*/
					"space-x": [{ "space-x": scaleUnambiguousSpacing() }],
					/**
					* Space Between X Reverse
					* @see https://tailwindcss.com/docs/margin#adding-space-between-children
					*/
					"space-x-reverse": ["space-x-reverse"],
					/**
					* Space Between Y
					* @see https://tailwindcss.com/docs/margin#adding-space-between-children
					*/
					"space-y": [{ "space-y": scaleUnambiguousSpacing() }],
					/**
					* Space Between Y Reverse
					* @see https://tailwindcss.com/docs/margin#adding-space-between-children
					*/
					"space-y-reverse": ["space-y-reverse"],
					/**
					* Size
					* @see https://tailwindcss.com/docs/width#setting-both-width-and-height
					*/
					size: [{ size: scaleSizing() }],
					/**
					* Inline Size
					* @see https://tailwindcss.com/docs/width
					*/
					"inline-size": [{ inline: ["auto", ...scaleSizingInline()] }],
					/**
					* Min-Inline Size
					* @see https://tailwindcss.com/docs/min-width
					*/
					"min-inline-size": [{ "min-inline": ["auto", ...scaleSizingInline()] }],
					/**
					* Max-Inline Size
					* @see https://tailwindcss.com/docs/max-width
					*/
					"max-inline-size": [{ "max-inline": ["none", ...scaleSizingInline()] }],
					/**
					* Block Size
					* @see https://tailwindcss.com/docs/height
					*/
					"block-size": [{ block: ["auto", ...scaleSizingBlock()] }],
					/**
					* Min-Block Size
					* @see https://tailwindcss.com/docs/min-height
					*/
					"min-block-size": [{ "min-block": ["auto", ...scaleSizingBlock()] }],
					/**
					* Max-Block Size
					* @see https://tailwindcss.com/docs/max-height
					*/
					"max-block-size": [{ "max-block": ["none", ...scaleSizingBlock()] }],
					/**
					* Width
					* @see https://tailwindcss.com/docs/width
					*/
					w: [{ w: [
						themeContainer,
						"screen",
						...scaleSizing()
					] }],
					/**
					* Min-Width
					* @see https://tailwindcss.com/docs/min-width
					*/
					"min-w": [{ "min-w": [
						themeContainer,
						"screen",
						"none",
						...scaleSizing()
					] }],
					/**
					* Max-Width
					* @see https://tailwindcss.com/docs/max-width
					*/
					"max-w": [{ "max-w": [
						themeContainer,
						"screen",
						"none",
						"prose",
						{ screen: [themeBreakpoint] },
						...scaleSizing()
					] }],
					/**
					* Height
					* @see https://tailwindcss.com/docs/height
					*/
					h: [{ h: [
						"screen",
						"lh",
						...scaleSizing()
					] }],
					/**
					* Min-Height
					* @see https://tailwindcss.com/docs/min-height
					*/
					"min-h": [{ "min-h": [
						"screen",
						"lh",
						"none",
						...scaleSizing()
					] }],
					/**
					* Max-Height
					* @see https://tailwindcss.com/docs/max-height
					*/
					"max-h": [{ "max-h": [
						"screen",
						"lh",
						...scaleSizing()
					] }],
					/**
					* Font Size
					* @see https://tailwindcss.com/docs/font-size
					*/
					"font-size": [{ text: [
						"base",
						themeText,
						isArbitraryVariableLength,
						isArbitraryLength
					] }],
					/**
					* Font Smoothing
					* @see https://tailwindcss.com/docs/font-smoothing
					*/
					"font-smoothing": ["antialiased", "subpixel-antialiased"],
					/**
					* Font Style
					* @see https://tailwindcss.com/docs/font-style
					*/
					"font-style": ["italic", "not-italic"],
					/**
					* Font Weight
					* @see https://tailwindcss.com/docs/font-weight
					*/
					"font-weight": [{ font: [
						themeFontWeight,
						isArbitraryVariableWeight,
						isArbitraryWeight
					] }],
					/**
					* Font Stretch
					* @see https://tailwindcss.com/docs/font-stretch
					*/
					"font-stretch": [{ "font-stretch": [
						"ultra-condensed",
						"extra-condensed",
						"condensed",
						"semi-condensed",
						"normal",
						"semi-expanded",
						"expanded",
						"extra-expanded",
						"ultra-expanded",
						isPercent,
						isArbitraryValue
					] }],
					/**
					* Font Family
					* @see https://tailwindcss.com/docs/font-family
					*/
					"font-family": [{ font: [
						isArbitraryVariableFamilyName,
						isArbitraryFamilyName,
						themeFont
					] }],
					/**
					* Font Feature Settings
					* @see https://tailwindcss.com/docs/font-feature-settings
					*/
					"font-features": [{ "font-features": [isArbitraryValue] }],
					/**
					* Font Variant Numeric
					* @see https://tailwindcss.com/docs/font-variant-numeric
					*/
					"fvn-normal": ["normal-nums"],
					/**
					* Font Variant Numeric
					* @see https://tailwindcss.com/docs/font-variant-numeric
					*/
					"fvn-ordinal": ["ordinal"],
					/**
					* Font Variant Numeric
					* @see https://tailwindcss.com/docs/font-variant-numeric
					*/
					"fvn-slashed-zero": ["slashed-zero"],
					/**
					* Font Variant Numeric
					* @see https://tailwindcss.com/docs/font-variant-numeric
					*/
					"fvn-figure": ["lining-nums", "oldstyle-nums"],
					/**
					* Font Variant Numeric
					* @see https://tailwindcss.com/docs/font-variant-numeric
					*/
					"fvn-spacing": ["proportional-nums", "tabular-nums"],
					/**
					* Font Variant Numeric
					* @see https://tailwindcss.com/docs/font-variant-numeric
					*/
					"fvn-fraction": ["diagonal-fractions", "stacked-fractions"],
					/**
					* Letter Spacing
					* @see https://tailwindcss.com/docs/letter-spacing
					*/
					tracking: [{ tracking: [
						themeTracking,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Line Clamp
					* @see https://tailwindcss.com/docs/line-clamp
					*/
					"line-clamp": [{ "line-clamp": [
						isNumber,
						"none",
						isArbitraryVariable,
						isArbitraryNumber
					] }],
					/**
					* Line Height
					* @see https://tailwindcss.com/docs/line-height
					*/
					leading: [{ leading: [themeLeading, ...scaleUnambiguousSpacing()] }],
					/**
					* List Style Image
					* @see https://tailwindcss.com/docs/list-style-image
					*/
					"list-image": [{ "list-image": [
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* List Style Position
					* @see https://tailwindcss.com/docs/list-style-position
					*/
					"list-style-position": [{ list: ["inside", "outside"] }],
					/**
					* List Style Type
					* @see https://tailwindcss.com/docs/list-style-type
					*/
					"list-style-type": [{ list: [
						"disc",
						"decimal",
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Text Alignment
					* @see https://tailwindcss.com/docs/text-align
					*/
					"text-alignment": [{ text: [
						"left",
						"center",
						"right",
						"justify",
						"start",
						"end"
					] }],
					/**
					* Placeholder Color
					* @deprecated since Tailwind CSS v3.0.0
					* @see https://v3.tailwindcss.com/docs/placeholder-color
					*/
					"placeholder-color": [{ placeholder: scaleColor() }],
					/**
					* Text Color
					* @see https://tailwindcss.com/docs/text-color
					*/
					"text-color": [{ text: scaleColor() }],
					/**
					* Text Decoration
					* @see https://tailwindcss.com/docs/text-decoration
					*/
					"text-decoration": [
						"underline",
						"overline",
						"line-through",
						"no-underline"
					],
					/**
					* Text Decoration Style
					* @see https://tailwindcss.com/docs/text-decoration-style
					*/
					"text-decoration-style": [{ decoration: [...scaleLineStyle(), "wavy"] }],
					/**
					* Text Decoration Thickness
					* @see https://tailwindcss.com/docs/text-decoration-thickness
					*/
					"text-decoration-thickness": [{ decoration: [
						isNumber,
						"from-font",
						"auto",
						isArbitraryVariable,
						isArbitraryLength
					] }],
					/**
					* Text Decoration Color
					* @see https://tailwindcss.com/docs/text-decoration-color
					*/
					"text-decoration-color": [{ decoration: scaleColor() }],
					/**
					* Text Underline Offset
					* @see https://tailwindcss.com/docs/text-underline-offset
					*/
					"underline-offset": [{ "underline-offset": [
						isNumber,
						"auto",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Text Transform
					* @see https://tailwindcss.com/docs/text-transform
					*/
					"text-transform": [
						"uppercase",
						"lowercase",
						"capitalize",
						"normal-case"
					],
					/**
					* Text Overflow
					* @see https://tailwindcss.com/docs/text-overflow
					*/
					"text-overflow": [
						"truncate",
						"text-ellipsis",
						"text-clip"
					],
					/**
					* Text Wrap
					* @see https://tailwindcss.com/docs/text-wrap
					*/
					"text-wrap": [{ text: [
						"wrap",
						"nowrap",
						"balance",
						"pretty"
					] }],
					/**
					* Text Indent
					* @see https://tailwindcss.com/docs/text-indent
					*/
					indent: [{ indent: scaleUnambiguousSpacing() }],
					/**
					* Tab Size
					* @see https://tailwindcss.com/docs/tab-size
					*/
					"tab-size": [{ tab: [
						isInteger,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Vertical Alignment
					* @see https://tailwindcss.com/docs/vertical-align
					*/
					"vertical-align": [{ align: [
						"baseline",
						"top",
						"middle",
						"bottom",
						"text-top",
						"text-bottom",
						"sub",
						"super",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Whitespace
					* @see https://tailwindcss.com/docs/whitespace
					*/
					whitespace: [{ whitespace: [
						"normal",
						"nowrap",
						"pre",
						"pre-line",
						"pre-wrap",
						"break-spaces"
					] }],
					/**
					* Word Break
					* @see https://tailwindcss.com/docs/word-break
					*/
					break: [{ break: [
						"normal",
						"words",
						"all",
						"keep"
					] }],
					/**
					* Overflow Wrap
					* @see https://tailwindcss.com/docs/overflow-wrap
					*/
					wrap: [{ wrap: [
						"break-word",
						"anywhere",
						"normal"
					] }],
					/**
					* Hyphens
					* @see https://tailwindcss.com/docs/hyphens
					*/
					hyphens: [{ hyphens: [
						"none",
						"manual",
						"auto"
					] }],
					/**
					* Content
					* @see https://tailwindcss.com/docs/content
					*/
					content: [{ content: [
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Background Attachment
					* @see https://tailwindcss.com/docs/background-attachment
					*/
					"bg-attachment": [{ bg: [
						"fixed",
						"local",
						"scroll"
					] }],
					/**
					* Background Clip
					* @see https://tailwindcss.com/docs/background-clip
					*/
					"bg-clip": [{ "bg-clip": [
						"border",
						"padding",
						"content",
						"text"
					] }],
					/**
					* Background Origin
					* @see https://tailwindcss.com/docs/background-origin
					*/
					"bg-origin": [{ "bg-origin": [
						"border",
						"padding",
						"content"
					] }],
					/**
					* Background Position
					* @see https://tailwindcss.com/docs/background-position
					*/
					"bg-position": [{ bg: scaleBgPosition() }],
					/**
					* Background Repeat
					* @see https://tailwindcss.com/docs/background-repeat
					*/
					"bg-repeat": [{ bg: scaleBgRepeat() }],
					/**
					* Background Size
					* @see https://tailwindcss.com/docs/background-size
					*/
					"bg-size": [{ bg: scaleBgSize() }],
					/**
					* Background Image
					* @see https://tailwindcss.com/docs/background-image
					*/
					"bg-image": [{ bg: [
						"none",
						{
							linear: [
								{ to: [
									"t",
									"tr",
									"r",
									"br",
									"b",
									"bl",
									"l",
									"tl"
								] },
								isInteger,
								isArbitraryVariable,
								isArbitraryValue
							],
							radial: [
								"",
								isArbitraryVariable,
								isArbitraryValue
							],
							conic: [
								isInteger,
								isArbitraryVariable,
								isArbitraryValue
							]
						},
						isArbitraryVariableImage,
						isArbitraryImage
					] }],
					/**
					* Background Color
					* @see https://tailwindcss.com/docs/background-color
					*/
					"bg-color": [{ bg: scaleColor() }],
					/**
					* Gradient Color Stops From Position
					* @see https://tailwindcss.com/docs/gradient-color-stops
					*/
					"gradient-from-pos": [{ from: scaleGradientStopPosition() }],
					/**
					* Gradient Color Stops Via Position
					* @see https://tailwindcss.com/docs/gradient-color-stops
					*/
					"gradient-via-pos": [{ via: scaleGradientStopPosition() }],
					/**
					* Gradient Color Stops To Position
					* @see https://tailwindcss.com/docs/gradient-color-stops
					*/
					"gradient-to-pos": [{ to: scaleGradientStopPosition() }],
					/**
					* Gradient Color Stops From
					* @see https://tailwindcss.com/docs/gradient-color-stops
					*/
					"gradient-from": [{ from: scaleColor() }],
					/**
					* Gradient Color Stops Via
					* @see https://tailwindcss.com/docs/gradient-color-stops
					*/
					"gradient-via": [{ via: scaleColor() }],
					/**
					* Gradient Color Stops To
					* @see https://tailwindcss.com/docs/gradient-color-stops
					*/
					"gradient-to": [{ to: scaleColor() }],
					/**
					* Border Radius
					* @see https://tailwindcss.com/docs/border-radius
					*/
					rounded: [{ rounded: scaleRadius() }],
					/**
					* Border Radius Start
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-s": [{ "rounded-s": scaleRadius() }],
					/**
					* Border Radius End
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-e": [{ "rounded-e": scaleRadius() }],
					/**
					* Border Radius Top
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-t": [{ "rounded-t": scaleRadius() }],
					/**
					* Border Radius Right
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-r": [{ "rounded-r": scaleRadius() }],
					/**
					* Border Radius Bottom
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-b": [{ "rounded-b": scaleRadius() }],
					/**
					* Border Radius Left
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-l": [{ "rounded-l": scaleRadius() }],
					/**
					* Border Radius Start Start
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-ss": [{ "rounded-ss": scaleRadius() }],
					/**
					* Border Radius Start End
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-se": [{ "rounded-se": scaleRadius() }],
					/**
					* Border Radius End End
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-ee": [{ "rounded-ee": scaleRadius() }],
					/**
					* Border Radius End Start
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-es": [{ "rounded-es": scaleRadius() }],
					/**
					* Border Radius Top Left
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-tl": [{ "rounded-tl": scaleRadius() }],
					/**
					* Border Radius Top Right
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-tr": [{ "rounded-tr": scaleRadius() }],
					/**
					* Border Radius Bottom Right
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-br": [{ "rounded-br": scaleRadius() }],
					/**
					* Border Radius Bottom Left
					* @see https://tailwindcss.com/docs/border-radius
					*/
					"rounded-bl": [{ "rounded-bl": scaleRadius() }],
					/**
					* Border Width
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w": [{ border: scaleBorderWidth() }],
					/**
					* Border Width Inline
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-x": [{ "border-x": scaleBorderWidth() }],
					/**
					* Border Width Block
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-y": [{ "border-y": scaleBorderWidth() }],
					/**
					* Border Width Inline Start
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-s": [{ "border-s": scaleBorderWidth() }],
					/**
					* Border Width Inline End
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-e": [{ "border-e": scaleBorderWidth() }],
					/**
					* Border Width Block Start
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-bs": [{ "border-bs": scaleBorderWidth() }],
					/**
					* Border Width Block End
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-be": [{ "border-be": scaleBorderWidth() }],
					/**
					* Border Width Top
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-t": [{ "border-t": scaleBorderWidth() }],
					/**
					* Border Width Right
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-r": [{ "border-r": scaleBorderWidth() }],
					/**
					* Border Width Bottom
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-b": [{ "border-b": scaleBorderWidth() }],
					/**
					* Border Width Left
					* @see https://tailwindcss.com/docs/border-width
					*/
					"border-w-l": [{ "border-l": scaleBorderWidth() }],
					/**
					* Divide Width X
					* @see https://tailwindcss.com/docs/border-width#between-children
					*/
					"divide-x": [{ "divide-x": scaleBorderWidth() }],
					/**
					* Divide Width X Reverse
					* @see https://tailwindcss.com/docs/border-width#between-children
					*/
					"divide-x-reverse": ["divide-x-reverse"],
					/**
					* Divide Width Y
					* @see https://tailwindcss.com/docs/border-width#between-children
					*/
					"divide-y": [{ "divide-y": scaleBorderWidth() }],
					/**
					* Divide Width Y Reverse
					* @see https://tailwindcss.com/docs/border-width#between-children
					*/
					"divide-y-reverse": ["divide-y-reverse"],
					/**
					* Border Style
					* @see https://tailwindcss.com/docs/border-style
					*/
					"border-style": [{ border: [
						...scaleLineStyle(),
						"hidden",
						"none"
					] }],
					/**
					* Divide Style
					* @see https://tailwindcss.com/docs/border-style#setting-the-divider-style
					*/
					"divide-style": [{ divide: [
						...scaleLineStyle(),
						"hidden",
						"none"
					] }],
					/**
					* Border Color
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color": [{ border: scaleColor() }],
					/**
					* Border Color Inline
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-x": [{ "border-x": scaleColor() }],
					/**
					* Border Color Block
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-y": [{ "border-y": scaleColor() }],
					/**
					* Border Color Inline Start
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-s": [{ "border-s": scaleColor() }],
					/**
					* Border Color Inline End
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-e": [{ "border-e": scaleColor() }],
					/**
					* Border Color Block Start
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-bs": [{ "border-bs": scaleColor() }],
					/**
					* Border Color Block End
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-be": [{ "border-be": scaleColor() }],
					/**
					* Border Color Top
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-t": [{ "border-t": scaleColor() }],
					/**
					* Border Color Right
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-r": [{ "border-r": scaleColor() }],
					/**
					* Border Color Bottom
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-b": [{ "border-b": scaleColor() }],
					/**
					* Border Color Left
					* @see https://tailwindcss.com/docs/border-color
					*/
					"border-color-l": [{ "border-l": scaleColor() }],
					/**
					* Divide Color
					* @see https://tailwindcss.com/docs/divide-color
					*/
					"divide-color": [{ divide: scaleColor() }],
					/**
					* Outline Style
					* @see https://tailwindcss.com/docs/outline-style
					*/
					"outline-style": [{ outline: [
						...scaleLineStyle(),
						"none",
						"hidden"
					] }],
					/**
					* Outline Offset
					* @see https://tailwindcss.com/docs/outline-offset
					*/
					"outline-offset": [{ "outline-offset": [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Outline Width
					* @see https://tailwindcss.com/docs/outline-width
					*/
					"outline-w": [{ outline: [
						"",
						isNumber,
						isArbitraryVariableLength,
						isArbitraryLength
					] }],
					/**
					* Outline Color
					* @see https://tailwindcss.com/docs/outline-color
					*/
					"outline-color": [{ outline: scaleColor() }],
					/**
					* Box Shadow
					* @see https://tailwindcss.com/docs/box-shadow
					*/
					shadow: [{ shadow: [
						"",
						"none",
						themeShadow,
						isArbitraryVariableShadow,
						isArbitraryShadow
					] }],
					/**
					* Box Shadow Color
					* @see https://tailwindcss.com/docs/box-shadow#setting-the-shadow-color
					*/
					"shadow-color": [{ shadow: scaleColor() }],
					/**
					* Inset Box Shadow
					* @see https://tailwindcss.com/docs/box-shadow#adding-an-inset-shadow
					*/
					"inset-shadow": [{ "inset-shadow": [
						"none",
						themeInsetShadow,
						isArbitraryVariableShadow,
						isArbitraryShadow
					] }],
					/**
					* Inset Box Shadow Color
					* @see https://tailwindcss.com/docs/box-shadow#setting-the-inset-shadow-color
					*/
					"inset-shadow-color": [{ "inset-shadow": scaleColor() }],
					/**
					* Ring Width
					* @see https://tailwindcss.com/docs/box-shadow#adding-a-ring
					*/
					"ring-w": [{ ring: scaleBorderWidth() }],
					/**
					* Ring Width Inset
					* @see https://v3.tailwindcss.com/docs/ring-width#inset-rings
					* @deprecated since Tailwind CSS v4.0.0
					* @see https://github.com/tailwindlabs/tailwindcss/blob/v4.0.0/packages/tailwindcss/src/utilities.ts#L4158
					*/
					"ring-w-inset": ["ring-inset"],
					/**
					* Ring Color
					* @see https://tailwindcss.com/docs/box-shadow#setting-the-ring-color
					*/
					"ring-color": [{ ring: scaleColor() }],
					/**
					* Ring Offset Width
					* @see https://v3.tailwindcss.com/docs/ring-offset-width
					* @deprecated since Tailwind CSS v4.0.0
					* @see https://github.com/tailwindlabs/tailwindcss/blob/v4.0.0/packages/tailwindcss/src/utilities.ts#L4158
					*/
					"ring-offset-w": [{ "ring-offset": [isNumber, isArbitraryLength] }],
					/**
					* Ring Offset Color
					* @see https://v3.tailwindcss.com/docs/ring-offset-color
					* @deprecated since Tailwind CSS v4.0.0
					* @see https://github.com/tailwindlabs/tailwindcss/blob/v4.0.0/packages/tailwindcss/src/utilities.ts#L4158
					*/
					"ring-offset-color": [{ "ring-offset": scaleColor() }],
					/**
					* Inset Ring Width
					* @see https://tailwindcss.com/docs/box-shadow#adding-an-inset-ring
					*/
					"inset-ring-w": [{ "inset-ring": scaleBorderWidth() }],
					/**
					* Inset Ring Color
					* @see https://tailwindcss.com/docs/box-shadow#setting-the-inset-ring-color
					*/
					"inset-ring-color": [{ "inset-ring": scaleColor() }],
					/**
					* Text Shadow
					* @see https://tailwindcss.com/docs/text-shadow
					*/
					"text-shadow": [{ "text-shadow": [
						"none",
						themeTextShadow,
						isArbitraryVariableShadow,
						isArbitraryShadow
					] }],
					/**
					* Text Shadow Color
					* @see https://tailwindcss.com/docs/text-shadow#setting-the-shadow-color
					*/
					"text-shadow-color": [{ "text-shadow": scaleColor() }],
					/**
					* Opacity
					* @see https://tailwindcss.com/docs/opacity
					*/
					opacity: [{ opacity: [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Mix Blend Mode
					* @see https://tailwindcss.com/docs/mix-blend-mode
					*/
					"mix-blend": [{ "mix-blend": [
						...scaleBlendMode(),
						"plus-darker",
						"plus-lighter"
					] }],
					/**
					* Background Blend Mode
					* @see https://tailwindcss.com/docs/background-blend-mode
					*/
					"bg-blend": [{ "bg-blend": scaleBlendMode() }],
					/**
					* Mask Clip
					* @see https://tailwindcss.com/docs/mask-clip
					*/
					"mask-clip": [{ "mask-clip": [
						"border",
						"padding",
						"content",
						"fill",
						"stroke",
						"view"
					] }, "mask-no-clip"],
					/**
					* Mask Composite
					* @see https://tailwindcss.com/docs/mask-composite
					*/
					"mask-composite": [{ mask: [
						"add",
						"subtract",
						"intersect",
						"exclude"
					] }],
					/**
					* Mask Image
					* @see https://tailwindcss.com/docs/mask-image
					*/
					"mask-image-linear-pos": [{ "mask-linear": [isNumber] }],
					"mask-image-linear-from-pos": [{ "mask-linear-from": scaleMaskImagePosition() }],
					"mask-image-linear-to-pos": [{ "mask-linear-to": scaleMaskImagePosition() }],
					"mask-image-linear-from-color": [{ "mask-linear-from": scaleColor() }],
					"mask-image-linear-to-color": [{ "mask-linear-to": scaleColor() }],
					"mask-image-t-from-pos": [{ "mask-t-from": scaleMaskImagePosition() }],
					"mask-image-t-to-pos": [{ "mask-t-to": scaleMaskImagePosition() }],
					"mask-image-t-from-color": [{ "mask-t-from": scaleColor() }],
					"mask-image-t-to-color": [{ "mask-t-to": scaleColor() }],
					"mask-image-r-from-pos": [{ "mask-r-from": scaleMaskImagePosition() }],
					"mask-image-r-to-pos": [{ "mask-r-to": scaleMaskImagePosition() }],
					"mask-image-r-from-color": [{ "mask-r-from": scaleColor() }],
					"mask-image-r-to-color": [{ "mask-r-to": scaleColor() }],
					"mask-image-b-from-pos": [{ "mask-b-from": scaleMaskImagePosition() }],
					"mask-image-b-to-pos": [{ "mask-b-to": scaleMaskImagePosition() }],
					"mask-image-b-from-color": [{ "mask-b-from": scaleColor() }],
					"mask-image-b-to-color": [{ "mask-b-to": scaleColor() }],
					"mask-image-l-from-pos": [{ "mask-l-from": scaleMaskImagePosition() }],
					"mask-image-l-to-pos": [{ "mask-l-to": scaleMaskImagePosition() }],
					"mask-image-l-from-color": [{ "mask-l-from": scaleColor() }],
					"mask-image-l-to-color": [{ "mask-l-to": scaleColor() }],
					"mask-image-x-from-pos": [{ "mask-x-from": scaleMaskImagePosition() }],
					"mask-image-x-to-pos": [{ "mask-x-to": scaleMaskImagePosition() }],
					"mask-image-x-from-color": [{ "mask-x-from": scaleColor() }],
					"mask-image-x-to-color": [{ "mask-x-to": scaleColor() }],
					"mask-image-y-from-pos": [{ "mask-y-from": scaleMaskImagePosition() }],
					"mask-image-y-to-pos": [{ "mask-y-to": scaleMaskImagePosition() }],
					"mask-image-y-from-color": [{ "mask-y-from": scaleColor() }],
					"mask-image-y-to-color": [{ "mask-y-to": scaleColor() }],
					"mask-image-radial": [{ "mask-radial": [isArbitraryVariable, isArbitraryValue] }],
					"mask-image-radial-from-pos": [{ "mask-radial-from": scaleMaskImagePosition() }],
					"mask-image-radial-to-pos": [{ "mask-radial-to": scaleMaskImagePosition() }],
					"mask-image-radial-from-color": [{ "mask-radial-from": scaleColor() }],
					"mask-image-radial-to-color": [{ "mask-radial-to": scaleColor() }],
					"mask-image-radial-shape": [{ "mask-radial": ["circle", "ellipse"] }],
					"mask-image-radial-size": [{ "mask-radial": [{
						closest: ["side", "corner"],
						farthest: ["side", "corner"]
					}] }],
					"mask-image-radial-pos": [{ "mask-radial-at": scalePosition() }],
					"mask-image-conic-pos": [{ "mask-conic": [isNumber] }],
					"mask-image-conic-from-pos": [{ "mask-conic-from": scaleMaskImagePosition() }],
					"mask-image-conic-to-pos": [{ "mask-conic-to": scaleMaskImagePosition() }],
					"mask-image-conic-from-color": [{ "mask-conic-from": scaleColor() }],
					"mask-image-conic-to-color": [{ "mask-conic-to": scaleColor() }],
					/**
					* Mask Mode
					* @see https://tailwindcss.com/docs/mask-mode
					*/
					"mask-mode": [{ mask: [
						"alpha",
						"luminance",
						"match"
					] }],
					/**
					* Mask Origin
					* @see https://tailwindcss.com/docs/mask-origin
					*/
					"mask-origin": [{ "mask-origin": [
						"border",
						"padding",
						"content",
						"fill",
						"stroke",
						"view"
					] }],
					/**
					* Mask Position
					* @see https://tailwindcss.com/docs/mask-position
					*/
					"mask-position": [{ mask: scaleBgPosition() }],
					/**
					* Mask Repeat
					* @see https://tailwindcss.com/docs/mask-repeat
					*/
					"mask-repeat": [{ mask: scaleBgRepeat() }],
					/**
					* Mask Size
					* @see https://tailwindcss.com/docs/mask-size
					*/
					"mask-size": [{ mask: scaleBgSize() }],
					/**
					* Mask Type
					* @see https://tailwindcss.com/docs/mask-type
					*/
					"mask-type": [{ "mask-type": ["alpha", "luminance"] }],
					/**
					* Mask Image
					* @see https://tailwindcss.com/docs/mask-image
					*/
					"mask-image": [{ mask: [
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Filter
					* @see https://tailwindcss.com/docs/filter
					*/
					filter: [{ filter: [
						"",
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Blur
					* @see https://tailwindcss.com/docs/blur
					*/
					blur: [{ blur: scaleBlur() }],
					/**
					* Brightness
					* @see https://tailwindcss.com/docs/brightness
					*/
					brightness: [{ brightness: [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Contrast
					* @see https://tailwindcss.com/docs/contrast
					*/
					contrast: [{ contrast: [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Drop Shadow
					* @see https://tailwindcss.com/docs/drop-shadow
					*/
					"drop-shadow": [{ "drop-shadow": [
						"",
						"none",
						themeDropShadow,
						isArbitraryVariableShadow,
						isArbitraryShadow
					] }],
					/**
					* Drop Shadow Color
					* @see https://tailwindcss.com/docs/filter-drop-shadow#setting-the-shadow-color
					*/
					"drop-shadow-color": [{ "drop-shadow": scaleColor() }],
					/**
					* Grayscale
					* @see https://tailwindcss.com/docs/grayscale
					*/
					grayscale: [{ grayscale: [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Hue Rotate
					* @see https://tailwindcss.com/docs/hue-rotate
					*/
					"hue-rotate": [{ "hue-rotate": [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Invert
					* @see https://tailwindcss.com/docs/invert
					*/
					invert: [{ invert: [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Saturate
					* @see https://tailwindcss.com/docs/saturate
					*/
					saturate: [{ saturate: [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Sepia
					* @see https://tailwindcss.com/docs/sepia
					*/
					sepia: [{ sepia: [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Filter
					* @see https://tailwindcss.com/docs/backdrop-filter
					*/
					"backdrop-filter": [{ "backdrop-filter": [
						"",
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Blur
					* @see https://tailwindcss.com/docs/backdrop-blur
					*/
					"backdrop-blur": [{ "backdrop-blur": scaleBlur() }],
					/**
					* Backdrop Brightness
					* @see https://tailwindcss.com/docs/backdrop-brightness
					*/
					"backdrop-brightness": [{ "backdrop-brightness": [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Contrast
					* @see https://tailwindcss.com/docs/backdrop-contrast
					*/
					"backdrop-contrast": [{ "backdrop-contrast": [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Grayscale
					* @see https://tailwindcss.com/docs/backdrop-grayscale
					*/
					"backdrop-grayscale": [{ "backdrop-grayscale": [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Hue Rotate
					* @see https://tailwindcss.com/docs/backdrop-hue-rotate
					*/
					"backdrop-hue-rotate": [{ "backdrop-hue-rotate": [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Invert
					* @see https://tailwindcss.com/docs/backdrop-invert
					*/
					"backdrop-invert": [{ "backdrop-invert": [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Opacity
					* @see https://tailwindcss.com/docs/backdrop-opacity
					*/
					"backdrop-opacity": [{ "backdrop-opacity": [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Saturate
					* @see https://tailwindcss.com/docs/backdrop-saturate
					*/
					"backdrop-saturate": [{ "backdrop-saturate": [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backdrop Sepia
					* @see https://tailwindcss.com/docs/backdrop-sepia
					*/
					"backdrop-sepia": [{ "backdrop-sepia": [
						"",
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Border Collapse
					* @see https://tailwindcss.com/docs/border-collapse
					*/
					"border-collapse": [{ border: ["collapse", "separate"] }],
					/**
					* Border Spacing
					* @see https://tailwindcss.com/docs/border-spacing
					*/
					"border-spacing": [{ "border-spacing": scaleUnambiguousSpacing() }],
					/**
					* Border Spacing X
					* @see https://tailwindcss.com/docs/border-spacing
					*/
					"border-spacing-x": [{ "border-spacing-x": scaleUnambiguousSpacing() }],
					/**
					* Border Spacing Y
					* @see https://tailwindcss.com/docs/border-spacing
					*/
					"border-spacing-y": [{ "border-spacing-y": scaleUnambiguousSpacing() }],
					/**
					* Table Layout
					* @see https://tailwindcss.com/docs/table-layout
					*/
					"table-layout": [{ table: ["auto", "fixed"] }],
					/**
					* Caption Side
					* @see https://tailwindcss.com/docs/caption-side
					*/
					caption: [{ caption: ["top", "bottom"] }],
					/**
					* Transition Property
					* @see https://tailwindcss.com/docs/transition-property
					*/
					transition: [{ transition: [
						"",
						"all",
						"colors",
						"opacity",
						"shadow",
						"transform",
						"none",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Transition Behavior
					* @see https://tailwindcss.com/docs/transition-behavior
					*/
					"transition-behavior": [{ transition: ["normal", "discrete"] }],
					/**
					* Transition Duration
					* @see https://tailwindcss.com/docs/transition-duration
					*/
					duration: [{ duration: [
						isNumber,
						"initial",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Transition Timing Function
					* @see https://tailwindcss.com/docs/transition-timing-function
					*/
					ease: [{ ease: [
						"linear",
						"initial",
						themeEase,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Transition Delay
					* @see https://tailwindcss.com/docs/transition-delay
					*/
					delay: [{ delay: [
						isNumber,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Animation
					* @see https://tailwindcss.com/docs/animation
					*/
					animate: [{ animate: [
						"none",
						themeAnimate,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Backface Visibility
					* @see https://tailwindcss.com/docs/backface-visibility
					*/
					backface: [{ backface: ["hidden", "visible"] }],
					/**
					* Perspective
					* @see https://tailwindcss.com/docs/perspective
					*/
					perspective: [{ perspective: [
						themePerspective,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Perspective Origin
					* @see https://tailwindcss.com/docs/perspective-origin
					*/
					"perspective-origin": [{ "perspective-origin": scalePositionWithArbitrary() }],
					/**
					* Rotate
					* @see https://tailwindcss.com/docs/rotate
					*/
					rotate: [{ rotate: scaleRotate() }],
					/**
					* Rotate X
					* @see https://tailwindcss.com/docs/rotate
					*/
					"rotate-x": [{ "rotate-x": scaleRotate() }],
					/**
					* Rotate Y
					* @see https://tailwindcss.com/docs/rotate
					*/
					"rotate-y": [{ "rotate-y": scaleRotate() }],
					/**
					* Rotate Z
					* @see https://tailwindcss.com/docs/rotate
					*/
					"rotate-z": [{ "rotate-z": scaleRotate() }],
					/**
					* Scale
					* @see https://tailwindcss.com/docs/scale
					*/
					scale: [{ scale: scaleScale() }],
					/**
					* Scale X
					* @see https://tailwindcss.com/docs/scale
					*/
					"scale-x": [{ "scale-x": scaleScale() }],
					/**
					* Scale Y
					* @see https://tailwindcss.com/docs/scale
					*/
					"scale-y": [{ "scale-y": scaleScale() }],
					/**
					* Scale Z
					* @see https://tailwindcss.com/docs/scale
					*/
					"scale-z": [{ "scale-z": scaleScale() }],
					/**
					* Scale 3D
					* @see https://tailwindcss.com/docs/scale
					*/
					"scale-3d": ["scale-3d"],
					/**
					* Skew
					* @see https://tailwindcss.com/docs/skew
					*/
					skew: [{ skew: scaleSkew() }],
					/**
					* Skew X
					* @see https://tailwindcss.com/docs/skew
					*/
					"skew-x": [{ "skew-x": scaleSkew() }],
					/**
					* Skew Y
					* @see https://tailwindcss.com/docs/skew
					*/
					"skew-y": [{ "skew-y": scaleSkew() }],
					/**
					* Transform
					* @see https://tailwindcss.com/docs/transform
					*/
					transform: [{ transform: [
						isArbitraryVariable,
						isArbitraryValue,
						"",
						"none",
						"gpu",
						"cpu"
					] }],
					/**
					* Transform Origin
					* @see https://tailwindcss.com/docs/transform-origin
					*/
					"transform-origin": [{ origin: scalePositionWithArbitrary() }],
					/**
					* Transform Style
					* @see https://tailwindcss.com/docs/transform-style
					*/
					"transform-style": [{ transform: ["3d", "flat"] }],
					/**
					* Translate
					* @see https://tailwindcss.com/docs/translate
					*/
					translate: [{ translate: scaleTranslate() }],
					/**
					* Translate X
					* @see https://tailwindcss.com/docs/translate
					*/
					"translate-x": [{ "translate-x": scaleTranslate() }],
					/**
					* Translate Y
					* @see https://tailwindcss.com/docs/translate
					*/
					"translate-y": [{ "translate-y": scaleTranslate() }],
					/**
					* Translate Z
					* @see https://tailwindcss.com/docs/translate
					*/
					"translate-z": [{ "translate-z": scaleTranslate() }],
					/**
					* Translate None
					* @see https://tailwindcss.com/docs/translate
					*/
					"translate-none": ["translate-none"],
					/**
					* Zoom
					* @see https://tailwindcss.com/docs/zoom
					*/
					zoom: [{ zoom: [
						isInteger,
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Accent Color
					* @see https://tailwindcss.com/docs/accent-color
					*/
					accent: [{ accent: scaleColor() }],
					/**
					* Appearance
					* @see https://tailwindcss.com/docs/appearance
					*/
					appearance: [{ appearance: ["none", "auto"] }],
					/**
					* Caret Color
					* @see https://tailwindcss.com/docs/just-in-time-mode#caret-color-utilities
					*/
					"caret-color": [{ caret: scaleColor() }],
					/**
					* Color Scheme
					* @see https://tailwindcss.com/docs/color-scheme
					*/
					"color-scheme": [{ scheme: [
						"normal",
						"dark",
						"light",
						"light-dark",
						"only-dark",
						"only-light"
					] }],
					/**
					* Cursor
					* @see https://tailwindcss.com/docs/cursor
					*/
					cursor: [{ cursor: [
						"auto",
						"default",
						"pointer",
						"wait",
						"text",
						"move",
						"help",
						"not-allowed",
						"none",
						"context-menu",
						"progress",
						"cell",
						"crosshair",
						"vertical-text",
						"alias",
						"copy",
						"no-drop",
						"grab",
						"grabbing",
						"all-scroll",
						"col-resize",
						"row-resize",
						"n-resize",
						"e-resize",
						"s-resize",
						"w-resize",
						"ne-resize",
						"nw-resize",
						"se-resize",
						"sw-resize",
						"ew-resize",
						"ns-resize",
						"nesw-resize",
						"nwse-resize",
						"zoom-in",
						"zoom-out",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Field Sizing
					* @see https://tailwindcss.com/docs/field-sizing
					*/
					"field-sizing": [{ "field-sizing": ["fixed", "content"] }],
					/**
					* Pointer Events
					* @see https://tailwindcss.com/docs/pointer-events
					*/
					"pointer-events": [{ "pointer-events": ["auto", "none"] }],
					/**
					* Resize
					* @see https://tailwindcss.com/docs/resize
					*/
					resize: [{ resize: [
						"none",
						"",
						"y",
						"x"
					] }],
					/**
					* Scroll Behavior
					* @see https://tailwindcss.com/docs/scroll-behavior
					*/
					"scroll-behavior": [{ scroll: ["auto", "smooth"] }],
					/**
					* Scrollbar Thumb Color
					* @see https://tailwindcss.com/docs/scrollbar-color
					*/
					"scrollbar-thumb-color": [{ "scrollbar-thumb": scaleColor() }],
					/**
					* Scrollbar Track Color
					* @see https://tailwindcss.com/docs/scrollbar-color
					*/
					"scrollbar-track-color": [{ "scrollbar-track": scaleColor() }],
					/**
					* Scrollbar Gutter
					* @see https://tailwindcss.com/docs/scrollbar-gutter
					*/
					"scrollbar-gutter": [{ "scrollbar-gutter": [
						"auto",
						"stable",
						"both"
					] }],
					/**
					* Scrollbar Width
					* @see https://tailwindcss.com/docs/scrollbar-width
					*/
					"scrollbar-w": [{ scrollbar: [
						"auto",
						"thin",
						"none"
					] }],
					/**
					* Scroll Margin
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-m": [{ "scroll-m": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Inline
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-mx": [{ "scroll-mx": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Block
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-my": [{ "scroll-my": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Inline Start
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-ms": [{ "scroll-ms": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Inline End
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-me": [{ "scroll-me": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Block Start
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-mbs": [{ "scroll-mbs": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Block End
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-mbe": [{ "scroll-mbe": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Top
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-mt": [{ "scroll-mt": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Right
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-mr": [{ "scroll-mr": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Bottom
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-mb": [{ "scroll-mb": scaleUnambiguousSpacing() }],
					/**
					* Scroll Margin Left
					* @see https://tailwindcss.com/docs/scroll-margin
					*/
					"scroll-ml": [{ "scroll-ml": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-p": [{ "scroll-p": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Inline
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-px": [{ "scroll-px": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Block
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-py": [{ "scroll-py": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Inline Start
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-ps": [{ "scroll-ps": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Inline End
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-pe": [{ "scroll-pe": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Block Start
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-pbs": [{ "scroll-pbs": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Block End
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-pbe": [{ "scroll-pbe": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Top
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-pt": [{ "scroll-pt": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Right
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-pr": [{ "scroll-pr": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Bottom
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-pb": [{ "scroll-pb": scaleUnambiguousSpacing() }],
					/**
					* Scroll Padding Left
					* @see https://tailwindcss.com/docs/scroll-padding
					*/
					"scroll-pl": [{ "scroll-pl": scaleUnambiguousSpacing() }],
					/**
					* Scroll Snap Align
					* @see https://tailwindcss.com/docs/scroll-snap-align
					*/
					"snap-align": [{ snap: [
						"start",
						"end",
						"center",
						"align-none"
					] }],
					/**
					* Scroll Snap Stop
					* @see https://tailwindcss.com/docs/scroll-snap-stop
					*/
					"snap-stop": [{ snap: ["normal", "always"] }],
					/**
					* Scroll Snap Type
					* @see https://tailwindcss.com/docs/scroll-snap-type
					*/
					"snap-type": [{ snap: [
						"none",
						"x",
						"y",
						"both"
					] }],
					/**
					* Scroll Snap Type Strictness
					* @see https://tailwindcss.com/docs/scroll-snap-type
					*/
					"snap-strictness": [{ snap: ["mandatory", "proximity"] }],
					/**
					* Touch Action
					* @see https://tailwindcss.com/docs/touch-action
					*/
					touch: [{ touch: [
						"auto",
						"none",
						"manipulation"
					] }],
					/**
					* Touch Action X
					* @see https://tailwindcss.com/docs/touch-action
					*/
					"touch-x": [{ "touch-pan": [
						"x",
						"left",
						"right"
					] }],
					/**
					* Touch Action Y
					* @see https://tailwindcss.com/docs/touch-action
					*/
					"touch-y": [{ "touch-pan": [
						"y",
						"up",
						"down"
					] }],
					/**
					* Touch Action Pinch Zoom
					* @see https://tailwindcss.com/docs/touch-action
					*/
					"touch-pz": ["touch-pinch-zoom"],
					/**
					* User Select
					* @see https://tailwindcss.com/docs/user-select
					*/
					select: [{ select: [
						"none",
						"text",
						"all",
						"auto"
					] }],
					/**
					* Will Change
					* @see https://tailwindcss.com/docs/will-change
					*/
					"will-change": [{ "will-change": [
						"auto",
						"scroll",
						"contents",
						"transform",
						isArbitraryVariable,
						isArbitraryValue
					] }],
					/**
					* Fill
					* @see https://tailwindcss.com/docs/fill
					*/
					fill: [{ fill: ["none", ...scaleColor()] }],
					/**
					* Stroke Width
					* @see https://tailwindcss.com/docs/stroke-width
					*/
					"stroke-w": [{ stroke: [
						isNumber,
						isArbitraryVariableLength,
						isArbitraryLength,
						isArbitraryNumber
					] }],
					/**
					* Stroke
					* @see https://tailwindcss.com/docs/stroke
					*/
					stroke: [{ stroke: ["none", ...scaleColor()] }],
					/**
					* Forced Color Adjust
					* @see https://tailwindcss.com/docs/forced-color-adjust
					*/
					"forced-color-adjust": [{ "forced-color-adjust": ["auto", "none"] }]
				},
				conflictingClassGroups: {
					"container-named": ["container-type"],
					overflow: ["overflow-x", "overflow-y"],
					overscroll: ["overscroll-x", "overscroll-y"],
					inset: [
						"inset-x",
						"inset-y",
						"inset-bs",
						"inset-be",
						"start",
						"end",
						"top",
						"right",
						"bottom",
						"left"
					],
					"inset-x": ["right", "left"],
					"inset-y": ["top", "bottom"],
					flex: [
						"basis",
						"grow",
						"shrink"
					],
					gap: ["gap-x", "gap-y"],
					p: [
						"px",
						"py",
						"ps",
						"pe",
						"pbs",
						"pbe",
						"pt",
						"pr",
						"pb",
						"pl"
					],
					px: ["pr", "pl"],
					py: ["pt", "pb"],
					m: [
						"mx",
						"my",
						"ms",
						"me",
						"mbs",
						"mbe",
						"mt",
						"mr",
						"mb",
						"ml"
					],
					mx: ["mr", "ml"],
					my: ["mt", "mb"],
					size: ["w", "h"],
					"font-size": ["leading"],
					"fvn-normal": [
						"fvn-ordinal",
						"fvn-slashed-zero",
						"fvn-figure",
						"fvn-spacing",
						"fvn-fraction"
					],
					"fvn-ordinal": ["fvn-normal"],
					"fvn-slashed-zero": ["fvn-normal"],
					"fvn-figure": ["fvn-normal"],
					"fvn-spacing": ["fvn-normal"],
					"fvn-fraction": ["fvn-normal"],
					"line-clamp": ["display", "overflow"],
					rounded: [
						"rounded-s",
						"rounded-e",
						"rounded-t",
						"rounded-r",
						"rounded-b",
						"rounded-l",
						"rounded-ss",
						"rounded-se",
						"rounded-ee",
						"rounded-es",
						"rounded-tl",
						"rounded-tr",
						"rounded-br",
						"rounded-bl"
					],
					"rounded-s": ["rounded-ss", "rounded-es"],
					"rounded-e": ["rounded-se", "rounded-ee"],
					"rounded-t": ["rounded-tl", "rounded-tr"],
					"rounded-r": ["rounded-tr", "rounded-br"],
					"rounded-b": ["rounded-br", "rounded-bl"],
					"rounded-l": ["rounded-tl", "rounded-bl"],
					"border-spacing": ["border-spacing-x", "border-spacing-y"],
					"border-w": [
						"border-w-x",
						"border-w-y",
						"border-w-s",
						"border-w-e",
						"border-w-bs",
						"border-w-be",
						"border-w-t",
						"border-w-r",
						"border-w-b",
						"border-w-l"
					],
					"border-w-x": ["border-w-r", "border-w-l"],
					"border-w-y": ["border-w-t", "border-w-b"],
					"border-color": [
						"border-color-x",
						"border-color-y",
						"border-color-s",
						"border-color-e",
						"border-color-bs",
						"border-color-be",
						"border-color-t",
						"border-color-r",
						"border-color-b",
						"border-color-l"
					],
					"border-color-x": ["border-color-r", "border-color-l"],
					"border-color-y": ["border-color-t", "border-color-b"],
					translate: [
						"translate-x",
						"translate-y",
						"translate-none"
					],
					"translate-none": [
						"translate",
						"translate-x",
						"translate-y",
						"translate-z"
					],
					"scroll-m": [
						"scroll-mx",
						"scroll-my",
						"scroll-ms",
						"scroll-me",
						"scroll-mbs",
						"scroll-mbe",
						"scroll-mt",
						"scroll-mr",
						"scroll-mb",
						"scroll-ml"
					],
					"scroll-mx": ["scroll-mr", "scroll-ml"],
					"scroll-my": ["scroll-mt", "scroll-mb"],
					"scroll-p": [
						"scroll-px",
						"scroll-py",
						"scroll-ps",
						"scroll-pe",
						"scroll-pbs",
						"scroll-pbe",
						"scroll-pt",
						"scroll-pr",
						"scroll-pb",
						"scroll-pl"
					],
					"scroll-px": ["scroll-pr", "scroll-pl"],
					"scroll-py": ["scroll-pt", "scroll-pb"],
					touch: [
						"touch-x",
						"touch-y",
						"touch-pz"
					],
					"touch-x": ["touch"],
					"touch-y": ["touch"],
					"touch-pz": ["touch"]
				},
				conflictingClassGroupModifiers: { "font-size": ["leading"] },
				postfixLookupClassGroups: ["container-type"],
				orderSensitiveModifiers: [
					"*",
					"**",
					"after",
					"backdrop",
					"before",
					"details-content",
					"file",
					"first-letter",
					"first-line",
					"marker",
					"placeholder",
					"selection"
				]
			};
		};
		const twMerge = /*#__PURE__*/ createTailwindMerge(getDefaultConfig);
		//#endregion
		//#region ../ui/src/lib/utils.ts
		function cn(...inputs) {
			return twMerge(clsx(inputs));
		}
		//#endregion
		//#region ../../node_modules/.pnpm/@remixicon+react@4.9.0_react@18.3.1/node_modules/@remixicon/react/index.mjs
		/**
		* @remixicon/react v4.1.0 - Remix Icon License 1.0
		*/
		const f1 = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M11.9999 13.1714L16.9497 8.22168L18.3639 9.63589L11.9999 15.9999L5.63599 9.63589L7.0502 8.22168L11.9999 13.1714Z" }));
		const s7 = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M9.9997 15.1709L19.1921 5.97852L20.6063 7.39273L9.9997 17.9993L3.63574 11.6354L5.04996 10.2212L9.9997 15.1709Z" }));
		const A7 = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22ZM12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20ZM12 10.5858L14.8284 7.75736L16.2426 9.17157L13.4142 12L16.2426 14.8284L14.8284 16.2426L12 13.4142L9.17157 16.2426L7.75736 14.8284L10.5858 12L7.75736 9.17157L9.17157 7.75736L12 10.5858Z" }));
		const P7 = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M11.9997 10.5865L16.9495 5.63672L18.3637 7.05093L13.4139 12.0007L18.3637 16.9504L16.9495 18.3646L11.9997 13.4149L7.04996 18.3646L5.63574 16.9504L10.5855 12.0007L5.63574 7.05093L7.04996 5.63672L11.9997 10.5865Z" }));
		const Rt = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22ZM12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20ZM11 15H13V17H11V15ZM11 7H13V13H11V7Z" }));
		const Yn = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M21 3C21.5523 3 22 3.44772 22 4V20C22 20.5523 21.5523 21 21 21H3C2.44772 21 2 20.5523 2 20V4C2 3.44772 2.44772 3 3 3H21ZM15 5H4V19H15V5ZM20 5H17V19H20V5Z" }));
		const hg = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M21 3C21.5523 3 22 3.44772 22 4V11H20V5H4V19H10V21H3C2.44772 21 2 20.5523 2 20V4C2 3.44772 2.44772 3 3 3H21ZM21 13C21.5523 13 22 13.4477 22 14V20C22 20.5523 21.5523 21 21 21H13C12.4477 21 12 20.5523 12 20V14C12 13.4477 12.4477 13 13 13H21ZM20 15H14V19H20V15ZM6.70711 6.29289L8.95689 8.54289L11 6.5V12H5.5L7.54289 9.95689L5.29289 7.70711L6.70711 6.29289Z" }));
		const SM = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M22.3126 10.1753L20.8984 11.5895L20.1913 10.8824L15.9486 15.125L15.2415 18.6606L13.8273 20.0748L9.58466 15.8321L4.63492 20.7819L3.2207 19.3677L8.17045 14.4179L3.92781 10.1753L5.34202 8.76107L8.87756 8.05396L13.1202 3.81132L12.4131 3.10422L13.8273 1.69L22.3126 10.1753Z" }));
		const LN = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22ZM12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20ZM9 9H15V15H9V9Z" }));
		const Tz = ({ color: C = "currentColor", size: e = 24, className: l, ...i }) => react.default.createElement("svg", {
			viewBox: "0 0 24 24",
			xmlns: "http://www.w3.org/2000/svg",
			width: e,
			height: e,
			fill: C,
			...i,
			className: "remixicon " + (l || "")
		}, react.default.createElement("path", { d: "M21 3C21.5523 3 22 3.44772 22 4V20C22 20.5523 21.5523 21 21 21H3C2.44772 21 2 20.5523 2 20V4C2 3.44772 2.44772 3 3 3H21ZM20 11H4V19H20V11ZM20 5H4V9H20V5ZM11 6V8H9V6H11ZM7 6V8H5V6H7Z" }));
		//#endregion
		//#region \0bsk-css:/home/runner/work/BrowserSkill/BrowserSkill/packages/dsh-plugin-browserskill/src/client/ObservationOverlay.module.css.mjs
		const css = ".E3eAtG_card{z-index:40;pointer-events:auto;background:var(--card);border:1px solid var(--border);border-radius:var(--radius);color:var(--card-foreground);flex-direction:column;display:flex;position:fixed;overflow:hidden;box-shadow:0 8px 28px #0000002e}.E3eAtG_body{background:var(--card);height:100%;min-height:0;color:var(--card-foreground);flex-direction:column;font-size:12px;display:flex}.E3eAtG_sidebar-tab{flex-direction:column;height:100%;min-height:0;display:flex}.E3eAtG_sidebar-tab>*{flex:1;min-height:0}.E3eAtG_brand-icon{border-radius:22%;display:block}.E3eAtG_tab-title{align-items:center;gap:6px;display:inline-flex}.E3eAtG_floating-notice{height:100%;color:var(--muted-foreground);flex-direction:column;justify-content:center;align-items:center;gap:12px;padding:16px;font-size:12px;display:flex}.E3eAtG_floating-notice button{border:1px solid var(--border);border-radius:var(--radius);background:var(--card);color:var(--card-foreground);cursor:pointer;padding:6px 12px}.E3eAtG_header{user-select:none;touch-action:none;border-bottom:1px solid var(--border);align-items:center;gap:6px;padding:6px 8px;display:flex}.E3eAtG_header[data-draggable]{cursor:move}.E3eAtG_sidebar-tab .E3eAtG_header{border-bottom:none}.E3eAtG_status-text{text-overflow:ellipsis;white-space:nowrap;flex:1;min-width:0;font-weight:500;overflow:hidden}.E3eAtG_icon-button{border-radius:calc(var(--radius) - 6px);font:inherit;color:var(--muted-foreground);cursor:pointer;background:0 0;border:none;flex:none;align-items:center;padding:2px;display:inline-flex}.E3eAtG_icon-button:hover{color:var(--foreground);background:var(--accent)}.E3eAtG_stage{background:var(--background);flex:1;justify-content:center;align-items:center;min-height:0;display:flex;position:relative;overflow:hidden}.E3eAtG_thumb{object-fit:contain;max-width:100%;max-height:100%;animation:.24s ease-out E3eAtG_bsk-obs-fade-in}@keyframes E3eAtG_bsk-obs-fade-in{0%{opacity:0}to{opacity:1}}.E3eAtG_placeholder{color:var(--muted-foreground);font-size:12px}.E3eAtG_badge{cursor:pointer;color:oklch(70% .19 60);background:var(--card);border:1px solid oklch(70% .19 60);border-radius:50%;justify-content:center;align-items:center;width:16px;height:16px;padding:0;display:inline-flex;position:absolute;top:6px;right:6px}.E3eAtG_actions{z-index:3;border-top:1px solid var(--border);justify-content:space-between;align-items:center;gap:4px;min-height:32px;padding:4px 8px;display:flex;position:relative}.E3eAtG_actions-group{align-items:center;gap:4px;display:inline-flex}.E3eAtG_tool-wrap{display:inline-flex;position:relative}.E3eAtG_tool-button{border-radius:calc(var(--radius) - 4px);width:28px;height:28px;color:var(--muted-foreground);cursor:pointer;background:0 0;border:none;flex:none;justify-content:center;align-items:center;padding:0;display:inline-flex}.E3eAtG_tool-button:disabled{opacity:.4;cursor:default}.E3eAtG_tool-button:focus-visible{outline:2px solid var(--ring);outline-offset:2px}.E3eAtG_tool-danger,.E3eAtG_tool-stop{color:var(--destructive)}.E3eAtG_tool-wrap:hover .E3eAtG_tool-button:not(:disabled){color:var(--foreground);background:var(--accent)}.E3eAtG_tool-wrap:hover .E3eAtG_tool-danger:not(:disabled){color:var(--destructive);background:color-mix(in oklch, var(--destructive) 18%, var(--card))}.E3eAtG_tool-wrap:hover .E3eAtG_tool-button:disabled{background:var(--accent)}.E3eAtG_tool-button.E3eAtG_tool-stop-armed,.E3eAtG_tool-wrap:hover .E3eAtG_tool-button.E3eAtG_tool-stop-armed{color:var(--destructive-foreground);background:var(--destructive)}.E3eAtG_hint{border-radius:calc(var(--radius) - 4px);background:var(--card);border:1px solid var(--border);width:max-content;max-width:220px;color:var(--muted-foreground);z-index:41;padding:4px 6px;font-size:11px;line-height:1.35;position:absolute;bottom:calc(100% + 4px);left:0;box-shadow:0 4px 14px #00000029}.E3eAtG_hint[data-align=end]{left:auto;right:0}.E3eAtG_resize-handle{touch-action:none;z-index:4;width:16px;height:16px;position:absolute}.E3eAtG_resize-handle[data-corner=nw]{cursor:nwse-resize;top:0;left:0}.E3eAtG_resize-handle[data-corner=ne]{cursor:nesw-resize;top:0;right:0}.E3eAtG_resize-handle[data-corner=sw]{cursor:nesw-resize;bottom:0;left:0}.E3eAtG_resize-handle[data-corner=se]{cursor:nwse-resize;bottom:0;right:0}.E3eAtG_capsule{z-index:40;pointer-events:auto;border:1px solid var(--border);background:var(--card);color:var(--muted-foreground);cursor:pointer;border-radius:999px;align-items:center;gap:6px;padding:6px 12px;font-size:12px;display:inline-flex;position:fixed;top:64px;right:16px;box-shadow:0 4px 14px #00000029}.E3eAtG_capsule:hover{background:var(--accent)}.E3eAtG_strip{border-top:1px solid var(--border);gap:6px;padding:4px 8px;display:flex;overflow-x:auto}.E3eAtG_strip-item{border-radius:calc(var(--radius) - 6px);border:1px solid var(--border);flex:none;position:relative}.E3eAtG_strip-item[data-focused]{border-color:var(--primary)}.E3eAtG_strip-item[data-state=error]{border-color:var(--destructive)}.E3eAtG_strip-item[data-state=dead]{opacity:.45}.E3eAtG_strip-main{border-radius:calc(var(--radius) - 6px);font:inherit;color:var(--muted-foreground);cursor:pointer;background:0 0;border:none;align-items:center;gap:4px;padding:3px 6px;font-size:11px;display:flex}.E3eAtG_strip-main:hover{background:var(--accent)}.E3eAtG_strip-thumb{background:var(--background);border-radius:3px;width:28px;height:20px;display:inline-flex;overflow:hidden}.E3eAtG_strip-thumb img{object-fit:cover;width:100%;height:100%}.E3eAtG_strip-thumb-empty{width:100%;height:100%}.E3eAtG_strip-id{text-overflow:ellipsis;max-width:64px;overflow:hidden}.E3eAtG_pin-badge{color:var(--primary)}.E3eAtG_strip-interrupt{border:1px solid var(--destructive);background:var(--card);width:16px;height:16px;color:var(--destructive);cursor:pointer;border-radius:50%;justify-content:center;align-items:center;padding:0;display:inline-flex;position:absolute;top:-7px;right:-7px}.E3eAtG_strip-interrupt:disabled{opacity:.4;cursor:default}.E3eAtG_icon-button:focus-visible,.E3eAtG_capsule:focus-visible,.E3eAtG_strip-main:focus-visible,.E3eAtG_strip-interrupt:focus-visible{outline:2px solid var(--ring);outline-offset:2px}@media (prefers-reduced-motion:reduce){.E3eAtG_thumb{animation:none}}";
		const tagId = "@wxg-prc-cpg/browser-skill-dsh-plugin/ObservationOverlay.module.css";
		if (typeof document !== "undefined" && document.querySelector("style[data-plugin-css=" + JSON.stringify(tagId) + "]") === null) {
			const tag = document.createElement("style");
			tag.dataset.plugin = "@wxg-prc-cpg/browser-skill-dsh-plugin";
			tag.dataset.pluginCss = tagId;
			tag.textContent = css;
			document.head.appendChild(tag);
		}
		var ObservationOverlay_module_css_default = {
			"pin-badge": "E3eAtG_pin-badge",
			"icon-button": "E3eAtG_icon-button",
			"strip-main": "E3eAtG_strip-main",
			"strip-item": "E3eAtG_strip-item",
			"strip-thumb-empty": "E3eAtG_strip-thumb-empty",
			"thumb": "E3eAtG_thumb",
			"strip-thumb": "E3eAtG_strip-thumb",
			"badge": "E3eAtG_badge",
			"strip": "E3eAtG_strip",
			"strip-id": "E3eAtG_strip-id",
			"status-text": "E3eAtG_status-text",
			"floating-notice": "E3eAtG_floating-notice",
			"strip-interrupt": "E3eAtG_strip-interrupt",
			"brand-icon": "E3eAtG_brand-icon",
			"card": "E3eAtG_card",
			"bsk-obs-fade-in": "E3eAtG_bsk-obs-fade-in",
			"tab-title": "E3eAtG_tab-title",
			"stage": "E3eAtG_stage",
			"placeholder": "E3eAtG_placeholder",
			"tool-stop": "E3eAtG_tool-stop",
			"tool-stop-armed": "E3eAtG_tool-stop-armed",
			"tool-wrap": "E3eAtG_tool-wrap",
			"header": "E3eAtG_header",
			"actions": "E3eAtG_actions",
			"hint": "E3eAtG_hint",
			"sidebar-tab": "E3eAtG_sidebar-tab",
			"resize-handle": "E3eAtG_resize-handle",
			"capsule": "E3eAtG_capsule",
			"actions-group": "E3eAtG_actions-group",
			"tool-danger": "E3eAtG_tool-danger",
			"tool-button": "E3eAtG_tool-button",
			"body": "E3eAtG_body"
		};
		//#endregion
		//#region src/client/observation-view.ts
		/**
		* Shared view logic for the observation carriers (the floating overlay card
		* and the native sidebar tab): the store-backed view model (snapshot, focus
		* pinning, elapsed ticker) and the Document PiP pop-out. Extracted from
		* ObservationOverlay so both carriers run the same focus/interrupt behavior
		* without duplicating hooks.
		*/
		/**
		* Auto-follow focus: the most recently touched session, but never yank focus
		* to a session whose latest action failed (errors flag the strip item, they
		* do not steal the stage) or one already reported dead.
		*/
		function focusOf(sessions) {
			if (sessions.length === 0) return void 0;
			const byRecency = [...sessions].sort((a, b) => b.since - a.since);
			return byRecency.find((s) => s.lastError === void 0 && s.dead !== true) ?? byRecency[0];
		}
		function statusOf(obs) {
			if (obs.lastError !== void 0 && obs.action === "idle") return "error";
			return obs.action === "idle" ? "idle" : "active";
		}
		function pipApi() {
			if (typeof window === "undefined") return void 0;
			return window.documentPictureInPicture;
		}
		/** Clone the host document's style/link nodes into a PiP window. */
		function cloneStylesInto(pipWindow) {
			for (const node of document.querySelectorAll("link[rel=\"stylesheet\"], style")) pipWindow.document.head.appendChild(node.cloneNode(true));
		}
		/**
		* Whether one session is visible on a surface scoped to one DSH
		* conversation: sessions started by that conversation or its descendants
		* (the lineage ancestors ride along on the entry). Untracked sessions (no
		* owner recorded) are hidden from scoped surfaces but stay in global views.
		*/
		function visibleToScope(obs, scopeId) {
			return obs.dshSessionIds?.includes(scopeId) === true;
		}
		/**
		* The store-backed observation view model. Holds the feed for the component
		* lifetime (refcounted — overlapping carriers never kill each other's
		* stream). `scopeId` narrows the view to one DSH conversation's sessions
		* (the native sidebar tab); undefined keeps the global view (floating
		* card, PiP).
		*/
		function useObservationView(store, scopeId) {
			const snapshot = (0, react.useSyncExternalStore)(store.subscribe, store.getSnapshot);
			(0, react.useEffect)(() => {
				store.acquire();
				return () => store.release();
			}, [store]);
			const scopedSnapshot = scopeId === void 0 ? snapshot : {
				...snapshot,
				sessions: snapshot.sessions.filter((s) => visibleToScope(s, scopeId))
			};
			const [pinnedId, setPinnedId] = (0, react.useState)(null);
			const pinned = pinnedId !== null ? scopedSnapshot.sessions.find((s) => s.sessionId === pinnedId) : void 0;
			const focus = pinned ?? focusOf(scopedSnapshot.sessions);
			const onTogglePin = (0, react.useCallback)((sessionId) => {
				setPinnedId((current) => current === sessionId ? null : sessionId);
			}, []);
			const anyActive = scopedSnapshot.sessions.some((s) => s.action !== "idle");
			const [now, setNow] = (0, react.useState)(() => Date.now());
			(0, react.useEffect)(() => {
				if (!anyActive) return;
				const timer = setInterval(() => setNow(Date.now()), 1e3);
				return () => clearInterval(timer);
			}, [anyActive]);
			return {
				snapshot: scopedSnapshot,
				focus,
				pinnedId: pinned !== void 0 ? pinnedId : null,
				onTogglePin,
				now
			};
		}
		/** A mounted metadata watcher is not necessarily a visible screenshot viewer. */
		function useThumbnailObservation(store, enabled) {
			const ref = (0, react.useRef)(null);
			(0, react.useEffect)(() => {
				const element = ref.current;
				if (!enabled || element === null) return;
				const doc = element.ownerDocument;
				const Observer = doc.defaultView?.IntersectionObserver;
				let active = true;
				let intersecting = Observer === void 0;
				let release;
				const update = () => {
					if (!active) return;
					const visible = intersecting && doc.visibilityState !== "hidden";
					if (visible && release === void 0) release = store.watchThumbnails();
					else if (!visible && release !== void 0) {
						release();
						release = void 0;
					}
				};
				const observer = Observer === void 0 ? void 0 : new Observer((entries) => {
					intersecting = entries.some((entry) => entry.target === element && entry.isIntersecting);
					update();
				});
				observer?.observe(element);
				doc.addEventListener("visibilitychange", update);
				update();
				return () => {
					active = false;
					observer?.disconnect();
					doc.removeEventListener("visibilitychange", update);
					release?.();
				};
			}, [store, enabled]);
			return ref;
		}
		function usePip() {
			const [pipWindow, setPipWindow] = (0, react.useState)(null);
			const popOut = (0, react.useCallback)((size) => {
				const pip = pipApi();
				if (pip === void 0) return;
				pip.requestWindow(size).then((win) => {
					cloneStylesInto(win);
					win.addEventListener("pagehide", () => setPipWindow(null));
					setPipWindow(win);
				}).catch(() => {});
			}, []);
			(0, react.useEffect)(() => () => pipWindow?.close(), [pipWindow]);
			return {
				pipWindow,
				pipSupported: pipApi() !== void 0,
				popOut
			};
		}
		//#endregion
		//#region src/client/ObservationOverlay.tsx
		const asIcon = (component) => component;
		const IconStop = asIcon(LN);
		const IconPip = asIcon(hg);
		const IconDown = asIcon(f1);
		const IconClose = asIcon(P7);
		const IconWarn = asIcon(Rt);
		const IconPin = asIcon(SM);
		const IconCloseSession = asIcon(A7);
		const IconCheck = asIcon(s7);
		const IconSidebar = asIcon(Yn);
		const IconWindow = asIcon(Tz);
		const DEFAULT_SIZE = {
			w: 320,
			h: 240
		};
		const MIN_SIZE = {
			w: 240,
			h: 180
		};
		const EDGE_MARGIN = 16;
		/** Default dock: top-right, just under the shell's top bar (no spacing tokens exist in dsh 0.1). */
		const TOP_OFFSET = 64;
		function clampSize(size, viewport) {
			const maxW = viewport.w * .8;
			const maxH = viewport.h * .8;
			return {
				w: Math.min(Math.max(size.w, MIN_SIZE.w), maxW),
				h: Math.min(Math.max(size.h, MIN_SIZE.h), maxH)
			};
		}
		function clampPos(pos, size, viewport) {
			return {
				x: Math.min(Math.max(pos.x, 0), Math.max(0, viewport.w - size.w)),
				y: Math.min(Math.max(pos.y, 0), Math.max(0, viewport.h - size.h))
			};
		}
		/** Grow/shrink from one corner, keeping the opposite corner planted. */
		function applyResize(base, corner, dx, dy, viewport) {
			const size = clampSize({
				w: corner === "ne" || corner === "se" ? base.w + dx : base.w - dx,
				h: corner === "sw" || corner === "se" ? base.h + dy : base.h - dy
			}, viewport);
			return {
				pos: clampPos({
					x: corner === "nw" || corner === "sw" ? base.x + base.w - size.w : base.x,
					y: corner === "nw" || corner === "ne" ? base.y + base.h - size.h : base.y
				}, size, viewport),
				size
			};
		}
		const CORNER_LABEL = {
			nw: "top left",
			ne: "top right",
			sw: "bottom left",
			se: "bottom right"
		};
		function formatElapsed(sinceMs, nowMs) {
			const total = Math.max(0, Math.floor((nowMs - sinceMs) / 1e3));
			return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
		}
		/** Compact toolbar icon: no label, hover bubble for the name / semantics. */
		function IconAction(props) {
			const [open, setOpen] = (0, react.useState)(false);
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", {
				className: ObservationOverlay_module_css_default["tool-wrap"],
				onPointerEnter: () => setOpen(true),
				onPointerLeave: () => setOpen(false),
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
					type: "button",
					className: cn(ObservationOverlay_module_css_default["tool-button"], props.danger === true && ObservationOverlay_module_css_default["tool-danger"]),
					disabled: props.disabled,
					"aria-label": props.label,
					onClick: props.onClick,
					children: props.children
				}), open ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
					className: ObservationOverlay_module_css_default.hint,
					"data-align": props.align,
					role: "tooltip",
					children: props.hint
				}) : null]
			});
		}
		/** How long the stop button stays armed before the confirm click expires. */
		const STOP_ARM_MS = 3e3;
		/**
		* Stop-session button with a lightweight two-click confirm: the first click
		* arms the button (solid red, check icon, short expiry), the second executes
		* the stop. No dialog, no layout shift — and a stray single click can never
		* close an Agent Window.
		*/
		function StopSessionAction(props) {
			const { sessionId, onStop } = props;
			const [hover, setHover] = (0, react.useState)(false);
			const [armed, setArmed] = (0, react.useState)(false);
			const [stopping, setStopping] = (0, react.useState)(false);
			(0, react.useEffect)(() => {
				if (!armed) return;
				const timer = setTimeout(() => setArmed(false), STOP_ARM_MS);
				return () => clearTimeout(timer);
			}, [armed]);
			(0, react.useEffect)(() => {
				if (sessionId === void 0) setArmed(false);
			}, [sessionId]);
			const disabled = sessionId === void 0 || stopping;
			const label = stopping ? "Stopping session…" : armed ? `Confirm stop session ${sessionId ?? ""}` : `Stop session ${sessionId ?? ""}`;
			const hint = stopping ? "Closing the Agent Window…" : armed ? `Click again to stop session ${sessionId ?? ""} and close its Agent Window.` : `Stop session ${sessionId ?? ""} and close its Agent Window.`;
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", {
				className: ObservationOverlay_module_css_default["tool-wrap"],
				onPointerEnter: () => setHover(true),
				onPointerLeave: () => setHover(false),
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
					type: "button",
					className: cn(ObservationOverlay_module_css_default["tool-button"], ObservationOverlay_module_css_default["tool-stop"], armed && ObservationOverlay_module_css_default["tool-stop-armed"]),
					disabled,
					"aria-label": label,
					"aria-pressed": armed,
					"data-armed": armed || void 0,
					onClick: () => {
						if (sessionId === void 0 || stopping) return;
						if (!armed) {
							setArmed(true);
							return;
						}
						setArmed(false);
						setStopping(true);
						onStop(sessionId).finally(() => setStopping(false));
					},
					children: armed ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconCheck, { size: 16 }) : /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconCloseSession, { size: 16 })
				}), hover ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
					className: ObservationOverlay_module_css_default.hint,
					role: "tooltip",
					children: hint
				}) : null]
			});
		}
		/** Flat status dot, specced after the BSK popup's ConnectionStatusIndicator. */
		function StatusDot({ state }) {
			return /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
				className: cn("size-2 shrink-0 rounded-full ring-2 ring-background", state === "active" ? "bg-emerald-500" : state === "error" ? "bg-red-500" : state === "dead" || state === "reconnecting" ? "bg-amber-500" : "bg-muted-foreground/40"),
				"data-state": state,
				"aria-hidden": true
			});
		}
		/** One strip item: mini frame, id, status dot, hover interrupt, pin toggle. */
		function StripItem(props) {
			const { store, obs, pinned, focused, onTogglePin } = props;
			const [hover, setHover] = (0, react.useState)(false);
			const thumbId = obs.thumbnailAttachmentId;
			(0, react.useEffect)(() => {
				store.ensureThumbnail(thumbId);
			}, [store, thumbId]);
			const thumb = store.getSnapshot().displayFrames[obs.sessionId];
			const state = obs.dead === true ? "dead" : statusOf(obs);
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: ObservationOverlay_module_css_default["strip-item"],
				"data-state": state,
				"data-focused": focused || void 0,
				"data-pinned": pinned || void 0,
				onPointerEnter: () => setHover(true),
				onPointerLeave: () => setHover(false),
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("button", {
					type: "button",
					className: ObservationOverlay_module_css_default["strip-main"],
					"aria-label": `${pinned ? "Unpin" : "Pin"} session ${obs.sessionId}`,
					"aria-pressed": pinned,
					onClick: () => onTogglePin(obs.sessionId),
					children: [
						/* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
							className: ObservationOverlay_module_css_default["strip-thumb"],
							children: thumb?.url !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("img", {
								src: thumb.url,
								alt: ""
							}) : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", { className: ObservationOverlay_module_css_default["strip-thumb-empty"] })
						}),
						/* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
							className: ObservationOverlay_module_css_default["strip-id"],
							children: obs.sessionId
						}),
						/* @__PURE__ */ (0, react_jsx_runtime.jsx)(StatusDot, { state }),
						pinned ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconPin, {
							size: 9,
							className: ObservationOverlay_module_css_default["pin-badge"],
							"aria-label": "pinned"
						}) : null
					]
				}), hover && !pinned && obs.dead !== true ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
					type: "button",
					className: ObservationOverlay_module_css_default["strip-interrupt"],
					"aria-label": `Interrupt session ${obs.sessionId}`,
					disabled: obs.action === "idle",
					onClick: (event) => {
						event.stopPropagation();
						store.interrupt(obs.sessionId);
					},
					children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconStop, { size: 10 })
				}) : null]
			});
		}
		/** The floating card / PiP / sidebar-tab shared content. */
		function OverlayBody(props) {
			const { store, focus, sessions, available, reconnecting, pinnedId, onTogglePin, now, onPopOut, onCollapse, onClosePip, onUseFloating, onUseSidebar, visible = true, inPip, onHeaderPointerDown } = props;
			const [interrupting, setInterrupting] = (0, react.useState)(false);
			const viewRef = useThumbnailObservation(store, visible && sessions.length > 0);
			const thumbId = focus?.thumbnailAttachmentId;
			(0, react.useEffect)(() => {
				store.ensureThumbnail(thumbId);
			}, [store, thumbId]);
			const thumb = focus !== void 0 ? store.getSnapshot().displayFrames[focus.sessionId] : void 0;
			const displayUrl = thumb?.url;
			const canInterrupt = available && !interrupting && focus !== void 0 && focus.action !== "idle" && focus.dead !== true;
			const onInterrupt = () => {
				if (!canInterrupt || focus === void 0) return;
				setInterrupting(true);
				store.interrupt(focus.sessionId).finally(() => setInterrupting(false));
			};
			const statusText = reconnecting ? "reconnecting…" : !available ? "browser unavailable" : focus === void 0 ? "no session" : `${focus.sessionId} · ${focus.action === "idle" ? "idle" : focus.action} · ${formatElapsed(focus.since, now)}`;
			const state = reconnecting ? "reconnecting" : !available ? "error" : focus !== void 0 ? statusOf(focus) : "idle";
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				ref: viewRef,
				className: cn(ObservationOverlay_module_css_default.body, "bsk-obs"),
				"data-state": state,
				"data-in-pip": inPip || void 0,
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: ObservationOverlay_module_css_default.header,
						"data-testid": "obs-header",
						"data-draggable": onHeaderPointerDown !== void 0 || void 0,
						onPointerDown: onHeaderPointerDown,
						role: "presentation",
						children: [
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)(StatusDot, { state }),
							/* @__PURE__ */ (0, react_jsx_runtime.jsx)("span", {
								className: ObservationOverlay_module_css_default["status-text"],
								children: statusText
							}),
							onUseFloating !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconAction, {
								label: "Use floating view",
								hint: "Show the browser view in a floating window.",
								onClick: onUseFloating,
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconWindow, { size: 14 })
							}) : null,
							onUseSidebar !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconAction, {
								label: "Move to sidebar",
								hint: "Show the browser view in the sidebar.",
								onClick: onUseSidebar,
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconSidebar, { size: 14 })
							}) : null,
							onCollapse !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: ObservationOverlay_module_css_default["icon-button"],
								"aria-label": "Collapse",
								onClick: onCollapse,
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconDown, { size: 14 })
							}) : null,
							onClosePip !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
								type: "button",
								className: ObservationOverlay_module_css_default["icon-button"],
								"aria-label": "Close mini window",
								onClick: onClosePip,
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconClose, { size: 14 })
							}) : null
						]
					}),
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: ObservationOverlay_module_css_default.stage,
						children: [displayUrl !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("img", {
							className: ObservationOverlay_module_css_default.thumb,
							src: displayUrl,
							alt: `session ${focus?.sessionId ?? ""} view`
						}) : /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
							className: ObservationOverlay_module_css_default.placeholder,
							children: !available ? "last frame kept" : thumb?.status === "error" ? "frame unavailable" : "waiting for page"
						}), thumb?.status === "error" ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
							type: "button",
							className: ObservationOverlay_module_css_default.badge,
							"aria-label": "Retry thumbnail",
							title: "Frame unavailable — retry",
							onClick: () => store.retryThumbnail(thumbId),
							children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconWarn, { size: 12 })
						}) : null]
					}),
					sessions.length >= 2 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
						className: ObservationOverlay_module_css_default.strip,
						"data-testid": "obs-strip",
						role: "list",
						children: sessions.map((obs) => /* @__PURE__ */ (0, react_jsx_runtime.jsx)(StripItem, {
							store,
							obs,
							pinned: pinnedId === obs.sessionId,
							focused: focus?.sessionId === obs.sessionId,
							onTogglePin
						}, obs.sessionId))
					}) : null,
					/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
						className: ObservationOverlay_module_css_default.actions,
						children: [/* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
							className: ObservationOverlay_module_css_default["actions-group"],
							children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconAction, {
								label: interrupting ? "Interrupting…" : "Interrupt the current browser action",
								hint: "Stop the current browser action.",
								disabled: !canInterrupt,
								danger: true,
								onClick: onInterrupt,
								children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconStop, { size: 16 })
							}), /* @__PURE__ */ (0, react_jsx_runtime.jsx)(StopSessionAction, {
								sessionId: focus?.sessionId,
								onStop: (sessionId) => store.stopSession(sessionId)
							})]
						}), onPopOut !== void 0 ? /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconAction, {
							label: "Pop out into a mini window",
							hint: "Pop out into a mini window",
							align: "end",
							onClick: onPopOut,
							children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(IconPip, { size: 16 })
						}) : null]
					})
				]
			});
		}
		function ObservationOverlay({ store }) {
			const { snapshot, focus, pinnedId, onTogglePin, now } = useObservationView(store);
			const presentation = (0, react.useSyncExternalStore)(store.presentation.subscribe, store.presentation.getSnapshot);
			const [collapsed, setCollapsed] = (0, react.useState)(false);
			const [pos, setPos] = (0, react.useState)(null);
			const [size, setSize] = (0, react.useState)(DEFAULT_SIZE);
			const { pipWindow, pipSupported, popOut } = usePip();
			const dragRef = (0, react.useRef)(null);
			const viewport = () => ({
				w: window.innerWidth,
				h: window.innerHeight
			});
			const onPointerMove = (0, react.useCallback)((event) => {
				const drag = dragRef.current;
				if (drag === null) return;
				const dx = event.clientX - drag.startX;
				const dy = event.clientY - drag.startY;
				if (drag.kind === "move") setPos(clampPos({
					x: drag.base.x + dx,
					y: drag.base.y + dy
				}, {
					w: drag.base.w,
					h: drag.base.h
				}, viewport()));
				else if (drag.corner !== void 0) {
					const next = applyResize(drag.base, drag.corner, dx, dy, viewport());
					setPos(next.pos);
					setSize(next.size);
				}
			}, []);
			const onPointerUp = (0, react.useCallback)(() => {
				dragRef.current = null;
				document.removeEventListener("pointermove", onPointerMove);
				document.removeEventListener("pointerup", onPointerUp);
			}, [onPointerMove]);
			const cardOrigin = () => {
				const vp = viewport();
				return {
					x: pos?.x ?? vp.w - size.w - EDGE_MARGIN,
					y: pos?.y ?? TOP_OFFSET,
					w: size.w,
					h: size.h
				};
			};
			const beginMove = (event) => {
				event.preventDefault();
				const rect = event.currentTarget.closest("[data-obs-card]")?.getBoundingClientRect();
				const origin = cardOrigin();
				const base = {
					x: rect?.left ?? origin.x,
					y: rect?.top ?? origin.y,
					w: rect !== void 0 && rect.width > 0 ? rect.width : origin.w,
					h: rect !== void 0 && rect.height > 0 ? rect.height : origin.h
				};
				dragRef.current = {
					kind: "move",
					startX: event.clientX,
					startY: event.clientY,
					base
				};
				setPos({
					x: base.x,
					y: base.y
				});
				document.addEventListener("pointermove", onPointerMove);
				document.addEventListener("pointerup", onPointerUp);
			};
			const beginResize = (corner) => (event) => {
				event.preventDefault();
				event.stopPropagation();
				const rect = event.currentTarget.closest("[data-obs-card]")?.getBoundingClientRect();
				const origin = cardOrigin();
				const base = {
					x: rect?.left ?? origin.x,
					y: rect?.top ?? origin.y,
					w: rect !== void 0 && rect.width > 0 ? rect.width : origin.w,
					h: rect !== void 0 && rect.height > 0 ? rect.height : origin.h
				};
				dragRef.current = {
					kind: "resize",
					corner,
					startX: event.clientX,
					startY: event.clientY,
					base
				};
				setPos({
					x: base.x,
					y: base.y
				});
				document.addEventListener("pointermove", onPointerMove);
				document.addEventListener("pointerup", onPointerUp);
			};
			const body = /* @__PURE__ */ (0, react_jsx_runtime.jsx)(OverlayBody, {
				store,
				focus,
				sessions: snapshot.sessions,
				available: snapshot.available,
				reconnecting: snapshot.reconnecting,
				pinnedId,
				onTogglePin,
				now,
				inPip: pipWindow !== null,
				onPopOut: pipWindow === null && pipSupported ? () => popOut({
					width: size.w,
					height: size.h
				}) : void 0,
				onCollapse: pipWindow === null ? () => setCollapsed(true) : void 0,
				onClosePip: pipWindow !== null ? () => pipWindow.close() : void 0,
				onUseSidebar: presentation.sidebarAvailable ? () => {
					pipWindow?.close();
					store.presentation.showSidebar();
				} : void 0,
				onHeaderPointerDown: pipWindow === null ? beginMove : void 0
			});
			if (pipWindow !== null) return (0, react_dom.createPortal)(body, pipWindow.document.body);
			if (!presentation.floating) return null;
			if (snapshot.sessions.length === 0) return null;
			if (collapsed) {
				const state = snapshot.reconnecting ? "reconnecting" : focus !== void 0 ? statusOf(focus) : "idle";
				return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("button", {
					type: "button",
					className: cn(ObservationOverlay_module_css_default.capsule, "bsk-obs"),
					"data-state": state,
					"data-testid": "obs-capsule",
					"aria-label": "Expand browser observation overlay",
					onClick: () => setCollapsed(false),
					children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)(StatusDot, { state }), /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", {
						className: ObservationOverlay_module_css_default["capsule-text"],
						children: [
							snapshot.sessions.length,
							" session",
							snapshot.sessions.length === 1 ? "" : "s",
							snapshot.reconnecting ? " · reconnecting…" : focus !== void 0 && focus.action !== "idle" ? ` · ${focus.action} · ${formatElapsed(focus.since, now)}` : ""
						]
					})]
				});
			}
			const style = pos !== null ? {
				left: pos.x,
				top: pos.y,
				width: size.w,
				height: size.h
			} : {
				right: EDGE_MARGIN,
				top: TOP_OFFSET,
				width: size.w,
				height: size.h
			};
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: cn(ObservationOverlay_module_css_default.card, "bsk-obs"),
				style,
				"data-obs-card": true,
				"data-testid": "obs-card",
				children: [body, [
					"nw",
					"ne",
					"sw",
					"se"
				].map((corner) => /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
					className: ObservationOverlay_module_css_default["resize-handle"],
					"data-corner": corner,
					"data-testid": `obs-resize-${corner}`,
					"aria-label": `Resize overlay from the ${CORNER_LABEL[corner]}`,
					role: "separator",
					"aria-valuenow": size.w,
					"aria-valuetext": `${Math.round(size.w)} by ${Math.round(size.h)} pixels`,
					"aria-valuemin": MIN_SIZE.w,
					"aria-valuemax": Math.round(viewport().w * .8),
					tabIndex: corner === "se" ? 0 : -1,
					onPointerDown: beginResize(corner),
					onKeyDown: corner === "se" ? (event) => {
						const step = 16;
						const dx = event.key === "ArrowRight" ? step : event.key === "ArrowLeft" ? -16 : 0;
						const dy = event.key === "ArrowDown" ? step : event.key === "ArrowUp" ? -16 : 0;
						if (dx === 0 && dy === 0) return;
						event.preventDefault();
						const next = applyResize(cardOrigin(), "se", dx, dy, viewport());
						setPos(next.pos);
						setSize(next.size);
					} : void 0
				}, corner))]
			});
		}
		//#endregion
		//#region src/client/brand-icon.ts
		/**
		* The BrowserSkill product mark (apps/extension/assets/logo.png, downscaled
		* to 32px and inlined): the sidebar tab icon, so the tracking view reads as
		* BSK's own surface next to the host's other sidebar tabs.
		* Regenerate with: resize the source to 32x32 PNG and replace the payload.
		*/
		const BSK_LOGO_URL = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAJGklEQVR42m2X248c2V3HP79zTlV19/TcPOOZWY+vsdmQFdngTQJswgs3hQ15iCJslAey+8xDAIHgH+CBNxIpj0gkEXnAKyKQWITEJlKWXSssEMfJRmuMHa/W9njHMz3Tl+nprsv5/XiomlugpdN16lR1n9/1e75fAbAbeLlO3Pz2ly4vjO7+sRbF58oiP49FEREOPwaIYQjS3IvUy0fP66kAZoYJIM7SpPVY0uy14cyzXzn9u9+4c+MG/vp1otiNa16uvxr7X/+1L3bK3lcTyU9P80ilR/uKCGYHOxxd6s2kmdaGIVYb1qyY1e8HD63UUblstwirf9J95fW/sRvXvAD0v/6Z6/O68Xf5/pS8kigiTsTkuCfIwfzAxQN37chjaAyt594JIXE4gbKMhog6Nd/qthi7lVe6L3/nG7Jz4/fPJ9u3b4U4WSwiKog/EcsTLh+PSuO3GXYs/D44QnBgyniQs7OV0+8bOxPPM6c9l660VEyF0Nmrlp9/IYT+/S/PUJ4a5C46EW8HnjVhBKjXaHJfz9Xq95xzhMThHZR5xXBrwtPNnP6eRzvzhJWzrF1d5UI759//9iYra5lrt1ycDeXsaOf+n4Y4HX8+r9SciasLS/6P12LHoyF47wipYKpM9gqePpyyvVMxjBn+1DILH17l0rkZlhYcLetT3n/A5FGfTtvTCh4ibjpVI81/J0il62V1rNStLp6j4jowxwiJh2iMevtsbU3pjYQim6Nz9hJLH13i8mrGbJgSxjuUvQfEjT2GOw4Zddh0BakPtFPH/jRKqaCSrwetKi9mKIIYhyFuOg6TurCcF7ae5ty5t48/vcTiz3+Ei+szLC06WnGA7TykfNCnmObk5nAtj+53yDcTZteMwZOChbkMVa3/14DCJBBBFBxyopndUUBqwwxu/XjAJ778Bc4ul+R3fgyj9yg/GDMpFZxHgoe0hQ9QDRzFI48zqHzJeKhcvNKiLAyiQ1BwgsOcqNZ9Xo96rgaVgmqdiBACH740y/1/+j4b7w1x80tUkwLM4VptJAQMcN6o+p7iYQIqhBQmsUIrR7cd0NI12ODAwJnWTpvZoRGqdRrS1CECeR7Z2NzHRBjeecgbX/kut39UUlx5ATe3gFUlAM5Tb/4oHCJl0oadUcFsK8E5sFh7ZaqgEFBr3KyhxAySxDMplDv3BuztlmhhZC6QqONDrXkW28Kbf/1d7A9+i6vPr7N/e5vQDZQ7jvxxUqdPwFRwHaP3sGR1boZYGabWwHdd7MHMMBUMh5mRJp6tnZx33tllJbS53J1jZj6BKpKPpkhZ8e5mj3Nf/FV+8ZOnmNz6Pr6TUvUcxeOAc0cN6xwUoWIyUhZXMqqq6bKmrQ0hWBQzdXX+nGOwV3LnJ30+trjE8mxGUUVG/Qn7owkt7/hpb5fwmat84qXz5P95EwmOqh8oNkKTSkFqICTJYDgpCOppZYGyjIfgdoApQRScCtEgDZ5b93dxuWd3OuWD8T5pqbSLyEyWcHdrh+S3r/Kpz15i/z/eQhIh9hOKjQRTyBYgFhAndSpDW+gNC+ZbGR6jjM3BVsNsXQNqhqpiOPIicuXcLFvdKT7xtNXTG+X0Bopt9Wi9dJUXX7rI5O23kNQRB4F8I0CEzgr0/B7t2YB/2iJOQDrK8HHJ+dkZqqI+JfUAYRXMGc6pM1MBBa2Ubttz5ews51c6XFjr8OJHT5N0HPIbz/Ppz11h/+03IXVUg4T8ccAidFaNp7LHzdd2uXdvj866gQq5L8nHMD+TUJUG0aAyJBqidZQcUfFqeK2vlEo1rYh5iUXl6eMB5eoKv37tOUb/9q+4LBD7gXKj3ry7Ak/iHrdeH/ArC7P07ykPekMWzkFvkNMmkHkgagNoDciogYKzaGg0YgSNYLEJj0ndfiGj6o/Z3i5or5+j2BTKJwlawcwKbMQ9fvK9Ib+QtgitQMg8d384wVYLdocls1mCad1+Fg1TDgeV4UxFTB2ow44NjQ5rwcwZ+MjqmLe/dZOBLOHHCeUUZteED+Ie73xvyHNpmyx1TERozXieW1/mO/+yzWhTWVtsU5aCqKvRT4+GmRDEHGJ6yPPMBHNGeqbCdwqs02X9l14g/a8H3PzWm/zyp1ZZXgn8tDfgnbeGPN/qIEDSyhh7w2OcPZWRsESWCq3EoVFPksZDMuUIps7qLjDUQBxkZ0tcu8KSGdIXPo3OLbOSdfj4YMIPbg6ZXxux9wRmWimFKe0k0J7v8H5vQHchpSoiC536bKhKEDlCJzM7JLJqhqtxoEYoUcEFw/Yduh8Jl38OP7eAlBOm29ssziV87JlluqMOL15ZZWVlhu0yMrfYxQQmZUU3TbCmnog1mRE1xOp9xOoaEwVRI9QnUGOECLoPNlcRlucJ6xeIgx3K/3kX23pINWnRdnBxtYNWkbkkMJ5JSLKEwaSgRFlsJWh1mNBDfsEBzZGaYIiBIgRtImCmmArhlBFOR1Q9xZ3b6OOHWFnifEq1U/84TpXgHL4hUh7hv3t91pY6eCeUpTYs2o6RisYEO6B5hpoSRCWaanNAGP5UVZ+r+YB4dxcbZ4QVx/Q9wSYg/uDYNvKyoorwgyfbtDuBC6c65HmFa5Jsh56f3NgO4iGeYC68n8Ty2WkphAUVv2hoLuggoXgUcJkRh0rVF7wHi3Vw1YxuSMh1zMpcmw8tdynzCmmCL1JjzQG3lpNU37xAdNnjYC79xywUfzbJNboEX214iqeeuN/IrkKw6BAvNUiJ1a2K0HKOT144jQBFoTWRayrc9LhWa1TTUTNqJ038wCf/LO//xZfWWw9u/zCt8uWpSiTixZ3UeJzQPkerdkyq1RryZwVN87CJBALR0ExULMtG4/VnPy4AG3/4m19YGG/9fT7NmZpEhzg5lCNHOo//774u7UYp2WEt1XrSftYMTcF32ynbrfmXz3ztjW86u3bNn/nq69/eaZ/6PUlbm/PB+YSapUozMG2uB0M5fI7WSKoKasjBu6qHczEjwWQ2iJc03XmaLb5y5mtvfNOuNeLUruHlVeK7f/7yxbXBu39U5sXnq7I8L2pH6rwRgCZyUpeKnZSOB5K2EbT1l5hPk0c+zV7bXXzmry7/5T/cvXENf/1V4v8CSYIrURx3kHgAAAAASUVORK5CYII=";
		//#endregion
		//#region src/client/observation-sidebar.tsx
		/** Native DSH right-Sidebar carrier. All runtime collaboration uses optional host services. */
		const OBSERVATION_TAB_KIND = "browserskill-observation";
		const OBSERVATION_TAB_ID = "@wxg-prc-cpg/browser-skill-dsh-plugin/observation";
		const TAB_TITLE = "Browser Skill";
		const BODY_SLOT = "sidebar.right.pane.tab";
		const TITLE_SLOT = "sidebar.right.pane.tab.title";
		function TabIcon({ size = 16 }) {
			return /* @__PURE__ */ (0, react_jsx_runtime.jsx)("img", {
				src: BSK_LOGO_URL,
				width: size,
				height: size,
				alt: "",
				"aria-hidden": true,
				className: ObservationOverlay_module_css_default["brand-icon"]
			});
		}
		/** Same observation content as the overlay, scoped by the native slot's session identity. */
		function ObservationSidebarTab({ store, scopeId, visible = true }) {
			const { snapshot, focus, pinnedId, onTogglePin, now } = useObservationView(store, scopeId);
			const { pipWindow, pipSupported, popOut } = usePip();
			const presentation = (0, react.useSyncExternalStore)(store.presentation.subscribe, store.presentation.getSnapshot);
			if (presentation.floating && presentation.sidebarAvailable) return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("div", {
				className: `${ObservationOverlay_module_css_default["floating-notice"]} bsk-obs`,
				children: [/* @__PURE__ */ (0, react_jsx_runtime.jsx)("p", { children: "The browser view is in a floating window." }), /* @__PURE__ */ (0, react_jsx_runtime.jsx)("button", {
					type: "button",
					onClick: store.presentation.showSidebar,
					children: "Show here"
				})]
			});
			const body = /* @__PURE__ */ (0, react_jsx_runtime.jsx)(OverlayBody, {
				store,
				focus,
				sessions: snapshot.sessions,
				available: snapshot.available,
				reconnecting: snapshot.reconnecting,
				pinnedId,
				onTogglePin,
				now,
				visible: visible || pipWindow !== null,
				inPip: pipWindow !== null,
				onPopOut: pipWindow === null && pipSupported ? () => popOut() : void 0,
				onClosePip: pipWindow !== null ? () => pipWindow.close() : void 0,
				onUseFloating: () => {
					pipWindow?.close();
					store.presentation.showFloating();
				}
			});
			if (pipWindow !== null) return (0, react_dom.createPortal)(body, pipWindow.document.body);
			return /* @__PURE__ */ (0, react_jsx_runtime.jsx)("div", {
				className: ObservationOverlay_module_css_default["sidebar-tab"],
				children: body
			});
		}
		function ObservationTitle({ store, scopeId }) {
			const count = (0, react.useSyncExternalStore)(store.subscribe, store.getSnapshot).sessions.filter((session) => visibleToScope(session, scopeId)).length;
			return /* @__PURE__ */ (0, react_jsx_runtime.jsxs)("span", {
				className: ObservationOverlay_module_css_default["tab-title"],
				children: [
					/* @__PURE__ */ (0, react_jsx_runtime.jsx)(TabIcon, { size: 14 }),
					TAB_TITLE,
					count > 0 ? ` (${count})` : ""
				]
			});
		}
		function reportFailure(error) {
			console.warn("[browser-skill] native sidebar unavailable; using floating view", error);
		}
		/** A native hook or view failure must never reach DSH's surrounding workbench. */
		var ObservationBoundary = class extends react.Component {
			state = { failed: false };
			static getDerivedStateFromError() {
				return { failed: true };
			}
			componentDidCatch(error) {
				this.props.onError(error);
			}
			render() {
				return this.state.failed ? null : this.props.children;
			}
		};
		/**
		* Wait for both native seats, then install the type and its keyed renderers as
		* one lifetime. Missing native services are normal, including on older DSH.
		*/
		function registerObservationSidebar(host, store) {
			const sidebar = host.get("sidebarRight");
			const tabs = host.get("sidebarRightTabs");
			const sessions = host.get("sessions")?.list;
			if (typeof sidebar?.openTabIn !== "function" || typeof tabs?.register !== "function" || typeof sessions?.getSnapshot !== "function" || typeof sessions?.subscribe !== "function") return () => {};
			return host.slots.inject(BODY_SLOT, () => host.slots.inject(TITLE_SLOT, () => {
				const disposers = [];
				const autoOpened = /* @__PURE__ */ new Set();
				const mounted = /* @__PURE__ */ new Map();
				let disposed = false;
				let failed = false;
				let scheduled = false;
				let disconnect;
				let pending;
				const cancelOpen = () => {
					clearTimeout(pending?.timer);
					pending = void 0;
				};
				const fallback = () => {
					cancelOpen();
					disconnect?.();
					disconnect = void 0;
				};
				const dispose = () => {
					if (disposed) return;
					disposed = true;
					fallback();
					for (const release of disposers.reverse()) release();
				};
				const connect = () => {
					if (failed) return;
					disconnect ??= store.presentation.connectSidebar(reveal);
				};
				const fail = (error) => {
					failed = true;
					fallback();
					reportFailure(error);
				};
				const open = (sessionId) => {
					cancelOpen();
					const request = {
						sessionId,
						attempts: 0,
						timer: void 0
					};
					pending = request;
					const attempt = () => {
						if (disposed || pending !== request) return;
						try {
							if (sessions.getSnapshot().current !== sessionId || store.presentation.getSnapshot().floating) {
								cancelOpen();
								return;
							}
							sidebar.openTabIn(sessionId, OBSERVATION_TAB_KIND);
						} catch (error) {
							fail(error);
							return;
						}
						if (mounted.has(sessionId)) {
							autoOpened.add(sessionId);
							cancelOpen();
						}
						if (pending !== request) return;
						request.attempts += 1;
						request.timer = setTimeout(() => {
							if (pending !== request) return;
							if (request.attempts < 4) attempt();
							else fallback();
						}, 250);
					};
					attempt();
				};
				function reveal() {
					try {
						const current = sessions?.getSnapshot().current;
						if (current !== void 0) open(current);
					} catch (error) {
						fail(error);
					}
				}
				const acknowledge = (sessionId) => {
					if (disposed) return;
					mounted.set(sessionId, (mounted.get(sessionId) ?? 0) + 1);
					autoOpened.add(sessionId);
					if (pending?.sessionId === sessionId) cancelOpen();
					connect();
					return () => {
						const remaining = (mounted.get(sessionId) ?? 1) - 1;
						if (remaining === 0) mounted.delete(sessionId);
						else mounted.set(sessionId, remaining);
					};
				};
				const evaluate = () => {
					if (disposed) return;
					const current = sessions.getSnapshot().current;
					const observations = store.getSnapshot().sessions;
					for (const id of autoOpened) if (!observations.some((session) => visibleToScope(session, id))) autoOpened.delete(id);
					if (pending !== void 0 && (pending.sessionId !== current || store.presentation.getSnapshot().floating)) cancelOpen();
					if (current === void 0 || store.presentation.getSnapshot().floating || pending !== void 0 || autoOpened.has(current)) return;
					if (observations.some((session) => visibleToScope(session, current))) open(current);
				};
				const schedule = () => {
					if (disposed || scheduled) return;
					scheduled = true;
					queueMicrotask(() => {
						scheduled = false;
						try {
							evaluate();
						} catch (error) {
							fail(error);
						}
					});
				};
				function NativeBody(props) {
					const { tab } = props.useTabInfo();
					(0, react.useEffect)(() => acknowledge(props.sessionId), [props.sessionId]);
					return /* @__PURE__ */ (0, react_jsx_runtime.jsx)(ObservationSidebarTab, {
						store,
						scopeId: props.sessionId,
						visible: tab.visible
					});
				}
				try {
					disposers.push(host.slots.register({
						name: BODY_SLOT,
						key: OBSERVATION_TAB_ID
					}, function Body(props) {
						return /* @__PURE__ */ (0, react_jsx_runtime.jsx)(ObservationBoundary, {
							onError: fail,
							children: /* @__PURE__ */ (0, react_jsx_runtime.jsx)(NativeBody, { ...props })
						});
					}));
					disposers.push(host.slots.register({
						name: TITLE_SLOT,
						key: OBSERVATION_TAB_ID
					}, function Title(props) {
						(0, react.useEffect)(() => acknowledge(props.sessionId), [props.sessionId]);
						return /* @__PURE__ */ (0, react_jsx_runtime.jsx)(ObservationTitle, {
							store,
							scopeId: props.sessionId
						});
					}));
					disposers.push(tabs.register({
						id: OBSERVATION_TAB_ID,
						kind: OBSERVATION_TAB_KIND,
						title: () => TAB_TITLE,
						guide: [{
							order: 60,
							title: () => TAB_TITLE,
							description: () => "Watch browser activity and control running sessions.",
							icon: TabIcon
						}]
					}));
					disposers.push(() => store.release());
					store.acquire();
					disposers.push(store.subscribe(schedule));
					disposers.push(sessions.subscribe(schedule));
					disposers.push(store.presentation.subscribe(schedule));
					connect();
					schedule();
				} catch (error) {
					dispose();
					reportFailure(error);
				}
				return dispose;
			}));
		}
		//#endregion
		//#region src/client/observation-presentation.ts
		/** Page-local presentation choice. Nothing is persisted or shared between client instances. */
		var ObservationPresentation = class {
			listeners = /* @__PURE__ */ new Set();
			reveal;
			floating = false;
			snapshot = {
				sidebarAvailable: false,
				floating: true
			};
			getSnapshot = () => this.snapshot;
			subscribe = (listener) => {
				this.listeners.add(listener);
				return () => this.listeners.delete(listener);
			};
			/** A successfully registered native carrier; removing it restores the overlay. */
			connectSidebar(reveal) {
				this.reveal = reveal;
				this.publish();
				return () => {
					if (this.reveal !== reveal) return;
					this.reveal = void 0;
					this.publish();
				};
			}
			showFloating = () => {
				this.floating = true;
				this.publish();
			};
			showSidebar = () => {
				if (this.reveal === void 0) return;
				this.floating = false;
				this.publish();
				this.reveal();
			};
			publish() {
				const sidebarAvailable = this.reveal !== void 0;
				const floating = this.floating || !sidebarAvailable;
				if (sidebarAvailable === this.snapshot.sidebarAvailable && floating === this.snapshot.floating) return;
				this.snapshot = {
					sidebarAvailable,
					floating
				};
				for (const listener of [...this.listeners]) listener();
			}
		};
		//#endregion
		//#region src/client/observation-store.ts
		const EVENTS_URL = "/bsk-observation/events";
		const INTERRUPT_URL = "/bsk-observation/interrupt";
		const STOP_URL = "/bsk-observation/stop";
		const THUMBNAIL_RETRY_DELAYS_MS = [1e3, 3e3];
		/**
		* Backoff for re-creating the live event stream after a *fatal* failure.
		* A non-200 response (for example the observation route answering 404 while
		* the plugin is still starting, or right after a plugin reload) is terminal
		* for an EventSource: the browser never retries it. Without this the view
		* stays empty until the page is reloaded.
		*/
		const EVENT_STREAM_RETRY_DELAYS_MS = [
			1e3,
			3e3,
			1e4,
			3e4
		];
		function revoke(url) {
			if (url !== void 0 && typeof URL.revokeObjectURL === "function") URL.revokeObjectURL(url);
		}
		var ObservationClientStore = class {
			deps;
			presentation = new ObservationPresentation();
			sessions = /* @__PURE__ */ new Map();
			thumbs = /* @__PURE__ */ new Map();
			/** sessionId → the attachment id currently advertised for it. */
			thumbBySession = /* @__PURE__ */ new Map();
			/** sessionId → last successfully loaded frame (held across the next load). */
			lastReady = /* @__PURE__ */ new Map();
			retryTimers = /* @__PURE__ */ new Map();
			retryCounts = /* @__PURE__ */ new Map();
			listeners = /* @__PURE__ */ new Set();
			events;
			/** Pending re-creation of the event stream after a fatal failure. */
			reconnectTimer;
			reconnectAttempts = 0;
			snapshot = {
				sessions: [],
				subscribed: false,
				thumbnails: {},
				displayFrames: {},
				available: true,
				reconnecting: false
			};
			available = true;
			reconnecting = false;
			started = false;
			/** Refcount of mounted consumers (overlay card, sidebar tab, sidebar fiber). */
			consumers = 0;
			thumbnailConsumers = 0;
			constructor(deps) {
				this.deps = deps;
			}
			subscribe = (listener) => {
				this.listeners.add(listener);
				return () => this.listeners.delete(listener);
			};
			getSnapshot = () => this.snapshot;
			publish() {
				this.snapshot = {
					sessions: [...this.sessions.values()],
					subscribed: this.events !== void 0,
					thumbnails: Object.fromEntries(this.thumbs),
					displayFrames: this.buildDisplayFrames(),
					available: this.available,
					reconnecting: this.reconnecting
				};
				for (const listener of [...this.listeners]) listener();
			}
			/** Frame the overlay should paint for one session (last good while next loads). */
			frameFor(sessionId) {
				const attachmentId = this.thumbBySession.get(sessionId);
				const current = attachmentId !== void 0 ? this.thumbs.get(attachmentId) : void 0;
				if (current?.status === "ready" && current.url !== void 0) return current;
				const held = this.lastReady.get(sessionId);
				if (held !== void 0) return {
					status: current?.status === "error" ? "error" : "ready",
					url: held.url
				};
				return current;
			}
			buildDisplayFrames() {
				const frames = {};
				for (const sessionId of this.sessions.keys()) {
					const frame = this.frameFor(sessionId);
					if (frame !== void 0) frames[sessionId] = frame;
				}
				return frames;
			}
			sessionOf(attachmentId) {
				for (const [sessionId, id] of this.thumbBySession) if (id === attachmentId) return sessionId;
			}
			connectEvents() {
				clearTimeout(this.reconnectTimer);
				this.reconnectTimer = void 0;
				const previous = this.events;
				this.events = void 0;
				previous?.close();
				const events = this.deps.eventSourceFactory(`${EVENTS_URL}?thumbnails=${this.thumbnailConsumers > 0 ? "1" : "0"}`);
				this.events = events;
				events.onmessage = (message) => {
					if (!this.started || this.events !== events) return;
					let event;
					try {
						event = JSON.parse(message.data);
					} catch {
						return;
					}
					this.reconnectAttempts = 0;
					this.reconnecting = false;
					this.apply(event);
				};
				events.onerror = () => {
					if (!this.started || this.events !== events) return;
					if (events.readyState === void 0 || events.readyState === 2) this.scheduleReconnect();
					if (this.reconnecting) return;
					this.reconnecting = true;
					this.publish();
				};
			}
			/** Re-create the stream after a fatal failure, with a bounded backoff. */
			scheduleReconnect() {
				if (this.reconnectTimer !== void 0) return;
				const index = Math.min(this.reconnectAttempts, EVENT_STREAM_RETRY_DELAYS_MS.length - 1);
				const delay = EVENT_STREAM_RETRY_DELAYS_MS[index];
				if (delay === void 0) return;
				this.reconnectAttempts += 1;
				this.reconnectTimer = setTimeout(() => {
					this.reconnectTimer = void 0;
					if (!this.started) return;
					this.connectEvents();
				}, delay);
			}
			/** Every initial connection and reconnect starts with the server's snapshot. */
			start() {
				if (this.started) return;
				this.started = true;
				this.connectEvents();
				this.publish();
			}
			stop() {
				const previous = this.events;
				this.events = void 0;
				this.started = false;
				previous?.close();
				clearTimeout(this.reconnectTimer);
				this.reconnectTimer = void 0;
				this.reconnectAttempts = 0;
				this.reconnecting = false;
				this.clearThumbnails();
				this.sessions.clear();
				this.available = true;
				this.publish();
			}
			/**
			* Hold the feed for one consumer's lifetime: the stream starts with the
			* first holder and stops with the last release. Several carriers can share
			* the store (the floating card, the native sidebar tab, and the sidebar
			* integration fiber) without one unmount killing the others' updates.
			*/
			acquire() {
				this.consumers += 1;
				if (this.consumers === 1) this.start();
			}
			release() {
				if (this.consumers === 0) return;
				this.consumers -= 1;
				if (this.consumers === 0) this.stop();
			}
			/** Only visible image surfaces request screenshots; metadata watchers do not. */
			watchThumbnails() {
				this.thumbnailConsumers += 1;
				if (this.thumbnailConsumers === 1 && this.started) this.connectEvents();
				let released = false;
				return () => {
					if (released) return;
					released = true;
					this.thumbnailConsumers -= 1;
					if (this.thumbnailConsumers === 0 && this.started) this.connectEvents();
				};
			}
			forgetThumbnail(attachmentId) {
				clearTimeout(this.retryTimers.get(attachmentId));
				this.retryTimers.delete(attachmentId);
				this.retryCounts.delete(attachmentId);
				revoke(this.thumbs.get(attachmentId)?.url);
				this.thumbs.delete(attachmentId);
			}
			clearThumbnails() {
				for (const attachmentId of this.thumbs.keys()) this.forgetThumbnail(attachmentId);
				this.thumbBySession.clear();
				this.lastReady.clear();
			}
			/** Forget one session's tracked + held frames, revoking their blob URLs. */
			dropThumb(sessionId) {
				const attachmentId = this.thumbBySession.get(sessionId);
				const held = this.lastReady.get(sessionId);
				this.thumbBySession.delete(sessionId);
				this.lastReady.delete(sessionId);
				if (attachmentId !== void 0) this.forgetThumbnail(attachmentId);
				if (held !== void 0 && held.attachmentId !== attachmentId) this.forgetThumbnail(held.attachmentId);
			}
			/**
			* Track the frame a session currently advertises. The last *ready* blob is
			* kept until the replacement loads — dropping it on the upsert is what
			* made the overlay flash a placeholder between breaths.
			*/
			trackThumb(sessionId, attachmentId) {
				if (attachmentId === void 0) {
					this.dropThumb(sessionId);
					return;
				}
				const previous = this.thumbBySession.get(sessionId);
				if (previous === attachmentId) return;
				this.thumbBySession.set(sessionId, attachmentId);
				const heldId = this.lastReady.get(sessionId)?.attachmentId;
				if (previous !== void 0 && previous !== heldId) this.forgetThumbnail(previous);
			}
			apply(event) {
				if (event.type === "snapshot") {
					this.sessions = new Map(event.sessions.map((session) => [session.sessionId, session]));
					for (const sessionId of this.thumbBySession.keys()) if (!this.sessions.has(sessionId)) this.dropThumb(sessionId);
					for (const session of event.sessions) this.trackThumb(session.sessionId, session.thumbnailAttachmentId);
					this.available = event.available;
				} else if (event.type === "reset") {
					this.sessions.clear();
					this.clearThumbnails();
				} else if (event.type === "remove" && event.session !== void 0) {
					this.sessions.delete(event.session.sessionId);
					this.dropThumb(event.session.sessionId);
				} else if (event.type === "upsert" && event.session !== void 0) {
					this.sessions.set(event.session.sessionId, event.session);
					this.trackThumb(event.session.sessionId, event.session.thumbnailAttachmentId);
				} else if (event.type === "availability") this.available = event.available;
				this.publish();
				if (event.type === "snapshot") for (const session of event.sessions) this.retryThumbnail(session.thumbnailAttachmentId);
			}
			/**
			* Ensure a thumbnail load is in flight for one attachment reference. New
			* frames replace the old URL only after they load; failures keep the last
			* good frame (the caller renders `status: 'error'` as a small badge over
			* the old image).
			*/
			ensureThumbnail(attachmentId) {
				if (!this.started || attachmentId === void 0 || this.sessionOf(attachmentId) === void 0 || this.thumbs.has(attachmentId)) return;
				const loading = { status: "loading" };
				this.thumbs.set(attachmentId, loading);
				this.publish();
				this.deps.loadImage(attachmentId).then((url) => {
					if (this.thumbs.get(attachmentId) !== loading) {
						revoke(url);
						return;
					}
					this.retryCounts.delete(attachmentId);
					this.thumbs.set(attachmentId, {
						status: "ready",
						url
					});
					const sessionId = this.sessionOf(attachmentId);
					if (sessionId !== void 0) {
						const previous = this.lastReady.get(sessionId);
						if (previous !== void 0 && previous.attachmentId !== attachmentId) this.forgetThumbnail(previous.attachmentId);
						this.lastReady.set(sessionId, {
							attachmentId,
							url
						});
					}
					this.publish();
				}, () => {
					if (this.thumbs.get(attachmentId) !== loading) return;
					const failed = { status: "error" };
					this.thumbs.set(attachmentId, failed);
					const attempts = this.retryCounts.get(attachmentId) ?? 0;
					const delay = THUMBNAIL_RETRY_DELAYS_MS[attempts];
					if (delay !== void 0) {
						this.retryCounts.set(attachmentId, attempts + 1);
						this.retryTimers.set(attachmentId, setTimeout(() => {
							this.retryTimers.delete(attachmentId);
							if (this.thumbs.get(attachmentId) !== failed) return;
							this.thumbs.delete(attachmentId);
							this.ensureThumbnail(attachmentId);
						}, delay));
					}
					this.publish();
				});
			}
			/** Explicit retry (also used on a fresh connection after an outage). */
			retryThumbnail(attachmentId) {
				if (attachmentId === void 0 || this.thumbs.get(attachmentId)?.status !== "error") return;
				this.forgetThumbnail(attachmentId);
				this.ensureThumbnail(attachmentId);
			}
			/** Interrupt the current or named session's in-flight call. */
			async interrupt(sessionId) {
				try {
					const res = await this.deps.fetchFn(INTERRUPT_URL, {
						method: "POST",
						headers: { "content-type": "application/json" },
						body: JSON.stringify(sessionId === void 0 ? {} : { sessionId })
					});
					if (!res.ok) return false;
					return (await res.json()).interrupted === true;
				} catch {
					return false;
				}
			}
			/**
			* Stop one session and close its Agent Window. The session's removal
			* arrives through the SSE remove event — no local state is touched here.
			*/
			async stopSession(sessionId) {
				try {
					const res = await this.deps.fetchFn(STOP_URL, {
						method: "POST",
						headers: { "content-type": "application/json" },
						body: JSON.stringify({ sessionId })
					});
					if (!res.ok) return false;
					return (await res.json()).stopped === true;
				} catch {
					return false;
				}
			}
		};
		//#endregion
		//#region src/client/index.ts
		/** Required services: slots, session-scoped attachment reads, and the overlay seat. */
		const inject = ["slots", "sessions"];
		/** Return a fresh component-owned URL; the host's loadImage may return shared cached URLs. */
		async function loadSessionImage(sessions, sessionId, attachment) {
			const session = sessions.binding(sessionId)?.session;
			if (session === void 0) throw new Error(`screenshot toolview: session "${String(sessionId)}" is not bound`);
			const result = await session.readAttachment(attachment.attachmentId);
			if (!result.ok) throw new Error(`screenshot toolview: readAttachment failed: ${result.error.code}: ${result.error.message}`);
			const bytes = Uint8Array.from(result.value.data);
			return URL.createObjectURL(new Blob([bytes.buffer], { type: result.value.attachment.mediaType }));
		}
		/**
		* Thumbnail loader for the overlay: frames are plugin-owned runtime data
		* (never referenced by a session log, so the session-authorized RPC refuses
		* them), served by the host over the plugin's own route.
		*/
		async function overlayImageLoader(attachmentId) {
			const res = await fetch(`/bsk-observation/thumbnail/${encodeURIComponent(attachmentId)}`);
			if (!res.ok) throw new Error(`thumbnail fetch failed: ${res.status}`);
			return URL.createObjectURL(await res.blob());
		}
		/**
		* Client plugin body: register the keyed toolview and the observation overlay.
		* @param ctx - client root context.
		*/
		function apply(ctx) {
			const sessions = ctx.get("sessions");
			ctx.slots.inject("tool.call.toolview", () => ctx.slots.register({
				name: "tool.call.toolview",
				key: "browser_inspect"
			}, function BrowserInspectSessionView(props) {
				const loadImage = (0, react.useCallback)((attachment) => loadSessionImage(sessions, props.sessionId, attachment), [props.sessionId]);
				return (0, react.createElement)(BrowserInspectToolView, {
					...props,
					loadImage
				});
			}));
			const store = new ObservationClientStore({
				fetchFn: (url, init) => fetch(url, init),
				eventSourceFactory: (url) => new EventSource(url),
				loadImage: overlayImageLoader
			});
			ctx.slots.inject("shell.overlay", () => ctx.slots.register({
				name: "shell.overlay",
				id: "bsk-observation"
			}, () => (0, react.createElement)(ObservationOverlay, { store })));
			ctx.inject(["sidebarRight", "sidebarRightTabs"], (injected) => registerObservationSidebar(injected, store));
		}
		//#endregion
		exports.apply = apply;
		exports.inject = inject;
		return module.exports;
	}
});
