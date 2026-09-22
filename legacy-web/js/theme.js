/**
 * theme.js — Light / Dark / System theme, persisted locally for instant
 * paint on next load, and synced to Firestore (themeSettings/{uid}) so it
 * follows a signed-in user across devices.
 */
import { db, doc, getDoc, setDoc, serverTimestamp } from "./firebase-init.js";

const STORAGE_KEY = "jmc-theme"; // local value only, never used for auth/security

function systemPrefersDark() {
  return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
}

/** Applies {light|dark|system} to <html data-theme>. Call this immediately, before paint. */
export function applyTheme(pref) {
  const resolved = pref === "system" ? (systemPrefersDark() ? "dark" : "light") : pref;
  document.documentElement.setAttribute("data-theme", resolved === "light" ? "light" : "dark");
  document.documentElement.setAttribute("data-theme-pref", pref);
}

/** Reads the locally cached preference synchronously (used for the very first paint). */
export function getLocalThemePref() {
  return localStorage.getItem(STORAGE_KEY) || "dark";
}

export function setLocalThemePref(pref) {
  localStorage.setItem(STORAGE_KEY, pref);
  applyTheme(pref);
}

/** Pulls the saved theme from Firestore (once signed in) and applies + caches it locally. */
export async function syncThemeFromCloud(uid) {
  try {
    const snap = await getDoc(doc(db, "themeSettings", uid));
    if (snap.exists() && snap.data().theme) {
      setLocalThemePref(snap.data().theme);
      return snap.data().theme;
    }
  } catch (e) { console.warn("Theme cloud sync (read) failed:", e); }
  return getLocalThemePref();
}

/** Saves a theme choice to Firestore so other devices pick it up next time they load. */
export async function saveThemeToCloud(uid, pref) {
  setLocalThemePref(pref);
  try {
    await setDoc(doc(db, "themeSettings", uid), { theme: pref, updatedAt: serverTimestamp() }, { merge: true });
  } catch (e) { console.warn("Theme cloud sync (write) failed:", e); }
}

// Live-update if the OS theme changes while pref === 'system'
if (window.matchMedia) {
  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
    if (getLocalThemePref() === "system") applyTheme("system");
  });
}
