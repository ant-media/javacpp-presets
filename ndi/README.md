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

Runtime requirements and troubleshooting
----------------------------------------

NDI source discovery is provided by the operating system's mDNS stack and by
NDI's own UDP traffic on the local subnet. A successful build is not enough to
guarantee that sources will show up; the runtime environment must allow that
traffic to flow.

### Linux: install and start Avahi

The NDI runtime on Linux delegates mDNS service registration and browsing to
Avahi. Without it, even two NDI processes running on the same Linux host (or
the same WSL distro) will not see each other. Install and start the daemon:

```bash
sudo apt install avahi-daemon libnss-mdns
sudo service dbus start
sudo service avahi-daemon start
```

You can confirm announcements are being made/seen with:

```bash
avahi-browse -a -r           # live list of all mDNS services on the subnet
```

### WSL2

By default WSL2 sits behind a Hyper-V NAT, so multicast (mDNS on UDP 5353 and
the NDI discovery traffic on UDP 5959–5969) does not flow between the Windows
host and the WSL guest, nor between WSL guests. As a result:

  * a finder in WSL will not see NDI sources running on Windows or on other
    machines on the LAN,
  * a sender in WSL will not be discoverable from Windows or other LAN peers.

Pick whichever of the following works for your setup:

  * **NDI Discovery Server (recommended for mixed WSL ↔ Windows setups).** Run
    `NDIDiscoveryServer.exe` (ships with NDI Tools on Windows). Then in WSL:
    ```bash
    export NDI_DISCOVERY_SERVER=<windows-host-IP>
    ```
    The same variable can be set on Windows (via NDI Access Manager or the
    `NDI_DISCOVERY_SERVER` environment variable) so all parties register with
    and query the same server, bypassing multicast entirely.
  * **Unicast probing.** If you only need to reach a known set of hosts, list
    their IPs explicitly:
    ```bash
    export NDI_EXTRA_IPS="192.168.1.10,192.168.1.11"
    ```
  * **WSL2 mirrored networking** (Windows 11 22H2+). Add the following to
    `%UserProfile%\.wslconfig` on the Windows host and run `wsl --shutdown`:
    ```
    [wsl2]
    networkingMode=mirrored
    ```
    After this WSL shares the Windows network stack and multicast works
    directly, so no discovery server is needed.

### Enabling NDI runtime logging

The NDI runtime reads several environment variables at `NDIlib_initialize()`
time and can write a verbose log of discovery, connections and frame I/O to a
file. This is the fastest way to find out what the library is actually doing
when a source fails to show up:

```bash
export NDI_LOG_FILE=/tmp/ndi.log
export NDI_LOG_LEVEL=verbose          # one of: off | status | info | verbose
export NDI_RUNTIME_LOG_LEVEL=verbose  # same values; covers the runtime itself
```

Other useful runtime tricks:

  * `NDIlib_send_get_no_connections(sender, 0)` on the sender side returns the
    current number of receivers — if it stays `0`, the source is not being
    seen anywhere (network/discovery problem rather than a streaming bug).
  * `tcpdump -i any -nn 'udp port 5353 or udp portrange 5959-5969'` shows
    whether NDI/mDNS packets are actually leaving and arriving on the host.

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
