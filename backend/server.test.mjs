import test from 'node:test';
import assert from 'node:assert/strict';
import {makeServer} from './server.mjs';

const CODE = 'lab_test_pairing_code_1234567890ABCD';
const TEST_KEY = 'fake_key_only_for_unit_tests_1234567890';
const DEVICE_ID = '8c7d1c8e-4d85-4d11-9b43-4a1c53c68e90';
const DAY = 24 * 60 * 60 * 1000;
const fakeGoogle = async () => ({ok: true, json: async () => ({name: 'ephemeral-test-token'})});

async function serve(config, run) {
  const server = makeServer({
    env: {LAB_PAIRING_CODE: CODE, GEMINI_API_KEY: TEST_KEY, ...config?.env},
    fetcher: config?.fetcher || fakeGoogle,
    clock: config?.clock || Date.now
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  try { await run('http://127.0.0.1:' + server.address().port); }
  finally { await new Promise(resolve => server.close(resolve)); }
}

const auth = token => ({Authorization: 'Bearer ' + token});
async function enroll(base, deviceId = DEVICE_ID, code = CODE) {
  return fetch(base + '/auth/enroll', {
    method: 'POST',
    headers: {...auth(code), 'Content-Type': 'application/json'},
    body: JSON.stringify({deviceId})
  });
}

test('health responde sem credencial', async () => serve({}, async base => {
  const r = await fetch(base + '/health');
  assert.equal(r.status, 200);
  assert.equal((await r.json()).status, 'ok');
}));

test('enrollment exige código correto', async () => serve({}, async base => {
  const r = await enroll(base, DEVICE_ID, 'wrong-pairing-code');
  assert.equal(r.status, 401);
}));

test('enrollment rejeita identificador que não seja UUID v4', async () => serve({}, async base => {
  const r = await enroll(base, 'not-a-device-id');
  assert.equal(r.status, 400);
}));

test('enrollment emite credencial assinada de 365 dias sem revelar segredo', async () => serve({}, async base => {
  const r = await enroll(base);
  assert.equal(r.status, 200);
  const body = await r.json();
  assert.equal(body.deviceId, DEVICE_ID);
  assert.equal(body.expiresInSeconds, 365 * 24 * 60 * 60);
  assert.equal(body.expiresAtEpochSeconds > Math.floor(Date.now() / 1000), true);
  assert.equal(typeof body.deviceToken, 'string');
  assert.equal(body.deviceToken.includes(CODE), false);
  assert.equal(body.deviceToken.includes(TEST_KEY), false);
}));

test('refresh renova credencial e mantém identidade do dispositivo', async () => serve({}, async base => {
  const first = await (await enroll(base)).json();
  const refreshed = await fetch(base + '/auth/refresh', {
    method: 'POST', headers: auth(first.deviceToken)
  });
  assert.equal(refreshed.status, 200);
  const second = await refreshed.json();
  assert.equal(second.deviceId, DEVICE_ID);
  assert.notEqual(second.deviceToken, first.deviceToken);
}));

test('refresh rejeita credencial adulterada', async () => serve({}, async base => {
  const first = await (await enroll(base)).json();
  const tampered = first.deviceToken.slice(0, -1) + (first.deviceToken.endsWith('A') ? 'B' : 'A');
  const r = await fetch(base + '/auth/refresh', {method: 'POST', headers: auth(tampered)});
  assert.equal(r.status, 401);
}));

test('credencial expirada não renova nem emite token Gemini', async () => {
  let now = Date.now();
  await serve({clock: () => now}, async base => {
    const first = await (await enroll(base)).json();
    now += 366 * DAY;
    const refresh = await fetch(base + '/auth/refresh', {
      method: 'POST', headers: auth(first.deviceToken)
    });
    assert.equal(refresh.status, 401);
    const live = await fetch(base + '/live/token', {method: 'POST', headers: auth(first.deviceToken)});
    assert.equal(live.status, 401);
  });
});

test('credencial de dispositivo autoriza emissão de token Gemini efêmero', async () => {
  let sent;
  await serve({fetcher: async (url, options) => {
    sent = {url, options};
    return fakeGoogle();
  }}, async base => {
    const device = await (await enroll(base)).json();
    const r = await fetch(base + '/live/token', {method: 'POST', headers: auth(device.deviceToken)});
    assert.equal(r.status, 200);
    assert.equal((await r.json()).token, 'ephemeral-test-token');
  });
  assert.equal(sent.options.headers['x-goog-api-key'], TEST_KEY);
  const payload = JSON.parse(sent.options.body);
  assert.equal(payload.uses, 1);
  assert.equal(payload.liveConnectConstraints, undefined);
  assert.equal(typeof payload.expireTime, 'string');
  assert.equal(typeof payload.newSessionExpireTime, 'string');
});

test('token Gemini continua exigindo credencial válida', async () => serve({}, async base => {
  const r = await fetch(base + '/live/token', {method: 'POST', headers: auth('invalid')});
  assert.equal(r.status, 401);
}));

test('código legado continua compatível durante atualização dos APKs', async () => serve({}, async base => {
  const r = await fetch(base + '/live/token', {method: 'POST', headers: auth(CODE)});
  assert.equal(r.status, 200);
  assert.equal((await r.json()).token, 'ephemeral-test-token');
}));

test('limite de sessões é aplicado por dispositivo', async () => serve({}, async base => {
  const device = await (await enroll(base)).json();
  for (let i = 0; i < 6; i++) {
    const r = await fetch(base + '/live/token', {method: 'POST', headers: auth(device.deviceToken)});
    assert.equal(r.status, 200);
  }
  const limited = await fetch(base + '/live/token', {method: 'POST', headers: auth(device.deviceToken)});
  assert.equal(limited.status, 429);
}));

test('ausência de chave do provedor não emite token', async () => serve({
  env: {GEMINI_API_KEY: ''}
}, async base => {
  const device = await (await enroll(base)).json();
  const r = await fetch(base + '/live/token', {method: 'POST', headers: auth(device.deviceToken)});
  assert.equal(r.status, 503);
}));

test('falha upstream não expõe a chave Gemini', async () => serve({
  fetcher: async () => ({ok: false, status: 403})
}, async base => {
  const device = await (await enroll(base)).json();
  const r = await fetch(base + '/live/token', {method: 'POST', headers: auth(device.deviceToken)});
  assert.equal(r.status, 502);
  assert.equal((await r.text()).includes(TEST_KEY), false);
}));
