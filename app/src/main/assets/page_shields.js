/*
 * Spectra page shields: injected at document start into every frame, before any page script.
 * Kotlin (PageShields.kt) fills in the config placeholder below with:
 *   { fp: bool, webrtc: bool, seed: string, off: [site keys with protection off] }
 *
 * Fingerprint shield: adds tiny, deterministic noise to canvas, audio and font-metric readouts.
 *   The noise is stable for one site within a session (so sites don't break or detect flicker),
 *   but differs between sites and sessions, so the readouts can't link you across them.
 *   Also covers OffscreenCanvas on the page, and hides the exact GPU model from WebGL.
 * WebRTC leak guard: removes ICE candidates (and stats entries) that expose a local/private IP address.
 *   Public (server-reflexive) and relay candidates are kept, so calls still connect.
 */
(function () {
  'use strict';
  var C = __SPECTRA_CONFIG__;

  // ---- Which site is this? (the top-level site, also from inside iframes) ----
  var topHost = location.hostname || '';
  try {
    var ao = location.ancestorOrigins;
    if (ao && ao.length) topHost = new URL(ao[ao.length - 1]).hostname;
  } catch (e) {}
  topHost = topHost.toLowerCase();
  for (var s = 0; s < C.off.length; s++) {
    var key = C.off[s];
    if (topHost === key || topHost.slice(-(key.length + 1)) === '.' + key) return;
  }

  // ---- Deterministic per-site noise ----
  function fnv(str) {
    var h = 0x811c9dc5;
    for (var i = 0; i < str.length; i++) {
      h ^= str.charCodeAt(i);
      h = Math.imul(h, 0x01000193);
    }
    return h >>> 0;
  }
  function mix(x) {
    x = Math.imul(x ^ (x >>> 16), 0x7feb352d);
    x = Math.imul(x ^ (x >>> 15), 0x846ca68b);
    return (x ^ (x >>> 16)) >>> 0;
  }
  var siteSeed = fnv(C.seed + '|' + topHost);

  // ---- Keep wrapped functions looking native to toString() checks ----
  var nativeToString = Function.prototype.toString;
  var originals = new WeakMap();
  function wrap(obj, name, makeWrapper) {
    if (!obj) return;
    var orig = obj[name];
    if (typeof orig !== 'function') return;
    var wrapped = makeWrapper(orig);
    originals.set(wrapped, orig);
    try {
      Object.defineProperty(obj, name, { value: wrapped, writable: true, configurable: true, enumerable: false });
    } catch (e) {}
  }
  var patchedToString = function toString() {
    return nativeToString.call(originals.get(this) || this);
  };
  originals.set(patchedToString, nativeToString);
  try {
    Object.defineProperty(Function.prototype, 'toString', { value: patchedToString, writable: true, configurable: true, enumerable: false });
  } catch (e) {}

  if (C.fp) {
    // ---- Canvas ----
    var MAX_PIXELS = 16000000;
    function noiseImageData(data) {
      for (var i = 0; i < data.length; i += 4) {
        if (data[i + 3] === 0) continue; // leave fully transparent pixels alone
        var h = mix(siteSeed ^ (i >>> 2));
        if ((h & 7) === 0) data[i + ((h >>> 3) % 3)] ^= 1; // flip the lowest bit of R, G or B
      }
    }
    var C2D = window.CanvasRenderingContext2D && CanvasRenderingContext2D.prototype;
    var origGetImageData = C2D && C2D.getImageData;

    wrap(C2D, 'getImageData', function (orig) {
      return function getImageData() {
        var img = orig.apply(this, arguments);
        try { noiseImageData(img.data); } catch (e) {}
        return img;
      };
    });

    // toDataURL / toBlob: export a noised copy, never touch the page's own canvas
    function noisedCopy(canvas) {
      var w = canvas.width, h = canvas.height;
      if (!w || !h || w * h > MAX_PIXELS || !origGetImageData) return null;
      var copy = document.createElement('canvas');
      copy.width = w;
      copy.height = h;
      var ctx = copy.getContext('2d');
      if (!ctx) return null;
      ctx.drawImage(canvas, 0, 0);
      var img = origGetImageData.call(ctx, 0, 0, w, h);
      noiseImageData(img.data);
      ctx.putImageData(img, 0, 0);
      return copy;
    }
    var CanvasProto = window.HTMLCanvasElement && HTMLCanvasElement.prototype;
    wrap(CanvasProto, 'toDataURL', function (orig) {
      return function toDataURL() {
        var copy = null;
        try { copy = noisedCopy(this); } catch (e) {}
        return orig.apply(copy || this, arguments);
      };
    });
    wrap(CanvasProto, 'toBlob', function (orig) {
      return function toBlob() {
        var copy = null;
        try { copy = noisedCopy(this); } catch (e) {}
        return orig.apply(copy || this, arguments);
      };
    });

    // WebGL pixel reads
    function wrapReadPixels(proto) {
      wrap(proto, 'readPixels', function (orig) {
        return function readPixels() {
          var r = orig.apply(this, arguments);
          try {
            var px = arguments[6];
            if (px && px.length && px.BYTES_PER_ELEMENT === 1) noiseImageData(px);
          } catch (e) {}
          return r;
        };
      });
    }
    if (window.WebGLRenderingContext) wrapReadPixels(WebGLRenderingContext.prototype);
    if (window.WebGL2RenderingContext) wrapReadPixels(WebGL2RenderingContext.prototype);

    // ---- OffscreenCanvas (on the page) ----
    var O2D = window.OffscreenCanvasRenderingContext2D && OffscreenCanvasRenderingContext2D.prototype;
    var origOffscreenGetImageData = O2D && O2D.getImageData;
    wrap(O2D, 'getImageData', function (orig) {
      return function getImageData() {
        var img = orig.apply(this, arguments);
        try { noiseImageData(img.data); } catch (e) {}
        return img;
      };
    });
    var OCProto = window.OffscreenCanvas && OffscreenCanvas.prototype;
    wrap(OCProto, 'convertToBlob', function (orig) {
      return function convertToBlob() {
        var copy = null;
        try {
          var w = this.width, h = this.height;
          if (w && h && w * h <= MAX_PIXELS && origOffscreenGetImageData) {
            copy = new OffscreenCanvas(w, h);
            var octx = copy.getContext('2d');
            octx.drawImage(this, 0, 0);
            var oimg = origOffscreenGetImageData.call(octx, 0, 0, w, h);
            noiseImageData(oimg.data);
            octx.putImageData(oimg, 0, 0);
          }
        } catch (e) { copy = null; }
        return orig.apply(copy || this, arguments);
      };
    });

    // ---- WebGL: hide the exact GPU model ----
    // UNMASKED_VENDOR_WEBGL / UNMASKED_RENDERER_WEBGL (WEBGL_debug_renderer_info)
    var GPU_VENDOR = 0x9245, GPU_RENDERER = 0x9246;
    function wrapGetParameter(proto) {
      wrap(proto, 'getParameter', function (orig) {
        return function getParameter(pname) {
          if (pname === GPU_VENDOR) return 'WebKit';
          if (pname === GPU_RENDERER) return 'WebKit WebGL';
          return orig.apply(this, arguments);
        };
      });
    }
    if (window.WebGLRenderingContext) wrapGetParameter(WebGLRenderingContext.prototype);
    if (window.WebGL2RenderingContext) wrapGetParameter(WebGL2RenderingContext.prototype);

    // ---- Fonts: canvas text metrics ----
    function nudgeMetrics(ctx, m) {
      try {
        var f = 1 + ((mix(siteSeed ^ fnv(String(ctx.font))) % 1000) / 1000 - 0.5) * 0.0006;
        Object.defineProperty(m, 'width', { value: m.width * f, configurable: true, enumerable: true });
      } catch (e) {}
      return m;
    }
    wrap(O2D, 'measureText', function (orig) {
      return function measureText() {
        return nudgeMetrics(this, orig.apply(this, arguments));
      };
    });
    wrap(C2D, 'measureText', function (orig) {
      return function measureText() {
        return nudgeMetrics(this, orig.apply(this, arguments));
      };
    });

    // ---- Audio ----
    var noisedBuffers = new WeakMap();
    function noiseChannel(buffer, channel, data) {
      var done = noisedBuffers.get(buffer);
      if (!done) { done = {}; noisedBuffers.set(buffer, done); }
      if (done[channel]) return; // noise each channel once; it's stored in place
      done[channel] = true;
      for (var i = 0; i < data.length; i++) {
        var h = mix(siteSeed ^ i ^ (channel << 24));
        if ((h & 15) === 0) data[i] += ((h >>> 4) / 268435456 - 0.5) * 1e-7;
      }
    }
    var ABProto = window.AudioBuffer && AudioBuffer.prototype;
    var origGetChannelData = ABProto && ABProto.getChannelData;
    wrap(ABProto, 'getChannelData', function (orig) {
      return function getChannelData(channel) {
        var data = orig.apply(this, arguments);
        try { noiseChannel(this, channel | 0, data); } catch (e) {}
        return data;
      };
    });
    wrap(ABProto, 'copyFromChannel', function (orig) {
      return function copyFromChannel(dest, channel) {
        try { noiseChannel(this, channel | 0, origGetChannelData.call(this, channel)); } catch (e) {}
        return orig.apply(this, arguments);
      };
    });
    var ANProto = window.AnalyserNode && AnalyserNode.prototype;
    wrap(ANProto, 'getFloatFrequencyData', function (orig) {
      return function getFloatFrequencyData(arr) {
        var r = orig.apply(this, arguments);
        try {
          for (var i = 0; i < arr.length; i++) {
            var h = mix(siteSeed ^ i ^ 0x5a5a5a5a);
            if ((h & 7) === 0) arr[i] += ((h >>> 3) / 536870912 - 0.5) * 0.1;
          }
        } catch (e) {}
        return r;
      };
    });
  }

  if (C.webrtc && window.RTCPeerConnection) {
    var PC = RTCPeerConnection.prototype;

    function isPrivateIp(ip) {
      if (/^(10\.|127\.|169\.254\.|192\.168\.)/.test(ip)) return true;
      var m = /^172\.(\d+)\./.exec(ip);
      if (m && +m[1] >= 16 && +m[1] <= 31) return true;
      m = /^100\.(\d+)\./.exec(ip);
      if (m && +m[1] >= 64 && +m[1] <= 127) return true; // carrier-grade NAT
      ip = ip.toLowerCase();
      return ip.indexOf(':') >= 0 && /^(fc|fd|fe8|fe9|fea|feb|::1$)/.test(ip);
    }
    // Returns the cleaned candidate line, or null to drop it.
    function cleanCandidate(line) {
      if (!line) return line;
      var parts = line.split(' ');
      var addr = parts[4] || '';
      var typ = parts[7];
      var isMdns = /\.local$/i.test(addr);
      // Host candidates with a raw IP reveal the device's own address
      if (typ === 'host' && !isMdns) return null;
      if (isPrivateIp(addr)) return null;
      // The "related address" of reflexive/relay candidates can carry the local IP too
      for (var i = 8; i < parts.length - 1; i++) {
        if (parts[i] === 'raddr' && isPrivateIp(parts[i + 1])) parts[i + 1] = '0.0.0.0';
        if (parts[i] === 'rport' && parts[i - 2] === 'raddr' && parts[i - 1] === '0.0.0.0') parts[i + 1] = '0';
      }
      return parts.join(' ');
    }
    function cleanSdp(sdp) {
      return sdp.split('\r\n').filter(function (l) {
        return l.indexOf('a=candidate:') !== 0 || cleanCandidate(l.slice(2)) !== null;
      }).map(function (l) {
        return l.indexOf('a=candidate:') === 0 ? 'a=' + cleanCandidate(l.slice(2)) : l;
      }).join('\r\n');
    }

    // Filter icecandidate events (both addEventListener and onicecandidate)
    function filterIceEvent(ev) {
      var c = ev && ev.candidate;
      if (!c || !c.candidate) return ev; // end-of-candidates
      var cleaned = cleanCandidate(c.candidate);
      if (cleaned === null) return null;
      if (cleaned === c.candidate) return ev;
      try {
        return new RTCPeerConnectionIceEvent('icecandidate', {
          candidate: new RTCIceCandidate({ candidate: cleaned, sdpMid: c.sdpMid, sdpMLineIndex: c.sdpMLineIndex })
        });
      } catch (e) { return null; }
    }
    var listenerMap = new WeakMap();
    function guardListener(fn) {
      if (typeof fn !== 'function') return fn;
      var g = listenerMap.get(fn);
      if (!g) {
        g = function (ev) {
          var out = filterIceEvent(ev);
          if (out) return fn.call(this, out);
        };
        listenerMap.set(fn, g);
      }
      return g;
    }
    wrap(PC, 'addEventListener', function (orig) {
      return function addEventListener(type, fn) {
        var args = Array.prototype.slice.call(arguments);
        if (type === 'icecandidate') args[1] = guardListener(fn);
        return orig.apply(this, args);
      };
    });
    wrap(PC, 'removeEventListener', function (orig) {
      return function removeEventListener(type, fn) {
        var args = Array.prototype.slice.call(arguments);
        if (type === 'icecandidate' && typeof fn === 'function' && listenerMap.has(fn)) args[1] = listenerMap.get(fn);
        return orig.apply(this, args);
      };
    });
    var onIce = Object.getOwnPropertyDescriptor(PC, 'onicecandidate');
    if (onIce && onIce.set) {
      var userHandlers = new WeakMap();
      Object.defineProperty(PC, 'onicecandidate', {
        configurable: true,
        enumerable: onIce.enumerable,
        get: function () { return userHandlers.has(this) ? userHandlers.get(this) : onIce.get.call(this); },
        set: function (fn) {
          userHandlers.set(this, fn);
          onIce.set.call(this, typeof fn === 'function' ? guardListener(fn) : fn);
        }
      });
    }

    // Scrub local addresses from connection stats (getStats on the connection, senders, receivers)
    function scrubStats(report) {
      if (!report || typeof report.forEach !== 'function') return report;
      var out = new Map();
      report.forEach(function (stat, id) {
        if (stat && (stat.type === 'local-candidate' || stat.type === 'remote-candidate')) {
          var copy = {};
          for (var k in stat) copy[k] = stat[k];
          var addr = String(copy.address || copy.ip || '');
          var leaky = stat.type === 'local-candidate' &&
            ((copy.candidateType === 'host' && !/\.local$/i.test(addr)) || isPrivateIp(addr));
          if (leaky) { delete copy.address; delete copy.ip; }
          if (copy.relatedAddress && isPrivateIp(String(copy.relatedAddress))) {
            copy.relatedAddress = '0.0.0.0';
            copy.relatedPort = 0;
          }
          out.set(id, copy);
        } else {
          out.set(id, stat);
        }
      });
      return out;
    }
    function wrapGetStats(proto) {
      wrap(proto, 'getStats', function (orig) {
        return function getStats() {
          var p = orig.apply(this, arguments);
          return p && typeof p.then === 'function' ? p.then(scrubStats) : p;
        };
      });
    }
    wrapGetStats(PC);
    if (window.RTCRtpSender) wrapGetStats(RTCRtpSender.prototype);
    if (window.RTCRtpReceiver) wrapGetStats(RTCRtpReceiver.prototype);

    // Filter candidates out of local session descriptions
    ['localDescription', 'currentLocalDescription', 'pendingLocalDescription'].forEach(function (prop) {
      var d = Object.getOwnPropertyDescriptor(PC, prop);
      if (!d || !d.get) return;
      Object.defineProperty(PC, prop, {
        configurable: true,
        enumerable: d.enumerable,
        get: function () {
          var desc = d.get.call(this);
          if (!desc || !desc.sdp) return desc;
          try { return new RTCSessionDescription({ type: desc.type, sdp: cleanSdp(desc.sdp) }); } catch (e) { return desc; }
        }
      });
    });
  }
})();
