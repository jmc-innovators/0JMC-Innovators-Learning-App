# ARCHITECTURE.md

MVVM + repository, no DI framework (the supplied project had none, so
`ViewModelFactory.kt` wires dependencies by hand rather than adding Hilt as an
extra thing to learn/debug).

```
ui/screens/*        Compose screens. Stateless where practical; read state via
                     collectAsState() from a ViewModel.
viewmodel/           One StateFlow<UiState> per screen (or a small sealed class
                     for multi-state flows like login/join-classroom). Talks
                     only to repositories, never to FirebaseFirestore directly.
data/repository/    One class per Firestore area (UserRepository, NotesRepository,
                     ClassroomRepository, ...). Returns Flow for live data,
                     suspend fun for one-shot reads/writes. This is the only
                     layer that imports com.google.firebase.*.
data/firebase/      Two objects: FirebaseRefs (default app = jmc-home2) and
                     ClassroomFirebaseRefs (secondary app = jmc-class, built by
                     hand-parsing res/raw/jmc_class_services.json since the
                     Google Services Gradle plugin only wires up the default app
                     automatically).
data/model/          Plain data classes matching Firestore documents. Field
                     names were taken from legacy-web/js/*.js, not invented.
data/local/          UserPreferences — DataStore for onboarding-done + theme,
                     the only local persistence in the app besides Firestore's
                     own offline cache.
navigation/          Routes (string constants) + JmcNavGraph (NavHost + bottom
                     bar). One NavHost; bottom nav shows only on the five
                     top-level routes (Routes.BOTTOM_NAV_ROUTES).
ui/theme/            Material 3 ColorScheme built from legacy-web/css/app-shell.css's
                     actual --bg-0/--blue/--gold/etc. custom properties.
ui/components/       Shared ShimmerBlock / FullScreenLoading / ErrorState /
                     EmptyState / OfflineBanner used across screens so every
                     Firestore-backed screen handles loading/error/empty the
                     same way (reqs. 21/22/26).
notifications/       JmcMessagingService (FCM) — writes nothing itself; the
                     source of truth for the in-app notification center stays
                     notifications/{uid}/items, matching legacy-web.
```

## Why a manually-built secondary FirebaseApp

The Google Services Gradle plugin (`com.google.gms.google-services`) only reads
`app/google-services.json` and only configures the **default** FirebaseApp. It
has no supported way to wire up a second Firebase project from a second
`google-services.json` in the same module. `ClassroomFirebaseRefs.kt` works
around this by shipping the second file as a raw resource and building a
`FirebaseOptions` object from it at runtime with `FirebaseApp.initializeApp(context, options, "jmc-class")`.
Every classroom repository call goes through `Firebase.auth(app)` /
`Firebase.firestore(app)` with that named app, never the default one.

## State flow shape

Every screen with live Firestore data follows the same pattern:

```
Repository.observeX(): Flow<T>
  -> ViewModel: StateFlow<T> = repo.observeX().stateIn(viewModelScope, WhileSubscribed(5000), default)
  -> Screen: val x by viewModel.x.collectAsState()
```

`WhileSubscribed(5000)` keeps the Firestore listener attached for 5s after the
screen leaves composition, so rotating the device or briefly navigating away
doesn't tear down and re-attach the snapshot listener.

## What's intentionally NOT abstracted

There's no repository-interface + fake-implementation pair for tests, because
no ViewModel currently has a unit test that needs one (see TESTING.md) — adding
that abstraction without a test that exercises it would be speculative. If you
add ViewModel tests, extract interfaces at that point rather than upfront.
