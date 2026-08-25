# UserKit for Compose

**A provider-neutral "current user" layer for Jetpack Compose.**

The Kotlin sibling of [`rozd/user-kit`](https://github.com/rozd/user-kit) (Swift / SwiftUI). One observable `User` you provide once and read anywhere; it owns the session *as seen from the client* and stays out of the identity business — a real auth backend plugs in behind it through a small adapter (Firebase, your own API, …).

- 🧩 **Provider-neutral** — the core knows nothing about Firebase or your backend. Swap adapters without touching app code.
- 👀 **Compose-native** — `user.info`, `user.isAuthenticated`, `user.isAdmin` are snapshot state: read them in a composable and it recomposes on sign-in, sign-out and token refresh. No `collectAsState` at every call site.
- 🌊 **Flow for everything else** — `user.infos: StateFlow<UserInfo?>` and `user.userIds: Flow<UserId?>` for repositories, view models, a Ktor auth plugin.
- 🪶 **Tiny** — depends on `compose-runtime` and `kotlinx-coroutines`, nothing else.

```kotlin
val user = LocalUser.current

if (user.isAuthenticated) {
    Text(user.info?.profile?.displayName ?: "Signed in")
    if (user.isAdmin) AdminPanelLink()
}
```

## Installation

Served by [JitPack](https://jitpack.io/#rozdonmobile/user-kit) straight from this repository's tags — the Android counterpart of adding a Swift package by its GitHub URL.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") { content { includeGroup("com.github.rozdonmobile") } }
    }
}
```

```kotlin
// app/build.gradle.kts
implementation("com.github.rozdonmobile:user-kit:0.1.0")
```

You will usually also add a **provider adapter**, e.g. [`user-kit-firebase`](https://github.com/rozdonmobile/user-kit-firebase) — it depends on this artifact, so one line brings both. Adapters are separate repositories so an app that doesn't use a provider never links its SDK.

**Developing against a local checkout** — `includeBuild("../user-kit")` in the consumer's `settings.gradle.kts` (or `./gradlew --include-build ../user-kit …`) substitutes this working tree for the published coordinates, the way a local SPM override does.

## Quick start

**1. Build one `User` from an adapter and provide it.** Only this file imports the adapter; the rest of the app imports just `io.github.rozd.userkit`.

```kotlin
import io.github.rozd.userkit.LocalUser
import io.github.rozd.userkit.User
import io.github.rozd.userkit.firebase.*   // your chosen adapter

class MyApplication : Application() {
    lateinit var user: User
    override fun onCreate() {
        super.onCreate()
        user = User(
            service = FirebaseUserService(this, FirebaseUserServiceConfiguration(
                authDomain = "auth.example.com",
                packageName = packageName,
            )),
            storage = FirebaseUserStorage(),
            synchronizer = FirebaseUserSynchronizer(),
        )
    }
}

// MainActivity
setContent {
    CompositionLocalProvider(LocalUser provides (application as MyApplication).user) {
        AppRoot()
        FirebaseAuthHost()   // hosts the sign-in sheet; from the adapter
    }
}
```

(With Koin or Hilt, register the `User` as a singleton there and `provide` it from the same place.)

**2. Read the current user anywhere.**

```kotlin
@Composable
fun ProfileScreen() {
    val user = LocalUser.current
    val scope = rememberCoroutineScope()

    if (user.isAuthenticated) {
        Text(user.info?.profile?.displayName ?: "Signed in")
        if (user.isAdmin) AdminPanelLink()                 // gate admin-only UI
        Button(onClick = { scope.launch { user.signOut() } }) { Text("Sign out") }
    } else {
        Button(onClick = { user.authenticate() }) { Text("Sign in") }
    }
}
```

**3. Outside composition, use the flows.**

```kotlin
// Scope a Firestore query to the signed-in user; restarts on sign-in/out, not on token refresh.
val bookings: Flow<List<Booking>> = user.userIds.flatMapLatest { id ->
    if (id == null) flowOf(emptyList()) else bookingsOf(id)
}

// Attach the bearer token in a Ktor client.
val token = user.info?.session?.accessToken()
```

## What you get

Everything below is on `User`:

| Member | What it does |
| --- | --- |
| `info` | Current user snapshot (`id`, `session`, `profile`, `role`), or `null` when signed out. **Snapshot state.** |
| `isAuthenticated` | Whether there's a valid session. Snapshot state. |
| `isAdmin` | Convenience for `role == "admin"`. Snapshot state. |
| `infos` | `StateFlow<UserInfo?>` of the same value, for non-Compose code. |
| `userIds` | `Flow<UserId?>`, de-duplicated — the stream to `flatMapLatest` per-user queries off. |
| `signIn()` / `signOut()` | Suspend; sign in / out via the provider. |
| `authenticate()` | Present the provider's sign-in UI. |
| `ensureAuthenticated()` | Present it only if needed; returns whether already authenticated. |
| `withAuthentication { … }` | Run work with a guaranteed session, presenting sign-in first. Throws `AuthenticationCancelledException` if the user backs out. |
| `close()` | Stop syncing (tests, previews). |

`UserInfo` exposes `id: UserId`, `session: UserSession`, `profile: UserProfile`, `role: String?`, and the extension `isAdmin`.
`UserSession` exposes `isAuthenticated`, `refreshToken: String?`, `suspend fun accessToken(): String?`.
`UserProfile` exposes `displayName: String?` and the extension `initials`.

## Why not just `StateFlow` + `collectAsStateWithLifecycle()`?

Compose has no user or auth abstraction of its own; what it has are building blocks, and this library is those blocks assembled the way SwiftUI's `@Environment(User.self)` + `@Observable` assembles them:

| SwiftUI (`rozd/user-kit`) | Compose (this library) |
| --- | --- |
| `@Environment(User.self)` | `LocalUser` (`staticCompositionLocalOf`) |
| `@Observable final class User` | `@Stable class User` with `mutableStateOf` |
| `user.infos: AsyncSequence` | `user.infos: StateFlow` + `user.userIds: Flow` |
| `accessToken: String? { get async }` | `suspend fun accessToken(): String?` |
| `AsyncStream` over an SDK listener | `callbackFlow { … awaitClose { … } }` |
| `throw CancellationError()` | `throw AuthenticationCancelledException()` (a `CancellationException` would read as coroutine cancellation and be swallowed) |

A bare `StateFlow<FirebaseUser?>` in a view model works, but it puts `collectAsStateWithLifecycle()` at every call site, ties the app to one provider, and leaves `isAdmin` to be reinvented per screen. Holding the value as snapshot state *and* as a `StateFlow` gives each consumer its native shape, fed by one collector so they never disagree.

## Bring your own provider

An adapter is a separate build that depends on this one and implements six interfaces:

- **Behaviour:** `UserService` (sign in/out, verify email, present sign-in UI, `withAuthentication`), `UserStorage` (seed the last-known user), `UserSynchronizer` (auth state as a `Flow`).
- **Data:** `UserInfo`, `UserSession`, `UserProfile`.

Then the app builds `User(service, storage, synchronizer)`. See [`user-kit-firebase`](https://github.com/rozdonmobile/user-kit-firebase) for the reference implementation, and `src/test/.../Stubs.kt` for the smallest possible conformer set.

## Requirements

- Kotlin 2.4, AGP 9.3, Compose BOM 2026.08 (runtime only), minSdk 26
- Build & test: `./gradlew testDebugUnitTest assembleRelease` (needs `local.properties` → `sdk.dir`, or `ANDROID_HOME`)
- Release: bump `VERSION_NAME` in `gradle.properties`, push a tag with the same name — JitPack builds it on first request (`jitpack.yml`)

## License

[MIT](LICENSE) © Max Rozdobudko
