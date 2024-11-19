#!/bin/bash
# This file is meant to be included by the parent cppbuild.sh script
if [[ -z "$PLATFORM" ]]; then
    pushd ..
    bash cppbuild.sh "$@" srt
    popd
    exit
fi

COMPRESSED_FILE_NAME=zixi_sdks-antmedia-16.4.44489.tar.gz
FOLDER_NAME=zixi_sdks-antmedia-16.4.44489
FILE=~/Downloads/zixi_sdks-antmedia-16.4.44489.tar.gz
cp $FILE $TOP_PATH/downloads
cp $FILE .
tar -xvf $COMPRESSED_FILE_NAME

cd $FOLDER_NAME
tar -xvf zixi_sdks-antmedia-mac-16.4.44489.tar.xz
tar -xvf zixi_sdks-antmedia-linux64-16.4.44489.tar.xz
cd ..

if [ ! -d "$PLATFORM" ]; then
  mkdir -p $PLATFORM
  case $PLATFORM in
     linux-x86_64)
        mv $FOLDER_NAME/zixi_sdks-antmedia-linux64-16.4.44489/* $PLATFORM/
     ;;
     macosx-x86_64)
        mv $FOLDER_NAME/zixi_sdks-antmedia-mac-16.4.44489/* $PLATFORM/
     ;;
    *)
    echo "Error: Platform \"$PLATFORM\" is not supported"
    ;;
  esac

  #rename folder name for compatibility
  mv $PLATFORM/interface $PLATFORM/include
fi

 cd $PLATFORM


#unzip ../srt-v$SRT_VERSION.zip 



case $PLATFORM in
    android-arm)
        CC="$ANDROID_CC" CFLAGS="$ANDROID_FLAGS" ./configure --prefix=.. --static
        make -j $MAKEJ
        make install
        ;;
    android-arm64)
        CC="$ANDROID_CC" CFLAGS="$ANDROID_FLAGS" ./configure --prefix=.. --static
        make -j $MAKEJ
        make install
        ;;
    android-x86)
        CC="$ANDROID_CC" CFLAGS="$ANDROID_FLAGS" ./configure --prefix=.. --static
        make -j $MAKEJ
        make install
        ;;
    android-x86_64)
        CC="$ANDROID_CC" CFLAGS="$ANDROID_FLAGS" ./configure --prefix=.. --static
        make -j $MAKEJ
        make install
        ;;
    linux-x86)
        CC="gcc -m32 -fPIC" ./configure --prefix=.. --static
        make -j $MAKEJ
        make install
        ;;
    linux-x86_64)
#        CC="gcc -m64 -fPIC" ./configure --prefix=.. --static
#       ./configure --prefix=..
#        make -j $MAKEJ
#        make install
        ;;
    linux-arm64)
#        CC="gcc -m64 -fPIC" ./configure --prefix=.. --static
#       ./configure --prefix=..
#        make -j $MAKEJ
#        make install
        ;;
    macosx-x86_64)
    
        ;;
    windows-x86)
        nmake -f win32/Makefile.msc zlib.lib
        mkdir -p ../include ../lib
        cp zconf.h zlib.h ../include/
        cp zlib.lib ../lib/
        ;;
    windows-x86_64)
        nmake -f win32/Makefile.msc zlib.lib
 



       mkdir -p ../include ../lib
        cp zconf.h zlib.h ../include/
        cp zlib.lib ../lib/
        ;;
    *)
        echo "Error: Platform \"$PLATFORM\" is not supported"
        ;;
esac

cd ..
