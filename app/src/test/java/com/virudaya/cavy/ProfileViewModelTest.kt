package com.virudaya.cavy

import com.virudaya.cavy.data.auth.AuthRepository
import com.virudaya.cavy.data.auth.AuthResult
import com.virudaya.cavy.data.auth.CavyUser
import com.virudaya.cavy.data.profile.PrivateProfile
import com.virudaya.cavy.data.profile.ProfileRepository
import com.virudaya.cavy.data.profile.ProfileResult
import com.virudaya.cavy.data.profile.UserProfile
import com.virudaya.cavy.ui.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeAuthRepo
    private lateinit var fakeProfileRepo: FakeProfileRepo
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepo()
        fakeProfileRepo = FakeProfileRepo()
        viewModel = ProfileViewModel(fakeAuthRepo, fakeProfileRepo)
        fakeAuthRepo.setLoggedIn("user123", "test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun profileInitialization_loadsProfileAndAuthenticatedUid() {
        assertEquals("user123", viewModel.uiState.value.authenticatedUid)
        assertEquals("user123", viewModel.uiState.value.userProfile?.uid)
        assertEquals("Test User", viewModel.uiState.value.userProfile?.displayName)
        assertEquals("test@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun startEditingDisplayName_updatesState() {
        viewModel.startEditingDisplayName()
        assertTrue(viewModel.uiState.value.isEditingDisplayName)
    }

    @Test
    fun cancelEditingDisplayName_resetsState() {
        viewModel.startEditingDisplayName()
        viewModel.onDisplayNameInputChanged("New Name")
        viewModel.cancelEditingDisplayName()

        assertFalse(viewModel.uiState.value.isEditingDisplayName)
    }

    @Test
    fun saveDisplayName_validationErrorWhenTooShort() {
        viewModel.startEditingDisplayName()
        viewModel.onDisplayNameInputChanged("A")
        viewModel.saveDisplayName()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun saveDisplayName_success_updatesName() {
        viewModel.startEditingDisplayName()
        viewModel.onDisplayNameInputChanged("Valid Name")
        viewModel.saveDisplayName()

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isEditingDisplayName)
        assertEquals("Profile updated successfully!", viewModel.uiState.value.successMessage)
    }

    @Test
    fun checkEngineStatus_returnsRealConnectionResult() {
        var statusResult: String? = null
        viewModel.checkEngineStatus { statusResult = it }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(statusResult)
        assertTrue(statusResult!!.contains("CAVY Data Engine active"))
    }

    private class FakeAuthRepo : AuthRepository {
        private val _authState = MutableStateFlow<CavyUser?>(null)
        override val currentUser: CavyUser? get() = _authState.value
        override val authStateFlow: Flow<CavyUser?> get() = _authState

        fun setLoggedIn(uid: String, email: String) {
            _authState.value = CavyUser(uid = uid, email = email)
        }

        override suspend fun signInWithEmail(email: String, password: String): AuthResult = AuthResult.Success
        override suspend fun signUpWithEmail(email: String, password: String): AuthResult = AuthResult.Success
        override suspend fun sendPasswordResetEmail(email: String): AuthResult = AuthResult.Success
        override fun signOut() {
            _authState.value = null
        }
    }

    private class FakeProfileRepo : ProfileRepository {
        override fun getProfileFlow(uid: String): Flow<ProfileResult<UserProfile>> {
            return flowOf(ProfileResult.Success(UserProfile(uid = uid, displayName = "Test User")))
        }

        override fun getPrivateProfileFlow(uid: String): Flow<ProfileResult<PrivateProfile>> {
            return flowOf(ProfileResult.Success(PrivateProfile(email = "test@example.com")))
        }

        override suspend fun ensureProfileInitialized(
            uid: String,
            email: String?,
            fallbackDisplayName: String?
        ): ProfileResult<UserProfile> {
            return ProfileResult.Success(UserProfile(uid = uid, displayName = "Test User"))
        }

        override suspend fun updateDisplayName(uid: String, newDisplayName: String): ProfileResult<Unit> {
            return ProfileResult.Success(Unit)
        }

        override suspend fun checkEngineConnection(uid: String): ProfileResult<String> {
            return ProfileResult.Success("CAVY Data Engine active & verified (RTDB connected for UID: ${uid.take(8)})")
        }
    }
}
