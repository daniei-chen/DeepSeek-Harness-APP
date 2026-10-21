window.__ModuleLoader__.load({
  id: "@dsh-android/dsh-client-ui-responsive-bundle",
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
      // where they would hide the native speed/identity indicators. Only the Android shell has
      // the DshShellBridge (or a WebView user agent).
      const androidShell = !!window.DshShellBridge || /;\s*wv\)/i.test(navigator.userAgent);
      if (!androidShell) return;
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
