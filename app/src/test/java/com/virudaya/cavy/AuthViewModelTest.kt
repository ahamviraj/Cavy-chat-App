package com.virudaya.cavy

import com.virudaya.cavy.data.auth.AuthRepository
import com.virudaya.cavy.data.auth.AuthResult
import com.virudaya.cavy.data.auth.CavyUser
import com.virudaya.cavy.ui.auth.AuthSessionState
import com.virudaya.cavy.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isUnauthenticatedWhenUserNull() {
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.sessionState.value is AuthSessionState.Unauthenticated)
    }

    @Test
    fun signIn_withInvalidEmail_setsErrorWithoutCallingRepo() {
        viewModel.onEmailChanged("invalid-email")
        viewModel.onPasswordChanged("123456")
        viewModel.signIn()

        assertNotNull(viewModel.uiState.value.emailError)
        assertEquals(0, fakeRepository.signInCallCount)
    }

    @Test
    fun register_withMismatchPassword_setsErrorWithoutCallingRepo() {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("different123")
        viewModel.register()

        assertNotNull(viewModel.uiState.value.confirmPasswordError)
        assertEquals(0, fakeRepository.signUpCallCount)
    }

    @Test
    fun signIn_success_resetsFormAndCallsRepo() {
        viewModel.onEmailChanged("user@cavy.com")
        viewModel.onPasswordChanged("validPassword")
        viewModel.signIn()

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.signInCallCount)
        assertNull(viewModel.uiState.value.generalError)
    }

    private class FakeAuthRepository : AuthRepository {
        var signInCallCount = 0
        var signUpCallCount = 0
        private val _authStateFlow = MutableStateFlow<CavyUser?>(null)

        override val currentUser: CavyUser?
            get() = _authStateFlow.value

        override val authStateFlow: Flow<CavyUser?>
            get() = _authStateFlow

        override suspend fun signInWithEmail(email: String, password: String): AuthResult {
            signInCallCount++
            return AuthResult.Success
        }

        override suspend fun signUpWithEmail(email: String, password: String): AuthResult {
            signUpCallCount++
            return AuthResult.Success
        }

        override suspend fun sendPasswordResetEmail(email: String): AuthResult {
            return AuthResult.Success
        }

        override fun signOut() {
            _authStateFlow.value = null
        }
    }
}
