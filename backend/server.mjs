import http from 'node:http';
import {createHmac, randomUUID, timingSafeEqual} from 'node:crypto';

const MODEL = 'gemini-3.8-live-extended-thinking';
const GOOGLE_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/auth_tokens';
const DEVICE_TOKEN_TTL_SECONDS = 365 * 24 * 60 * 60;
const MAX_BODY_BYTES = 2048;
const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function equalSafe(a, b) {
  const one = Buffer.from(a || '', 'utf8');
  const two = Buffer.from(b || '', 'utf8');
  return one.length > 0 && one.length === two.length && timingSafeEqual(one, two);
}

function signingKey(env) {
  const secret = (env.LAB_PAIRING_CODE || '').trim();
  if (secret.length < 32) return null;
  return createHmac('sha256', Buffer.from(secret, 'utf8'))
    .update('android-lab-live-broker/device-credential/v1')
    .digest();
}

function issueDeviceCredential(deviceId, env, now) {
  const key = signingKey(env);
  if (!key) throw new Error('DEVICE_SIGNING_KEY_UNAVAILABLE');
  const issuedAt = Math.floor(now / 1000);
  const payload = {
    iss: 'android-lab-live-broker',
    aud: 'gemini-live',
    ver: 1,
    sub: deviceId,
    iat: issuedAt,
    exp: issuedAt + DEVICE_TOKEN_TTL_SECONDS,
    jti: randomUUID()
  };
  const encoded = Buffer.from(JSON.stringify(payload)).toString('base64url');
  const signature = createHmac('sha256', key).update(encoded).digest('base64url');
  return {token: encoded + '.' + signature, payload};
}

function verifyDeviceCredential(token, env, now) {
  if (typeof token !== 'string' || token.length > 2048) return null;
  const pieces = token.split('.');
  if (pieces.length !== 2 || !/^[A-Za-z0-9_-]+$/.test(pieces[0]) ||
      !/^[A-Za-z0-9_-]+$/.test(pieces[1])) return null;
  const key = signingKey(env);
  if (!key) return null;
  const expected = createHmac('sha256', key).update(pieces[0]).digest();
  let supplied;
  try { supplied = Buffer.from(pieces[1], 'base64url'); } catch { return null; }
  if (supplied.length !== expected.length || !timingSafeEqual(supplied, expected)) return null;

  let payload;
  try { payload = JSON.parse(Buffer.from(pieces[0], 'base64url').toString('utf8')); }
  catch { return null; }
  const nowSeconds = Math.floor(now / 1000);
  if (payload?.iss !== 'android-lab-live-broker' ||
      payload?.aud !== 'gemini-live' ||
      payload?.ver !== 1 ||
      typeof payload?.sub !== 'string' || !UUID_V4.test(payload.sub) ||
      !Number.isSafeInteger(payload?.iat) || !Number.isSafeInteger(payload?.exp) ||
      payload.iat > nowSeconds + 60 || payload.exp <= nowSeconds ||
      payload.exp <= payload.iat || payload.exp - payload.iat > DEVICE_TOKEN_TTL_SECONDS) {
    return null;
  }
  return payload;
}

async function readJson(req) {
  return new Promise((resolve, reject) => {
    let raw = '';
    let tooLarge = false;
    req.on('data', chunk => {
      if (tooLarge) return;
      raw += chunk.toString('utf8');
      if (Buffer.byteLength(raw, 'utf8') > MAX_BODY_BYTES) {
        tooLarge = true;
        raw = '';
      }
    });
    req.on('end', () => {
      if (tooLarge) return reject(new Error('BODY_TOO_LARGE'));
      if (!raw.trim()) return resolve({});
      try {
        const parsed = JSON.parse(raw);
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
          return reject(new Error('INVALID_JSON_OBJECT'));
        }
        resolve(parsed);
      } catch {
        reject(new Error('INVALID_JSON'));
      }
    });
    req.on('error', reject);
  });
}

function bearer(req) {
  const value = req.headers.authorization || '';
  return value.startsWith('Bearer ') ? value.slice(7).trim() : '';
}

function credentialResponse(issued) {
  return {
    deviceToken: issued.token,
    deviceId: issued.payload.sub,
    expiresAtEpochSeconds: issued.payload.exp,
    expiresInSeconds: DEVICE_TOKEN_TTL_SECONDS
  };
}

export function makeServer({env = process.env, fetcher = fetch, clock = Date.now} = {}) {
  const requests = new Map();
  return http.createServer(async (req, res) => {
    res.setHeader('Cache-Control', 'no-store, max-age=0');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    const reply = (status, body) => {
      res.statusCode = status;
      res.end(JSON.stringify(body));
    };
    const now = clock();

    if (req.method === 'GET' && req.url === '/health') {
      return reply(200, {status: 'ok', service: 'android-lab-live-broker'});
    }

    if (req.method === 'POST' && req.url === '/auth/enroll') {
      const expected = (env.LAB_PAIRING_CODE || '').trim();
      if (!equalSafe(bearer(req), expected)) {
        return reply(401, {error: 'Acesso não autorizado'});
      }
      let body;
      try { body = await readJson(req); }
      catch (error) {
        return reply(error.message === 'BODY_TOO_LARGE' ? 413 : 400, {error: 'Corpo JSON inválido'});
      }
      const deviceId = typeof body.deviceId === 'string' ? body.deviceId.trim() : '';
      if (!UUID_V4.test(deviceId)) {
        return reply(400, {error: 'Identificador de dispositivo inválido'});
      }
      try {
        return reply(200, credentialResponse(issueDeviceCredential(deviceId, env, now)));
      } catch {
        return reply(503, {error: 'Assinatura de dispositivo indisponível'});
      }
    }

    if (req.method === 'POST' && req.url === '/auth/refresh') {
      const current = verifyDeviceCredential(bearer(req), env, now);
      if (!current) return reply(401, {error: 'Credencial de dispositivo inválida ou expirada'});
      return reply(200, credentialResponse(issueDeviceCredential(current.sub, env, now)));
    }

    if (req.method !== 'POST' || req.url !== '/live/token') {
      return reply(404, {error: 'Rota inexistente'});
    }

    const supplied = bearer(req);
    const device = verifyDeviceCredential(supplied, env, now);
    const expected = (env.LAB_PAIRING_CODE || '').trim();
    // Compatibilidade temporária com versões anteriores do APK.
    const legacyAuthorized = !device && equalSafe(supplied, expected);
    if (!device && !legacyAuthorized) {
      return reply(401, {error: 'Acesso não autorizado'});
    }
    if (!env.GEMINI_API_KEY || env.GEMINI_API_KEY.length < 30) {
      return reply(503, {error: 'Chave Gemini ainda não configurada no servidor'});
    }

    const remote = (req.socket.remoteAddress || 'unknown').slice(0, 100);
    const identity = device ? 'device:' + device.sub : 'legacy-ip:' + remote;
    const limit = requests.get(identity) || {start: now, count: 0};
    if (now - limit.start >= 3600000) {
      limit.start = now;
      limit.count = 0;
    }
    if (limit.count >= 6) return reply(429, {error: 'Limite de sessões deste laboratório atingido'});
    limit.count++;
    requests.set(identity, limit);

    try {
      const upstream = await fetcher(GOOGLE_ENDPOINT, {
        method: 'POST',
        headers: {'x-goog-api-key': env.GEMINI_API_KEY, 'Content-Type': 'application/json'},
        body: JSON.stringify({
          uses: 1,
          newSessionExpireTime: new Date(now + 60000).toISOString(),
          expireTime: new Date(now + 30 * 60000).toISOString()
        }),
        signal: AbortSignal.timeout(12000)
      });
      if (!upstream.ok) {
        let providerStatus = 'unknown';
        let providerReason = 'none';
        if (typeof upstream.text === 'function') {
          try {
            const error = JSON.parse(await upstream.text()).error || {};
            providerStatus = String(error.status || 'unknown').slice(0, 40);
            providerReason = String(error.message || 'none')
              .replace(/AIza[\w-]+/g, '[REDACTED]')
              .replace(/[A-Za-z0-9_-]{38,}/g, '[REDACTED]')
              .slice(0, 180);
          } catch { /* Nunca registrar payload bruto do provedor. */ }
        }
        console.error('Gemini token rejected', upstream.status, providerStatus, providerReason);
        return reply(502, {error: 'Falha ao emitir token temporário', upstreamStatus: upstream.status});
      }
      const data = await upstream.json();
      if (typeof data.name !== 'string' || !data.name) {
        return reply(502, {error: 'Resposta de token inválida'});
      }
      return reply(200, {token: data.name, model: MODEL, expiresInSeconds: 60});
    } catch (error) {
      console.error('Token service unavailable', error?.name || 'unknown');
      return reply(502, {error: 'Serviço de token indisponível'});
    }
  });
}

if (process.argv[1] && import.meta.url === new URL('file://' + process.argv[1]).href) {
  const port = Number(process.env.PORT || 10000);
  makeServer().listen(port, '0.0.0.0', () => console.log('Listening on ' + port));
}
