# Ant Media JavaCPP Presets fork

This repository (`ant-media/javacpp-presets`) is a fork of
`bytedeco/javacpp-presets`. It carries Ant Media specific modules and native
build customizations. Some tailoring was originally developed in
`mekya/javacpp-presets`, particularly its `ams-1.5.8` and `ams-1.5.11` branches.

## Branches and releases

- Keep `master` synchronized with `bytedeco/javacpp-presets:master`. Use it as
  the upstream integration branch; do not develop Ant Media changes there.
- Port upstream updates from `master` to `ams-master`, resolving conflicts
  while preserving Ant Media modules and customizations. Check native library
  versions, generated bindings, Maven profiles, and platform artifacts together.
- Develop Ant Media features and fixes on feature branches based on
  `ams-master`. Open PRs targeting `ams-master`.
- Make Ant Media releases from `ams-master`. It is this fork's default branch.
- When work spans Ant Media repositories, use matching feature branch names
  because CI matches branches across repositories.

## Ant Media tailoring

- `srt/`: standalone JavaCPP bindings for Haivision SRT, distinct from FFmpeg's
  SRT protocol support. Builds a shared libsrt with encryption and static
  OpenSSL, with bindings/platform artifacts for Linux x86_64, Linux arm64, and
  macOS x86_64. The restored module uses SRT 1.5.4, matching FFmpeg's version.
  Keep module registration, POMs, native build script, and presets aligned.
- `ndi/`: bindings for the proprietary NDI 6 SDK, including send/receive,
  discovery, frame synchronization, routing, and examples. The SDK must be
  installed separately; use `NDI_SDK_DIR`. Preserve the sender shutdown race
  fix and existing supported platforms. See `ANTMEDIA.md` for publishing and
  runtime compatibility guidance.
- `ffmpeg/`: retains upstream FFmpeg features (including SRT, XML2, and NVIDIA
  decoding/encoding support) and restores optional CUDA/NPP GPU scaling on
  Linux x86_64/arm64 via `FFMPEG_NPP=1`. NPP preloads match this tree's CUDA 13
  presets. Updating CUDA requires checking these ABI versions too.
- `ffmpeg/cppbuild_rebuild.sh`: rebuilds only FFmpeg using already prepared
  sources/dependencies and the current build recipe. The historical
  `cppbuild_rebuild_fast_only_ffmpeg.sh` entry point also enables CUDA/NPP.
  Do not replace these with a copied build script that freezes older upstream
  dependencies, patches, or configure arguments.

## Working and validating changes

Preserve local uncommitted work; use a separate worktree when appropriate.
Do not merge all historical mekya changes indiscriminately: that fork also has
other modules and older dependency versions outside this fork's current scope.
Keep generated Java bindings consistent with the native headers used to build.

Run `bash -n` for modified shell scripts and the focused tests in
`ffmpeg/tests/test_build_modes.py`. Validate Maven metadata and build the native
module on each affected platform when available. An x86_64 build does not
validate arm64 cross compilation or macOS builds. CUDA/NPP requires the CUDA
SDK and a separate build/runtime check.

The existing branch has a root version of 1.5.13 while several upstream module
POMs still reference 1.5.14-SNAPSHOT. Verify the effective parent and artifact
versions before building or publishing; do not silently change every module's
version as part of an unrelated feature. Publish only platform classifiers
that have actually been built. Releases and publishing require compatible
JavaCPP artifacts and native runtime baselines; follow `ANTMEDIA.md`.
