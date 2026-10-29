#!/bin/bash
# Reuse the current build recipe and the dependencies from a completed native build.
set -eu
cd "$(dirname "$0")/.."
export FFMPEG_REBUILD=1
exec bash cppbuild.sh "$@" install ffmpeg
