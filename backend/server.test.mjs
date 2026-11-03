import test from 'node:test';
import assert from 'node:assert/strict';
import {makeServer} from './server.mjs';

const CODE = 'lab_test_pairing_code_1234567890ABCD';
const TEST_KEY = 'fake_key_only_for_unit_tests_1234567890';
async function serve(config, run) {
  const server = makeServer({env:{LAB_PAIRING_CODE:CODE,GEMINI_API_KEY:TEST_KEY,...config?.env},fetcher:config?.fetcher||(async()=>({ok:true,json:async()=>({name:'ephemeral-test-token'})}))});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  try {await run(`http://127.0.0.1:${server.address().port}`);} finally {await new Promise(resolve=>server.close(resolve));}
}
test('health responde sem credencial',async()=>serve({},async base=>{
  const r=await fetch(base+'/health');assert.equal(r.status,200);assert.equal((await r.json()).status,'ok');
}));
test('token exige credencial de pareamento',async()=>serve({},async base=>{
  const r=await fetch(base+'/live/token',{method:'POST'});assert.equal(r.status,401);
}));
test('nao emite token sem configuracao do provedor',async()=>serve({env:{GEMINI_API_KEY:''}},async base=>{
  const r=await fetch(base+'/live/token',{method:'POST',headers:{Authorization:`Bearer ${CODE}`}});assert.equal(r.status,503);
}));
test('token de sessao usa modelo e restricoes',async()=>{
  let sent;
  await serve({fetcher:async(url,opts)=>{sent={url,opts};return {ok:true,json:async()=>({name:'ephemeral-test-token'})};}},async base=>{
    const r=await fetch(base+'/live/token',{method:'POST',headers:{Authorization:`Bearer ${CODE}`}});
    assert.equal(r.status,200);assert.equal((await r.json()).token,'ephemeral-test-token');
  });
  assert.equal(sent.opts.headers['x-goog-api-key'],TEST_KEY);
  const payload=JSON.parse(sent.opts.body);
  assert.equal(payload.uses,1);
  assert.equal(payload.liveConnectConstraints.model,'models/gemini-3.8-live-extended-thinking');
});
test('falha upstream nao expoe chave',async()=>serve({fetcher:async()=>({ok:false,status:403})},async base=>{
  const r=await fetch(base+'/live/token',{method:'POST',headers:{Authorization:`Bearer ${CODE}`}});
  assert.equal(r.status,502);assert.equal((await r.text()).includes(TEST_KEY),false);
}));
