import org.bytedeco.javacpp.*;
import org.bytedeco.ndi.*;

import static org.bytedeco.ndi.global.ndi.*;

/**
 * Publishes a static SMPTE-style color bar picture as an NDI source on the network.
 *
 * Run this and the source should become discoverable (e.g. by NDIFind, NDI Studio Monitor,
 * OBS NDI plugin, etc.) under the name "Java Color Bars".
 *
 * Press Ctrl+C to stop.
 */
public class NDISendColorBars {

    // Frame dimensions and rate.
    private static final int WIDTH       = 1280;
    private static final int HEIGHT      = 720;
    private static final int FRAME_RATE_N = 30000;
    private static final int FRAME_RATE_D = 1001;

    // 75% SMPTE color bars (top region) in BGRA byte order.
    // Order: 75% white/gray, yellow, cyan, green, magenta, red, blue.
    private static final int[][] BARS_BGRA = {
        { 191, 191, 191, 255 }, // 75% white
        {   0, 191, 191, 255 }, // yellow  (B=0,  G=191, R=191)
        { 191, 191,   0, 255 }, // cyan    (B=191,G=191, R=0)
        {   0, 191,   0, 255 }, // green
        { 191,   0, 191, 255 }, // magenta
        {   0,   0, 191, 255 }, // red
        { 191,   0,   0, 255 }, // blue
    };

    public static void main(String[] args) throws Exception {
        if (!NDIlib_initialize()) {
            System.err.println("NDI library failed to initialize (CPU not supported?).");
            System.exit(1);
        }

        // Configure the sender.
        NDIlib_send_create_t createSettings = new NDIlib_send_create_t();
        createSettings.p_ndi_name(new BytePointer("Java Color Bars"));
        createSettings.p_groups(null);
        createSettings.clock_video(true);
        createSettings.clock_audio(false);

        NDIlib_send_instance_t sender = NDIlib_send_create(createSettings);
        if (sender == null || sender.isNull()) {
            System.err.println("Failed to create NDI sender.");
            NDIlib_destroy();
            System.exit(1);
        }

        // Allocate and fill a single static BGRA color bar frame.
        int strideBytes = WIDTH * 4;
        int frameBytes  = strideBytes * HEIGHT;
        BytePointer pixels = new BytePointer(frameBytes);
        fillColorBars(pixels, WIDTH, HEIGHT, strideBytes);

        NDIlib_video_frame_v2_t videoFrame = new NDIlib_video_frame_v2_t();
        videoFrame.xres(WIDTH);
        videoFrame.yres(HEIGHT);
        videoFrame.FourCC(NDIlib_FourCC_video_type_BGRA);
        videoFrame.frame_rate_N(FRAME_RATE_N);
        videoFrame.frame_rate_D(FRAME_RATE_D);
        videoFrame.picture_aspect_ratio(16.0f / 9.0f);
        videoFrame.frame_format_type(NDIlib_frame_format_type_progressive);
        videoFrame.line_stride_in_bytes(strideBytes);
        videoFrame.p_data(pixels);

        System.out.println("Streaming \"" + createSettings.p_ndi_name().getString() + "\" at "
                + WIDTH + "x" + HEIGHT + " @ "
                + ((float) FRAME_RATE_N / FRAME_RATE_D) + " fps.");
        System.out.println("Press Ctrl+C to stop.");

        // Ensure resources are released on shutdown.
        final NDIlib_send_instance_t senderRef = sender;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                NDIlib_send_destroy(senderRef);
                NDIlib_destroy();
                System.out.println("NDI sender stopped.");
            }
        }));

        // Since clock_video=true, NDIlib_send_send_video_v2 will rate-limit to the frame rate.
        while (true) {
            NDIlib_send_send_video_v2(sender, videoFrame);
        }
    }

    /**
     * Fills the given buffer with a 7-bar 75% SMPTE color bar pattern in BGRA.
     */
    private static void fillColorBars(BytePointer pixels, int width, int height, int strideBytes) {
        // Pre-compute one row of pixels, then replicate it down the frame.
        byte[] row = new byte[strideBytes];
        int bars = BARS_BGRA.length;
        for (int x = 0; x < width; x++) {
            int bar = Math.min(bars - 1, (x * bars) / width);
            int[] color = BARS_BGRA[bar];
            int off = x * 4;
            row[off    ] = (byte) color[0]; // B
            row[off + 1] = (byte) color[1]; // G
            row[off + 2] = (byte) color[2]; // R
            row[off + 3] = (byte) color[3]; // A
        }
        for (int y = 0; y < height; y++) {
            pixels.position((long) y * strideBytes).put(row, 0, strideBytes);
        }
        pixels.position(0);
    }
}
