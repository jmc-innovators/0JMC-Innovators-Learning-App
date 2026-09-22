/**
 * firebase/classroom/functions/index.js
 * ------------------------------------------------------------------
 * Cloud Functions for the "jmc-class" project. Deploy separately from
 * the jmc-home2 functions in /functions at the repo root:
 *   cd firebase/classroom/functions && npm install
 *   firebase deploy --only functions --project jmc-class
 *
 * Why this needs a function at all: firestore.rules (../firestore.rules)
 * denies students direct write access to quizAttempts, and the
 * QuizQuestion.correctIndex field is never readable by a student's
 * client (see the questions/{questionId} rule) — so grading has to
 * happen here, server-side, where the answer key can safely be read.
 * ------------------------------------------------------------------
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Grades a quiz attempt and is the ONLY way a quizAttempts/{id} document is
 * created (see firestore.rules: `match /quizAttempts/{attemptId} { allow write: if false; }`).
 *
 * request.data: { classId, quizId, answers: { [questionId]: selectedIndex }, durationSeconds }
 */
exports.gradeQuizAttempt = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in first.");
  const { classId, quizId, answers, durationSeconds } = request.data || {};
  if (!classId || !quizId || typeof answers !== "object") {
    throw new HttpsError("invalid-argument", "classId, quizId and answers are required.");
  }

  // A student must actually be a member of the class to submit an attempt.
  const membership = await db.doc(`classes/${classId}/students/${request.auth.uid}`).get();
  if (!membership.exists) {
    throw new HttpsError("permission-denied", "You are not a member of this classroom.");
  }

  const questionsSnap = await db.collection(`classes/${classId}/quizzes/${quizId}/questions`).get();
  if (questionsSnap.empty) {
    throw new HttpsError("failed-precondition", "This quiz has no questions.");
  }

  let correct = 0;
  questionsSnap.forEach((doc) => {
    const q = doc.data();
    if (answers[doc.id] === q.correctIndex) correct += 1;
  });
  const total = questionsSnap.size;
  const percentage = total > 0 ? Math.round((correct / total) * 10000) / 100 : 0;

  const attemptRef = db.collection("quizAttempts").doc();
  const attempt = {
    quizId,
    classId,
    studentUid: request.auth.uid,
    answers,
    score: correct,
    percentage,
    durationSeconds: Number(durationSeconds) || 0,
    submittedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  await attemptRef.set(attempt);

  // Score only — never the answer key — goes back to the student's client.
  return { attemptId: attemptRef.id, score: correct, total, percentage };
});

/**
 * Validates and redeems a teacher invite (see firestore.rules: invites/{inviteId} is
 * create-only from the client; only this function may mark one as consumed and grant
 * the teacher role, so a student can never self-promote by writing teachers/{uid} directly).
 */
exports.acceptTeacherInvite = onCall(async (request) => {
  if (!request.auth?.token?.email) throw new HttpsError("unauthenticated", "Sign in first.");
  const { inviteId } = request.data || {};
  if (!inviteId) throw new HttpsError("invalid-argument", "inviteId is required.");

  const inviteRef = db.collection("invites").doc(inviteId);
  return db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists) throw new HttpsError("not-found", "Invite not found.");
    const data = invite.data();
    if (data.consumedAt) throw new HttpsError("failed-precondition", "Invite already used.");
    if (data.email !== request.auth.token.email) {
      throw new HttpsError("permission-denied", "This invite was issued to a different email.");
    }
    tx.set(db.collection("teachers").doc(request.auth.uid), {
      schoolId: data.schoolId,
      email: data.email,
      joinedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    tx.update(inviteRef, { consumedAt: admin.firestore.FieldValue.serverTimestamp(), consumedBy: request.auth.uid });
    return { ok: true, schoolId: data.schoolId };
  });
});
