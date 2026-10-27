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
