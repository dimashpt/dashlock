#!/bin/bash
# Build DashLock tanpa Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Pakai: ./build.sh            (build saja)
#        ./build.sh install    (build + pasang ke headunit)
#        ./build.sh release    (APK rilis → build/dashlock-<versi>.apk)
set -euo pipefail
cd "$(dirname "$0")"

VERSION_NAME=${VERSION_NAME:-1.1.0}
VERSION_CODE=${VERSION_CODE:-2}

SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=$SDK/build-tools/36.0.0
JAR=$SDK/platforms/android-35/android.jar
DEVICE=${DEVICE:-192.168.1.17:5555}
MODE=${1:-}

rm -rf build
mkdir -p build/classes build/dex build/gen build/assets

"$BT/aapt2" compile --dir res -o build/res.zip

DEBUG_FLAG=""; [ "$MODE" != "release" ] && DEBUG_FLAG="--debug-mode"
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml -A build/assets $DEBUG_FLAG \
    --min-sdk-version 24 --target-sdk-version 29 --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
    --java build/gen build/res.zip

javac --release 8 -encoding UTF-8 -classpath "$JAR" \
    -d build/classes $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 24 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

cp build/base.apk build/unaligned.apk
(cd build/dex && zip -q ../unaligned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unaligned.apk build/aligned.apk

DEBUG_KS=${DEBUG_KS:-$HOME/.android/debug.keystore}
if [ -f dashlock.keystore ]; then
    KC_SERVICE=dashlock-keystore
    export KS_PASS=${DASHLOCK_KS_PASS:-$(security find-generic-password -a dashlock -s "$KC_SERVICE" -w 2>/dev/null || true)}
    if [ -z "$KS_PASS" ]; then
        echo "Password dashlock.keystore: set DASHLOCK_KS_PASS atau Keychain ($KC_SERVICE)." >&2
        exit 1
    fi
    "$BT/apksigner" sign --ks dashlock.keystore --ks-pass env:KS_PASS --key-pass env:KS_PASS \
        --out build/dashlock.apk build/aligned.apk
else
    "$BT/apksigner" sign --ks "$DEBUG_KS" --ks-pass pass:android --key-pass pass:android \
        --out build/dashlock.apk build/aligned.apk
fi

if [ "$MODE" = "release" ]; then
    cp build/dashlock.apk "build/dashlock-$VERSION_NAME.apk"
    ls -lh "build/dashlock-$VERSION_NAME.apk"
else
    ls -lh build/dashlock.apk
fi

if [ "$MODE" = "install" ]; then
    adb -s "$DEVICE" install -r build/dashlock.apk
    adb -s "$DEVICE" shell am start -n id.sooon.dashlock/.MainActivity
fi
