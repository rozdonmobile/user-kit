package io.github.rozd.userkit

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    private val seeded = StubUserInfo(UserId("seeded"))
    private val live = StubUserInfo(UserId("live"), role = "admin")

    @Test fun `info is seeded from storage before the synchronizer speaks`() = runTest {
        val updates = MutableSharedFlow<UserInfo?>()
        val user = User(StubUserService(), StubUserStorage(seeded), StubUserSynchronizer(updates), scope = this)
        testScheduler.runCurrent()

        assertEquals(seeded, user.info)
        assertEquals(seeded, user.infos.value)
        assertTrue(user.isAuthenticated)
        assertFalse(user.isAdmin)
        user.close()
    }

    @Test fun `info follows the synchronizer`() = runTest {
        val updates = MutableSharedFlow<UserInfo?>()
        val user = User(StubUserService(), StubUserStorage(seeded), StubUserSynchronizer(updates), scope = this)
        testScheduler.runCurrent()

        updates.emit(live)
        assertEquals(live, user.info)
        assertTrue(user.isAdmin)

        updates.emit(null)
        assertNull(user.info)
        assertFalse(user.isAuthenticated)
        assertFalse(user.isAdmin)
        user.close()
    }

    @Test fun `userIds de-duplicates a refreshed token for the same user`() = runTest {
        val updates = MutableSharedFlow<UserInfo?>()
        val user = User(StubUserService(), StubUserStorage(null), StubUserSynchronizer(updates), scope = this)
        testScheduler.runCurrent()

        val seen = mutableListOf<UserId?>()
        val collector = launch { user.userIds.toList(seen) }
        testScheduler.runCurrent()

        updates.emit(StubUserInfo(UserId("a"), role = null))
        updates.emit(StubUserInfo(UserId("a"), role = "admin")) // same user, new claims
        updates.emit(StubUserInfo(UserId("b")))
        updates.emit(null)
        testScheduler.runCurrent()

        assertEquals(listOf(null, UserId("a"), UserId("b"), null), seen)
        collector.cancel()
        user.close()
    }

    @Test fun `close cancels the sync and disposes the synchronizer`() = runTest {
        val updates = MutableSharedFlow<UserInfo?>()
        val synchronizer = StubUserSynchronizer(updates)
        val user = User(StubUserService(), StubUserStorage(null), synchronizer, scope = this)
        testScheduler.runCurrent()

        user.close()
        testScheduler.runCurrent()
        assertTrue(synchronizer.disposed)
        assertEquals(0, updates.subscriptionCount.value)
    }

    @Test fun `withAuthentication forwards to the service`() = runTest {
        val user = User(StubUserService(), StubUserStorage(null), StubUserSynchronizer(), scope = this)
        assertEquals(42, user.withAuthentication { 42 })
        user.close()
    }
}
