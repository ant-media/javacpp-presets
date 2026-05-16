JavaCPP Presets for NDI
=======================

[![Gitter](https://badges.gitter.im/bytedeco/javacpp.svg)](https://gitter.im/bytedeco/javacpp) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.bytedeco/ndi/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.bytedeco/ndi) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.bytedeco/ndi.svg)](http://bytedeco.org/builds/)  
<sup>Build status for all platforms:</sup> [![ndi](https://github.com/bytedeco/javacpp-presets/workflows/ndi/badge.svg)](https://github.com/bytedeco/javacpp-presets/actions?query=workflow%3Andi)


Introduction
------------
This directory contains the JavaCPP Presets module for:

 * NDI 6  http://ndi.video/

The [NDI SDK](http://ndi.video/) (Network Device Interface), developed by Vizrt
(formerly NewTek), is a royalty-free standard for real-time, low-latency video
and audio over IP networks.

Please refer to the parent README.md file for more detailed information about the JavaCPP Presets.

The NDI SDK itself is proprietary and must be downloaded and installed
separately from http://ndi.video/ . Once installed, set the `NDI_SDK_DIR`
environment variable to the SDK's root directory if it is not at the default
location used by the installer.


Documentation
-------------
Java API documentation is available here:

 * http://bytedeco.org/javacpp-presets/ndi/apidocs/


Sample Usage
------------
Here is a small example showing how to enumerate NDI sources on the network.
We can use [Maven 3](http://maven.apache.org/) to download and install
automatically all the class files as well as the native binaries. To run this
sample code, after creating the `pom.xml` and `NDIFind.java` source files
below, simply execute on the command line:
```bash
 $ mvn compile exec:java
```

### The `pom.xml` build file
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.bytedeco.ndi</groupId>
    <artifactId>ndifind</artifactId>
    <version>1.5.14-SNAPSHOT</version>
    <properties>
        <exec.mainClass>NDIFind</exec.mainClass>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.bytedeco</groupId>
            <artifactId>ndi-platform</artifactId>
            <version>6.0.0-1.5.14-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <build>
        <sourceDirectory>.</sourceDirectory>
    </build>
</project>
```

### The `NDIFind.java` source file
```java
import org.bytedeco.javacpp.*;
import org.bytedeco.ndi.*;

import static org.bytedeco.ndi.global.ndi.*;

public class NDIFind {
    public static void main(String[] args) throws Exception {
        if (!NDIlib_initialize()) {
            System.err.println("NDI library failed to initialize (CPU not supported?).");
            System.exit(1);
        }

        NDIlib_find_instance_t find = NDIlib_find_create_v2(null);
        if (find == null || find.isNull()) {
            System.err.println("Failed to create NDI finder.");
            System.exit(1);
        }

        // Wait up to 5 seconds for sources to appear on the network.
        NDIlib_find_wait_for_sources(find, 5000);

        IntPointer noSources = new IntPointer(1);
        NDIlib_source_t sources = NDIlib_find_get_current_sources(find, noSources);

        int n = noSources.get();
        System.out.println("Found " + n + " NDI source(s):");
        for (int i = 0; i < n; i++) {
            NDIlib_source_t s = sources.position(i);
            System.out.println("  " + s.p_ndi_name().getString()
                             + " @ " + s.p_url_address().getString());
        }

        NDIlib_find_destroy(find);
        NDIlib_destroy();
    }
}
```
