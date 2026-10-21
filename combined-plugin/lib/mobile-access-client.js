/**
 * Settings → 手机访问.
 *
 * This is deliberately a status panel, not a second connection manager. It answers the three
 * questions a phone user actually has when a LAN scan finds nothing:
 *
 *   1. where is this DSH listening,
 *   2. is that address reachable from a phone,
 *   3. if not, which supported path (LAN bind or dsh-relay) will work.
 *
 * All facts come from the host's /dsh-mobile/info route. No credential is read or displayed.
 */
window.__ModuleLoader__.load({
  id: "@dsh-android/deepseek-harness-app-plugin/mobile-access",
  factory: (require) => {
    const React = require("react");
    const h = React.createElement;

    const S = {
      wrap: {
        display: "flex",
        flexDirection: "column",
        gap: "16px",
        width: "100%",
        maxWidth: "760px",
        padding: "4px 0 28px",
        color: "var(--dsw-alias-label-primary, #111)",
        fontFamily: "inherit",
      },
      card: {
        display: "flex",
        flexDirection: "column",
        gap: "12px",
        padding: "16px",
        border: "1px solid var(--dsw-alias-border-l2, rgba(127,127,127,.25))",
        borderRadius: "14px",
        background: "var(--dsw-alias-bg-layer-1, transparent)",
      },
      title: { fontSize: "15px", fontWeight: 600, margin: 0 },
      hint: {
        margin: 0,
        color: "var(--dsw-alias-label-secondary, #666)",
        fontSize: "12px",
        lineHeight: 1.6,
      },
      row: {
        display: "flex",
        alignItems: "flex-start",
        justifyContent: "space-between",
        gap: "12px",
        padding: "7px 0",
        borderTop: "1px solid var(--dsw-alias-border-l1, rgba(127,127,127,.14))",
        fontSize: "12px",
        lineHeight: 1.5,
      },
      rowFirst: { borderTop: "none", paddingTop: 0 },
      label: { color: "var(--dsw-alias-label-secondary, #666)", flex: "0 0 auto" },
      value: {
        color: "var(--dsw-alias-label-primary, #111)",
        textAlign: "right",
        overflowWrap: "anywhere",
        minWidth: 0,
      },
      code: {
        fontFamily: "ui-monospace, SFMono-Regular, Consolas, monospace",
        fontSize: "11px",
        background: "var(--dsw-alias-bg-layer-2, rgba(127,127,127,.1))",
        borderRadius: "5px",
        padding: "2px 5px",
      },
      badge: {
        display: "inline-flex",
        alignItems: "center",
        borderRadius: "999px",
        padding: "1px 7px",
        fontSize: "11px",
        fontWeight: 500,
        whiteSpace: "nowrap",
      },
      actions: { display: "flex", flexWrap: "wrap", gap: "8px" },
      button: {
        border: "1px solid var(--dsw-alias-border-l2, rgba(127,127,127,.25))",
        background: "var(--dsw-alias-bg-layer-1, transparent)",
        color: "var(--dsw-alias-label-primary, #111)",
        borderRadius: "8px",
        padding: "7px 11px",
        fontSize: "12px",
        cursor: "pointer",
        fontFamily: "inherit",
      },
      buttonPrimary: {
        background: "var(--dsw-alias-button-primary-fill, #2563eb)",
        borderColor: "transparent",
        color: "var(--dsw-alias-label-primary-foreground, #fff)",
      },
      buttonDisabled: { opacity: 0.45, cursor: "not-allowed" },
      list: { margin: 0, paddingLeft: "18px", display: "flex", flexDirection: "column", gap: "6px" },
      listItem: { fontSize: "12px", lineHeight: 1.6, color: "var(--dsw-alias-label-secondary, #555)" },
      error: {
        color: "var(--dsw-alias-state-error-primary, #b42318)",
        fontSize: "12px",
        lineHeight: 1.6,
        margin: 0,
      },
      auditRow: {
        display: "flex",
        flexDirection: "column",
        gap: "2px",
        padding: "7px 0",
        borderTop: "1px solid var(--dsw-alias-border-l1, rgba(127,127,127,.14))",
        fontSize: "11.5px",
        lineHeight: 1.5,
      },
      auditMeta: { color: "var(--dsw-alias-label-secondary, #666)", fontSize: "11px" },
      spinner: { fontSize: "12px", color: "var(--dsw-alias-label-secondary, #666)" },
      muted: { color: "var(--dsw-alias-label-secondary, #666)" },
      monoList: {
        display: "flex",
        flexDirection: "column",
        gap: "3px",
        alignItems: "flex-end",
      },
    };

    function badge(tone, text) {
      const colors = {
        success: ["var(--dsw-alias-state-success-primary, #067647)", "rgba(6,118,71,.12)"],
        warning: ["var(--dsw-alias-state-warning-primary, #b54708)", "rgba(181,71,8,.12)"],
        neutral: ["var(--dsw-alias-label-secondary, #666)", "rgba(127,127,127,.12)"],
        danger: ["var(--dsw-alias-state-error-primary, #b42318)", "rgba(180,35,24,.12)"],
      };
      const [color, background] = colors[tone] || colors.neutral;
      return h("span", { style: { ...S.badge, color, background } }, text);
    }

    function code(value) {
      return h("code", { style: S.code }, value);
    }

    function row(label, value, first = false) {
      return h(
        "div",
        { style: { ...S.row, ...(first ? S.rowFirst : {}) } },
        h("span", { style: S.label }, label),
        h("span", { style: S.value }, value),
      );
    }

    function button(label, onClick, options = {}) {
      return h(
        "button",
        {
          type: "button",
          onClick,
          disabled: Boolean(options.disabled),
          style: {
            ...S.button,
            ...(options.primary ? S.buttonPrimary : {}),
            ...(options.disabled ? S.buttonDisabled : {}),
          },
        },
        label,
      );
    }

    function fallbackCopy(text) {
      try {
        const input = document.createElement("textarea");
        input.value = text;
        input.setAttribute("readonly", "");
        input.style.position = "fixed";
        input.style.opacity = "0";
        document.body.appendChild(input);
        input.select();
        document.execCommand("copy");
        document.body.removeChild(input);
        return true;
      } catch {
        return false;
      }
    }

    async function copyText(text) {
      if (!text) return false;
      try {
        if (navigator.clipboard && typeof navigator.clipboard.writeText === "function") {
          await navigator.clipboard.writeText(text);
          return true;
        }
      } catch {
        // HTTP LAN origins are not secure contexts; fall through to the textarea path.
      }
      return fallbackCopy(text);
    }

    function MobileAccessSection() {
      const [info, setInfo] = React.useState(null);
      const [audit, setAudit] = React.useState(null);
      const [error, setError] = React.useState("");
      const [loading, setLoading] = React.useState(true);
      const [notice, setNotice] = React.useState("");
      const noticeTimer = React.useRef(null);

      const load = React.useCallback(async () => {
        setLoading(true);
        setError("");
        try {
          const response = await fetch("/dsh-mobile/info", {
            credentials: "same-origin",
            cache: "no-store",
          });
          if (!response.ok) throw new Error(`HTTP ${response.status}`);
          const next = await response.json();
          if (!next || next.ok !== true) throw new Error(next?.error || "invalid response");
          setInfo(next);
          try {
            const auditResponse = await fetch("/dsh-mobile/audit?limit=8", {
              credentials: "same-origin",
              cache: "no-store",
            });
            if (auditResponse.ok) {
              const auditValue = await auditResponse.json();
              if (auditValue?.ok === true) setAudit(auditValue);
            }
          } catch {
            // The audit route is additive; a plugin version without it must not break this panel.
          }
        } catch (cause) {
          setError(cause instanceof Error ? cause.message : String(cause));
        } finally {
          setLoading(false);
        }
      }, []);

      React.useEffect(() => {
        load();
        return () => {
          if (noticeTimer.current !== null) window.clearTimeout(noticeTimer.current);
        };
      }, [load]);

      const flash = (message) => {
        setNotice(message);
        if (noticeTimer.current !== null) window.clearTimeout(noticeTimer.current);
        noticeTimer.current = window.setTimeout(() => setNotice(""), 2200);
      };

      const copy = async (text, label) => {
        const ok = await copyText(text);
        flash(ok ? `${label}已复制` : "复制失败，请长按选择文本");
      };

      const lanUrl = info?.urls?.[0]?.url || "";
      const pairUrl = info?.relay?.pairUrl || "";
      const currentUrl = info?.origin || (info?.bindHost && info?.port ? `http://${info.bindHost}:${info.port}/` : "");

      return h(
        "div",
        { style: S.wrap },
        h(
          "div",
          { style: S.card },
          h("h3", { style: S.title }, "手机访问"),
          h(
            "p",
            { style: S.hint },
            "这里只显示当前 DSH 的真实监听地址和可用连接路径，不会修改 DSH 的监听设置，也不会显示任何配对凭据。",
          ),
          loading
            ? h("p", { style: S.spinner }, "正在读取主机信息…")
            : error
              ? h(
                  "div",
                  { style: { display: "flex", flexDirection: "column", gap: "10px" } },
                  h("p", { style: S.error }, `读取失败：${error}`),
                  h("div", { style: S.actions }, button("重试", load, { primary: true })),
                )
              : info
                ? h(
                    React.Fragment,
                    null,
                    row("当前面板地址", code(currentUrl || "—"), true),
                    row(
                      "监听地址",
                      h(
                        "span",
                        { style: { display: "inline-flex", alignItems: "center", gap: "6px", flexWrap: "wrap", justifyContent: "flex-end" } },
                        code(`${info.bindHost || "?"}:${info.port ?? "?"}`),
                        info.loopbackOnly
                          ? badge("warning", "仅本机")
                          : badge("success", "局域网可达"),
                      ),
                    ),
                    row(
                      "局域网地址",
                      info.lanReachable && info.urls?.length
                        ? h(
                            "span",
                            { style: S.monoList },
                            ...info.urls.map((item) => code(item.url)),
                          )
                        : h("span", { style: S.muted }, "不可用：DSH 只监听回环地址"),
                    ),
                    row(
                      "中继",
                      h(
                        "span",
                        { style: { display: "inline-flex", alignItems: "center", gap: "6px", flexWrap: "wrap", justifyContent: "flex-end" } },
                        info.relay?.ok ? badge("success", "已运行") : badge("neutral", "未运行"),
                        code(`127.0.0.1:${info.relay?.port ?? 3443}`),
                      ),
                    ),
                    row(
                      "文件根目录",
                      info.fileRoots?.length
                        ? h("span", { style: S.monoList }, ...info.fileRoots.map((root) => code(root)))
                        : h("span", { style: S.muted }, "未配置（使用进程工作目录）"),
                    ),
                    h(
                      "div",
                      { style: S.actions },
                      button("复制当前地址", () => copy(currentUrl, "当前地址"), { disabled: !currentUrl }),
                      button("复制局域网地址", () => copy(lanUrl, "局域网地址"), {
                        primary: info.lanReachable && Boolean(lanUrl),
                        disabled: !info.lanReachable || !lanUrl,
                      }),
                      button("复制配对地址", () => copy(pairUrl, "配对地址"), {
                        disabled: !info.relay?.ok || !pairUrl,
                      }),
                      button("刷新", load),
                    ),
                    notice ? h("p", { style: S.hint }, notice) : null,
                  )
                : null,
        ),
        h(
          "div",
          { style: S.card },
          h("h3", { style: S.title }, "最近文件操作"),
          audit === null
            ? h("p", { style: S.hint }, "审计日志未启用或当前插件版本不支持。")
            : audit.configured === false
              ? h("p", { style: S.hint }, "未配置 auditLog；在 deepseek-app 的 config 里设置 auditLog 后生效。")
              : audit.entries?.length
                ? h(
                    "div",
                    { style: { display: "flex", flexDirection: "column" } },
                    ...[...audit.entries].reverse().map((entry, index) =>
                      h(
                        "div",
                        { style: S.auditRow, key: `${entry.time || ""}:${index}` },
                        h("div", null, `${entry.action || "?"} · ${entry.result || ""}`),
                        h(
                          "div",
                          { style: S.auditMeta },
                          `${entry.time ? new Date(entry.time).toLocaleString() : ""}${entry.remote ? " · " + entry.remote : ""}`,
                        ),
                        h("code", { style: S.code }, String(entry.path || "")),
                      ),
                    ),
                  )
                : h("p", { style: S.hint }, "暂无文件操作记录。"),
        ),
        h(
          "div",
          { style: S.card },
          h("h3", { style: S.title }, "手机怎么连"),
          h(
            "ol",
            { style: S.list },
            h("li", { style: S.listItem }, "手机和电脑连同一个 Wi-Fi；访客 Wi-Fi 或 IoT Wi-Fi 通常做了客户端隔离，扫描会失败。"),
            h(
              "li",
              { style: S.listItem },
              info?.lanReachable
                ? "在手机 APP 的“设备”里添加上面的局域网地址。"
                : "当前 DSH Desktop 只监听 127.0.0.1，手机无法直接访问；这是 DSH 的安全默认值，不是扫描按钮坏了。",
            ),
            h(
              "li",
              { style: S.listItem },
              info?.relay?.ok
                ? "打开中继配对页，扫描二维码或输入 8 位码；配对后再回到 APP 选择该设备。"
                : "需要局域网访问时，在电脑上安装并启动 dsh-relay；它会监听 0.0.0.0:3443，并提供二维码/8 位码配对与设备撤销。",
            ),
            h("li", { style: S.listItem }, "如果扫描仍为空，检查电脑防火墙是否放行 DSH/中继端口，以及手机是否连在 VPN 上。"),
          ),
          h(
            "p",
            { style: S.hint },
            "APP 的“扫描局域网”只扫描手机当前 /24 网段；DSH Desktop 的随机端口和回环监听不会出现在结果里。",
          ),
        ),
      );
    }

    function apply(ctx) {
      ctx.slots.inject("settings.section", () => ctx.slots.register({
        name: "settings.section",
        id: "dsh-mobile-access",
        order: 96,
        label: () => "手机访问",
      }, MobileAccessSection));
    }

    return { apply, inject: ["slots"] };
  }
});
