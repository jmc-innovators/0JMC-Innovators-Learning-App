# JMC Innovators — Upgrade Setup Guide

This upgrade adds: a mandatory profile + role-selection step after Google
Login, four role-based dashboards, a Settings page, and a full Parent
Control system — all built on your existing `jmc-home2` Firebase project
(the one your Home Page Google Login already uses). `jmc_Classroom.html`
and its separate Firebase project were never opened or touched.

**Read this whole file before going live.** Step 1 below is not optional —
without it, the parental controls are just UI with no lock on the door.

---

## 1. Deploy the security rules (required)

Right now, your Firestore database is (most likely) still running whatever
rules it had before. The new `firestore.rules` and `storage.rules` files in
this project are what actually *enforce* everything — role locking, and
especially: **a child's account can read its parental controls but can
never write them.** That check runs on Firebase's servers, not in the
browser, which is what makes it real instead of cosmetic.

**Option A — Firebase CLI (recommended):**
```bash
npm install -g firebase-tools
firebase login
cd jmc-innovators              # this project folder
firebase deploy --only firestore:rules,storage:rules
```
`.firebaserc` is already set to your `jmc-home2` project, so this deploys
to the correct project automatically — not the Classroom one.

**Option B — Firebase Console (no CLI needed):**
1. Go to console.firebase.google.com → **jmc-home2** project
2. Firestore Database → Rules tab → paste the contents of `firestore.rules` → Publish
3. Storage → Rules tab → paste the contents of `storage.rules` → Publish

Either way, use the **Rules Playground** in the console afterward to sanity
check a couple of cases (e.g. simulate a `get` on `parentControls/{someUid}`
as a random other user and confirm it's denied).

---

## 2. Deploy the site to Netlify

Nothing changes about how you deploy — same Netlify site, same repo/drag-and-drop
workflow. New pages (`dashboard.html`, `settings.html`, `parent-control.html`,
`profile.html`) and new `css/` and `js/` folders just come along for the ride.
`netlify.toml` already has clean-URL redirects added for them (`/dashboard`,
`/settings`, `/parent-control`, `/profile`).

---

## 3. How the login → role → dashboard flow works

1. Person clicks **Login with Google** (unchanged — same button, same Firebase project).
2. On first sign-in, they're sent to `/profile.html` — required fields:
   Full Name, School, Grade *(students only)*, Country, Language, and Role
   (Student / Teacher / Parent / School Operator).
3. That's saved to `users/{uid}` in Firestore, plus a matching shard doc in
   `students/`, `teachers/`, `parents/`, or `operators/`.
4. They land on `/dashboard.html`, which renders completely different
   widgets depending on `role` — same URL, same file, role-driven content.
   Returning users skip straight to their dashboard.
5. **Role is locked after the first save.** Firestore rules reject any
   attempt (by anyone but you, the admin) to change `role` on an existing
   profile. This matters specifically so a monitored student can't just
   relabel themselves "teacher" to shed parental controls.

"Parent Control" only ever appears in the sidebar for `role: 'parent'` — it
isn't hidden with CSS, it's simply never added to the menu for anyone else,
and the page itself (`parent-control.html`) redirects non-parents away.

---

## 4. How parental control is actually enforced

This is the part worth understanding properly rather than taking on faith.

**Linking (parent-initiated, on purpose):** a parent generates a 6-digit
code on `/parent-control.html` (expires in 15 minutes, single-use) and
gives it to their child directly. The child enters it on their own
`/settings.html`. This direction — parent generates, child enters — means a
stranger's account can never attach itself as "parent" over a child it
doesn't already have a code from.

**Approval (second confirmation):** after the child enters the code, the
link sits as `pending` until the parent taps **Approve**. Nothing is
monitored or restricted before that.

**The actual lock:** once active, `parentControls/{childUid}` is the single
source of truth for restrictions (AI Tutor, downloads, chat, videos, study
mode, allowed subjects, daily/weekly limits). Per `firestore.rules`:
- The child can **read** it (so their app knows what's restricted)
- Only the linked, active parent (or you, the admin) can **write** it
- The child's own signed-in session — no matter what the JavaScript in
  their browser is told to do — is rejected by Firestore itself if it ever
  tries to write to that document.

That's the difference between "the UI happens to hide a button" and "the
server refuses the write." You can verify it yourself: sign in as the
student, open the browser console, and try
`setDoc(doc(db,'parentControls','<own-uid>'), {aiEnabled:true})` — it will
throw a permission-denied error even though the button is right there in
the parent's UI.

**Unlinking:** either the parent or the child can dissolve a link (delete
`parentLinks/{childUid}`) in the current rules. If you'd rather only the
parent be able to unlink, remove the `|| isSelf(childUid)` clause from the
`parentLinks` delete rule in `firestore.rules` and redeploy. We shipped it
this way deliberately — a monitoring system a person can never leave has a
different (and heavier) safety profile than this one, and that's a decision
better made by you than defaulted by us.

**PIN protection:** the optional 4-digit PIN on the Parent Control →
Security tab is a *local, shared-device* convenience — e.g. a family tablet
where the parent stays signed in. The real security boundary is still the
Firestore rule above, which checks the parent's actual signed-in Google
account, not the PIN.

---

## 5. Scope: what this can and can't restrict — please read this part

This is a **web app's own parental control system**, in the spirit of
Google Family Link but not the same category of product. Being upfront
about the boundary:

**It can genuinely restrict/monitor**, inside JMC Innovators itself:
- AI Tutor, downloads, chat, video access (checked by
  `parent-controls-enforcer.js` wherever it's wired in — currently the
  dashboard's AI Tutor tile as a working example)
- Study time limits, allowed subjects, Study Mode
- Login history, study session history, resource-open activity — all
  self-logged by the child's own browser and readable only by their
  active parent

**It cannot** restrict anything outside this site — other websites, other
apps, or the device itself. That's what OS-level tools like the real Google
Family Link are for, and they work at a completely different layer (device
management), which a website has no access to.

**Wired into the dashboard now, but not yet into every existing tool
page:** `exampapers.html`, `text_books.html`, `chatbot.html`, etc. weren't
modified (per your instructions to never break existing pages), so they
don't currently check `parentControls` before letting someone in. To
extend real enforcement to one of them, add near the top of that page:

```js
import { getMyControls, isFeatureEnabled } from "/js/parent-controls-enforcer.js";
import { getCurrentUser } from "/js/auth-guard.js";

const user = await getCurrentUser();
if (user) {
  const controls = await getMyControls(user.uid);
  if (!isFeatureEnabled(controls, "downloads")) {
    // e.g. hide the download button, or redirect with a message
  }
}
```

**Honestly scaffolded, not yet populated:** the Monitoring tab's "Downloads
history", "Quiz results", "Assignments" and "Attendance" cards show a
"coming soon" state rather than fabricated numbers, because there's no
quiz/assignment/attendance system built yet to source real data from. The
`securityLogs`, `sessions`, `reports`, and `analytics` collections have
rules in place (per your required collection list) but nothing writes to
them yet — they're ready for whenever those features get built.

---

## 6. Optional: the bonus Cloud Function

Everything above works with **zero Cloud Functions** — Firestore rules
alone are doing the real enforcement, on the free Spark plan. One thing
genuinely can't be done from client code alone: revoking a session on a
device that *isn't* the one you're currently using ("log out everywhere").
`functions/index.js` has:

- `revokeAllSessions` — powers the real "Log out everywhere" button in Settings
- `onUserProfileDeleted` — server-side cleanup backstop for account deletion

Deploying needs the Blaze (pay-as-you-go) plan, but has a generous free
tier that these two low-traffic functions won't come close to:
```bash
cd functions && npm install
firebase deploy --only functions
```
If you skip this, "Log out everywhere" still works — it just signs out the
current device only, with an honest toast message saying so.

---

## 7. Testing checklist

- [ ] Deploy `firestore.rules` and `storage.rules` (Section 1)
- [ ] Sign in fresh (or clear the `users/{uid}` doc in Firestore) → confirm you land on `/profile.html`
- [ ] Complete profile as **Student** → confirm you land on `/dashboard.html` with student widgets
- [ ] In a second browser (or incognito), sign in as a different Google account, choose **Parent**
- [ ] On the parent's `/parent-control.html`, generate a code
- [ ] On the student's `/settings.html` → Parental Controls, enter the code → confirm "waiting for approval"
- [ ] Approve on the parent side → confirm the student's Settings now shows real restriction values
- [ ] Toggle "AI Tutor" off as the parent → confirm the student dashboard's AI Tutor tile shows "paused" (may take a moment; it's a live `onSnapshot`)
- [ ] Try the browser-console write test from Section 4 as the student → confirm it's rejected
- [ ] Settings → Privacy & Security → Export My Data → confirm a JSON file downloads
- [ ] Favicon: hard-refresh (Ctrl/Cmd+Shift+R) — browsers cache favicons aggressively

---

## 8. What's new vs. untouched, at a glance

**New pages:** `profile.html`, `dashboard.html`, `settings.html`, `parent-control.html`
**New folders:** `css/app-shell.css`, `js/*.js` (12 modules), `functions/`
**New config:** `firestore.rules`, `storage.rules`, `firebase.json`, `.firebaserc`, `service-worker.js`
**Edited (surgically):** `index.html` (only the post-login redirect + one new "Go to Dashboard" button in the existing profile modal + service worker registration + favicon binaries), `site.webmanifest` (added shortcuts), `netlify.toml` (added redirects/cache headers)
**Untouched:** every other existing page (`about2.html`, `exampapers.html`, `text_books.html`, `educational_tools.html`, `chatbot.html`, `notebook.html`, `tell_us.html`, `dictionary_.html`, `mathslab.html`, `science.html`, `404.html`) and **`jmc_Classroom.html` — never opened, never referenced, separate Firebase project untouched**, exactly as instructed.
**Replaced (same filenames, drop-in):** `favicon.ico`, `favicon.svg`, `favicon-16x16.png`, `favicon-32x32.png`, `apple-touch-icon.png`, `android-chrome-192x192.png`, `android-chrome-512x512.png` — regenerated from your uploaded logo, so no HTML anywhere needed to change.

Support/feedback links in Settings → About point to `jmc.innovators2027@gmail.com`, which is also the identity used as the Storage/Firestore admin account in the rules — update that one string in `firestore.rules` and `storage.rules` if that address ever changes.
