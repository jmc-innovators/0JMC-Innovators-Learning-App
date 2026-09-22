/**
 * parent-controls-enforcer.js
 * ------------------------------------------------------------------
 * STUDENT-SIDE half of the parental control system. A child's own
 * browser reads (never writes) parentControls/{uid} and uses it to
 * gate what's shown/allowed, and self-logs its own activity so a
 * parent can see it.
 *
 * The actual security boundary is enforced by Firestore rules
 * (see firestore.rules): only the linked, *active* parent can write
 * parentControls/{childUid}. This module is the UX layer on top of
 * that real, server-enforced boundary — see SETUP_GUIDE.md for the
 * full explanation of what is and isn't guaranteed.
 * ------------------------------------------------------------------
 */
import {
  db, doc, getDoc, onSnapshot, setDoc, addDoc, collection, serverTimestamp, Timestamp
} from "./firebase-init.js";
import { describeDevice } from "./ui-utils.js";

const DEFAULT_CONTROLS = {
  linked: false,
  studyModeEnabled: false,
  aiEnabled: true,
  downloadsEnabled: true,
  chatEnabled: true,
  videosEnabled: true,
  allowedSubjects: ["all"],
  dailyLimitMinutes: 0,   // 0 = no limit
  weeklyLimitMinutes: 0,
  monthlyReportEnabled: true,
  emailNotifications: true,
  pushNotifications: true,
  emergencyLock: false,
};

export async function getMyControls(uid) {
  try {
    const snap = await getDoc(doc(db, "parentControls", uid));
    return snap.exists() ? { ...DEFAULT_CONTROLS, ...snap.data(), linked: true } : { ...DEFAULT_CONTROLS };
  } catch {
    return { ...DEFAULT_CONTROLS };
  }
}

/** Live subscription — so a control a parent flips takes effect without the child needing to reload. */
export function subscribeMyControls(uid, cb) {
  return onSnapshot(doc(db, "parentControls", uid), (snap) => {
    cb(snap.exists() ? { ...DEFAULT_CONTROLS, ...snap.data(), linked: true } : { ...DEFAULT_CONTROLS });
  }, () => cb({ ...DEFAULT_CONTROLS }));
}

export function isFeatureEnabled(controls, feature) {
  if (controls.emergencyLock) return false;
  const map = { ai: "aiEnabled", downloads: "downloadsEnabled", chat: "chatEnabled", videos: "videosEnabled" };
  const key = map[feature];
  return key ? controls[key] !== false : true;
}

export function isSubjectAllowed(controls, subject) {
  if (!controls.allowedSubjects || controls.allowedSubjects.includes("all")) return true;
  return controls.allowedSubjects.includes(subject);
}

/** Compares today's already-logged minutes against the parent's daily limit. */
export function checkDailyLimit(controls, minutesUsedToday) {
  if (!controls.dailyLimitMinutes || controls.dailyLimitMinutes <= 0) {
    return { limited: false, remaining: Infinity };
  }
  const remaining = Math.max(0, controls.dailyLimitMinutes - minutesUsedToday);
  return { limited: remaining <= 0, remaining };
}

/** Applies a "Study Mode" banner + disables non-allowed nav items in a rendered page, if active. */
export function applyStudyModeToDom(controls) {
  document.documentElement.classList.toggle("study-mode-active", !!controls.studyModeEnabled || !!controls.emergencyLock);
}

/* -------- Self-logging (client writes only to its own uid path; see rules) -------- */

export async function logLoginEvent(uid) {
  try {
    await addDoc(collection(db, "loginHistory", uid, "entries"), {
      at: serverTimestamp(),
      device: describeDevice().label,
      userAgent: describeDevice().userAgent,
    });
  } catch (e) { console.warn("logLoginEvent failed:", e); }
}

export async function logStudyMinutes(uid, minutes, subject = "General") {
  if (!minutes || minutes <= 0) return;
  try {
    await addDoc(collection(db, "studyHistory", uid, "entries"), {
      at: serverTimestamp(), minutes, subject,
    });
  } catch (e) { console.warn("logStudyMinutes failed:", e); }
}

export async function logActivity(uid, type, detail = "") {
  try {
    await addDoc(collection(db, "activityLogs", uid, "entries"), {
      at: serverTimestamp(), type, detail,
    });
  } catch (e) { console.warn("logActivity failed:", e); }
}

/** Sums today's logged study minutes (client-side aggregation over the day's entries). */
export async function getTodayMinutes(uid) {
  const { getDocs, query, where } = await import("./firebase-init.js");
  const startOfDay = new Date(); startOfDay.setHours(0, 0, 0, 0);
  try {
    const q = query(
      collection(db, "studyHistory", uid, "entries"),
      where("at", ">=", Timestamp.fromDate(startOfDay))
    );
    const snap = await getDocs(q);
    let total = 0;
    snap.forEach(d => total += (d.data().minutes || 0));
    return total;
  } catch { return 0; }
}
