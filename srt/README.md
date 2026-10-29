# JavaCPP Presets for SRT

Standalone Java bindings for Haivision SRT, ported from
`mekya/javacpp-presets:ams-1.5.11`. These bindings let Ant Media call SRT directly;
FFmpeg's built-in SRT protocol support does not provide this Java API.

The native recipe builds shared SRT 1.5.4 with encryption and static OpenSSL
3.5.5. Supported binding targets are `linux-x86_64`, `linux-arm64` (using the
`aarch64-linux-gnu` toolchain), and `macosx-x86_64`. macOS arm64 and Windows
bindings are not currently included.

From the repository root:

```bash
mvn -f srt/pom.xml package -Djavacpp.platform=linux-x86_64
```

The native build also runs through the common driver:

```bash
bash cppbuild.sh install srt -platform linux-x86_64
```

Use both `org.bytedeco:srt:1.5.4-1.5.13` and its native classifier artifact
(for example `linux-x86_64`) when consuming a single platform. Publish
`srt-platform` only after all three classifier artifacts are available.

Keep the Maven version and `LIBSRT_VERSION` in sync. FFmpeg currently also uses
SRT 1.5.4, but builds its own static SRT dependency rather than using this module.
The preset and build were originally brought over from mekya commit
`3d5788081e`; older platform limitations are preserved.

Generated bindings are target dependent because the preset also parses system
socket headers. Maven regenerates them under `src/gen/java`; they are not
checked in.
