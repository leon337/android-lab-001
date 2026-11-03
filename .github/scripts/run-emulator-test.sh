#!/usr/bin/env bash
set -euo pipefail
EVIDENCE_DIR="artifacts/emulator"
mkdir -p "$EVIDENCE_DIR"

echo "=== Android virtual: informacoes do dispositivo ==="
adb devices -l | tee "$EVIDENCE_DIR/adb-devices.txt"
adb shell getprop ro.build.version.release | tee "$EVIDENCE_DIR/android-version.txt"
adb shell getprop ro.build.version.sdk | tee "$EVIDENCE_DIR/android-api.txt"

echo "=== Executando teste real de instrumentacao Compose ==="
gradle --no-daemon :app:connectedDebugAndroidTest

echo "=== Instalando e abrindo aplicativo para screenshots ADB ==="
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop br.com.laboratorio.android || true
adb shell am start -W -n br.com.laboratorio.android/.MainActivity
sleep 3

echo "=== Estado inicial: screenshot e hierarquia da tela ==="
adb exec-out screencap -p > "$EVIDENCE_DIR/contador-antes.png"
adb shell uiautomator dump /sdcard/contador-antes.xml
adb pull /sdcard/contador-antes.xml "$EVIDENCE_DIR/contador-antes.xml"

echo "=== Localizando botao pelo texto real do Android ==="
POINT=$(python3 - "$EVIDENCE_DIR/contador-antes.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
nodes=list(root.iter('node'))
def visible(node,needle):
    return needle in (node.get('text','')+' '+node.get('content-desc',''))
assert any(visible(node,'Interações: 0') for node in nodes), 'Estado inicial nao localizado no Android virtual'
matches=[n for n in nodes if visible(n,'Testar interação')]
assert matches, 'Botao nao encontrado na hierarquia real do Android'
coords=[int(x) for x in re.findall(r'\d+',matches[0].get('bounds',''))]
assert len(coords)==4 and coords[2]>coords[0] and coords[3]>coords[1],coords
print((coords[0]+coords[2])//2,(coords[1]+coords[3])//2)
PY
)
echo "Botao localizado nas coordenadas: $POINT"
adb shell input tap $POINT
sleep 2

echo "=== Estado depois do toque: screenshot e validacao ==="
adb exec-out screencap -p > "$EVIDENCE_DIR/contador-depois.png"
adb shell uiautomator dump /sdcard/contador-depois.xml
adb pull /sdcard/contador-depois.xml "$EVIDENCE_DIR/contador-depois.xml"
python3 - "$EVIDENCE_DIR/contador-depois.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter('node'))
texts=[n.get('text','')+' '+n.get('content-desc','') for n in nodes]
assert any('Interações: 1' in s for s in texts), 'Contador nao mudou de 0 para 1 no Android virtual'
print('PASS: contador mudou para Interações: 1 no emulador.')
PY

test -s "$EVIDENCE_DIR/contador-antes.png"
test -s "$EVIDENCE_DIR/contador-depois.png"
file "$EVIDENCE_DIR/"*.png
sha256sum "$EVIDENCE_DIR/"*.png | tee "$EVIDENCE_DIR/screenshots-sha256.txt"
echo "RESULTADO: Teste Compose e teste de interacao ADB aprovados; duas screenshots geradas."
