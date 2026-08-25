package io.github.rozd.userkit

/**
 * A snapshot of the signed-in user: who they are, the session they hold, and
 * the role the identity provider assigns them.
 *
 * Adapters implement this over their SDK's user object. Implementations should
 * define `equals` so that a token refresh that changes nothing does not look like
 * a new user — [User.infos] is a `StateFlow`, and it conflates equal values.
 */
interface UserInfo {
    val id: UserId
    val session: UserSession
    val profile: UserProfile

    /**
     * Authorization role for the current user, surfaced from the identity provider
     * (e.g. a JWT custom claim). `null` when the provider exposes no role. Adapters
     * override this; the default keeps provider-agnostic implementations (fakes,
     * previews) compiling as non-admin.
     */
    val role: String? get() = null
}

/** `role == "admin"`, case-sensitive: `"Admin"`, `"ADMIN"`, `""` and `null` are all non-admin. */
val UserInfo.isAdmin: Boolean get() = role == ADMIN_ROLE

private const val ADMIN_ROLE = "admin"

interface UserSession {
    val isAuthenticated: Boolean
    val refreshToken: String?

    /** The bearer token for backend calls. A `suspend` function, not a property: it may refresh. */
    suspend fun accessToken(): String?
}

interface UserProfile {
    val displayName: String?
}

/** Up to two initials from [UserProfile.displayName] — `"Max Rozdobudko"` → `"MR"`. */
val UserProfile.initials: String
    get() = displayName.orEmpty()
        .split(' ')
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().toString() }
