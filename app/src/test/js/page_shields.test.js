// Run: node app/src/test/js/page_shields.test.js   (needs only Node.js, no Android SDK)
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const path = require("path");
const src = fs.readFileSync(process.argv[2] || path.join(__dirname, "../../main/assets/page_shields.js"), "utf8");

function makeEnv(cfg, host) {
  class ImageDataX { constructor(n){ this.data = new Uint8ClampedArray(n*4).fill(200); } }
  class C2D { getImageData(x,y,w,h){ return new ImageDataX(w*h); } measureText(t){ return { width: t.length*10 }; } }
  class O2D { getImageData(x,y,w,h){ return new ImageDataX(w*h); } measureText(t){ return { width: t.length*10 }; } }
  class GL { getParameter(p){ return p === 0x9245 ? 'Qualcomm' : p === 0x9246 ? 'Adreno (TM) 740' : 'other'; } }
  class AudioBuffer { constructor(){ this.d = new Float32Array(1000).fill(0.5); } getChannelData(){ return this.d; } }
  class PCEvent { constructor(t, o){ this.type=t; this.candidate=o.candidate; } }
  class IceCand { constructor(o){ Object.assign(this,o); } }
  class SD { constructor(o){ Object.assign(this,o); } }
  class RTCPeerConnection {
    constructor(){ this.ls=[]; this._on=null; }
    addEventListener(t,f){ this.ls.push(f); }
    removeEventListener(t,f){ this.ls = this.ls.filter(x=>x!==f); }
    getStats(){ return Promise.resolve(new Map([
      ['L1', { type: 'local-candidate', candidateType: 'host', address: '192.168.1.5', port: 1 }],
      ['L2', { type: 'local-candidate', candidateType: 'host', address: 'abcd.local', port: 1 }],
      ['L3', { type: 'local-candidate', candidateType: 'srflx', address: '203.0.113.9', relatedAddress: '192.168.1.5', relatedPort: 5 }],
      ['P1', { type: 'candidate-pair', localCandidateId: 'L1' }]
    ])); }
    fire(cand){ const ev={candidate:{candidate:cand,sdpMid:'0',sdpMLineIndex:0}}; this.ls.forEach(f=>f(ev)); if(this._on) this._on(ev); }
  }
  Object.defineProperty(RTCPeerConnection.prototype,'onicecandidate',{configurable:true,get(){return this._on;},set(f){this._on=f;}});
  Object.defineProperty(RTCPeerConnection.prototype,'localDescription',{configurable:true,get(){return {type:'offer',sdp:this._sdp};}});
  const w = { CanvasRenderingContext2D: C2D, OffscreenCanvasRenderingContext2D: O2D, WebGLRenderingContext: GL, WebGL2RenderingContext: class extends GL {}, AudioBuffer, RTCPeerConnection, RTCPeerConnectionIceEvent: PCEvent,
    RTCIceCandidate: IceCand, RTCSessionDescription: SD, location: { hostname: host, ancestorOrigins: [] }, URL };
  w.window = w;
  const ctx = vm.createContext(w);
  vm.runInContext(src.replace('__SPECTRA_CONFIG__', JSON.stringify(cfg)), ctx);
  return w;
}
const base = { fp: true, webrtc: true, seed: 'abc', off: ['broken.com'] };

// Canvas: noise applied, deterministic per site, different across sites
const a = makeEnv(base, 'example.com'), a2 = makeEnv(base, 'www.example.com'), b = makeEnv(base, 'other.org');
const img = e => Array.from(new e.CanvasRenderingContext2D().getImageData(0,0,50,50).data);
const ia = img(a), ia2 = img(makeEnv(base,'example.com')), ib = img(b);
const changed = ia.filter(v => v !== 200).length;
assert(changed > 0 && changed < ia.length * 0.1, 'some, but few, pixels change: ' + changed);
assert.deepStrictEqual(ia, ia2, 'same site + session -> same noise');
assert.notDeepStrictEqual(ia, ib, 'different site -> different noise');
assert(ia.every(v => Math.abs(v - 200) <= 1), 'noise is at most 1 level');
assert.notDeepStrictEqual(img(makeEnv({...base, seed:'xyz'}, 'example.com')), ia, 'new session -> new noise');

// Excluded site and its subdomains: untouched
assert(img(makeEnv(base, 'broken.com')).every(v => v === 200), 'excluded site untouched');
assert(img(makeEnv(base, 'shop.broken.com')).every(v => v === 200), 'excluded subdomain untouched');
assert(img(makeEnv(base, 'notbroken.com')).some(v => v !== 200), 'lookalike domain still protected');
// fp off
assert(img(makeEnv({...base, fp:false}, 'example.com')).every(v => v === 200), 'fp off -> untouched');

// Fonts
const mt = e => new e.CanvasRenderingContext2D().measureText('hello').width;
assert(mt(a) !== 50 && Math.abs(mt(a) - 50) < 0.05, 'measureText nudged slightly: ' + mt(a));

// Audio: noised once, small
const buf = new a.AudioBuffer(); const d1 = Array.from(buf.getChannelData(0)); const d2 = Array.from(buf.getChannelData(0));
assert.deepStrictEqual(d1, d2, 'repeat reads do not accumulate noise');
assert(d1.some(v => v !== 0.5) && d1.every(v => Math.abs(v - 0.5) < 1e-6), 'audio noise tiny');

// toString looks native
assert(String(a.CanvasRenderingContext2D.prototype.getImageData).indexOf('noise') < 0, 'wrapped fn hides its source');

// WebRTC
const pc = new a.RTCPeerConnection(); const got = []; pc.addEventListener('icecandidate', ev => got.push(ev.candidate.candidate));
const viaProp = []; pc.onicecandidate = ev => viaProp.push(ev.candidate.candidate);
pc.fire('candidate:1 1 udp 2122260223 192.168.1.5 54321 typ host generation 0');
pc.fire('candidate:2 1 udp 2122260223 abcd-1234.local 54321 typ host generation 0');
pc.fire('candidate:3 1 udp 1686052607 203.0.113.9 6000 typ srflx raddr 192.168.1.5 rport 54321 generation 0');
pc.fire('candidate:4 1 udp 2122260223 2001:db8::1 54321 typ host generation 0');
console.log('listener got:', got);
assert.strictEqual(got.length, 2, 'raw-IP host candidates dropped');
assert(got[0].includes('.local'), 'mDNS host kept');
assert(got[1].includes('203.0.113.9') && got[1].includes('raddr 0.0.0.0 rport 0') && !got[1].includes('192.168'), 'srflx kept, local raddr scrubbed');
assert.deepStrictEqual(viaProp, got, 'onicecandidate filtered the same way');
pc._sdp = 'v=0\r\na=candidate:1 1 udp 2122260223 10.0.0.2 5000 typ host\r\na=candidate:3 1 udp 1686052607 203.0.113.9 6000 typ srflx raddr 10.0.0.2 rport 5000\r\na=end';
const sdp = pc.localDescription.sdp; console.log(JSON.stringify(sdp));
assert(!sdp.includes('10.0.0.2') && sdp.includes('203.0.113.9'), 'SDP scrubbed');
// OffscreenCanvas
const oimg = Array.from(new a.OffscreenCanvasRenderingContext2D().getImageData(0,0,50,50).data);
assert.deepStrictEqual(oimg, ia, 'OffscreenCanvas gets the same noise as canvas');
assert(new a.OffscreenCanvasRenderingContext2D().measureText('hello').width !== 50, 'OffscreenCanvas text metrics nudged');

// WebGL GPU model hidden; other parameters untouched
const gl = new a.WebGLRenderingContext(), gl2 = new a.WebGL2RenderingContext();
assert.strictEqual(gl.getParameter(0x9246), 'WebKit WebGL');
assert.strictEqual(gl.getParameter(0x9245), 'WebKit');
assert.strictEqual(gl2.getParameter(0x9246), 'WebKit WebGL', 'WebGL2 too');
assert.strictEqual(gl.getParameter(0x1F00), 'other', 'other parameters pass through');
assert.strictEqual(new (makeEnv({...base, fp:false}, 'example.com').WebGLRenderingContext)().getParameter(0x9246), 'Adreno (TM) 740', 'fp off -> real GPU');

// WebRTC stats (async)
new a.RTCPeerConnection().getStats().then(rep => {
  const l1 = rep.get('L1'), l2 = rep.get('L2'), l3 = rep.get('L3');
  assert(!('address' in l1), 'raw-IP host candidate address removed from stats');
  assert.strictEqual(l2.address, 'abcd.local', 'mDNS address kept');
  assert.strictEqual(l3.address, '203.0.113.9', 'public address kept');
  assert.strictEqual(l3.relatedAddress, '0.0.0.0', 'local related address scrubbed');
  assert.strictEqual(rep.get('P1').localCandidateId, 'L1', 'other stats untouched');
  assert(rep.size === 4 && typeof rep.forEach === 'function', 'report still works like a map');
  console.log('ALL TESTS PASSED');
}).catch(e => { console.error(e); process.exit(1); });
