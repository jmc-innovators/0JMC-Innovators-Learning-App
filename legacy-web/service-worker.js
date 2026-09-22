/**
 * service-worker.js — conservative PWA caching.
 *
 * Deliberately simple and safe for a site that changes often:
 *  - HTML pages: network-first (always try fresh; fall back to cache only
 *    if offline). You will never get stuck seeing a stale page after a
 *    deploy.
 *  - Static assets (css/js/images/fonts): cache-first for speed, since
 *    they're versioned by CACHE_NAME below.
 *
 * IMPORTANT: bump CACHE_NAME on every deploy that changes CSS/JS files,
 * so returning visitors pick up the new versions instead of old cached
 * ones. A simple date string works fine, e.g. 'jmc-v2026-07-30'.
 */
const CACHE_NAME = "jmc-static-v1";
const OFFLINE_URL = "/index.html";

const PRECACHE = [
  "/css/app-shell.css",
  "/favicon-32x32.png",
  "/favicon-16x16.png",
  "/apple-touch-icon.png",
];

self.addEventListener("install", (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(PRECACHE)).catch(() => {})
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return; // never intercept Firebase/Google/CDN calls

  const isHTML = req.mode === "navigate" || req.headers.get("accept")?.includes("text/html");

  if (isHTML) {
    event.respondWith(
      fetch(req).catch(() => caches.match(req).then((r) => r || caches.match(OFFLINE_URL)))
    );
    return;
  }

  const isStaticAsset = /\.(css|js|png|jpg|jpeg|svg|webp|woff2?|ico)$/i.test(url.pathname);
  if (isStaticAsset) {
    event.respondWith(
      caches.match(req).then((cached) => {
        const fetchPromise = fetch(req).then((res) => {
          if (res && res.status === 200) {
            const clone = res.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(req, clone));
          }
          return res;
        }).catch(() => cached);
        return cached || fetchPromise;
      })
    );
  }
});
