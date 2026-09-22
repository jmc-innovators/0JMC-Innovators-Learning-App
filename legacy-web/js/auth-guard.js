/**
 * auth-guard.js
 * ------------------------------------------------------------------
 * The routing brain of the login system:
 *   Google Login → (profile incomplete?) → profile.html → dashboard.html
 *
 * Used by every protected page. Each page calls one guard function at
 * the top of its own script and gets back a signed-in user + Firestore
 * profile, or is redirected somewhere sensible automatically.
 * ------------------------------------------------------------------
 */

import { auth, db, doc, getDoc, onAuthStateChanged, persistenceReady } from "./firebase-init.js";

const ROLES = ["student", "teacher", "parent", "operator"];

/** Resolves once with the current Firebase user (or null), after auth state settles. */
export function getCurrentUser() {
  return persistenceReady.then(() => new Promise((resolve) => {
    const unsub = onAuthStateChanged(auth, (user) => { unsub(); resolve(user); });
  }));
}

/** Reads users/{uid} from Firestore. Returns null if it doesn't exist yet. */
export async function fetchProfile(uid) {
  const snap = await getDoc(doc(db, "users", uid));
  return snap.exists() ? snap.data() : null;
}

function isProfileComplete(profile) {
  return !!profile
    && profile.profileComplete === true
    && !!profile.fullName
    && !!profile.role
    && ROLES.includes(profile.role);
}

/**
 * Call this from index.html right after a successful sign-in transition.
 * Sends a brand-new or incomplete user to profile.html, an existing user
 * straight to their dashboard.
 */
export async function routeAfterLogin(uid) {
  const profile = await fetchProfile(uid);
  if (isProfileComplete(profile)) {
    window.location.href = "/dashboard.html";
  } else {
    window.location.href = "/profile.html?required=1";
  }
}

/**
 * Use at the top of any protected page (dashboard, settings, parent-control).
 * - Not signed in            -> back to the home page to log in
 * - Signed in, no profile    -> profile.html (mandatory step)
 * - Signed in, complete      -> resolves with { user, profile }
 */
export async function requireCompleteProfile() {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = "/index.html";
    return null;
  }
  const profile = await fetchProfile(user.uid);
  if (!isProfileComplete(profile)) {
    window.location.href = "/profile.html?required=1";
    return null;
  }
  return { user, profile };
}

/**
 * Use on role-restricted pages, e.g. parent-control.html:
 *   const ctx = await requireRole(['parent']);
 * Redirects non-matching roles back to their own dashboard rather than
 * showing an error, since landing on the wrong page is usually just a
 * stale bookmark or a shared link, not malicious intent.
 */
export async function requireRole(allowedRoles) {
  const ctx = await requireCompleteProfile();
  if (!ctx) return null;
  if (!allowedRoles.includes(ctx.profile.role)) {
    window.location.href = "/dashboard.html";
    return null;
  }
  return ctx;
}

export { ROLES };
