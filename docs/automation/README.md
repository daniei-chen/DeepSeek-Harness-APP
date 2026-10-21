# CI and release automation

GitHub only runs workflows from `.github/workflows/`. These files are kept here because the
current release token is an OAuth app without the **workflow** scope, so a push that adds them is
rejected with:

```
refusing to allow an OAuth App to create or update workflow ... without `workflow` scope
```

## Enable them

```bash
gh auth refresh -s workflow          # or use a PAT with the workflow scope
mkdir -p .github/workflows
cp docs/automation/ci.yml docs/automation/release.yml .github/workflows/
git add .github/workflows && git commit -m "ci: enable test and release workflows" && git push
```

## What they do

- `ci.yml` - on every push to `main` and every PR: host/plugin unit tests, an installer dry run,
  and the Android unit tests.
- `release.yml` - on a `v*` tag (or manual dispatch): builds the APK with `DSH_VERSION_NAME`, checks
  that `versionCode` rose, publishes the GitHub release and refreshes `latest.json`.

## Secrets

| Secret | Value |
| --- | --- |
| `DSH_KEYSTORE_BASE64` | `base64 -w0 dsh-kaogong.jks` |
| `DSH_KEYSTORE_PASSWORD` | keystore password |
| `DSH_KEY_ALIAS` | key alias |
| `DSH_KEY_PASSWORD` | key password |

Without `DSH_KEYSTORE_BASE64` the workflow still builds, but the APK is unsigned and cannot
upgrade an installed copy.
