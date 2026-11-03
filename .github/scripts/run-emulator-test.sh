#!/usr/bin/env bash
set -euo pipefail
EVIDENCE_DIR="artifacts/emulator"
mkdir -p "$EVIDENCE_DIR"
echo "=== Android virtual: informacoes do dispositivo ==="
adb devices -l | tee "$EVIDENCE_DIR/adb-devices.txt"
adb shell getprop ro.build.version.release | tee "$EVIDENCE_DIR/android-version.txt"
adb shell getprop ro.build.version.sdk | tee "$EVIDENCE_DIR/android-api.txt"
echo "=== Executando teste real de instrumentacao ==="
gradle --no-daemon :app:connectedDebugAndroidTest
echo "=== Coletando screenshots produzidas pelo teste Compose ==="
SOURCE="/sdcard/Android/data/br.com.laboratorio.android/files/Pictures/laboratorio"
adb shell ls -l "$SOURCE"
adb pull "$SOURCE/contador-antes.png" "$EVIDENCE_DIR/contador-antes.png"
adb pull "$SOURCE/contador-depois.png" "$EVIDENCE_DIR/contador-depois.png"
test -s "$EVIDENCE_DIR/contador-antes.png"
test -s "$EVIDENCE_DIR/contador-depois.png"
file "$EVIDENCE_DIR/"*.png
sha256sum "$EVIDENCE_DIR/"*.png | tee "$EVIDENCE_DIR/screenshots-sha256.txt"
echo "RESULTADO: Teste instrumentado passou e capturas de antes/depois foram coletadas."
