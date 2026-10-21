# Connections

The app deliberately does **not** ship an official relay or hosted service. Choose the route that
fits your network.

| Route | Works away from home | Setup | Notes |
|---|---|---|---|
| LAN direct | No | Easy | Same Wi-Fi; use your host's LAN address |
| Your own HTTPS | Yes | Medium | Domain + reverse proxy; recommended default |
| Self-hosted tunnel | Yes | Medium | Cloudflare Tunnel / frp / Tailscale |
| WireGuard / Tailscale | Yes | Medium–hard | Private network; system VPN |
| SSH port forwarding | Yes | Hard | Advanced users only |

## LAN scan and loopback

DSH Desktop deliberately binds its web server to `127.0.0.1`. A phone cannot reach that listener
over Wi-Fi, and the app's scan does not pretend otherwise. **Settings → Connection → Scan local
network** now sweeps the configured harness ports plus `3080`, reports progress, and also looks for
`dsh-relay` on `3443/3444`. If nothing is found, it shows the phone's subnet, the ports tried and
the three common causes (loopback-only DSH, firewall, guest-Wi-Fi isolation).

For a Desktop host, install `dsh-relay` and pair once. The relay listens on `0.0.0.0:3443`, keeps
the harness on loopback, and terminates TLS with a self-signed certificate by default. The app asks
once before trusting that certificate for the exact `host:port`; it never disables certificate
checks globally.

## Profiles

The app stores connection profiles (name + URL) so you can switch between a home LAN address and
a public HTTPS address without re-entering credentials. The current profile is persisted and
re-used on the next launch.

## No official relay

There is no `relay.example.com` operated by this project. If you need NAT traversal, run your own
tunnel or VPN and point the app at the resulting HTTPS URL. This keeps the project free of
per-user bandwidth, accounts and data custody.

## Future: local gateway

A future version may run a loopback gateway inside the app so the WebView origin stays stable while
the native layer switches between LAN/HTTPS/VPN transports. It is not required for the first
open-source release.
