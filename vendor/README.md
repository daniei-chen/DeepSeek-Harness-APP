# Vendored responsive base plugin

`dsh-mobile-setup.mjs` installs this tarball as
`@dsh-android/dsh-client-ui-responsive@0.3.4-dsh021.2` so a fresh profile does not depend on a
personal `file:local/...` path.

- Upstream: https://github.com/kelai141/dsh-client-ui-responsive
- Licence: MIT (see `LICENSE.dsh-client-ui-responsive`)
- Why it is vendored: the combined APP plugin's `cordis.patch.yml` inserts the
  `ui-responsive` row, and DSH must resolve that package from the profile's `node_modules`.
- Local repack: only `version` and the six `peerDependencies` exact versions were changed from
  `0.2.1-alpha.1` to `0.2.1-rc.2`, matching the DSH runtime this project targets. The compiled
  client code is unchanged. This avoids requiring a per-profile compatibility exemption.
- Do not edit the tarball in place. Replace it only after building and testing a new upstream
  version, then update `DEFAULT_BASE_PLUGIN` in `setup/lib.mjs` and this note.
