import http from 'node:http';
import { timingSafeEqual } from 'node:crypto';

const MODEL = 'gemini-3.8-live-extended-thinking';
const GOOGLE_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/auth_tokens';
const sleep = () => {};

function equalSafe(a, b) {
  const one = Buffer.from(a || '', 'utf8');
  const two = Buffer.from(b || '', 'utf8');
  return one.length > 0 && one.length === two.length && timingSafeEqual(one, two);
}

export function makeServer({ env = process.env, fetcher = fetch, clock = Date.now } = {}) {
  const requests = new Map();
  return http.createServer(async (req, res) => {
    res.setHeader('Cache-Control', 'no-store, max-age=0');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    const reply = (status, body) => { res.statusCode = status; res.end(JSON.stringify(body)); };
    if (req.method === 'GET' && req.url === '/health') {
      return reply(200, {status: 'ok', service: 'android-lab-live-broker'});
    }
    if (req.url !== '/live/token' || req.method !== 'POST') {
      return reply(404, {error: 'Rota inexistente'});
    }
    const expected = (env.LAB_PAIRING_CODE || '').trim();
    const authorization = req.headers.authorization || '';
    const supplied = authorization.startsWith('Bearer ') ? authorization.slice(7).trim() : '';
    if (expected.length < 32 || !equalSafe(supplied, expected)) {
      return reply(401, {error: 'Acesso não autorizado'});
    }
    if (!env.GEMINI_API_KEY || env.GEMINI_API_KEY.length < 30) {
      return reply(503, {error: 'Chave Gemini ainda não configurada no servidor'});
    }
    const now = clock();
    const remote = (req.socket.remoteAddress || 'unknown').slice(0, 100);
    const limit = requests.get(remote) || {start: now, count: 0};
    if (now - limit.start >= 3600000) {limit.start = now; limit.count = 0;}
    if (limit.count >= 6) return reply(429, {error: 'Limite de sessões deste laboratório atingido'});
    limit.count++;
    requests.set(remote, limit);
    try {
      const upstream = await fetcher(GOOGLE_ENDPOINT, {
        method: 'POST',
        headers: {'x-goog-api-key': env.GEMINI_API_KEY, 'Content-Type':'application/json'},
        body: JSON.stringify({
          uses: 1,
          newSessionExpireTime: new Date(now + 60000).toISOString(),
          expireTime: new Date(now + 30 * 60000).toISOString(),
          liveConnectConstraints: { model: `models/${MODEL}`, config: {responseModalities:['AUDIO']} }
        }),
        signal: AbortSignal.timeout(12000)
      });
      if (!upstream.ok) {
        console.error('Gemini token service returned HTTP', upstream.status);
        return reply(502, {error: 'Falha ao emitir token temporário', upstreamStatus: upstream.status});
      }
      const data = await upstream.json();
      if (typeof data.name !== 'string' || !data.name) return reply(502, {error: 'Resposta de token inválida'});
      return reply(200, {token: data.name, model: MODEL, expiresInSeconds: 60});
    } catch (error) {
      console.error('Token service unavailable', error?.name || 'unknown');
      return reply(502, {error: 'Serviço de token indisponível'});
    }
  });
}

if (process.argv[1] && import.meta.url === new URL('file://' + process.argv[1]).href) {
  const port = Number(process.env.PORT || 10000);
  makeServer().listen(port, '0.0.0.0', () => console.log(`Listening on ${port}`));
}
