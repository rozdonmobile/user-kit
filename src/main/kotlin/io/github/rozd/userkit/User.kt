package io.github.rozd.userkit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The app's one observable "current user".
 *
 * Construct it once from a provider adapter, provide it through [LocalUser], and
 * read it anywhere. Two views of the same value, for the two places it is read:
 *
 * - [info] (and [isAuthenticated], [isAdmin] on top of it) is Compose **snapshot
 *   state** — read it in a composable and the composable recomposes when it
 *   changes, with no `collectAsState` ceremony. This is the SwiftUI `@Observable`
 *   ergonomic: `if (user.isAdmin) AdminButton()`.
 * - [infos] is a **`StateFlow`** for everything that is not a composable — a
 *   repository that scopes a Firestore query to the signed-in user, a Ktor plugin
 *   that attaches the bearer token, a `ViewModel`.
 *
 * Both are fed by the same collector, on the main dispatcher, so they never
 * disagree. [scope] is injectable for tests; the default lives as long as the
 * process, which is right for an app-wide singleton. Call [close] when a `User`
 * is genuinely finished with (tests, previews).
 */
@Stable
class User(
    val service: UserService,
    private val storage: UserStorage,
    private val synchronizer: UserSynchronizer,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : AutoCloseable {

    private val _infos = MutableStateFlow<UserInfo?>(null)

    /** Hot stream of the current user; `null` while unresolved or signed out. */
    val infos: StateFlow<UserInfo?> = _infos.asStateFlow()

    /** Current user snapshot, or `null` when signed out. Snapshot state: observable in composition. */
    var info: UserInfo? by mutableStateOf(null)
        private set

    private val sync: Job = scope.launch {
        publish(storage.fetch())
        synchronizer.install().collect { publish(it) }
    }

    private fun publish(value: UserInfo?) {
        info = value
        _infos.value = value
    }

    override fun close() {
        sync.cancel()
        synchronizer.dispose()
    }

    // MARK: - Reading

    val isAuthenticated: Boolean get() = info?.session?.isAuthenticated ?: false

    /** `info?.isAdmin ?: false` — see [UserInfo.isAdmin]. */
    val isAdmin: Boolean get() = info?.isAdmin ?: false

    /**
     * The signed-in user's id, de-duplicated — the stream to `flatMapLatest` a
     * per-user query off, so a token refresh does not restart it and a sign-out
     * tears it down. The counterpart of the iOS `user.infos.compactMap { $0?.id }`.
     */
    val userIds: Flow<UserId?> = infos.map { it?.id }.distinctUntilChanged()

    // MARK: - Acting

    suspend fun signIn() = service.signIn()

    suspend fun signOut() = service.signOut()

    /** Present the provider's sign-in UI. */
    fun authenticate() = service.authenticate()

    /** Present sign-in only if needed; `true` when already authenticated. */
    fun ensureAuthenticated(): Boolean = service.authenticateIfNeeded()

    /** Run [operation] with a guaranteed session — see [UserService.withAuthentication]. */
    suspend fun <T> withAuthentication(operation: suspend () -> T): T =
        service.withAuthentication(operation)
}
