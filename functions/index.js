/**
 * functions/index.js
 * ------------------------------------------------------------------
 * OPTIONAL bonus functions. Nothing else in the site depends on these
 * being deployed — settings.html calls revokeAllSessions and falls
 * back gracefully to a local-only sign-out if it isn't deployed.
 *
 * Why these need a Cloud Function at all: everything else in this
 * project (parental controls, role locking, linking) is enforced by
 * Firestore Security Rules alone, which is real server-side
 * enforcement and needs no functions or Blaze plan. "Sign out every
 * device", specifically, is the one thing that genuinely requires
 * the Admin SDK (revokeRefreshTokens) — the client SDK can only ever
 * sign out the device it's running on.
 *
 * Deploy with (requires the Blaze pay-as-you-go plan):
 *   cd functions && npm install
 *   firebase deploy --only functions
 * ------------------------------------------------------------------
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Callable from settings.html's "Log out everywhere" button.
 * Revokes every refresh token for the CALLER's own account only —
 * a user can never revoke someone else's sessions with this.
 */
exports.revokeAllSessions = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in first.");
  await admin.auth().revokeRefreshTokens(request.auth.uid);
  return { ok: true, revokedAt: Date.now() };
});

/**
 * Cleanup when a users/{uid} profile document is deleted (e.g. via the
 * Firebase console, or a future admin tool) — removes the parallel
 * role-shard doc, settings, and any parent link so nothing orphaned is
 * left behind. The client-side "Delete account" flow in settings.html
 * already best-effort cleans up the main doc itself; this is the
 * server-side backstop.
 */
exports.onUserProfileDeleted = onDocumentDeleted("users/{uid}", async (event) => {
  const uid = event.params.uid;
  const role = event.data?.data()?.role;
  const batch = db.batch();
  const shardByRole = { student: "students", teacher: "teachers", parent: "parents", operator: "operators" };
  if (role && shardByRole[role]) batch.delete(db.collection(shardByRole[role]).doc(uid));
  batch.delete(db.collection("userSettings").doc(uid));
  batch.delete(db.collection("themeSettings").doc(uid));
  batch.delete(db.collection("parentLinks").doc(uid)); // if this uid was a child
  await batch.commit();
});

/**
 * Monthly parent report emailer — STUB. Runs on the 1st of each month
 * and finds parents with monthlyReportEnabled === true, but does not
 * send email yet: plug in your provider of choice (SendGrid, Resend,
 * Nodemailer + Gmail, etc.) where marked below. Left disabled by
 * default (see exports at the bottom) so an unfinished mailer never
 * silently fails in production.
 */
async function buildMonthlyReports() {
  const controlsSnap = await db.collection("parentControls").where("monthlyReportEnabled", "==", true).get();
  const results = [];
  for (const doc of controlsSnap.docs) {
    const childUid = doc.id;
    const linkSnap = await db.collection("parentLinks").doc(childUid).get();
    if (!linkSnap.exists || linkSnap.data().status !== "active") continue;
    const since = admin.firestore.Timestamp.fromMillis(Date.now() - 30 * 24 * 60 * 60 * 1000);
    const studySnap = await db.collection("studyHistory").doc(childUid).collection("entries").where("at", ">=", since).get();
    let minutes = 0;
    studySnap.forEach((d) => { minutes += d.data().minutes || 0; });
    results.push({ childUid, parentUid: linkSnap.data().parentUid, minutes });
    // TODO: send an email to the parent's address here using `minutes`.
  }
  return results;
}

// Left commented out until an email provider is wired into
// buildMonthlyReports() above — an unfinished mailer should never
// silently "run" and do nothing every month in production.
//
// exports.monthlyReportJob = onSchedule("0 6 1 * *", async () => {
//   const results = await buildMonthlyReports();
//   console.log(`Monthly report computed for ${results.length} linked children.`);
// });
