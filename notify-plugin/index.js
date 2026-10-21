/**
 * Self-hosted notification publisher.
 *
 * The phone cannot receive notifications after Android kills its process without a push path.
 * This plugin gives the operator a user-owned one: DSH posts terminal/action events to their own
 * ntfy server (or another HTTP endpoint that accepts ntfy-compatible headers). It never talks to
 * a project-operated cloud.
 *
 * Configuration (environment of the DSH host process):
 *   DSH_NTFY_URL    e.g. https://ntfy.example.com
 *   DSH_NTFY_TOPIC  e.g. dsh-alerts
 *   DSH_NTFY_TOKEN  optional bearer token
 *
 * The app can also subscribe to the same topic when it is alive; the point of this publisher is
 * the process-killed case.
 */
export const name = "dsh-mobile-notify";
export const inject = [];

const terminalTypes = [
    "turn/end",
    "turn/error",
    "turn/failed",
    "goal/change",
    "ask/request",
    "question/request",
    "question/asked",
];

export function apply(ctx) {
    const base = (process.env.DSH_NTFY_URL || "").replace(/\/+$/, "");
    const topic = process.env.DSH_NTFY_TOPIC || "";
    const token = process.env.DSH_NTFY_TOKEN || "";
    const unifiedEndpoint = process.env.DSH_UNIFIEDPUSH_ENDPOINT || "";

    if ((!base || !topic) && !unifiedEndpoint) {
        ctx.logger?.info?.("dsh-mobile-notify: DSH_NTFY_URL/DSH_NTFY_TOPIC and DSH_UNIFIEDPUSH_ENDPOINT are unset; publisher idle");
        return;
    }

    const post = async ({ title, body, priority = "default", tags = [], click = "" }) => {
        const headers = {
            "content-type": "text/plain; charset=utf-8",
            "Title": title,
            "Priority": priority,
            "Tags": tags.join(","),
        };
        if (click) headers.Click = click;
        if (token) headers.Authorization = `Bearer ${token}`;
        const response = await fetch(`${base}/${encodeURIComponent(topic)}`, {
            method: "POST",
            headers,
            body,
        });
        if (!response.ok) throw new Error(`ntfy ${response.status}: ${await response.text()}`);
    };

    const postUnified = async (body) => {
        if (!unifiedEndpoint) return;
        const response = await fetch(unifiedEndpoint, {
            method: "POST",
            headers: { "content-type": "text/plain; charset=utf-8" },
            body,
        });
        if (!response.ok) throw new Error(`UnifiedPush ${response.status}: ${await response.text()}`);
    };

    const publish = (event, session) => {
        const type = String(event?.type || "");
        if (!terminalTypes.some((candidate) => type === candidate || type.startsWith(candidate.split("/")[0] + "/"))) {
            return;
        }
        const title = session?.title || (session?.id ? `Session ${session.id}` : "Session");
        const needsAction = type.startsWith("ask/") || type.startsWith("question/") || type.includes("review");
        const notificationTitle = needsAction ? "任务需审批" : "任务已完成";
        const body = title;
        const priority = needsAction ? "high" : "default";
        const click = session?.id ? `dshmobile://host/current/session/${session.id}` : "";
        if (base && topic) {
            post({ title: notificationTitle, body, priority, tags: ["dsh"], click })
                .catch((error) => ctx.logger?.warn?.(error));
        }
        postUnified(`${notificationTitle}\n${body}`).catch((error) => ctx.logger?.warn?.(error));
    };

    ctx.on("session/event", (session, event) => publish(event, session));
    ctx.on("agent/error", ({ agent, error }) => {
        const session = agent?.session;
        const title = session?.title || "Session";
        if (base && topic) {
            post({
                title: "任务出错",
                body: title,
                priority: "high",
                tags: ["dsh", "error"],
            }).catch((cause) => ctx.logger?.warn?.(cause));
        }
        postUnified(`任务出错\n${title}\n${error?.message || String(error)}`).catch((cause) => ctx.logger?.warn?.(cause));
    });
    ctx.logger?.info?.("dsh-mobile-notify: publishing to " + base + "/" + topic);
}
