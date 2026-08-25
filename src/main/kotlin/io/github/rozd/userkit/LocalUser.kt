package io.github.rozd.userkit

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The composition's [User] — the counterpart of SwiftUI's `@Environment(User.self)`.
 *
 * `static` because the *instance* never changes for the life of the app; what
 * changes is its snapshot state, which recomposes readers on its own.
 *
 * ```
 * CompositionLocalProvider(LocalUser provides user) { AppRoot() }
 * …
 * val user = LocalUser.current
 * if (user.isAdmin) AdminButton()
 * ```
 */
val LocalUser = staticCompositionLocalOf<User> {
    error("No User provided. Wrap the app in CompositionLocalProvider(LocalUser provides user).")
}
