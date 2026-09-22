# JMC Innovators — Learning Platform (Native Android)

A native Kotlin + Jetpack Compose rewrite of the JMC Innovators learning website
(`legacy-web/`). No WebView, no wrapped browser tab — every screen is Compose,
backed by the same two Firebase projects the website already uses.

**Tagline:** Learn. Create. Innovate.
**Package:** `lk.jmcinnovators.learning`

## Quick links

| Doc | Covers |
|---|---|
| [docs/SETUP.md](docs/SETUP.md) | Opening the project, building, running |
| [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md) | Getting both `google-services.json` files, SHA-1, security rules, Cloud Functions |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | MVVM/repository structure, module layout |
| [MIGRATION.md](MIGRATION.md) | Website feature → native screen, per item, with status |
| [docs/TESTING.md](docs/TESTING.md) | What's tested, how to run it, what isn't yet |
| [docs/RELEASE.md](docs/RELEASE.md) | Signing, release build, what's blocked without your credentials |

## Status at a glance

This is a real, running app skeleton — auth, home, classroom join, notes, profile,
notifications and the dictionary tool are wired end-to-end to Firestore. It has
**not been compiled in this environment** (no Android SDK / network access here —
see docs/TESTING.md). Build it in Android Studio and see docs/SETUP.md for the
first-build checklist, since two config files and one SHA-1 registration are
required before it will run.

## Two Firebase projects, on purpose

The website already splits its backend across two Firebase projects, and this app
keeps that split rather than silently merging them:

- **jmc-home2** — accounts, profiles, notes, notifications, parental controls.
- **jmc-class** — schools, classes, assignments, quizzes.

See MIGRATION.md for why, and what it would take to merge them later.
