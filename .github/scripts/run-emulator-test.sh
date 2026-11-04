#!/usr/bin/env bash
set -euo pipefail
EVIDENCE_DIR="artifacts/emulator"
mkdir -p "$EVIDENCE_DIR"

echo "=== Android virtual: informacoes ==="
adb devices -l | tee "$EVIDENCE_DIR/adb-devices.txt"
adb shell getprop ro.build.version.release | tee "$EVIDENCE_DIR/android-version.txt"
adb shell getprop ro.build.version.sdk | tee "$EVIDENCE_DIR/android-api.txt"

echo "=== Executando testes de instrumentacao Compose ==="
gradle --no-daemon :app:connectedDebugAndroidTest

echo "=== Instalando e abrindo Gemini Live ==="
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop br.com.laboratorio.android || true
# Pixel Launcher can ANR while the emulator is under instrumentation load.
# Stop it before the UI smoke; the app is launched directly by component name.
adb shell am force-stop com.google.android.apps.nexuslauncher || true
sleep 2
adb shell am start -W -n br.com.laboratorio.android/.MainActivity
sleep 5

dump_ui() {
  local filename="$1"
  timeout 15s adb shell uiautomator dump /sdcard/window.xml >/dev/null
  timeout 10s adb pull /sdcard/window.xml "$EVIDENCE_DIR/$filename" >/dev/null
}

find_point() {
  python3 - "$EVIDENCE_DIR/window-current.xml" "$1" <<'PY'
import re,sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter('node'))
needle=sys.argv[2]
for node in nodes:
    text=node.get('text','')+' '+node.get('content-desc','')
    found=(node.get('class','').endswith('EditText') if needle=='INPUT' else needle in text)
    if not found: continue
    coords=[int(v) for v in re.findall(r'\d+',node.get('bounds',''))]
    if len(coords)==4 and coords[2]>coords[0] and coords[3]>coords[1]:
        print((coords[0]+coords[2])//2, (coords[1]+coords[3])//2)
        sys.exit(0)
sys.exit(4)
PY
}

echo "=== Validando tela de configuracao ==="
READY=0
for attempt in 1 2 3 4 5 6 7 8 9 10; do
  dump_ui "window-current.xml"
  if find_point "Chave de API do Gemini" >/dev/null && find_point INPUT >/dev/null; then
    echo "PASS: campo de API key e tela de configuracao visiveis (tentativa $attempt)"
    READY=1
    break
  fi
  sleep 2
done
if [ "$READY" -ne 1 ]; then
  echo "ERRO: a tela de configuracao do Gemini Live nao ficou visivel" >&2
  exit 1
fi

dump_ui "tela-configuracoes.xml"
python3 - "$EVIDENCE_DIR/tela-configuracoes.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter('node'))
texts=[n.get('text','')+' '+n.get('content-desc','') for n in nodes]
assert any('Gemini 3.8 Live (baixa latência)' in x for x in texts), 'Modelo Gemini 3.8 Live ausente'
assert any('Gemini 3.8 Live Extended Thinking' in x for x in texts), 'Modelo Extended Thinking ausente'
assert any('Chave de API do Gemini' in x for x in texts), 'Campo de API key ausente'
print('PASS: os dois modelos Live 3.8 e a configuracao da chave estao acessiveis.')
PY
adb exec-out screencap -p > "$EVIDENCE_DIR/app-inicial.png"

echo "=== Conferindo evidencias ==="
for name in app-inicial; do
  test -s "$EVIDENCE_DIR/$name.png"
  file "$EVIDENCE_DIR/$name.png"
done
sha256sum "$EVIDENCE_DIR/"*.png | tee "$EVIDENCE_DIR/screenshots-sha256.txt"
echo "RESULTADO: Gemini Live abriu e exibiu configuracao da API key e os dois modelos."
