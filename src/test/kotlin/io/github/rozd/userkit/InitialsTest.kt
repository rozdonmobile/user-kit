package io.github.rozd.userkit

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test fun `two initials from a full name`() = assertEquals("MR", StubUserProfile("Max Rozdobudko").initials)
    @Test fun `caps at two`() = assertEquals("AB", StubUserProfile("A B C").initials)
    @Test fun `single name gives one initial`() = assertEquals("M", StubUserProfile("Max").initials)
    @Test fun `empty for missing name`() = assertEquals("", StubUserProfile(null).initials)
    @Test fun `ignores repeated spaces`() = assertEquals("MR", StubUserProfile("Max   Rozdobudko").initials)
}
