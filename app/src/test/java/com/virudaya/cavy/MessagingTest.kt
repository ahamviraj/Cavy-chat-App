package com.virudaya.cavy

import com.virudaya.cavy.data.auth.AuthRepository
import com.virudaya.cavy.data.auth.AuthResult
import com.virudaya.cavy.data.auth.CavyUser
import com.virudaya.cavy.data.messaging.ChatMessage
import com.virudaya.cavy.data.messaging.ConversationSummary
import com.virudaya.cavy.data.messaging.MessagingRepository
import com.virudaya.cavy.data.messaging.MessagingResult
import com.virudaya.cavy.data.profile.PrivateProfile
import com.virudaya.cavy.data.profile.ProfileRepository
import com.virudaya.cavy.data.profile.ProfileResult
import com.virudaya.cavy.data.profile.UserProfile
import com.virudaya.cavy.ui.messaging.ChatViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeAuthRepo
    private lateinit var fakeMessagingRepo: FakeMessagingRepo
    private lateinit var fakeProfileRepo: FakeProfileRepo
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepo()
        fakeMessagingRepo = FakeMessagingRepo()
        fakeProfileRepo = FakeProfileRepo()

        fakeAuthRepo.setLoggedIn("userA", "usera@cavy.com")
        viewModel = ChatViewModel(fakeAuthRepo, fakeMessagingRepo, fakeProfileRepo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deterministicConversationId_isSymmetric() {
        val id1 = fakeMessagingRepo.generateConversationId("userA", "userB")
        val id2 = fakeMessagingRepo.generateConversationId("userB", "userA")
        assertEquals(id1, id2)
        assertEquals("userA_userB", id1)
    }

    @Test
    fun chatViewModel_sendMessage_resetsInputOnSuccess() {
        viewModel.initializeChat("userA_userB", "userB", "Viraj Cavy")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onMessageInputChanged("Hello Viraj!")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals(1, fakeMessagingRepo.sentMessages.size)
        assertEquals("Hello Viraj!", fakeMessagingRepo.sentMessages.first().text)
    }

    @Test
    fun chatViewModel_sendMessage_rejectsOverlyLongText() {
        viewModel.initializeChat("userA_userB", "userB", "Viraj Cavy")
        testDispatcher.scheduler.advanceUntilIdle()

        val longText = "a".repeat(4001)
        viewModel.onMessageInputChanged(longText)
        viewModel.sendMessage()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(0, fakeMessagingRepo.sentMessages.size)
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
            return flowOf(ProfileResult.Success(UserProfile(uid = uid, displayName = "Jordan Cavy")))
        }

        override fun getPrivateProfileFlow(uid: String): Flow<ProfileResult<PrivateProfile>> {
            return flowOf(ProfileResult.Success(PrivateProfile(email = "jordan@cavy.com")))
        }

        override suspend fun ensureProfileInitialized(
            uid: String,
            email: String?,
            fallbackDisplayName: String?
        ): ProfileResult<UserProfile> {
            return ProfileResult.Success(UserProfile(uid = uid, displayName = "Jordan Cavy"))
        }

        override suspend fun updateDisplayName(uid: String, newDisplayName: String): ProfileResult<Unit> {
            return ProfileResult.Success(Unit)
        }

        override suspend fun checkEngineConnection(uid: String): ProfileResult<String> {
            return ProfileResult.Success("Connected")
        }
    }

    private class FakeMessagingRepo : MessagingRepository {
        val sentMessages = mutableListOf<ChatMessage>()

        override fun generateConversationId(uidA: String, uidB: String): String {
            return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
        }

        override suspend fun getOrCreateConversation(
            currentUid: String,
            currentDisplayName: String,
            otherUid: String,
            otherDisplayName: String
        ): MessagingResult<String> {
            if (currentUid == otherUid) return MessagingResult.Error("Cannot start conversation with yourself")
            val convId = generateConversationId(currentUid, otherUid)
            return MessagingResult.Success(convId)
        }

        override fun getConversationsFlow(currentUid: String): Flow<MessagingResult<List<ConversationSummary>>> {
            return flowOf(MessagingResult.Success(emptyList()))
        }

        override fun getMessagesFlow(conversationId: String): Flow<MessagingResult<List<ChatMessage>>> {
            return flowOf(MessagingResult.Success(sentMessages))
        }

        override suspend fun sendMessage(
            conversationId: String,
            senderUid: String,
            otherUid: String,
            otherDisplayName: String,
            senderDisplayName: String,
            text: String
        ): MessagingResult<Unit> {
            val msg = ChatMessage("msg1", conversationId, senderUid, text, System.currentTimeMillis())
            sentMessages.add(msg)
            return MessagingResult.Success(Unit)
        }

        override suspend fun searchUsers(currentUid: String, query: String): MessagingResult<List<UserProfile>> {
            return MessagingResult.Success(emptyList())
        }
    }
}
