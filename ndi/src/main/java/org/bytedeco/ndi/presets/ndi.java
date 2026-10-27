/*
 * Copyright (C) 2026 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bytedeco.ndi.presets;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;
import org.bytedeco.javacpp.presets.javacpp;
import org.bytedeco.javacpp.tools.Info;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.InfoMapper;

/**
 * JavaCPP Presets configuration for the NewTek/Vizrt NDI 6 SDK.
 *
 * @author Samuel Audet
 */
@Properties(
    inherit = javacpp.class,
    target = "org.bytedeco.ndi",
    global = "org.bytedeco.ndi.global.ndi",
    value = {
        @Platform(
            value = {"linux-x86_64", "linux-arm64", "macosx", "windows-x86_64"},
            define = "PROCESSINGNDILIB_STATIC",
            // Processing.NDI.Lib.h must be listed first: it is the public umbrella header
            // that defines PROCESSINGNDILIB_API / PROCESSINGNDILIB_DEPRECATED. The remaining
            // entries are listed only so the JavaCPP parser walks them in order; once Lib.h
            // has been preprocessed, the rest are no-ops due to "#pragma once".
            include = {
                "Processing.NDI.Lib.h",
                "Processing.NDI.compat.h",
                "Processing.NDI.structs.h",
                "Processing.NDI.Find.h",
                "Processing.NDI.Recv.h",
                "Processing.NDI.Recv.ex.h",
                "Processing.NDI.RecvAdvertiser.h",
                "Processing.NDI.RecvListener.h",
                "Processing.NDI.Send.h",
                "Processing.NDI.SendAdvertiser.h",
                "Processing.NDI.SendListener.h",
                "Processing.NDI.Routing.h",
                "Processing.NDI.utilities.h",
                "Processing.NDI.deprecated.h",
                "Processing.NDI.FrameSync.h",
                "Processing.NDI.DynamicLoad.h"
            }
        ),
        @Platform(
            value = {"linux", "macosx"},
            define = "PROCESSINGNDILIB_STATIC",
            link = "ndi@.6",
            includepath = {
                "/usr/include/",
                "/usr/local/include/",
                "/Library/NDI SDK for Apple/include/"
            },
            linkpath = {
                "/usr/lib/",
                "/usr/local/lib/",
                "/Library/NDI SDK for Apple/lib/macOS/"
            }
        ),
        @Platform(
            value = "windows-x86_64",
            link = "Processing.NDI.Lib.x64",
            preload = "Processing.NDI.Lib.x64",
            includepath = {"C:/Program Files/NDI/NDI 6 SDK/Include/"},
            linkpath = {"C:/Program Files/NDI/NDI 6 SDK/Lib/x64/"},
            preloadpath = {"C:/Program Files/NDI/NDI 6 SDK/Bin/x64/"}
        )
    }
)
public class ndi implements InfoMapper {
    static { Loader.checkVersion("org.bytedeco", "ndi"); }

    public void map(InfoMap infoMap) {
        infoMap.put(new Info().enumerate())
               // The original NDI_LIB_FOURCC macro emits (uint32_t)(uint8_t)X casts inside enum
               // initializers. JavaCPP cannot translate those casts in that context; skip the
               // enum types and expose the FourCC values as plain Java int constants below.
               .put(new Info("NDIlib_FourCC_video_type_e", "NDIlib_FourCC_audio_type_e",
                             "NDIlib_FourCC_type_e")
                       .skip().cast().valueTypes("int").pointerTypes("IntPointer"))
               .put(new Info().javaText(
                   "    public static final int\n" +
                   "        NDIlib_FourCC_video_type_UYVY = fourcc('U','Y','V','Y'),\n" +
                   "        NDIlib_FourCC_video_type_UYVA = fourcc('U','Y','V','A'),\n" +
                   "        NDIlib_FourCC_video_type_P216 = fourcc('P','2','1','6'),\n" +
                   "        NDIlib_FourCC_video_type_PA16 = fourcc('P','A','1','6'),\n" +
                   "        NDIlib_FourCC_video_type_YV12 = fourcc('Y','V','1','2'),\n" +
                   "        NDIlib_FourCC_video_type_I420 = fourcc('I','4','2','0'),\n" +
                   "        NDIlib_FourCC_video_type_NV12 = fourcc('N','V','1','2'),\n" +
                   "        NDIlib_FourCC_video_type_BGRA = fourcc('B','G','R','A'),\n" +
                   "        NDIlib_FourCC_video_type_BGRX = fourcc('B','G','R','X'),\n" +
                   "        NDIlib_FourCC_video_type_RGBA = fourcc('R','G','B','A'),\n" +
                   "        NDIlib_FourCC_video_type_RGBX = fourcc('R','G','B','X'),\n" +
                   "        NDIlib_FourCC_video_type_max  = 0x7fffffff,\n" +
                   "        NDIlib_FourCC_type_UYVY = NDIlib_FourCC_video_type_UYVY,\n" +
                   "        NDIlib_FourCC_type_UYVA = NDIlib_FourCC_video_type_UYVA,\n" +
                   "        NDIlib_FourCC_type_P216 = NDIlib_FourCC_video_type_P216,\n" +
                   "        NDIlib_FourCC_type_PA16 = NDIlib_FourCC_video_type_PA16,\n" +
                   "        NDIlib_FourCC_type_YV12 = NDIlib_FourCC_video_type_YV12,\n" +
                   "        NDIlib_FourCC_type_I420 = NDIlib_FourCC_video_type_I420,\n" +
                   "        NDIlib_FourCC_type_NV12 = NDIlib_FourCC_video_type_NV12,\n" +
                   "        NDIlib_FourCC_type_BGRA = NDIlib_FourCC_video_type_BGRA,\n" +
                   "        NDIlib_FourCC_type_BGRX = NDIlib_FourCC_video_type_BGRX,\n" +
                   "        NDIlib_FourCC_type_RGBA = NDIlib_FourCC_video_type_RGBA,\n" +
                   "        NDIlib_FourCC_type_RGBX = NDIlib_FourCC_video_type_RGBX,\n" +
                   "        NDIlib_FourCC_audio_type_FLTP = fourcc('F','L','T','p'),\n" +
                   "        NDIlib_FourCC_type_FLTP = NDIlib_FourCC_audio_type_FLTP,\n" +
                   "        NDIlib_FourCC_audio_type_max  = 0x7fffffff;\n" +
                   "    private static int fourcc(int c0, int c1, int c2, int c3) {\n" +
                   "        return (c0 & 0xFF) | ((c1 & 0xFF) << 8) | ((c2 & 0xFF) << 16) | ((c3 & 0xFF) << 24);\n" +
                   "    }\n"))
               .put(new Info("PROCESSINGNDILIB_API",
                             "PROCESSINGNDILIB_DEPRECATED",
                             "PROCESSINGNDILIB_EXPORTS",
                             "PROCESSINGNDILIB_STATIC",
                             "NDILIB_CPP_DEFAULT_VALUE",
                             "NDILIB_CPP_DEFAULT_CONSTRUCTORS").cppTypes().annotations())
               // Strip GCC __attribute(...) qualifiers that the headers may still leak through
               // (the parser does not always honor the PROCESSINGNDILIB_STATIC define).
               .put(new Info("__attribute((visibility(\"default\")))",
                             "__attribute((deprecated))",
                             "__attribute__((visibility(\"default\")))",
                             "__attribute__((deprecated))").annotations())
               .put(new Info("NDILIB_LIBRARY_NAME",
                             "NDILIB_REDIST_FOLDER",
                             "NDILIB_REDIST_URL").cppTypes().annotations().define(false))
               // Expose each opaque "..._instance_type" struct under its public typedef name "..._instance_t",
               // and tell JavaCPP that the typedef itself is a pointer (so the JNI passes the pointer
               // directly instead of dereferencing it). This mirrors the pattern used by the LLVM preset
               // for its opaque Ref typedefs.
               .put(new Info("NDIlib_find_instance_type").pointerTypes("NDIlib_find_instance_t"))
               .put(new Info("NDIlib_find_instance_t").valueTypes("NDIlib_find_instance_t").pointerTypes("@ByPtrPtr NDIlib_find_instance_t", "@Cast(\"NDIlib_find_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_recv_instance_type").pointerTypes("NDIlib_recv_instance_t"))
               .put(new Info("NDIlib_recv_instance_t").valueTypes("NDIlib_recv_instance_t").pointerTypes("@ByPtrPtr NDIlib_recv_instance_t", "@Cast(\"NDIlib_recv_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_recv_advertiser_instance_type").pointerTypes("NDIlib_recv_advertiser_instance_t"))
               .put(new Info("NDIlib_recv_advertiser_instance_t").valueTypes("NDIlib_recv_advertiser_instance_t").pointerTypes("@ByPtrPtr NDIlib_recv_advertiser_instance_t", "@Cast(\"NDIlib_recv_advertiser_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_recv_listener_instance_type").pointerTypes("NDIlib_recv_listener_instance_t"))
               .put(new Info("NDIlib_recv_listener_instance_t").valueTypes("NDIlib_recv_listener_instance_t").pointerTypes("@ByPtrPtr NDIlib_recv_listener_instance_t", "@Cast(\"NDIlib_recv_listener_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_send_instance_type").pointerTypes("NDIlib_send_instance_t"))
               .put(new Info("NDIlib_send_instance_t").valueTypes("NDIlib_send_instance_t").pointerTypes("@ByPtrPtr NDIlib_send_instance_t", "@Cast(\"NDIlib_send_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_send_advertiser_instance_type").pointerTypes("NDIlib_send_advertiser_instance_t"))
               .put(new Info("NDIlib_send_advertiser_instance_t").valueTypes("NDIlib_send_advertiser_instance_t").pointerTypes("@ByPtrPtr NDIlib_send_advertiser_instance_t", "@Cast(\"NDIlib_send_advertiser_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_send_listener_instance_type").pointerTypes("NDIlib_send_listener_instance_t"))
               .put(new Info("NDIlib_send_listener_instance_t").valueTypes("NDIlib_send_listener_instance_t").pointerTypes("@ByPtrPtr NDIlib_send_listener_instance_t", "@Cast(\"NDIlib_send_listener_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_routing_instance_type").pointerTypes("NDIlib_routing_instance_t"))
               .put(new Info("NDIlib_routing_instance_t").valueTypes("NDIlib_routing_instance_t").pointerTypes("@ByPtrPtr NDIlib_routing_instance_t", "@Cast(\"NDIlib_routing_instance_t*\") PointerPointer"))
               .put(new Info("NDIlib_framesync_instance_type").pointerTypes("NDIlib_framesync_instance_t"))
               .put(new Info("NDIlib_framesync_instance_t").valueTypes("NDIlib_framesync_instance_t").pointerTypes("@ByPtrPtr NDIlib_framesync_instance_t", "@Cast(\"NDIlib_framesync_instance_t*\") PointerPointer"))
               // The C++ helper header is not parsed; users can call the C API directly.
               .put(new Info("Processing.NDI.Lib.cplusplus.h").skip());
    }
}
