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
