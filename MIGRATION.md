# MIGRATION.md — legacy-web feature → native screen

Source of truth for "what existed on the website and what replaced it." Every row
was verified against the actual code in `legacy-web/`, not assumed from the page
name.

## Firebase projects found

| Project | Used for | Registered in this repo as |
|---|---|---|
| `jmc-home2` | Google login, `users/{uid}` profile, notes, notifications, parent controls, settings | `app/google-services.json` (default FirebaseApp) |
| `jmc-class` | `legacy-web/jmc_Classroom.html` — schools, supervisors, teachers, invites | `app/src/main/res/raw/jmc_class_services.json` (secondary FirebaseApp, see `ClassroomFirebaseRefs.kt`) |

They are kept separate rather than merged. Merging is possible (see "Open
decision" below) but wasn't done without your sign-off, since it means migrating
every jmc-class account into jmc-home2's `users/{uid}` shape.

## Feature-by-feature

| Website feature | Source file | Native replacement | Firebase dependency | Status |
|---|---|---|---|---|
| Google Sign-In | `js/firebase-init.js` | `LoginScreen.kt` + `AuthRepository.kt` (Credential Manager) | jmc-home2 Auth | Done — **needs your SHA-1 registered**, see FIREBASE_SETUP.md |
| Profile creation (role/grade/language) | `profile.html`, `js/profile.page.js` | `ProfileSetupScreen.kt` | `users/{uid}` | Done, same document shape |
| Home dashboard | `dashboard.html` | `HomeScreen.kt` | `users/{uid}`, `notifications/{uid}/items` | Done. "Today's Learning" / "Progress" show an honest empty state — no Firestore-backed lesson/progress data exists yet on either project |
| Notifications | `js/notifications.js` | `NotificationsScreen.kt` + `JmcMessagingService.kt` | `notifications/{uid}/items`, FCM | Done |
| Dark/Light/System theme | `js/theme.js`, `themeSettings/{uid}` | `ProfileScreen.kt` theme selector + `JmcTheme` | DataStore locally; **not yet synced to `themeSettings/{uid}`** so it doesn't follow the user across devices the way the website's does | Partial — see "Remaining work" |
| Parental controls enforcement | `js/parent-controls-enforcer.js` | `ParentControlsRepository.kt` (model + read path only) | `parentControls/{uid}` | Partial — repository reads the same document and field names; **no screen enforces `studyModeEnabled`/`emergencyLock`/etc. in the UI yet** |
| Note Taker | *(none — did not exist on the website)* | `NotesScreen.kt`, `NoteEditorScreen.kt` | New `notes/{noteId}` collection + rule | Done — this is new functionality, not a migration |
| Dictionary | `dictionary_.html` | `DictionaryScreen.kt` | None (calls `api.dictionaryapi.dev` directly, same as the website) | Done |
| Periodic table / exam papers / textbooks | `periodic-table.html`, `exam-papers.html`, `text-books.html` | *(not yet built)* | `resources/` in Storage (per `storage.rules`) | **Not started** |
| Maths Lab | `maths.html` (assumed name) | `PendingToolScreen.kt` | — | Marked pending on purpose — see below |
| Science World | `science.html` (assumed name) | `PendingToolScreen.kt` | — | Marked pending on purpose — see below |
| AI Note Assistant | `notebook.html` (user supplies their own API key client-side) | *(not yet built)* | Needs a Cloud Function proxy — **no key may ship in the app** | **Not started** — see "Open decision" |
| Classroom: join by code | `jmc_Classroom.html` | `ClassroomScreen.kt` join dialog + `ClassroomRepository.joinByCode` | jmc-class `classes/{id}` (query by `joinCode`) + `classes/{id}/students/{uid}` (write) | Done, real Firestore validation — "Classroom code not found" is a real result, not a placeholder |
| Classroom: classes/assignments/quizzes storage | `jmc_Classroom.html` (kept in browser **localStorage**, confirmed in source comments) | `ClassroomRepository.kt`, `ClassroomDetailScreen.kt`, `Classroom.kt` models | jmc-class Firestore (`classes/{id}/assignments`, `/announcements`, `/quizzes/.../questions`) | **The native app is itself the migration off localStorage.** Reading works against real Firestore paths; there is no data there yet because the website never wrote any — a teacher-side "create class/assignment/quiz" UI still needs to be built so real documents exist to read |
| Quiz grading (correct answers hidden until submit) | *(new requirement — the website's quiz UI wasn't inspected in detail)* | `firebase/classroom/functions/index.js` → `gradeQuizAttempt` | jmc-class Firestore + Functions | Backend done: `firestore.rules` denies direct `quizAttempts` writes and denies reading `questions/{id}` to students, so grading **must** go through this function. Quiz-taking UI not yet built |
| Teacher invite → role grant | *(inferred from `invites` collection referenced in `jmc_Classroom.html`)* | `firebase/classroom/functions/index.js` → `acceptTeacherInvite` | jmc-class Firestore + Functions | Backend done; no invite-redeem screen yet |
| Session revoke ("log out everywhere") | `settings.html` → `revokeAllSessions` | *(function preserved, not yet called from a native screen)* | jmc-home2 Functions | Function copied byte-for-byte into `functions/index.js`; wire up from `ProfileScreen.kt`'s settings when built |
| Profile-delete cleanup | `functions/index.js` → `onUserProfileDeleted` | *(preserved as-is)* | jmc-home2 Functions | Done, unchanged |
| Monthly parent report | `functions/index.js` → `buildMonthlyReports` (stub, no mailer wired) | *(preserved as-is, still commented out)* | jmc-home2 Functions | Unchanged — was already an intentional stub on the website |

## Security changes made during migration

These are **not** 1:1 ports — they tighten what the website allowed:

1. **`firestore.rules` (jmc-home2):** the website let a user self-assign role
   `teacher` or `operator` at profile creation (`allow create` listed all four
   roles). The native app's copy restricts self-assignment to `student` and
   `parent` only — `teacher`/`operator` must be granted by an admin or a Cloud
   Function. See the `CHANGED from legacy-web` comment in `firebase/firestore.rules`.
2. **Hardcoded secrets found in the website source** (not present in this app):
   `jmc_Classroom.html` has an `OP_KEY` constant and `notebook.html` has an
   `ADMIN_PASS` constant, both readable by anyone who views page source. **Rotate
   both** — they're a pre-existing exposure, not something this migration
   introduced. The extracted Android keystore (`jmc_release.jks`) found in the
   original ZIP had its password in plaintext in `gradle.properties` — also
   rotate that, or reset the Play upload key via Play App Signing if it's already
   live.
3. **`quizAttempts` and `questions/{id}`** are locked down (see the classroom
   rules table entry above) so grading can only happen server-side — this is new
   protection the website's localStorage-based quizzes never had, since there was
   no server boundary at all.

## Open decision: merge the two Firebase projects?

Not done without your input (see the September chat where this was flagged).
Recommendation was to migrate `jmc-class` into `jmc-home2` for a single login and
config file; the alternative (kept for now) is two separate Firebase apps
registered in this one Android project, each with its own `google-services.json`.

## Remaining work

- Teacher-side screens: create class, create assignment, create quiz, post
  announcement, view/grade submissions.
- Student-side: assignment submission upload (Storage), quiz-taking UI calling
  `gradeQuizAttempt`.
- Sync theme preference to `themeSettings/{uid}` (currently device-local only via
  DataStore).
- Build the parental-control **enforcement** UI (the repository/model already
  mirror the website's fields).
- AI Note Assistant: needs a Cloud Function proxy for whatever AI provider you
  choose, so no API key ships in the APK. Not started.
- Search (req. 16): not yet built.
- Periodic table / exam papers / textbooks tools: not yet built.
- Global unit/instrumented test coverage beyond what's listed in
  `docs/TESTING.md`.
