package io.github.rozd.userkit

/** The identity provider's stable identifier for a user (a Firebase `uid`, for example). */
@JvmInline
value class UserId(val value: String) {
    override fun toString(): String = value
}
