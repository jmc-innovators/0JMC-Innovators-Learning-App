// Safe, non-blocking service worker registration. If this ever causes
// trouble, deleting this file's <script> tag from a page fully disables it
// for that page — nothing else depends on it.
if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/service-worker.js").catch((err) => {
      console.warn("Service worker registration failed:", err);
    });
  });
}
