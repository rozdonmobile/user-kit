package io.github.rozd.userkit

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminRoleTest {

    @Test fun `isAdmin is true when role is admin`() {
        assertTrue(StubUserInfo(UserId("u"), role = "admin").isAdmin)
    }

    @Test fun `isAdmin is false for a non-admin role`() {
        assertFalse(StubUserInfo(UserId("u"), role = "client").isAdmin)
    }

    @Test fun `isAdmin is false when role is null (default conformer)`() {
        assertFalse(StubUserInfo(UserId("u"), role = null).isAdmin)
    }

    @Test fun `isAdmin is false for empty role`() {
        assertFalse(StubUserInfo(UserId("u"), role = "").isAdmin)
    }

    @Test fun `isAdmin is case sensitive`() {
        assertFalse(StubUserInfo(UserId("u"), role = "ADMIN").isAdmin)
        assertFalse(StubUserInfo(UserId("u"), role = "Admin").isAdmin)
    }

    @Test fun `User isAdmin is false when info is null`() = runTest {
        val user = User(StubUserService(), StubUserStorage(null), StubUserSynchronizer(), scope = this)
        assertFalse(user.isAdmin)
        user.close()
    }
}
