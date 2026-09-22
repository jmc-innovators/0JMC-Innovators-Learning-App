/**
 * ui-utils.js — small shared helpers used across the new pages.
 */

/* ---------------- Toasts ---------------- */
let toastHost = null;
function ensureToastHost() {
  if (toastHost) return toastHost;
  toastHost = document.createElement("div");
  toastHost.className = "jmc-toast-host";
  document.body.appendChild(toastHost);
  return toastHost;
}
export function toast(message, type = "info", ms = 3600) {
  const host = ensureToastHost();
  const el = document.createElement("div");
  el.className = `jmc-toast jmc-toast--${type}`;
  el.setAttribute("role", "status");
  el.textContent = message;
  host.appendChild(el);
  requestAnimationFrame(() => el.classList.add("show"));
  setTimeout(() => {
    el.classList.remove("show");
    setTimeout(() => el.remove(), 300);
  }, ms);
}

/* ---------------- Sanitization ---------------- */
/** Strips tags/scripts from freeform text before it's ever written to Firestore or the DOM. */
export function sanitizeText(input, maxLen = 500) {
  if (typeof input !== "string") return "";
  const stripped = input.replace(/<[^>]*>/g, "").replace(/[<>]/g, "");
  return stripped.trim().slice(0, maxLen);
}

/** Escapes text for safe innerHTML interpolation (defends against stored-XSS from user-entered fields). */
export function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}

/* ---------------- Formatting ---------------- */
export function formatRelativeTime(date) {
  if (!date) return "—";
  const d = date instanceof Date ? date : date.toDate ? date.toDate() : new Date(date);
  const diffMs = Date.now() - d.getTime();
  const min = Math.floor(diffMs / 60000);
  if (min < 1) return "just now";
  if (min < 60) return `${min}m ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h ago`;
  const day = Math.floor(hr / 24);
  if (day < 7) return `${day}d ago`;
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

export function formatMinutes(mins) {
  mins = Math.max(0, Math.round(mins || 0));
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  if (h === 0) return `${m}m`;
  return `${h}h ${m}m`;
}

/* ---------------- Device / session fingerprint (client-reported, for login history) ---------------- */
export function describeDevice() {
  const ua = navigator.userAgent || "";
  let os = "Unknown OS";
  if (/Windows/i.test(ua)) os = "Windows";
  else if (/Android/i.test(ua)) os = "Android";
  else if (/iPhone|iPad|iPod/i.test(ua)) os = "iOS";
  else if (/Mac OS X/i.test(ua)) os = "macOS";
  else if (/Linux/i.test(ua)) os = "Linux";

  let browser = "Unknown browser";
  if (/Edg\//i.test(ua)) browser = "Edge";
  else if (/Chrome\//i.test(ua) && !/Edg\//i.test(ua)) browser = "Chrome";
  else if (/Firefox\//i.test(ua)) browser = "Firefox";
  else if (/Safari\//i.test(ua) && !/Chrome\//i.test(ua)) browser = "Safari";

  return { os, browser, label: `${browser} on ${os}`, userAgent: ua.slice(0, 300) };
}

/* ---------------- Small helpers ---------------- */
export function debounce(fn, wait = 300) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), wait); };
}

export function initials(name) {
  if (!name) return "?";
  return name.trim().split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() || "").join("");
}

export function qs(sel, root = document) { return root.querySelector(sel); }
export function qsa(sel, root = document) { return Array.from(root.querySelectorAll(sel)); }
