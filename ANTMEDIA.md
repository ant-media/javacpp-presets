# Ant Media NDI GitHub Packages Publishing

These notes capture the Ant Media publishing flow for the JavaCPP Presets NDI artifacts.

## Target Repository

Artifacts are deployed to GitHub Packages:

```text
https://maven.pkg.github.com/ant-media/javacpp-presets
```

The Maven `settings.xml` file must define credentials for server id `github`:

```xml
<server>
  <id>github</id>
  <username>GITHUB_USERNAME</username>
  <password>GITHUB_TOKEN</password>
</server>
```

For a private repository, the token needs `write:packages` and usually `repo`.

## Use a Released JavaCPP Version

Do not publish NDI as `1.5.14` unless `org.bytedeco:javacpp:1.5.14` and
`org.bytedeco:javacpp-platform:1.5.14` are available. Use an existing released
JavaCPP version, for example `1.5.13`.

Set the root project version from the repository root:

```bash
mvn versions:set -DnewVersion=1.5.13 -DgenerateBackupPoms=false
```

Verify these POM versions:

```text
pom.xml                         -> javacpp-presets 1.5.13
ndi/pom.xml                     -> parent 1.5.13
ndi/platform/pom.xml            -> parent 1.5.13
ndi and ndi-platform artifacts  -> 6.0.0-1.5.13
```

If `ndi/platform/pom.xml` still points at another parent version, update its
parent version manually.

## Publish `ndi`

The NDI SDK is proprietary and must be installed separately. Set `NDI_SDK_DIR`
if it is not in the installer default location:

```bash
export NDI_SDK_DIR=/path/to/NDI-SDK
```

Deploy the parent POM and the `ndi` module to GitHub Packages:

```bash
mvn clean deploy \
  --projects .,ndi \
  -Djavacpp.platform.host \
  -DaltDeploymentRepository=github::default::https://maven.pkg.github.com/ant-media/javacpp-presets
```

This publishes:

```text
org.bytedeco:javacpp-presets:1.5.13
org.bytedeco:ndi:6.0.0-1.5.13
org.bytedeco:ndi:6.0.0-1.5.13:<host-platform-classifier>
```

For Linux x86_64, the classifier is `linux-x86_64`.

## Publish `ndi-platform`

Only publish `ndi-platform` if all classifier dependencies declared by
`ndi/platform/pom.xml` are available. That POM depends on multiple platform
classifier jars, such as Linux, macOS, and Windows. If only one classifier has
been published, downstream projects should depend on `ndi` directly instead of
`ndi-platform`.

Deploy `ndi-platform` with:

```bash
mvn -f ndi/platform/pom.xml deploy \
  -Djavacpp.platform.host \
  -DaltDeploymentRepository=github::default::https://maven.pkg.github.com/ant-media/javacpp-presets
```

If dependency resolution fails for `ndi` artifacts already deployed to GitHub
Packages, make sure the consuming Maven settings or POM has this repository:

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/ant-media/javacpp-presets</url>
</repository>
```

## Consuming Only One Platform

Do not depend on `ndi-platform` when only one platform classifier was published.
Use the unclassified `ndi` jar for Java classes plus the classified `ndi` jar for
native binaries:

```xml
<dependency>
  <groupId>org.bytedeco</groupId>
  <artifactId>ndi</artifactId>
  <version>6.0.0-1.5.13</version>
</dependency>

<dependency>
  <groupId>org.bytedeco</groupId>
  <artifactId>ndi</artifactId>
  <version>6.0.0-1.5.13</version>
  <classifier>linux-x86_64</classifier>
</dependency>
```

The unclassified jar contains the `.class` files. The classified jar contains
the platform-specific native resources.


## FFmpeg release packs with GitHub Actions

Use **Actions → Release Ant Media FFmpeg → Run workflow**, selecting
`ams-master`. The workflow must first be merged into the default branch.
It accepts a version such as `8.1-1.5.13-antmedia1`; the JavaCPP parent remains
at the released 1.5.13 baseline. Increment the Ant Media revision for each new
release. Version preparation is confined to the three FFmpeg POMs in the
runner checkout; it does not rewrite the versions of other modules.

The pack contains regular and GPL native artifacts for Linux ARM64/x86_64
and macOS ARM64/x86_64. Android and Windows are excluded from the build matrix and from the published `ffmpeg-platform` and `ffmpeg-platform-gpl`
dependencies and module descriptors. The default configuration retains
upstream NVIDIA decoder/encoder support; optional CUDA/NPP builds are disabled
and are not validated by this workflow.

Leave **publish** unchecked for a build-only run, or check it to publish the
complete pack after all eight builds pass. Each build stages its native jar;
Linux x86_64 supplies the canonical Java bindings, sources, Javadoc, and POM.
One publisher uploads all classifiers together, followed by both platform
aggregators, to:

```text
https://maven.pkg.github.com/ant-media/javacpp-presets
org.bytedeco:ffmpeg:8.1-1.5.13-antmedia1
org.bytedeco:ffmpeg-platform:8.1-1.5.13-antmedia1
org.bytedeco:ffmpeg-platform-gpl:8.1-1.5.13-antmedia1
```

Authentication uses the workflow's `GITHUB_TOKEN` with `packages: write`;
no personal publishing token or Central credentials are needed. The parent POM
is uploaded only if absent. Publishing rejects an existing FFmpeg version.
GitHub Packages uploads are not transactional: if publishing fails after an
upload, inspect the package and either remove the incomplete version or choose
a fresh Ant Media revision before publishing again.

Build outputs remain downloadable as Actions artifacts for 14 days, including
build-only runs. A build-only run does not publish Maven packages. The existing
`ffmpeg` workflow continues to serve its upstream CI purpose.
