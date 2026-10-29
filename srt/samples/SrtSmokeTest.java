import static org.bytedeco.srt.global.srt.*;

/** Checks that both the JNI wrapper and bundled shared SRT library load. */
public class SrtSmokeTest {
    public static void main(String[] args) {
        if (srt_getversion() != 0x010504) {
            throw new AssertionError("Expected SRT 1.5.4");
        }
        if (srt_startup() != 0) {
            throw new AssertionError("SRT startup failed");
        }
        try {
            int socket = srt_create_socket();
            if (socket == SRT_INVALID_SOCK) {
                throw new AssertionError("SRT socket creation failed");
            }
            if (srt_close(socket) != 0) {
                throw new AssertionError("SRT socket close failed");
            }
        } finally {
            if (srt_cleanup() != 0) {
                throw new AssertionError("SRT cleanup failed");
            }
        }
        System.out.println("SRT 1.5.4 native load and socket lifecycle passed");
    }
}
