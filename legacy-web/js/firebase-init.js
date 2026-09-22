/**
 * firebase-init.js
 * ------------------------------------------------------------------
 * SINGLE SOURCE OF TRUTH for Firebase on every new authenticated page
 * (profile.html, dashboard.html, settings.html, parent-control.html).
 *
 * This uses ONLY the Home Page's Firebase project ("jmc-home2") — the
 * exact same apiKey / authDomain / projectId already used by the
 * Google Login button on index.html. It intentionally does NOT touch,
 * import from, or reference jmc_Classroom.html or its separate
 * Firebase project in any way.
 *
 * Every new page imports from here instead of re-declaring the config,
 * so there is exactly one place to update credentials in the future.
 * ------------------------------------------------------------------
 */

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged,
  setPersistence, browserLocalPersistence, browserSessionPersistence,
  indexedDBLocalPersistence, deleteUser, reauthenticateWithPopup
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  getFirestore, initializeFirestore, persistentLocalCache, persistentSingleTabManager,
  doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc, addDoc,
  collection, serverTimestamp, query, where, orderBy, limit, onSnapshot,
  arrayUnion, arrayRemove, increment, deleteField, Timestamp, writeBatch, runTransaction
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import {
  getStorage, ref, uploadBytes, getDownloadURL
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js";
import {
  getFunctions, httpsCallable
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-functions.js";

// EXACT same config as index.html's Google Login (Project A / jmc-home2).
// Do not point this at any second/other Firebase project.
export const firebaseConfig = {
  apiKey: "AIzaSyBqv-Ohj2QjgODRgnuOlIyXhgS82WJ_Ohs",
  authDomain: "jmc-home2.firebaseapp.com",
  projectId: "jmc-home2",
  storageBucket: "jmc-home2.firebasestorage.app",
  messagingSenderId: "129216148625",
  appId: "1:129216148625:web:14c548fd4c756f349ebb30",
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);

// Real Firestore offline persistence (IndexedDB-backed), not just in-memory
// caching. Falls back to the plain in-memory client if IndexedDB isn't
// available (e.g. private browsing) so the app still works either way.
let dbInstance;
try {
  dbInstance = initializeFirestore(app, {
    localCache: persistentLocalCache({ tabManager: persistentSingleTabManager({}) }),
  });
} catch (e) {
  console.warn("Persistent Firestore cache unavailable, using in-memory only:", e);
  dbInstance = getFirestore(app);
}
export const db = dbInstance;
export const storage = getStorage(app);
export const functions = getFunctions(app);
export const provider = new GoogleAuthProvider();

// Same resilient persistence chain used on the home page, so a signed-in
// session survives reloads consistently across every page of the site.
async function initPersistence() {
  try { await setPersistence(auth, indexedDBLocalPersistence); }
  catch {
    try { await setPersistence(auth, browserLocalPersistence); }
    catch {
      try { await setPersistence(auth, browserSessionPersistence); }
      catch (e) { console.warn("All persistence modes failed:", e); }
    }
  }
}
export const persistenceReady = initPersistence();

export {
  GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged,
  deleteUser, reauthenticateWithPopup,
  doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc, addDoc, collection,
  serverTimestamp, query, where, orderBy, limit, onSnapshot,
  arrayUnion, arrayRemove, increment, deleteField, Timestamp, writeBatch, runTransaction,
  ref, uploadBytes, getDownloadURL,
  httpsCallable,
};
