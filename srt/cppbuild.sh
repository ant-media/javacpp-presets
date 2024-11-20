#!/bin/bash
# This file is meant to be included by the parent cppbuild.sh script
if [[ -z "$PLATFORM" ]]; then
    pushd ..
    bash cppbuild.sh "$@" srt
    popd
    exit
fi

SRT_CONFIG="-DENABLE_APPS:BOOL=OFF -DENABLE_ENCRYPTION:BOOL=ON -DENABLE_SHARED:BOOL=ON -DCMAKE_BUILD_TYPE=Release -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_INSTALL_LIBDIR=lib -DCMAKE_INSTALL_INCLUDEDIR=include -DCMAKE_INSTALL_BINDIR=bin"

LIBSRT_VERSION=1.5.3
OPENSSL=openssl-3.3.2

download https://github.com/Haivision/srt/archive/refs/tags/v$LIBSRT_VERSION.tar.gz srt-$LIBSRT_VERSION.tar.gz
download https://www.openssl.org/source/$OPENSSL.tar.gz $OPENSSL.tar.gz

mkdir -p $PLATFORM
cd $PLATFORM
INSTALL_PATH=`pwd`

tar --totals -xzf ../srt-$LIBSRT_VERSION.tar.gz
tar --totals -xzf ../$OPENSSL.tar.gz

cd srt-$LIBSRT_VERSION



case $PLATFORM in

    linux-x86_64)
        cd ../$OPENSSL
        ./Configure linux-x86_64 -fPIC no-shared --prefix=$INSTALL_PATH --libdir=lib
        make -s -j $MAKEJ
        make install_sw
        cd ../srt-$LIBSRT_VERSION
        CC="gcc -m64" CXX="g++ -m64" CFLAGS="-I$INSTALL_PATH/include/" CXXFLAGS="-I$INSTALL_PATH/include/" LDFLAGS="-L$INSTALL_PATH/lib/" $CMAKE -DCMAKE_INSTALL_PREFIX=$INSTALL_PATH $SRT_CONFIG .
        make -j $MAKEJ V=0
        make install
        ;;
    linux-arm64)      

        export CFLAGS="-march=armv8-a+crypto -mcpu=cortex-a57+crypto -I$INSTALL_PATH/include -L$INSTALL_PATH/lib"
        export CXXFLAGS="$CFLAGS"
        export CPPFLAGS="$CFLAGS"
        cd ../$OPENSSL
        ./Configure linux-aarch64 -fPIC --prefix=$INSTALL_PATH --libdir=lib --cross-compile-prefix=aarch64-linux-gnu- "$CFLAGS" no-shared
        make -s -j $MAKEJ
        make install_sw
        cd ../srt-$LIBSRT_VERSION
    
        CFLAGS="-I$INSTALL_PATH/include/" CXXFLAGS="-I$INSTALL_PATH/include/" LDFLAGS="-L$INSTALL_PATH/lib/" $CMAKE -DCMAKE_INSTALL_PREFIX=$INSTALL_PATH $SRT_CONFIG -DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_VERSION=1 -DCMAKE_SYSTEM_PROCESSOR=armv8 -DCMAKE_CXX_FLAGS="$CXXFLAGS" -DCMAKE_C_FLAGS="$CFLAGS" -DCMAKE_C_COMPILER=aarch64-linux-gnu-gcc -DCMAKE_CXX_COMPILER=aarch64-linux-gnu-g++ .
        make -j $MAKEJ V=0
        make install
        ;;
    macosx-x86_64)
        cd ../$OPENSSL
        ./Configure darwin64-x86_64-cc -fPIC no-shared --prefix=$INSTALL_PATH --libdir=lib
        make -j $MAKEJ
        make install_sw



        cd ../srt-$LIBSRT_VERSION

        CFLAGS="-I$INSTALL_PATH/include/ -I/Library/Developer/CommandLineTools/SDKs/MacOSX14.sdk/usr/include/" CXXFLAGS="-I$INSTALL_PATH/include/" LDFLAGS="-L$INSTALL_PATH/lib/" $CMAKE -DCMAKE_INSTALL_PREFIX=$INSTALL_PATH $SRT_CONFIG .
        make -j $MAKEJ V=0
        make install
       

        ;;
     macosx-arm64)
        cd ../$OPENSSL
        ./Configure darwin64-arm64-cc -fPIC no-shared --prefix=$INSTALL_PATH --libdir=lib
        make -j $MAKEJ
        make install_sw

        
        cd ../srt-$LIBSRT_VERSION
        CFLAGS="-I$INSTALL_PATH/include/" CXXFLAGS="-I$INSTALL_PATH/include/" LDFLAGS="-L$INSTALL_PATH/lib/" $CMAKE -DCMAKE_INSTALL_PREFIX=$INSTALL_PATH $SRT_CONFIG .
        make -j $MAKEJ V=0
        make install
       

        ;;
    *)
        echo "Error: Platform \"$PLATFORM\" is not supported"
        ;;
esac

cd ../..
