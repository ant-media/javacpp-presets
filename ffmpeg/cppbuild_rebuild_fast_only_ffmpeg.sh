#!/bin/bash
# Compatibility entry point from mekya/ams-1.5.8, including CUDA/NPP scaling.
set -eu
export FFMPEG_NPP=1
exec bash "$(dirname "$0")/cppbuild_rebuild.sh" "$@"
