package io.github.rozd.userkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/** The canonical minimal conformer set — the smallest thing an adapter has to provide. */

class StubUserSession(
    override val isAuthenticated: Boolean = true,
    override val refreshToken: String? = "stub-refresh-token",
    private val token: String? = "stub-access-token",
) : UserSession {
    override suspend fun accessToken(): String? = token
}

class StubUserProfile(override val displayName: String? = "Stub User") : UserProfile

data class StubUserInfo(
    override val id: UserId,
    override val role: String? = null,
    override val session: UserSession = StubUserSession(),
    override val profile: UserProfile = StubUserProfile(),
) : UserInfo

class StubUserService : UserService {
    override val isEmailVerified: Flow<Boolean> = flowOf(true)
    override suspend fun signIn() {}
    override suspend fun signOut() {}
    override suspend fun sendVerificationEmail() {}
    override fun authenticate() {}
    override fun authenticateIfNeeded(): Boolean = true
    override suspend fun <T> withAuthentication(operation: suspend () -> T): T = operation()
}

class StubUserStorage(private val info: UserInfo? = null) : UserStorage {
    override suspend fun fetch(): UserInfo? = info
    override suspend fun store(info: UserInfo) {}
    override suspend fun clear() {}
}

class StubUserSynchronizer(private val updates: Flow<UserInfo?> = emptyFlow()) : UserSynchronizer {
    var disposed = false
        private set

    override fun install(): Flow<UserInfo?> = updates
    override fun dispose() {
        disposed = true
    }
}
