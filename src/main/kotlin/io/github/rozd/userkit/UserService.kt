package io.github.rozd.userkit

import kotlinx.coroutines.flow.Flow

/**
 * The behaviour seam: everything the app *does* with authentication.
 *
 * Presentation is the provider's business — [authenticate] asks the provider to
 * show its sign-in UI, and how that UI is hosted is the adapter's to define
 * (see `FirebaseAuthHost` in the Firebase adapter).
 */
interface UserService {

    /** Whether the current user's email is verified; `false` when signed out. */
    val isEmailVerified: Flow<Boolean>

    suspend fun signIn()
    suspend fun signOut()
    suspend fun sendVerificationEmail()

    /** Present the provider's sign-in UI. Fire-and-forget. */
    fun authenticate()

    /** Present the sign-in UI only if needed; returns whether the user is already authenticated. */
    fun authenticateIfNeeded(): Boolean

    /**
     * Run [operation] with a guaranteed session, presenting sign-in first if there is none.
     *
     * @throws AuthenticationCancelledException when the user dismisses sign-in without
     * completing it. Deliberately *not* a `CancellationException`: that would read as
     * coroutine cancellation and be swallowed by the caller's scope.
     */
    suspend fun <T> withAuthentication(operation: suspend () -> T): T
}

/** The user backed out of sign-in, so the operation guarded by [UserService.withAuthentication] did not run. */
class AuthenticationCancelledException : Exception("Authentication was cancelled")
