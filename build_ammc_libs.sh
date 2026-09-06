#!/usr/bin/env bash
#
# Builds the ammc-rs JNI shared library (libammc.so) for Android and installs it
# into ammc/src/main/jniLibs. Replaces the old Jenkins artifact download.
#
# Requirements:
#   rustup target add aarch64-linux-android x86_64-linux-android
#   cargo install cargo-ndk
#   Android NDK (set ANDROID_NDK_HOME, or let this script pick the newest one
#   from $ANDROID_HOME/ndk)
#
# Usage:
#   ./build_ammc_libs.sh                    # expects ammc-rs in ~/mywork/ammc-rs
#   AMMC_RS=/path/to/ammc-rs ./build_ammc_libs.sh

set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AMMC_RS="${AMMC_RS:-$HOME/mywork/ammc-rs}"
JNI_LIBS="$APP_DIR/ammc/src/main/jniLibs"

if [ ! -f "$AMMC_RS/Cargo.toml" ]; then
    echo "ammc-rs sources not found in $AMMC_RS; set AMMC_RS to its checkout" >&2
    exit 1
fi

if [ -z "${ANDROID_NDK_HOME:-}" ]; then
    sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
    ANDROID_NDK_HOME="$(find "$sdk/ndk" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -V | tail -1)"
    if [ -z "$ANDROID_NDK_HOME" ]; then
        echo "No Android NDK found; set ANDROID_NDK_HOME" >&2
        exit 1
    fi
fi
export ANDROID_NDK_HOME

echo "ammc-rs:     $AMMC_RS"
echo "Android NDK: $ANDROID_NDK_HOME"

# ammc_lib is the crates/dynlib package; [lib] name = "ammc" -> libammc.so
cd "$AMMC_RS"
cargo ndk -t arm64-v8a -t x86_64 -o "$JNI_LIBS" build --release -p ammc_lib

echo
echo "Installed:"
ls -l "$JNI_LIBS"/*/libammc.so
