/**
 * parent-link.js
 * ------------------------------------------------------------------
 * The pairing flow between a parent account and a child (student)
 * account. This is deliberately PARENT-INITIATED: the parent
 * generates a short-lived code and shares it with their own child
 * directly (out loud, written down, etc). The child enters it to
 * confirm the pairing. This direction matters for safety — it means
 * a stranger's account can never attach itself as "parent" over a
 * child it doesn't already know, since the child must be handed the
 * code by that exact parent first.
 *
 * A code is single-use and expires in 15 minutes. After a child
 * claims a code, the link sits as "pending" until the parent taps
 * Approve — a second, deliberate confirmation before any monitoring
 * or restriction begins.
 *
 * Every write below is re-checked server-side by firestore.rules —
 * this file is the UX layer, not the security boundary.
 * ------------------------------------------------------------------
 */
import {
  db, doc, setDoc, getDoc, updateDoc, deleteDoc, addDoc,
  collection, query, where, onSnapshot, serverTimestamp, Timestamp
} from "./firebase-init.js";
import { pushNotification } from "./notifications.js";

const CODE_TTL_MINUTES = 15;

function randomCode() {
  return String(Math.floor(100000 + Math.random() * 900000)); // 6 digits, never starts with 0
}

/** Parent action: mint a new pairing code to read aloud / send to their child. */
export async function generateLinkCode(parentUid) {
  const code = randomCode();
  const expiresAt = Timestamp.fromDate(new Date(Date.now() + CODE_TTL_MINUTES * 60 * 1000));
  await setDoc(doc(db, "linkCodes", code), {
    parentUid, createdAt: serverTimestamp(), expiresAt, claimedBy: null,
  });
  return { code, expiresAt, ttlMinutes: CODE_TTL_MINUTES };
}

/**
 * Child action: enter a code given to them by their parent.
 * Returns { ok: true } or { ok: false, reason }.
 */
export async function claimLinkCode(childUid, rawCode) {
  const code = String(rawCode || "").trim();
  if (!/^\d{6}$/.test(code)) return { ok: false, reason: "Enter the 6-digit code exactly as your parent gave it to you." };

  const codeRef = doc(db, "linkCodes", code);
  const codeSnap = await getDoc(codeRef);
  if (!codeSnap.exists()) return { ok: false, reason: "That code wasn't found. Ask your parent for a new one." };
  const codeData = codeSnap.data();

  if (codeData.claimedBy) return { ok: false, reason: "That code has already been used." };
  if (codeData.expiresAt && codeData.expiresAt.toMillis() < Date.now()) {
    return { ok: false, reason: "That code has expired. Ask your parent to generate a new one." };
  }

  try {
    await updateDoc(codeRef, { claimedBy: childUid, claimedAt: serverTimestamp() });
  } catch (e) {
    return { ok: false, reason: "That code was just claimed by someone else — ask your parent for a new one." };
  }

  await setDoc(doc(db, "parentLinks", childUid), {
    parentUid: codeData.parentUid,
    childUid,
    viaCode: code,
    status: "pending",
    linkedAt: serverTimestamp(),
  });

  return { ok: true, parentUid: codeData.parentUid };
}

/** Parent action: live list of every child linked to them, any status. */
export function subscribeMyChildren(parentUid, cb) {
  const q = query(collection(db, "parentLinks"), where("parentUid", "==", parentUid));
  return onSnapshot(q, (snap) => {
    const rows = [];
    snap.forEach(d => rows.push({ childUid: d.id, ...d.data() }));
    cb(rows);
  }, (err) => console.warn("subscribeMyChildren failed:", err));
}

/** Child action: see the status of their own link (or null if not linked). */
export function subscribeMyLink(childUid, cb) {
  return onSnapshot(doc(db, "parentLinks", childUid), (snap) => {
    cb(snap.exists() ? snap.data() : null);
  }, () => cb(null));
}

/** Parent action: approve a pending child, creating a default (permissive) controls doc. */
export async function approveChild(childUid) {
  await updateDoc(doc(db, "parentLinks", childUid), { status: "active", approvedAt: serverTimestamp() });
  const controlsRef = doc(db, "parentControls", childUid);
  const existing = await getDoc(controlsRef);
  if (!existing.exists()) {
    await setDoc(controlsRef, {
      studyModeEnabled: false, aiEnabled: true, downloadsEnabled: true, chatEnabled: true,
      videosEnabled: true, allowedSubjects: ["all"], dailyLimitMinutes: 0, weeklyLimitMinutes: 0,
      monthlyReportEnabled: true, emailNotifications: true, pushNotifications: true,
      emergencyLock: false, updatedAt: serverTimestamp(),
    });
  }
  try {
    await pushNotification(childUid, { category: "parent", title: "Parent linked", body: "A parent has approved and linked your account." });
  } catch { /* non-critical */ }
}

export async function rejectChild(childUid) {
  await updateDoc(doc(db, "parentLinks", childUid), { status: "rejected", rejectedAt: serverTimestamp() });
}

/** Fully unlinks — removes the link document. Controls doc is left in place harmlessly (rules require an active link to read/write it). */
export async function unlinkChild(childUid) {
  await deleteDoc(doc(db, "parentLinks", childUid));
}

export async function updateControls(childUid, patch) {
  await setDoc(doc(db, "parentControls", childUid), { ...patch, updatedAt: serverTimestamp() }, { merge: true });
}

export function subscribeControls(childUid, cb) {
  return onSnapshot(doc(db, "parentControls", childUid), (snap) => cb(snap.exists() ? snap.data() : null));
}
