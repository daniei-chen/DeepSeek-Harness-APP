# Set up HTTP / HTTPS for DeepSeek Harness

The app is only a client. It does **not** ship a server, relay or hosted address. You point it at
the DSH web UI you run.

There are four common setups. Start with the one that matches your network.

## 0. Find the DSH web port

Run DSH on your computer and open its local web UI in a browser. Note the port it prints (for
example `3080`). Keep it bound to loopback while you put a reverse proxy in front:

```text
http://127.0.0.1:3080
```

## 1. Same Wi-Fi: plain HTTP (simplest)

If DSH is bound to the LAN interface and both devices are on the same trusted network:

```text
http://192.168.1.50:3080
```

Add that address in the app:

**Settings → Devices → Add device → enter URL.**

Plain HTTP means anyone on that Wi-Fi can read the traffic. Use it only on a trusted home/lab
network, never on public Wi-Fi.

## 2. Public HTTPS with Caddy (recommended)

Caddy obtains and renews a certificate automatically.

```caddyfile
harness.example.com {
    reverse_proxy 127.0.0.1:3080
}
```

Point the DNS `A/AAAA` record at your server, open ports 80/443, then add
`https://harness.example.com` in the app.

## 3. Public HTTPS with nginx

```nginx
server {
    listen 443 ssl http2;
    server_name harness.example.com;

    ssl_certificate     /etc/letsencrypt/live/harness.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/harness.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:3080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;

        # WebSocket / SSE used by the harness UI
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
        proxy_buffering off;
    }
}
```

Get a certificate with certbot or your ACME client, then reload nginx. Add the HTTPS URL in the
app.

**Do not expose DSH without authentication.** Put the harness gate/access code in front, or use
your reverse proxy's auth. The project's own deployment uses a gate cookie; a bare DSH port is not a
public service.

## 4. No public IP: a tunnel you run

Pick one, run it yourself, and use the resulting HTTPS URL in the app:

- **Cloudflare Tunnel**: `cloudflared tunnel --url http://127.0.0.1:3080`
- **frp**: expose the local DSH port through your own frps server
- **Tailscale**: join both devices to your tailnet and use the machine's Tailscale HTTPS/MagicDNS
  address
- **WireGuard**: connect the phone to your private VPN and use the LAN address

The project provides **no official relay** and **no hosted cloud**. These are examples of services
you operate.

## 5. First launch in the app

A fresh install opens **Settings** first. It does not contain anyone's personal DSH URL.

1. Tap **Devices**.
2. Tap **Add device**.
3. Enter your own `http://` or `https://` address.
4. Complete the DSH gate/login page once.
5. The app remembers the device; later launches go straight to it.

Use **Settings → Scan local network** if DSH is advertising on the same LAN.

## Security checklist

- [ ] HTTPS for anything outside a trusted LAN.
- [ ] Gate/access code or reverse-proxy auth enabled.
- [ ] Firewall: only 80/443 (or your tunnel) exposed; DSH port stays on loopback/LAN.
- [ ] Backups of the DSH profile and workspace.
- [ ] Never commit gate cookies, private keys or signing keystores.
