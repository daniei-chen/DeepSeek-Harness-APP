# Security policy

## Never commit

- DSH gate cookies or session tokens;
- Android keystores (`*.jks`, `*.keystore`) or signing passwords;
- personal domains, IP addresses, SSH keys or workspace paths;
- APKs or build outputs containing signing material.

## Reporting

Please open a private security advisory on GitHub instead of a public issue for vulnerabilities.

## Scope

The app is a WebView shell for a self-hosted DSH instance. The security of the DSH host, its
reverse proxy, gate cookie and workspace permissions remains the operator's responsibility.
