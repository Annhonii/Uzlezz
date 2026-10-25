/* ClearTube page script. Runs before YouTube's own scripts (document-start).
 * window.__CT_CSS (the contents of clean.css) is put in front of this file by the app. */
(function () {
  'use strict';
  if (window.__ct) return;
  window.__ct = 1;

  /* ---------- 1. CSS ---------- */
  function injectCss() {
    if (document.getElementById('ct-css')) return true;
    var parent = document.head || document.documentElement;
    if (!parent) return false;
    var s = document.createElement('style');
    s.id = 'ct-css';
    s.textContent = window.__CT_CSS || '';
    parent.appendChild(s);
    return true;
  }
  if (!injectCss()) {
    var mo = new MutationObserver(function () { if (injectCss()) mo.disconnect(); });
    mo.observe(document, { childList: true, subtree: true });
  }
  document.addEventListener('yt-navigate-finish', injectCss);

  /* ---------- 2. remove ads from YouTube's data before the page reads it ---------- */
  var AD_ITEM = /^(adSlotRenderer|promotedSparklesWebRenderer|promotedVideoRenderer|compactPromotedVideoRenderer|inFeedAdLayoutRenderer|displayAdRenderer|bannerPromoRenderer|statementBannerRenderer|searchPyvRenderer|promotedSparklesTextSearchRenderer|videoMastheadAdV3Renderer|primetimePromoRenderer|brandVideoSingletonRenderer)$/;
  var AD_LISTS = { adPlacements: 1, playerAds: 1, adSlots: 1 };

  function hasAdKey(o) {
    for (var k in o) if (AD_ITEM.test(k)) return true;
    return false;
  }
  function isAdItem(item) {
    if (!item || typeof item !== 'object') return false;
    if (hasAdKey(item)) return true;
    // ads are usually wrapped: { richItemRenderer: { content: { adSlotRenderer } } }
    for (var k in item) {
      var c = item[k];
      if (c && typeof c === 'object') {
        if (hasAdKey(c)) return true;
        if (c.content && typeof c.content === 'object' && hasAdKey(c.content)) return true;
      }
    }
    return false;
  }
  function clean(o, depth) {
    if (!o || typeof o !== 'object' || depth > 40) return;
    if (Array.isArray(o)) {
      for (var i = o.length - 1; i >= 0; i--) {
        if (isAdItem(o[i])) o.splice(i, 1); else clean(o[i], depth + 1);
      }
      return;
    }
    for (var k in o) {
      if (AD_LISTS[k]) { o[k] = []; continue; }
      clean(o[k], depth + 1);
    }
  }
  function looksLikeYt(r) {
    return r && typeof r === 'object' &&
      (r.responseContext || r.contents || r.playerResponse || r.adPlacements || r.onResponseReceivedActions);
  }

  var parse = JSON.parse;
  JSON.parse = function () {
    var r = parse.apply(this, arguments);
    try { if (looksLikeYt(r)) clean(r, 0); } catch (e) {}
    return r;
  };

  var rjson = Response.prototype.json;
  Response.prototype.json = function () {
    return rjson.apply(this, arguments).then(function (r) {
      try { if (looksLikeYt(r)) clean(r, 0); } catch (e) {}
      return r;
    });
  };

  // the first page load carries its data inline (var ytInitialData = {...}), so catch the assignment
  ['ytInitialPlayerResponse', 'ytInitialData'].forEach(function (name) {
    var val;
    try {
      Object.defineProperty(window, name, {
        configurable: true,
        get: function () { return val; },
        set: function (x) { try { clean(x, 0); } catch (e) {} val = x; }
      });
    } catch (e) {}
  });

  /* ---------- 3. safety net: if an ad still plays, mute it and jump to its end ---------- */
  var mutedByUs = false;
  setInterval(function () {
    var v = document.querySelector('video');
    if (!v) return;
    if (document.querySelector('.ad-showing')) {
      if (!mutedByUs) { mutedByUs = true; v.__ctWasMuted = v.muted; }
      v.muted = true;
      if (isFinite(v.duration) && v.duration > 0) v.currentTime = v.duration;
      var b = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
      if (b) b.click();
    } else if (mutedByUs) {
      mutedByUs = false;
      v.muted = !!v.__ctWasMuted;
    }
  }, 400);
})();
