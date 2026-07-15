// This is exactly how a consumer uses the plugin — a plain import.
import { CapacitorCameraCrop } from 'capacitor-camera-crop';
import { Capacitor } from '@capacitor/core';

function readOptions() {
  const source = document.querySelector('input[name="source"]:checked').value;
  const aspectSel = document.getElementById('aspectRatio').value;
  const aspectRatio =
    aspectSel === 'custom'
      ? { x: Number(document.getElementById('ratioX').value), y: Number(document.getElementById('ratioY').value) }
      : aspectSel;
  const options = {
    source,
    enableCropping: document.getElementById('enableCropping').checked,
    nativeCropping: document.getElementById('nativeCropping').checked,
    useSystemEditingIfAvailable: document.getElementById('useSystemEditing').checked,
    aspectRatio,
    resultType: document.getElementById('resultType').value,
    quality: Number(document.getElementById('quality').value),
  };
  const w = document.getElementById('width').value;
  const h = document.getElementById('height').value;
  if (w) options.width = Number(w);
  if (h) options.height = Number(h);
  return options;
}

async function run() {
  const status = document.getElementById('status');
  const output = document.getElementById('output');
  const preview = document.getElementById('preview');
  preview.style.display = 'none';
  status.textContent = '';
  status.className = '';

  const options = readOptions();
  output.textContent = 'Requested options:\n' + JSON.stringify(options, null, 2);

  try {
    const result = await CapacitorCameraCrop.captureAndCrop(options);
    status.className = 'ok';
    status.textContent = '✅ Success — ' + result.width + '×' + result.height + ' ' + result.mimeType;
    output.textContent =
      'Requested options:\n' + JSON.stringify(options, null, 2) +
      '\n\nResult:\n' + JSON.stringify({ ...result, value: result.value.slice(0, 80) + '…(truncated)' }, null, 2);
    // For 'uri' results the value is a file:// path, which a WKWebView/Capacitor
    // webview cannot load directly — convertFileSrc() rewrites it to a URL the
    // webview can display. base64 results are shown as a data URI.
    preview.src =
      options.resultType === 'base64'
        ? 'data:' + result.mimeType + ';base64,' + result.value
        : Capacitor.convertFileSrc(result.value);
    preview.style.display = 'block';
  } catch (e) {
    status.className = 'err';
    status.textContent = '❌ ' + (e && e.message ? e.message : String(e));
  }
}

document.getElementById('run').addEventListener('click', run);
