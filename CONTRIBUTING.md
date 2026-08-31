# Contributing

Thanks for considering a contribution.

## Setting up

1. Follow the [Building](README.md#building) section of the README first — JDK 21, Android SDK,
   and a GitHub Packages token are required before anything compiles.
2. Fork, create a feature branch, and keep changes focused on one app or one fix per PR.

## Adding or updating a GMS patch

- All values in `Constants.kt` (package name, launcher activity, signing certificate hash) must be
  extracted from a real APK — never guessed. `GMSCORE_GOOGLE_APPS_GUIDE.md` documents the exact
  `aapt` / `apksigner` / androguard commands to use.
- Record the APK version each value was verified against in the `Constants.kt` provenance comment.
- If you change a fingerprint, re-verify it resolves in the target DEX before opening the PR.

## Commit style

Releases are published automatically on every push to `main` (see
`.github/workflows/release.yml`): the next version is one bump above the
highest version that exists anywhere (tags + releases), so numbers can never
collide or regress and old releases are never deleted. Commit prefixes only
control the changelog section and the bump level: `feat:` bumps the minor
version and lands under "New Features"; `fix:`, `bump:` (updated
app-version support) and `perf:` bump the patch version. Any other prefix
still releases a patch under "Other Changes".

## Legal

By contributing you agree your work is licensed under GPL v3 with the additional conditions in
[NOTICE](NOTICE). Patches derived from ReVanced code must keep their original source-file headers
(see `scripts/add-fork-headers.py`).
