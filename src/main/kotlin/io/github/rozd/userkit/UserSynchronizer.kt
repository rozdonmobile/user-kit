package io.github.rozd.userkit

import kotlinx.coroutines.flow.Flow

/**
 * The live auth-state stream. [install] must return a cold, cancel-safe [Flow]
 * that emits the current user — or `null` when signed out — every time the
 * provider's auth state changes, and unsubscribes when collection stops
 * (`callbackFlow { … awaitClose { … } }` is the idiom).
 */
interface UserSynchronizer {
    fun install(): Flow<UserInfo?>
    fun dispose()
}
