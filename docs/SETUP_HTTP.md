# 远程访问：打隧道 / HTTP / HTTPS

这个 App 只是客户端，不带服务器、不带中转。你要做的是：**让手机能访问到电脑上的 DSH**。
本文按「你有没有公网 IP / 域名 / VPS」给出几种方案，选一个照做即可。

> 先说结论：**最省心的是把隧道打到中继的 3443 端口**，而不是 DSH 的随机端口。
> DSH Desktop 每次重启端口都可能变（56072 → 63396 → 62128 …），而中继固定监听 3443，
> 并且在 DSH 进程内知道当前真实端口。隧道指向 3443，就永远不会因为 DSH 换端口而失效。

---

## 0. 先确认三件事

1. **DSH 在跑**：电脑上能打开 DSH 的 Web UI。
2. **中继在跑**（推荐）：电脑上打开
   `https://127.0.0.1:3443/relay/health`，应返回 `{"service":"dsh-relay","ok":true}`。
   没有的话在仓库根目录运行：
   ```bash
   node setup/dsh-mobile-setup.mjs install --profile desktop --file-root "$HOME" --relay-port 3443 --yes
   ```
   然后重启 DSH。
3. **知道自己的网络类型**：

| 你的情况 | 用哪个方案 |
| --- | --- |
| 手机和电脑同一个 Wi-Fi | 方案 A（局域网直连） |
| 想在外面用，但不想暴露到公网 | 方案 B（Tailscale，推荐） |
| 有域名，想用公网 HTTPS | 方案 C（Cloudflare Tunnel） |
| 有自己的 VPS | 方案 D（frp） |
| 有公网 IP + 会端口映射 | 方案 E（端口映射 + Caddy） |

---

## 方案 A：同一个 Wi-Fi（最简单）

手机直接访问电脑的局域网地址 + 中继端口：

```text
https://192.168.2.13:3443
```

- 中继是自签名证书，第一次打开时 App 会弹「是否信任」，点信任即可（只对这个 host:port 生效）。
- 手机和电脑必须在同一个 Wi-Fi；访客 Wi-Fi / IoT Wi-Fi 通常做了客户端隔离，连不上。
- 不需要任何隧道。

App 里：**设置 → 连接 → 管理设备 → 添加设备**，填这个地址。

---

## 方案 B：Tailscale（推荐，零公网、最安全）

适合「人在外面，但不想把 DSH 暴露到互联网」。

1. 电脑和手机都安装 Tailscale，并登录**同一个账号**。
2. 电脑上找到 Tailscale IP（`100.x.y.z`）：
   ```bash
   tailscale ip -4
   ```
3. 手机上直接访问：
   ```text
   https://100.x.y.z:3443
   ```
   （中继监听 0.0.0.0:3443，Tailscale 网络内可达。）

优点：不开放任何公网端口，不依赖域名，速度通常比公网隧道好。
注意：手机必须开着 Tailscale；第一次信任自签名证书。

App 里添加 `https://100.x.y.z:3443` 即可。

---

## 方案 C：Cloudflare Tunnel（有域名，公网 HTTPS）

适合「想用 `https://dsh.example.com` 这种地址，且没有公网 IP」。

### C1. 安装 cloudflared

- Windows：`winget install --id Cloudflare.cloudflared`
- macOS：`brew install cloudflared`
- Linux：按 Cloudflare 官方仓库安装

### C2. 登录并创建隧道

```bash
cloudflared tunnel login
cloudflared tunnel create dsh
```

记下输出的 Tunnel ID，凭证文件在 `~/.cloudflared/<ID>.json`（Windows 是
`C:\Users\<你>\.cloudflared\`）。

### C3. 写配置

`~/.cloudflared/config.yml`：

```yaml
tunnel: <TUNNEL_ID>
credentials-file: /home/<你>/.cloudflared/<TUNNEL_ID>.json

ingress:
  - hostname: dsh.example.com
    service: https://127.0.0.1:3443
    originRequest:
      noTLSVerify: true      # 中继是自签名证书
  - service: http_status:404
```

> 关键点：`service` 指向 **3443（中继）**，不是 DSH 的随机端口；`noTLSVerify` 用来接受中继的自签名证书。

### C4. 绑定域名并启动

```bash
cloudflared tunnel route dns dsh dsh.example.com
cloudflared tunnel run dsh
```

### C5. 装成开机服务（可选）

```bash
sudo cloudflared service install     # Linux
cloudflared service install          # Windows（管理员 PowerShell）
```

### C6. 在 App 里填

```text
https://dsh.example.com
```

第一次打开会出现中继的登录/配对页。**一定要先在中继上设置密码**（见下面的安全清单），
否则拿到域名的人都能尝试访问。

> 临时试用：`cloudflared tunnel --url https://127.0.0.1:3443 --no-tls-verify` 会给你一个
> `https://xxx.trycloudflare.com` 地址，重启就变，只适合测试。

---

## 方案 D：frp（有自己的 VPS）

### D1. VPS 上跑 frps

`frps.toml`：

```toml
bindPort = 7000
auth.method = "token"
auth.token = "换成一串随机字符"
```

启动：`./frps -c frps.toml`（或做成 systemd 服务）。
VPS 防火墙放行 `7000/tcp` 和 `3443/tcp`。

### D2. 家里电脑跑 frpc

`frpc.toml`：

```toml
serverAddr = "1.2.3.4"          # 你的 VPS 公网 IP
serverPort = 7000
auth.method = "token"
auth.token = "换成一串随机字符"

[[proxies]]
name = "dsh-relay"
type = "tcp"
localIP = "127.0.0.1"
localPort = 3443
remotePort = 3443
```

启动：`./frpc -c frpc.toml`。

### D3. App 里填

```text
https://1.2.3.4:3443
```

中继是自签名证书，App 会提示一次信任。
如果想让地址变成 `https://dsh.example.com`，在 VPS 上用 Caddy 反代到
`https://127.0.0.1:3443`（需要跳过上游证书校验）：

```caddyfile
dsh.example.com {
    reverse_proxy https://127.0.0.1:3443 {
        transport http {
            tls_insecure_skip_verify
        }
    }
}
```

---

## 方案 E：公网 IP + 路由器端口映射 + Caddy

1. 路由器把公网 `443` 转发到电脑的 `443`（或把 `3443` 直接转发到 `3443`）。
2. 域名 A 记录指向公网 IP；没有固定 IP 就用 DDNS。
3. 用 Caddy 终止 TLS（推荐，自动证书）：

```caddyfile
dsh.example.com {
    reverse_proxy https://127.0.0.1:3443 {
        transport http {
            tls_insecure_skip_verify
        }
    }
}
```

4. App 里填 `https://dsh.example.com`。

> 运营商常封 80/443 入站；被封就用方案 C 或 D。

---

## 如果不想用中继：直接隧道 DSH 端口

可以，但必须**固定 DSH 的端口**，否则每次重启隧道就断：

```bash
dsh --profile web --host 127.0.0.1 --port 3080 --no-open
```

然后把上面的 `service` / `localPort` 改成 `3080`，并去掉 `noTLSVerify`（DSH 是明文 HTTP）。
App 里直接填隧道地址即可。缺点：DSH Desktop 自己启动的实例端口不固定，需要你手动用
`dsh web --port 3080` 跑一个固定端口。

---

## App 里怎么填（简单模式）

1. 打开 App → **设置 → 连接**；
2. **管理设备 → 添加设备**；
3. 输入完整地址（`https://...`，不要漏掉 `https://`）；
4. 第一次打开会走 DSH 登录 / 中继密码页，完成一次即可；
5. 以后启动直接进。

默认开启「简单模式」：扫码、配对码、局域网扫描都收起来了。需要时在连接页最下面点
「显示高级连接方式」。

---

## 验证是否打通

在电脑上（换成你的地址）：

```bash
curl -kI https://dsh.example.com
# 期望：HTTP/2 200 或 302/401（说明隧道通了，到了中继/DSH）
```

在手机浏览器打开同一个地址，能看到 DSH 或中继登录页，就说明网络层没问题；再回 App 添加。

---

## 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `502 Bad Gateway` | 隧道指到了错误的端口，或 DSH 没在跑 | 改成 `https://127.0.0.1:3443`（中继），并确认中继健康 |
| 页面能开但一直「连接中」 | 反向代理没转发 WebSocket | Caddy 默认支持；nginx 需要 `Upgrade` / `Connection` 头 |
| 证书错误 | 隧道/代理用的是自签名证书 | App 首次会提示信任；或给域名换正式证书 |
| 手机打开是空白 | 缓存了旧的失败页 | App 里「重载」或清除缓存 |
| 只有 Wi-Fi 能用，4G 不行 | 隧道没启动 / 防火墙没放行 | 检查 cloudflared/frpc 进程与端口 |
| DSH 重启后隧道 502 | 隧道指向了 DSH 的旧随机端口 | 隧道改指 3443（中继），或固定 `dsh web --port 3080` |
| Cloudflare 报 1033 | 隧道没运行 | `cloudflared tunnel run dsh` 并装成服务 |

---

## 安全清单

- [ ] **中继一定要设密码**：`https://127.0.0.1:3443/relay/password`，否则公网隧道等于把 DSH 公开。
- [ ] 公网只暴露隧道/443，DSH 端口保持 127.0.0.1。
- [ ] 优先 HTTPS；明文 HTTP 只用于可信局域网。
- [ ] 不用时关掉隧道进程，或给 Cloudflare 加 Access 策略。
- [ ] 定期备份 DSH profile 和工作区。
- [ ] 不要把 gate cookie、私钥、keystore 提交到仓库。
