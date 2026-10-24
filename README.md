<div align="center">

<img src="docs/assets/app-icon.png" width="96" alt="DeepSeek Harness APP icon">

# DeepSeek Harness APP

**自托管 Android 客户端，给跑在你自己机器上的 DeepSeek Harness（DSH）用。**

手机是壳，界面是官方 DSH；原生层补上浏览器给不了的能力。

[![Release](https://img.shields.io/badge/release-1.1.18-0969da?style=flat-square)](https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3dd68c?style=flat-square)](#下载)
[![DSH](https://img.shields.io/badge/DSH-0.2.1-3964fe?style=flat-square)](#架构)
[![License](https://img.shields.io/badge/license-MIT-24292f?style=flat-square)](LICENSE)

**无云 · 无账号 · 无遥测 · 无内购** · 非官方社区项目

</div>

![DeepSeek Harness APP](docs/assets/hero.svg)

---

## 30 秒了解

| 你得到什么 | 说明 |
| --- | --- |
| **官方界面，手机上用** | WebView 里跑的就是 DSH 官方 Web UI，不是重新画的聊天界面 |
| **连你自己的服务器** | 直连 HTTP/HTTPS、局域网、relay 中继、Tailscale/Cloudflare/frp 隧道都支持 |
| **审批能直接处理** | 通知上带「同意 / 拒绝」，不用先打开网页 |
| **文件能管** | 上传、重命名、新建、下载、打包、递归删除、文本/图片预览 |
| **出问题能说清** | 连接体检、通知自检、离线快照、页面事件时间线 |
| **升级不靠人记** | 自托管 `latest.json`，版本号严格递增，覆盖安装即可 |
| **2.5 MB 的安装包** | 1.1.0 删掉了 9.4 MB 用不到的原生 OCR，下载和升级都快了 5 倍 |
| **分享进来，通知里直接回** | 任何 app 都能把文字/链接分享进来；完成和提问通知带回复框 |

> ⚠️ 这是一个**非官方**项目，与 DeepSeek 官方无关。「DeepSeek Harness」与鲸鱼标识归其各自所有者。

---

## 1.1.17：顶栏收紧，输入框离底部留出一点距离

| 问题 | 修法 |
| --- | --- |
| 顶部空太多：标题在状态栏下面 58px 处，下面到第一条消息还有一段空白 | 主题给手机已隐藏的 `对话/轨迹` 预留了 65px 标题行，34px 的图标行被垂直居中在一个 76px 的带子里。现在把行高钉成 34px、`grid-template-rows: auto 0px`、上下 2px 内边距——头部 76px → 39px，标题回到状态栏下方 19px。 |
| 输入框贴着底部手势条 | 主题的 composer 只留 8px 就到底了。手机形态给 `composer-stack` 加 12px，输入卡片底部从 820 升到 808（视口 852），手势条上方留出干净的一条。 |

## 1.1.16：手机端接住 Claude 主题（dsh-claude-style）

| 问题 | 修法 |
| --- | --- |
| 装了 `dsh-claude-style` 后，手机顶栏的字全叠在一起：`1 个后台任务运行中` 从 254px 一直画到 432px，压住 `…`（298）和右侧栏按钮（343），会话标题被挤成 11px | 主题是按桌面窗口排的。手机形态下隐藏 `对话/轨迹` 切换（你要求的）、隐藏子智能体计数（团队按钮保留），后台任务只留转圈图标，标题拿到整行剩余宽度并省略号。 |
| 底栏模型名叠成双影：`DeepSeek-V4.1-Flash` 印在 `DeepSeek-V4.1-Flash` 上 | 主题自己画了一套模型控件并藏掉了核心触发器，而插件从 1.1.3 起用 `::after` 重写核心触发器的标签——两套叠在同一个坐标。现在这套重写在主题的按钮上直接取消，只保留主题自己的标签。 |
| 抽屉里点会话，会话打开了、抽屉却还盖在上面 | 主题的侧栏不认识手机的抽屉。点 `会话行 / 新会话 / 面板行` 后 90ms 关闭抽屉（先让行自己的跳转跑完）。 |

---

## 1.1.14：底栏数字靠拢，外加"最慢资源"一目了然

| 问题 | 修法 |
| --- | --- |
| 底部 `198 tok/s`、`2909M tok · 99%`、`37%` 三个读数隔着一个拇指远 | 那个 dock 在某些版本里按**网格等宽列**排版，所以每个读数都占 1/3 行宽；只加 `justify-content: flex-start` 没用。现在强制 `display: flex` + 子项 `width: auto`，三个读数会挤在左边、间距 10px。 |
| 启动 `first=631ms` 但 `ready=5921ms`，6 秒才 finished | 页面健康多了 `slowRes=`：列出最慢的三个**子资源**（图片/脚本/样式表）。`dcl` 早就好了却迟迟不 finished，就是某个资源在拖，这一项直接点名。 |

---

## 1.1.13：诊断终于看的是"刚才"，更新弹窗也换了皮

| 问题 | 修法 |
| --- | --- |
| 切会话后打开诊断，看到的还是刚启动那条（`up=1s`） | 页面健康原来只在文档加载后 900ms 采样一次，之后打开诊断读的是缓存。现在**打开诊断就先采一次**再显示——切完会话立刻看，`slow=` 里才有消息接口。 |
| 「正在下载更新」是系统默认弹窗，跟别处不像一个 App | 换成 App 自己的卡片：同样的圆角、同样的标题/副标题字号、同样的蓝色进度条，和设置页一个语言。 |

---

## 1.1.12：会话切换慢，先分清是"拉"还是"画"

| 问题 | 修法 |
| --- | --- |
| 切会话、翻历史时卡，但不知道是网络还是渲染 | 页面健康里的 API 统计多了**最慢的三个请求**：`\| slow=api/sessions/xxx/messages 1240ms!`。之前只统计"调了几次"，现在每个请求都记耗时（失败带 `!`）。切一次会话再看这一行：如果消息接口自己就 1 秒多，问题在拉取；如果接口 120ms 而界面还是卡，问题在渲染。 |

---

## 1.1.11：想知道启动慢在哪，先把它量出来

| 问题 | 修法 |
| --- | --- |
| 想优化启动，但没人知道时间花在哪 | 诊断新增一行**启动耗时**：`first=640ms ready=1850ms ttfb=120 fcp=690 dcl=2100`。前两个是原生侧（点开 → 首帧、→ DOM 就绪），后三个是页面自己的（请求等到首字节、首次内容绘制、脚本跑完）。哪一段大，就说明问题在哪一段。 |

**为什么不做"先渲染骨架、再拉数据替换"**：这在 SPA + WebView 里是**两次渲染**——骨架画一遍、真页面再画一遍，WebView 的合成器要重建，而且替换会打断 React 自己的挂载（正是前面几版花力气修掉的"乱刷新"来源）。现在真正省时间的是暖启动时用 OkHttp **预取主文档**（已有，350ms 预算内直接交给 WebView，省一次往返）。

---

## 1.1.10：上传有进度、下载真的能存、断网也有东西看

| 问题 | 修法 |
| --- | --- |
| 传大文件像卡住 | 上传改用 `XMLHttpRequest` 带进度：底部出现「1/2 上传 report.zip · 47%」，完成自动消失；失败**留下原因**（`HTTP 413 · 文件过大` 这种），不再只弹一句"上传失败"。 |
| 文件面板的「下载」点了没反应 | 之前用 `blob:` URL + `a.download`，WebView 会**静默忽略**——手机上等于没实现。现在整条走系统下载管理器：通知栏有进度、完成后可点开，文件名取真实路径（不再是 `dsh-file-ops.bin`）。 |
| 长会话滚动卡不卡没人知道 | 诊断的页面健康多了 `scrollMin=54@7`：只在滚动期间用 `requestAnimationFrame` 采样帧率，记录最低值与样本数，滚动停止 250 ms 就结束，不驻留定时器。先有数据，再决定要不要做"只保留最近 N 条"。 |
| 断网打开只有一页错误 | 错误页**直接列出**上次同步的最近 3 个会话（带「进行中 / 待处理」标记），不用先点"离线查看最近会话"。 |
| 两套设置页、两个没人调用的桥方法 | 删掉 1.1.6 之后就没有调用者的旧设置页（约 260 行）和 `deviceSummary`/`openDevices`。留着它们的代价是下次改设置页容易改到没在跑的那一份。 |

---

## 1.1.9：附件按钮直通文件选择器

| 问题 | 修法 |
| --- | --- |
| 点回形针先弹「上传附件 / 上传图片」，还得再点一次 | 插件现在监听这个菜单**出现**就自动点「上传附件」，直接进 Android 文件选择器。 |
| 为什么选「附件」而不是「图片」 | 文件选择器里照样能选相册照片，反之不行。要传文档、压缩包、PDF 时不用再换一条路。 |
| 以前那版为什么失效了 | 旧实现靠触发器的 class/aria-label，再找 `button`/`menuitem` 行；菜单改版后这两条假设都不成立，于是菜单就干停在那儿。新实现完全不碰触发器，只按行的文字找人。 |

---

## 1.1.8：连接异常自动重连

| 问题 | 修法 |
| --- | --- |
| 断线后只给一个「连接异常，刷新重试」，要自己点 | 检测到这个橙色状态条**持续 2.6 秒**就替你点下去，点之前先弹一行「连接异常，正在自动重连…」。 |
| 会不会又变成「乱刷新」 | 只在三件事同时成立时动作：状态条真的稳定存在（短暂抖动会自己恢复）、输入框里没有未发送的草稿（重载会吞掉文字）、页面在前台。**2 分钟内最多 3 次**，计数存在 sessionStorage 里，刷页面也重置不了。 |
| 想知道它替我点了几次 | 设置 → 诊断 → 页面健康那一行多了 `autoRetry=N`。 |

---

## 1.1.7：设备管理收进设置，去掉重复入口

| 问题 | 修法 |
| --- | --- |
| 换个设备要点三层（设置 → 连接 → 设备管理） | 设置第一屏的「连接」**直接打开设备管理**：设备列表、切换、改名、删除、添加都在这一页；副标题顺带显示「当前：家 · 2 台设备」。 |
| 页面顶栏又多一个设备按钮 | 移除 1.1.6 加的那个顶栏设备药丸，设备入口只剩设置一处。 |
| 扫码 / 配对码 / 局域网扫描没地方去了 | 收进设备管理底部的「更多连接设置」——层级没有变深，只是换了个门。 |

---

## 1.1.3：重连不误报、模型名完整

| 问题 | 修法 |
| --- | --- |
| Wi-Fi 抖一下就报「连接失败」 | 失败不再逐次上报：连续 3 次失败**且**持续 8 秒以上才显示错误，成功立刻清空。期间状态只显示"重连中"。 |
| 「一直重连」停不下来 | 连续 20 次失败或持续 3 分钟就刹车：停止自动重连，并明确告知"需要手动重连"。手动连接/网络恢复会重新计数。 |
| 多个入口同时重连，互相打断 | 重建连接加了单飞：网络恢复、后台保活、界面重试三个入口合并成一个周期，不再各自启动一条连接。 |
| 被 nginx 限流（429）后越试越糟 | 识别 429：暂停自动重连 1 分钟再试一次，而不是每 500ms 撞一次桶。 |
| 模型名被截断、看不到思考强度 | 模型按钮往左扩 10px、字号 11.5px，并把一直隐藏的「思考强度」并进同一行：`DeepSeek-V4.1-Flash · 高`。 |
| 流式时后台还在做无用功 | 投影帧改走 50ms 节流（不再每帧同步全量重折）、无人订阅的 hostEvents 不再每个事件造对象、每帧 logcat 改成限流日志、insets 相同值不再发给渲染进程。 |

---

## 1.1.2：设置页重做

| 问题 | 修法 |
| --- | --- |
| 最后一行被屏幕切掉 | 弹层原来按内容高度铺满，超出屏幕的部分直接丢失——「刷新页面」就是这么没的。现在整页包在 ScrollView 里，高度封顶 88%，并留出导航栏安全区；**所有弹层**都受益。 |
| 「刷新」又小又难找 | 刷新和检查更新从"列表里的一行"变成顶部两个并排的胶囊按钮（46dp 触控，蓝色图标），第一屏就能点到。 |
| 一列 13 行没有层次 | 按功能分五组：连接 / 这台手机 / 通知与后台 / 数据与链接 / 诊断与支持，每组一个浅灰标题；退出设备仍在最底下、红色。 |

---

## 1.1.1：把「乱刷新」查到底

这版是稳定性专项。先说结论：**最可能的真凶不在 app 里，而在你电脑上装的那个插件包**。

| 发现 | 说明 |
| --- | --- |
| **主机插件被钉在 0.1.7** | DSH 桌面 profile 的 `package.json` 里，`@dsh-android/deepseek-harness-app-plugin` 指向 `dist/…-0.1.7.tgz`。那个包里的 `client.js` 第 258 行就是 `window.setInterval(compactStats, 2000)` —— **每 2 秒重写一次模型和速度读数**。所以就算 app 侧把定时器删了，只要依赖还钉在老包上，症状就会原样回来。 |
| **仓库里还留着两个会复现它的老包** | `mobile-web-plugin/`（`setInterval` 3000 / 2000 / 1500 三个都在）和 `host-plugin/`，而文档还在教人装它们。1.1.1 把两个目录删掉、文档改成只有一个 `combined-plugin/`。 |
| **shell 自己也会让页面重排** | `viewportSync()` 原来在"有变化"时 dispatch 一个**合成 `resize`**。而 DSH 在 resize 时会**重挂 composer 并重读模型列表** —— 这就是"它自己刷新了"。现在不再发合成事件（真被系统 resize 时平台本来就会发），改用布局视口 `innerHeight` 而不是 `visualViewport.height`，并且只在布局高度真变了才写。 |
| **一次白闪的根因** | `applyPageTheme()` 没有差异判断：页面每次因主题相关渲染重设 `data-ds-dark-theme`，bridge 回调都会重刷整块 WebView 背景 + 4 个子视图 + 两条系统栏。暗色下就是一次白闪。现在加了提前返回，只有重建 WebView 才强制重刷。 |
| **每次加载多抓一份 HTML** | 预取拦截里比较的是 `…?dsh_android=1` 和裸地址，永远不相等 → 预取从未命中，每次加载都抓两遍文档。已改成去 query 比较。 |
| **打开会话被导航两次** | `onPageCommitVisible` 和 `onPageFinished` 都调 `openPendingSession()`，第一次的回调还没回来第二次就发了。加了 in-flight 守卫。 |
| **UI 线程读盘** | 5 处 `runBlocking { hostsStore.settings.first() }` 在打开设置/通知页时阻塞主线程，换成 onCreate 里已经在收集的缓存镜像。 |
| **给"谁刷新的"装上了仪表** | 诊断页新增**页面健康**：`sync/layoutWrite/composerRemount/api` 计数（2 分钟窗口），以及**自行重载次数** —— 页面事件里凡是没有任何 shell 动作能解释的主框架加载，会被标成 `SELF` 并单独计数。下次再"自己刷新"，打开 设置 → 诊断 → 复制，一行字就能定位是谁。 |

---

## 1.1.0 更新了什么

| 变化 | 说明 |
| --- | --- |
| **安装包 12.34 MB → 2.47 MB** | `ui/` 里 25 k 行 Compose 没有任何入口能到达，R8 早就把它从 dex 里删了；但只有它在用的 ML Kit 中文 OCR 删不掉，9.39 MB 原生库占了下载量的 76%。整个目录已删除，Compose 与 ML Kit 依赖一并移除。旧界面留在 git 历史 `v1.0.5`。 |
| **注入脚本里再没有周期性定时器** | 两个 `setInterval`（3 秒结构探测、1.5 秒文件行装饰）改成 MutationObserver。这是「底部模型和速度自己刷新」最后一个可能的来源。 |
| **重连看得懂网络** | 新增 `NetworkCallback`：Wi-Fi/移动网络切换时立刻重连，并给页面发一次 `online` 事件，不再干等 30 秒退避。每次变化都写进诊断时间线，下次再「自己刷新」能说出是谁干的。 |
| **新入口** | 任意 app 分享文字/链接进来（插进输入框，失败落到剪贴板）；`dshmobile://` 链接可从外部打开；网页链接处理是**默认关闭**的开关，开了才会出现在系统「打开方式」里，且只接管你已保存的地址，其余交给浏览器。 |
| **设置能带走** | 导出设置写成一个可读 JSON（设备、主机、开关），导入会逐条校验并告诉你丢了什么。**永远不含凭据**；`allowBackup` 也已关闭。 |
| **通知能直接回复** | 完成/提问通知带回复框，文字按 queue 模式发到该会话。审批通知仍然只有同意/拒绝。 |
| **两半会互相校对** | shell 读主机插件版本并与 app 内期望值比较，设置页和连接体检直接指出是哪一半旧了。 |

---

## 为什么是这样设计的

1. **界面属于 DSH，不属于这个 App。** 手机端不重写聊天 UI，DSH 更新了界面，手机上立刻就是新的。
2. **没有中间人。** 没有项目云、项目 relay、项目推送；relay / ntfy / UnifiedPush 都是你自己跑的。
3. **失败必须可解释。** 「连不上」「没通知」「页面刷新了」都应该有一个页面告诉你原因，而不是靠猜。
4. **地址可以变，数据不能丢。** DSH 端口每次重启可能变；App 存设备、存令牌、存离线快照。

---

## 功能

### 连接

| 能力 | 说明 |
| --- | --- |
| 多设备管理 | 保存多个地址，随时切换、重命名、删除 |
| 简单模式（默认） | 连接页只留「管理设备 / 网络信息 / 连接体检 / 测试连接」 |
| 地址方式 | `http(s)://` 直连、局域网 IP、relay 中继、任意隧道域名 |
| 扫码 / 配对码 | 电脑上生成邀请，手机扫码或输入 8 位码（高级模式） |
| 局域网扫描 | mDNS + 网段探测；只对绑定局域网地址的 DSH 有效（Desktop 默认只听 127.0.0.1） |
| 连接体检 | DNS → TCP → TLS 证书 → HTTP/网关 → 主机插件 → 中继 → 页面连接，逐项给结论和修法 |
| 设备健康 | 并行探测所有已保存设备，标出在线/离线/证书未信任 |

### 会话

| 能力 | 说明 |
| --- | --- |
| 收件箱 | 按「等待审批/提问 → 运行中 → 最近」排序，支持搜索 |
| 会话驾驶舱 | 模型、token、解码速度、首字延迟、上下文压力、轮次 |
| 通知 | 任务完成、审批、提问、目标事件；内联「同意 / 拒绝 / 打开」 |
| 离线快照 | 断网时仍可查看最近一次同步的会话列表（只读） |
| 跨主机总览 | 每台主机并行拉一次会话数，点一下切过去 |

### 文件

| 能力 | 说明 |
| --- | --- |
| 浏览 / 上传 | 头部上传、新建文件夹 |
| 行内操作 | 下载、重命名、删除（删除即时生效，无二次确认） |
| 预览 | 文本（256 KB）与图片（6 MB）直接看，其他类型走下载 |
| 打包 / 删除 | 文件夹下载自动 zip；递归删除 |
| 审计 | 所有写操作追加到本地 audit log |

### 壳与诊断

| 能力 | 说明 |
| --- | --- |
| 排版自动修复 | 键盘、旋转、切前台后重新测量；手动「修复界面排版」可强制重绘 |
| 玻璃主题兼容 | 检测 `backdrop-filter` / 整屏 blur，只关磨砂层，保留配色 |
| 诊断页 | WebView 版本、页面尺寸、渲染崩溃次数、最近 24 条页面事件 |
| 通知自检 | 权限、系统开关、通道重要性、电池优化豁免、推送配置 |
| 更新 | `latest.json` 优先，GitHub API 兜底；插件版本不匹配会提示 |

---

## 架构

![Architecture](docs/assets/architecture.svg)

- **App**：Kotlin + WebView。可见的界面是 DSH 官方 Web UI；原生层负责连接、通知、文件、诊断。
- **主机插件**：一个 DSH 插件同时提供 `/dsh-mobile` 状态、`/dsh-file-ops` 文件操作、
  `/dsh-mobile/relay/pair` 配对邀请，以及 BrowserSkill 的 `browser_*` 工具。
- **中继（dsh-relay）**：跑在 DSH 进程里，固定监听 `3443`，负责 TLS、配对、设备撤销，并把请求转发给当前端口的 DSH。
- **推送（可选）**：自己部署的 ntfy，或 UnifiedPush。

---

## 四种连接方式

![Connection paths](docs/assets/connection-paths.svg)

| 场景 | 地址长什么样 | 怎么搭 |
| --- | --- | --- |
| 同一个 Wi-Fi | `https://192.168.2.13:3443` | 什么都不用做 |
| 在外面用，不暴露公网 | `https://100.x.y.z:3443` | 装 Tailscale，两台设备登录同一账号 |
| 有域名、无公网 IP | `https://dsh.example.com` | Cloudflare Tunnel，指向 `https://127.0.0.1:3443` |
| 有自己的 VPS | `https://vps-ip:3443` | frp TCP 3443 映射 |
| 直接 HTTP 网关 | `https://你的域名/`（Caddy/nginx） | 反向代理到 DSH 或中继 |

**为什么总是 3443？** DSH Desktop 的 Web 端口每次重启都可能变，中继端口固定，并且在 DSH 进程内知道真实端口。隧道指向中继，DSH 换端口也不会 502。

完整步骤（Tailscale / Cloudflare Tunnel / frp / 端口映射，含配置文件）：
**[docs/SETUP_HTTP.md](docs/SETUP_HTTP.md)**

---

## 配对流程

![Pairing flow](docs/assets/pairing-flow.svg)

- **配对码在电脑上生成**：打开 `https://电脑IP:3443/relay/pair`，或运行 `node setup/dsh-mobile-setup.mjs pair`。
- 手机扫码或输入 8 位码，完成一次即可；之后手机保存的是**中继根地址**，不会再回到配对页。
- 手机「设置 → 远控」里的二维码是**给第二台设备的邀请**，不是本机配对用的。

---

## 快速开始

### 1. 主机端（跑 DSH 的电脑）

```bash
git clone https://github.com/daniei-chen/DeepSeek-Harness-APP.git
cd DeepSeek-Harness-APP
node setup/dsh-mobile-setup.mjs install --profile desktop \
  --file-root "$HOME" --relay-port 3443 --yes
```

装好后 **重启 DSH**（插件只在进程启动时加载）。

```bash
# 看一眼状态；拿一份配对码
node setup/dsh-mobile-setup.mjs status --json
node setup/dsh-mobile-setup.mjs pair
```

### 2. 手机端

- 下载 APK：[最新 Release](https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/latest)
- 或：`adb install -r DeepSeek-Harness-1.1.18-personal.25.apk`

### 3. 第一次连接（简单模式）

1. 打开 App → **设置 → 连接 → 添加设备**
2. 填你自己的地址，例如 `https://192.168.2.13:3443` 或 `https://dsh.example.com`
3. 完成一次 DSH 登录 / 中继密码
4. 以后启动直接进

「连接」打开的就是设备管理：切换、改名、删除都在那一页。需要扫码/配对码/局域网扫描时，点那一页最下面的「更多连接设置」。

---

## 给 AI / 自动化安装

完整安装手册（版本、SHA-256、命令、验证、排错表）：
**[docs/INSTALL_FOR_AGENTS.md](docs/INSTALL_FOR_AGENTS.md)**

仓库规则（versionCode、重启、bundle 注册、发布纪律）：
**[AGENTS.md](AGENTS.md)**

---

## 下载

| 项目 | 版本 | 文件 | SHA-256 |
| --- | --- | --- | --- |
| App | **1.1.18**（`versionCode 50118`） | `DeepSeek-Harness-1.1.18-personal.25.apk` | `11C4B39C4F9AFBE09B16E247656C90A22091C76F6597E39646337BF96C4BE59C` |
| 主机插件 | **0.1.37** | `dsh-android-deepseek-harness-app-plugin-0.1.37.tgz` | 见 Release 页面 |

- [App Release](https://github.com/daniei-chen/DeepSeek-Harness-APP/releases/latest)
- [插件 Release](https://github.com/daniei-chen/DeepSeek-Harness-APP/releases)
- 更新检查读取仓库根目录的 [`latest.json`](latest.json)

---

## 从源码构建

```bash
./gradlew :app:testDebugUnitTest     # 单元测试
DSH_VERSION_NAME=1.1.18 ./gradlew :app:assembleRelease
```

签名用环境变量（不传则产出未签名包）：
`DSH_KEYSTORE` · `DSH_KEYSTORE_PASSWORD` · `DSH_KEY_ALIAS` · `DSH_KEY_PASSWORD`。
公开 APK 用项目个人密钥（`CN=DSH Kaogong Fork`），升级必须同签名。

主机端测试：

```bash
node --test combined-plugin/test/*.test.mjs setup/lib.test.mjs
node setup/dsh-mobile-setup.mjs install --dry-run --json
```

---

## 项目结构

| 路径 | 内容 |
| --- | --- |
| `app/` | Android 应用；真正的界面在 `webshell/WebShellActivity` |
| `core/` | 线协议、relay 配对、可测试的解析逻辑 |
| `combined-plugin/` | 主机插件：`/dsh-mobile`、`/dsh-file-ops`、手机端 WebView 适配 |
| `setup/` | 一键安装器、`status`、`doctor`、`pair`、可回滚 `uninstall` |
| `notify-plugin/` | 自托管 ntfy 发布器 |
| `vendor/` | 为 DSH 0.2.1 重新打包的 responsive 基础插件 |
| `docs/` | 安装、连接、隧道、文件、通知、架构、发布说明 |

---

## 文档

| 文档 | 内容 |
| --- | --- |
| [远程访问：打隧道 / HTTP](docs/SETUP_HTTP.md) | Tailscale、Cloudflare Tunnel、frp、端口映射全流程 |
| [连接](docs/CONNECTIONS.md) | 设备、局域网、中继、证书信任 |
| [自托管](docs/SELF_HOSTING.md) | 主机端组件与安装器细节 |
| [文件操作](docs/FILE_OPERATIONS.md) | 路由、根目录限制、审计 |
| [通知](docs/NOTIFICATIONS.md) | 通道、内联操作、ntfy / UnifiedPush |
| [架构](docs/ARCHITECTURE.md) | 模块边界与数据流 |
| [发布说明](docs/RELEASE.md) | 每个版本改了什么 |
| [给 AI 的安装手册](docs/INSTALL_FOR_AGENTS.md) | 可复制命令 + 排错表 |

---

## 安全模型

- App 不保存中继密码；配对后的设备令牌只存在本机。
- 自签名证书**按 host:port 单独信任**，第一次连接时显式确认。
- 公网访问必须自己加保护：中继密码、DSH 登录、或 Cloudflare Access。
- 文件操作限定在安装器配置的根目录内，并写本地审计日志。
- 没有项目服务器，因此也没有「项目被拖库」这条风险路径。
- **不进云备份**：1.1.0 起 `allowBackup="false"`，中继令牌、主机列表、会话快照不再进入安卓自动备份；换机走设置里的显式导出，**导出文件不含任何凭据**。
- **网页链接处理默认关闭**：开成 opt-in 后也只接管与你已保存设备同源的链接，其余交给浏览器。

细节见 [SECURITY.md](SECURITY.md)。

---

## 边界与路线图

**本版不做**

- 多主机实时审批聚合（需要每台主机常驻事件流）
- 设备级权限 / 审计归属（需要 relay 注入设备身份）
- 原生 PTY、实时浏览器画面、插件市场、自动化配方

**已经规划**

- 一个主机多个候选地址，启动时并行探测、自动切换
- 中继带路：手机向中继查询当前上游，DSH 换端口自动更新
- 上传进度 / 取消 / 重试
- 应用锁（指纹/面容）

---

## 致谢与许可

- MIT，见 [LICENSE](LICENSE)、[NOTICE.md](NOTICE.md)、[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
- 内置的 BrowserSkill、responsive 基础插件等按各自许可证分发
- 非官方项目；「DeepSeek Harness」与鲸鱼标识归其各自所有者
