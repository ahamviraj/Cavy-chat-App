package com.virudaya.cavy

import com.virudaya.cavy.data.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidationTest {

    @Test
    fun userProfile_defaultValues() {
        val profile = UserProfile(
            uid = "user123",
            displayName = "Cavy User"
        )

        assertEquals("user123", profile.uid)
        assertEquals("Cavy User", profile.displayName)
        assertNull(profile.photoUrl)
        assertEquals(0L, profile.createdAt)
        assertEquals(0L, profile.updatedAt)
    }

    @Test
    fun displayNameLength_validation() {
        fun isValidDisplayName(name: String): Boolean {
            val trimmed = name.trim()
            return trimmed.length in 2..50
        }

        assertTrue(isValidDisplayName("Cavy Member"))
        assertTrue(isValidDisplayName("AB"))
        assertFalse(isValidDisplayName("A"))
        assertFalse(isValidDisplayName(""))
        assertFalse(isValidDisplayName("a".repeat(51)))
    }

    @Test
    fun fallbackDisplayName_formatting() {
        fun generateFallback(email: String?): String {
            if (email.isNullOrBlank() || !email.contains("@")) return "Cavy Member"
            return email.substringBefore("@").replace(".", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        assertEquals("John doe", generateFallback("john.doe@example.com"))
        assertEquals("CavyUser", generateFallback("CavyUser@domain.com"))
        assertEquals("Cavy Member", generateFallback(""))
        assertEquals("Cavy Member", generateFallback(null))
    }
}
