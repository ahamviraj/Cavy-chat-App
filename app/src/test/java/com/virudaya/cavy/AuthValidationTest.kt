package com.virudaya.cavy

import com.virudaya.cavy.util.AuthValidation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validEmail_returnsTrue() {
        assertTrue(AuthValidation.isValidEmail("user@example.com"))
        assertTrue(AuthValidation.isValidEmail("test.user+cavy@domain.co.uk"))
    }

    @Test
    fun invalidEmail_returnsFalse() {
        assertFalse(AuthValidation.isValidEmail(""))
        assertFalse(AuthValidation.isValidEmail("invalid-email"))
        assertFalse(AuthValidation.isValidEmail("user@.com"))
        assertFalse(AuthValidation.isValidEmail("@domain.com"))
    }

    @Test
    fun passwordLength_validation() {
        assertTrue(AuthValidation.isValidPassword("123456"))
        assertTrue(AuthValidation.isValidPassword("securePassword123"))
        assertFalse(AuthValidation.isValidPassword("12345"))
        assertFalse(AuthValidation.isValidPassword(""))
    }

    @Test
    fun passwordsMatching_validation() {
        assertTrue(AuthValidation.doPasswordsMatch("pass123", "pass123"))
        assertFalse(AuthValidation.doPasswordsMatch("pass123", "pass1234"))
    }
}
