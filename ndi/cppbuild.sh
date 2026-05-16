#!/bin/bash
# This file is meant to be included by the parent cppbuild.sh script
if [[ -z "$PLATFORM" ]]; then
    pushd ..
    bash cppbuild.sh "$@" ndi
    popd
    exit
fi

# The NDI SDK is proprietary and must be downloaded and installed manually from
# http://ndi.video/ . The location of the SDK can be overridden by setting the
# NDI_SDK_DIR environment variable to the SDK's root directory (the directory
# that contains the "include" / "Include" subdirectory).

# Resolve a sensible default install location per platform.
if [[ -z "$NDI_SDK_DIR" ]]; then
    case $PLATFORM in
        linux-*)   NDI_SDK_DIR="/usr/local/NDI SDK for Linux" ;;
        macosx-*)  NDI_SDK_DIR="/Library/NDI SDK for Apple" ;;
        windows-*) NDI_SDK_DIR="/C/Program Files/NDI/NDI 6 SDK" ;;
    esac
fi

if [[ ! -d "$NDI_SDK_DIR" ]]; then
    echo "NDI SDK not found at \"$NDI_SDK_DIR\"."
    echo "Please download and install the NDI SDK from http://ndi.video/ ,"
    echo "then set NDI_SDK_DIR to its root directory."
    exit 1
fi

# Locate the include and lib directories (their casing varies between SDKs).
NDI_INC_DIR=""
for d in "$NDI_SDK_DIR/include" "$NDI_SDK_DIR/Include"; do
    if [[ -f "$d/Processing.NDI.Lib.h" ]]; then NDI_INC_DIR="$d"; break; fi
done
if [[ -z "$NDI_INC_DIR" ]]; then
    echo "Could not find Processing.NDI.Lib.h under \"$NDI_SDK_DIR\"."
    exit 1
fi

mkdir -p "$PLATFORM/include" "$PLATFORM/lib" "$PLATFORM/bin"
cp -fL "$NDI_INC_DIR"/*.h "$PLATFORM/include/"

# JavaCPP's parser doesn't recognize the single-underscore GCC __attribute(...)
# qualifiers used by the NDI headers on Linux/macOS. Replace them with the
# standard __attribute__(...) form (which the parser ignores cleanly).
for f in "$PLATFORM/include"/*.h; do
    sed -i \
        -e 's/__attribute((visibility("default")))//g' \
        -e 's/__attribute((deprecated))//g' \
        "$f"
done

case $PLATFORM in
    linux-x86_64)
        cp -fP "$NDI_SDK_DIR/lib/x86_64-linux-gnu/"libndi.so* "$PLATFORM/lib/"
        ;;
    linux-x86)
        cp -fP "$NDI_SDK_DIR/lib/i686-linux-gnu/"libndi.so* "$PLATFORM/lib/"
        ;;
    linux-arm64)
        # The Linux SDK ships ARM builds for Raspberry Pi only.
        cp -fP "$NDI_SDK_DIR/lib/aarch64-rpi4-linux-gnueabi/"libndi.so* "$PLATFORM/lib/"
        ;;
    linux-armhf)
        cp -fP "$NDI_SDK_DIR/lib/arm-rpi4-linux-gnueabihf/"libndi.so* "$PLATFORM/lib/"
        ;;
    macosx-*)
        cp -fP "$NDI_SDK_DIR/lib/macOS/"libndi*.dylib* "$PLATFORM/lib/"
        ;;
    windows-x86_64)
        cp -f "$NDI_SDK_DIR/Lib/x64/Processing.NDI.Lib.x64.lib" "$PLATFORM/lib/"
        cp -f "$NDI_SDK_DIR/Bin/x64/Processing.NDI.Lib.x64.dll" "$PLATFORM/bin/"
        ;;
    windows-x86)
        cp -f "$NDI_SDK_DIR/Lib/x86/Processing.NDI.Lib.x86.lib" "$PLATFORM/lib/"
        cp -f "$NDI_SDK_DIR/Bin/x86/Processing.NDI.Lib.x86.dll" "$PLATFORM/bin/"
        ;;
    *)
        echo "Error: Platform \"$PLATFORM\" is not supported"
        exit 1
        ;;
esac
