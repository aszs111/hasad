#!/usr/bin/env bash
# ينسخ مكتبة TDLib الجاهزة (نسخة Telegram X) داخل المشروع.
# الاستخدام: scripts/fetch-tdlib.sh <مجلد tgx-tdlib> <مجلد NDK>
set -euo pipefail

TGX="${1:?path to TGX-Android/tdlib checkout}"
NDK="${2:-${ANDROID_NDK_HOME:-}}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
API="android-29"
ABIS="arm64-v8a armeabi-v7a x86_64"

NDKV="$(ls "$TGX/src/main/libs" | sort -V | tail -1)"
echo "TGX TDLib built with NDK $NDKV ($API)"

JAVA_DST="$ROOT/app/src/main/java/org/drinkless/tdlib"
mkdir -p "$JAVA_DST"
cp "$TGX/src/main/java/org/drinkless/tdlib/"*.java "$JAVA_DST/"

for ABI in $ABIS; do
  DST="$ROOT/app/src/main/jniLibs/$ABI"
  mkdir -p "$DST"
  cp -L "$TGX/src/main/libs/$NDKV/$API/$ABI/libtdjni.so" "$DST/"
  for L in libcryptox.so libsslx.so; do
    if [ -e "$TGX/openssl/$NDKV/$API/$ABI/lib/$L" ]; then cp -L "$TGX/openssl/$NDKV/$API/$ABI/lib/$L" "$DST/"; fi
  done
  case "$ABI" in
    arm64-v8a) TRIPLE=aarch64-linux-android ;;
    armeabi-v7a) TRIPLE=arm-linux-androideabi ;;
    x86_64) TRIPLE=x86_64-linux-android ;;
    x86) TRIPLE=i686-linux-android ;;
  esac
  if [ -n "$NDK" ]; then
    CXX="$(find "$NDK/toolchains/llvm/prebuilt" -path "*sysroot/usr/lib/$TRIPLE/libc++_shared.so" | head -1)"
    [ -n "$CXX" ] && cp "$CXX" "$DST/"
  fi
  # تأكد إنها ملفات حقيقية مو مؤشرات LFS
  if ! head -c 4 "$DST/libtdjni.so" | grep -q "ELF"; then echo "ERROR: $ABI/libtdjni.so is not an ELF (git lfs pull missing?)"; exit 1; fi
  echo "== $ABI"; ls -la "$DST"
  (readelf -d "$DST/libtdjni.so" 2>/dev/null || true) | grep NEEDED || true
done
