# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **provider-neutral core** of a "current authenticated user" layer for Jetpack Compose — the Kotlin sibling of the Swift package `rozd/user-kit` (local clone at `~/dev/rozd/user-kit`). It ships interfaces and the `@Stable class User` orchestrator; no concrete auth provider. Dependencies: `compose-runtime` and `kotlinx-coroutines`, nothing else.

Concrete providers live in **separate repositories** (e.g. [`rozdonmobile/user-kit-firebase`](https://github.com/rozdonmobile/user-kit-firebase), package `io.github.rozd.userkit.firebase`, usually checked out at `../user-kit-firebase`). **Never add a provider SDK** (Firebase, Auth0, …) here: a dependency declared in this module reaches every consumer through the POM, so a provider here would drag its SDK into every app that links the core.

## Build & test

- Needs an Android SDK: `local.properties` with `sdk.dir=…` (gitignored) or `ANDROID_HOME`.
- The library **is the root project** (single-module build; sources in `src/`), so tasks have no `:module:` prefix. That layout is deliberate: it is what gives the short JitPack coordinates below.
- Build: `./gradlew assembleRelease`
- Test: `./gradlew testDebugUnitTest` — plain JVM JUnit 4 + `kotlinx-coroutines-test`; no Robolectric, no emulator. Compose snapshot state works on the JVM as-is.
- Single test: `./gradlew testDebugUnitTest --tests 'io.github.rozd.userkit.UserTest'`
- Publish locally: `./gradlew publishToMavenLocal` → `com.github.rozdonmobile:user-kit:<VERSION_NAME>` (coordinates in `gradle.properties`).

## Releasing

Consumers resolve `com.github.rozdonmobile:user-kit:<tag>` from **JitPack** (`https://jitpack.io`, config in `jitpack.yml`), which builds a tag on first request. To release: bump `VERSION_NAME` in `gradle.properties`, commit, tag with **exactly that string** (`git tag 0.2.0 && git push --tags`), then request it once (`https://jitpack.io/#rozdonmobile/user-kit`) and check the build log there. Then bump `userKit` in the adapter's `gradle/libs.versions.toml`.

Toolchain mirrors the reference consumer (`~/dev/fitnessart/fitnessart-android`): AGP 9.3 with **built-in Kotlin** (do not apply `org.jetbrains.kotlin.android`), Gradle 9.5, JDK 17 bytecode, Compose BOM 2026.08.

## Design (do not break)

- **`User.info` is Compose snapshot state; `User.infos` is a `StateFlow`.** Same value, one collector (`publish()`), two shapes. Composables read `info`/`isAdmin`/`isAuthenticated` directly; everything else collects `infos` or `userIds`. Keep them in lockstep — never write one without the other.
- `User.userIds` is `infos.map { it?.id }.distinctUntilChanged()`: a token refresh must **not** restart per-user queries hung off it; a sign-out must.
- `isAdmin` is `role == "admin"`, **case-sensitive**; `null`/empty → not admin. `UserInfo.role` defaults to `null`; adapters override it (e.g. from a JWT custom claim). Role *policy* lives here, the raw string comes from the adapter.
- `AuthenticationCancelledException` is a plain `Exception`, deliberately not a `CancellationException` — the latter reads as coroutine cancellation and gets swallowed.
- `User.service` is `public` because adapters downcast it (`User.firebaseUserService`). `storage`/`synchronizer` stay private.
- `LocalUser` is `staticCompositionLocalOf`: the instance never changes; its snapshot state does the recomposing.
- The Swift core's `singIn()` typo is **not** carried over — this API is `signIn()`.

## Adding a provider adapter

A separate Gradle build that depends on `com.github.rozdonmobile:user-kit` (or `includeBuild`s this checkout while developing), implements `UserService` / `UserStorage` / `UserSynchronizer` and `UserInfo` / `UserSession` / `UserProfile`, and lets the app construct `User(service, storage, synchronizer)`. `src/test/.../Stubs.kt` is the canonical minimal set; `rozdonmobile/user-kit-firebase` is the reference implementation.
