#!/usr/bin/env bash
set -euo pipefail
EVIDENCE_DIR="artifacts/emulator"
mkdir -p "$EVIDENCE_DIR"
echo "=== Android virtual: informacoes ==="
adb devices -l | tee "$EVIDENCE_DIR/adb-devices.txt"
adb shell getprop ro.build.version.release | tee "$EVIDENCE_DIR/android-version.txt"
adb shell getprop ro.build.version.sdk | tee "$EVIDENCE_DIR/android-api.txt"

echo "=== Executando testes reais de instrumentacao Compose ==="
gradle --no-daemon :app:connectedDebugAndroidTest

echo "=== Instalando e abrindo aplicativo para capturas ADB ==="
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop br.com.laboratorio.android || true
adb shell am start -W -n br.com.laboratorio.android/.MainActivity
sleep 3

dump_ui() {
  local filename="$1"
  adb shell uiautomator dump /sdcard/window.xml >/dev/null
  adb pull /sdcard/window.xml "$EVIDENCE_DIR/$filename" >/dev/null
}

dismiss_wait_dialogs() {
  local attempt point
  for attempt in 1 2 3 4; do
    dump_ui "window-current.xml"
    if point=$(find_point 'Wait'); then
      echo "AVISO TRANSITORIO: dialogo do launcher detectado apos acao; selecionando Wait em $point"
      adb exec-out screencap -p > "$EVIDENCE_DIR/diagnostico-launcher-anr.png"
      adb shell input tap $point
      sleep 3
    else
      return 0
    fi
  done
  dump_ui "window-current.xml"
  if find_point 'Wait' >/dev/null; then
    echo "ERRO: dialogo Wait persistiu apos quatro tentativas" >&2
    return 1
  fi
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

tap_text() {
  local target="$1" point=""
  for attempt in 1 2 3 4; do
    dump_ui "window-current.xml"
    if point=$(find_point "$target"); then
      echo "Tap '$target' em $point"
      adb shell input tap $point
      sleep 1
      dismiss_wait_dialogs
      return
    fi
    if point=$(find_point 'Wait'); then
      echo "AVISO TRANSITORIO: launcher Android sem resposta; selecionando Wait em $point"
      adb exec-out screencap -p > "$EVIDENCE_DIR/diagnostico-launcher-anr.png"
      adb shell input tap $point
      sleep 3
      continue
    fi
    echo "Elemento '$target' nao visivel; rolando tela, tentativa $attempt"
    adb shell input swipe 500 1830 500 630 350
    sleep 1
  done
  echo "ERRO: elemento '$target' nao localizado na hierarquia acessivel" >&2
  return 1
}

echo "=== Validando UI operacional e tratando avisos temporarios do launcher ==="
READY=0
for attempt in 1 2 3 4 5 6 7 8 9 10; do
  dump_ui "window-current.xml"
  if find_point INPUT >/dev/null; then
    echo "PASS: campo de anotacao acessivel (tentativa $attempt)"
    READY=1
    break
  fi
  if point=$(find_point 'Wait'); then
    echo "AVISO TRANSITORIO: launcher travado; selecionando Wait em $point"
    adb exec-out screencap -p > "$EVIDENCE_DIR/diagnostico-launcher-anr.png"
    adb shell input tap $point
    sleep 3
    continue
  fi
  echo "UI ainda nao pronta; nova tentativa $attempt"
  sleep 2
done
if [ "$READY" -ne 1 ]; then
  echo "ERRO: o app nao ficou visivel em 10 tentativas" >&2
  exit 1
fi
dump_ui "tela-inicial.xml"
adb exec-out screencap -p > "$EVIDENCE_DIR/app-inicial.png"
echo "=== Inserindo anotacao ficticia pelo Android virtual ==="
tap_text INPUT
adb shell input text 'Aprendemos%sKotlin%sCompose%se%sarquitetura%sAndroid'
adb shell input keyevent 4
tap_text 'Gerar resumo (Mock)'
sleep 3
dismiss_wait_dialogs
dump_ui "resumo-sugerido.xml"
python3 - "$EVIDENCE_DIR/resumo-sugerido.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter('node'))
words=[n.get('text','')+' '+n.get('content-desc','') for n in nodes]
assert any('Resumo simulado:' in x for x in words), 'Nao foi encontrado resumo Mock no Android virtual'
print('PASS: sugestao deterministica do Mock apareceu na UI.')
PY
adb exec-out screencap -p > "$EVIDENCE_DIR/resumo-sugerido.png"

echo "=== Human-in-the-Loop: aprovacao da sugestao ==="
tap_text 'Aprovar resumo'
sleep 1
dismiss_wait_dialogs
dump_ui "resumo-aprovado.xml"
python3 - "$EVIDENCE_DIR/resumo-aprovado.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter('node'))
words=[n.get('text','')+' '+n.get('content-desc','') for n in nodes]
assert any('Resumo aprovado' in x for x in words), 'A aprovacao humana simulada nao apareceu'
print('PASS: HITL interno registrou resumo aprovado.')
PY
adb exec-out screencap -p > "$EVIDENCE_DIR/resumo-aprovado.png"

echo "=== Conferindo as capturas produzidas ==="
for name in app-inicial resumo-sugerido resumo-aprovado; do
  test -s "$EVIDENCE_DIR/$name.png"
  file "$EVIDENCE_DIR/$name.png"
done
sha256sum "$EVIDENCE_DIR/"*.png | tee "$EVIDENCE_DIR/screenshots-sha256.txt"
echo "RESULTADO: Android virtual testado com Mock, sugestao gerada e aprovada."
