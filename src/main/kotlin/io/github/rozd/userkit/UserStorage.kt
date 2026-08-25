package io.github.rozd.userkit

/**
 * Persists the last-known user so [User.info] can be seeded before the live
 * stream from [UserSynchronizer] delivers its first value.
 *
 * Providers that own session persistence themselves (Firebase does) implement
 * [fetch] and leave [store] / [clear] as no-ops.
 */
interface UserStorage {
    suspend fun fetch(): UserInfo?
    suspend fun store(info: UserInfo)
    suspend fun clear()
}
