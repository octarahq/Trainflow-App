const CACHE_NAME = 'trainflow-cache-v1';
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './icon.png',
  './trainflowApp.js',
  './trainflowApp.wasm'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(ASSETS).catch(() => {});
    })
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    fetch(event.request).catch(() => {
      return caches.match(event.request);
    })
  );
});
