package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogEntity
import com.example.data.ContactEntity
import com.example.data.MessageEntity
import com.example.data.ServerSyncLogEntity
import com.example.data.StatusStoryEntity
import com.example.data.UserAccountEntity
import com.example.data.ZallCallDatabase
import com.example.data.ZallCallRepository
import com.example.media.LiveCallStreamEngine
import com.example.media.ProfileMediaHelper
import com.example.media.VoiceNoteAudioManager
import com.example.network.FirebaseSyncManager
import com.example.network.ZallCallServerEngine
import com.example.network.ZallRealtimeEnvelope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTab {
    CHATS, STATUS_SW, CALLS, SERVER_PROFILE
}

data class ActiveCallSession(
    val contact: ContactEntity,
    val isVideoCall: Boolean,
    val isIncoming: Boolean = false,
    val isRingingIncoming: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val useFrontCamera: Boolean = true,
    val elapsedSeconds: Int = 0,
    val connectionQuality: String = "HD • WSS Cloud Relay"
)

data class ChatPreviewItem(
    val contact: ContactEntity,
    val lastMessage: MessageEntity?
)

class ZallCallViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = ZallCallDatabase.getInstance(application)
    private val serverEngine = ZallCallServerEngine()
    val voiceNoteManager = VoiceNoteAudioManager(application)
    val liveCallStreamEngine = LiveCallStreamEngine()
    val firebaseManager = FirebaseSyncManager(application)
    val repository = ZallCallRepository(database.dao(), serverEngine, firebaseManager, voiceNoteManager)

    val currentUser: StateFlow<UserAccountEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contacts: StateFlow<List<ContactEntity>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudDirectoryUsers: StateFlow<List<ContactEntity>> = repository.cloudDirectoryUsers

    val allMessages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StatusStoryEntity>> = repository.stories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.callLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serverLogs: StateFlow<List<ServerSyncLogEntity>> = repository.serverLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(MainTab.CHATS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeChatContactId = MutableStateFlow<String?>(null)
    val activeChatContactId: StateFlow<String?> = _activeChatContactId.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyingToMessage: StateFlow<MessageEntity?> = _replyingToMessage.asStateFlow()

    private val _activeStoryViewer = MutableStateFlow<StatusStoryEntity?>(null)
    val activeStoryViewer: StateFlow<StatusStoryEntity?> = _activeStoryViewer.asStateFlow()

    private val _activeCallSession = MutableStateFlow<ActiveCallSession?>(null)
    val activeCallSession: StateFlow<ActiveCallSession?> = _activeCallSession.asStateFlow()

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private var callTimerJob: Job? = null

    val chatPreviewItems: StateFlow<List<ChatPreviewItem>> = combine(
        contacts,
        allMessages,
        _searchQuery
    ) { contactList, msgList, query ->
        val filtered = if (query.isBlank()) {
            contactList
        } else {
            val q = query.trim().lowercase()
            contactList.filter {
                it.displayName.lowercase().contains(q) ||
                    it.zallId.lowercase().contains(q) ||
                    it.username.lowercase().contains(q) ||
                    it.bio.lowercase().contains(q)
            }
        }
        filtered.map { contact ->
            val lastMsg = msgList.firstOrNull { it.contactId == contact.contactId }
            ChatPreviewItem(contact = contact, lastMessage = lastMsg)
        }.sortedWith(
            compareByDescending<ChatPreviewItem> { it.contact.isPinned }
                .thenByDescending { it.lastMessage?.timestamp ?: 0L }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeRealtimeSync(
                scope = viewModelScope,
                onIncomingCallSignal = ::handleIncomingCallSignal
            )
        }
    }

    private fun handleIncomingCallSignal(env: ZallRealtimeEnvelope) {
        viewModelScope.launch {
            when (env.messageType) {
                "CALL_INVITE_AUDIO", "CALL_INVITE_VIDEO" -> {
                    val isVid = env.messageType == "CALL_INVITE_VIDEO"
                    val callerContact = ContactEntity(
                        contactId = env.senderZallId,
                        zallId = env.senderZallId,
                        username = env.senderUsername.ifBlank { env.senderZallId.lowercase() },
                        displayName = env.senderName.ifBlank { env.senderZallId },
                        bio = env.senderBio,
                        avatarColorHex = env.senderColorHex.ifBlank { "#0A6E5C" },
                        avatarUri = env.senderAvatarB64,
                        isOnline = true,
                        lastSeenText = "Memanggil Anda..."
                    )
                    if (_activeCallSession.value == null) {
                        _activeCallSession.value = ActiveCallSession(
                            contact = callerContact,
                            isVideoCall = isVid,
                            isIncoming = true,
                            isRingingIncoming = true,
                            isSpeakerOn = isVid,
                            isCameraEnabled = isVid,
                            elapsedSeconds = 0
                        )
                    }
                }
                "CALL_END" -> {
                    if (_activeCallSession.value?.contact?.zallId == env.senderZallId) {
                        callTimerJob?.cancel()
                        _activeCallSession.value = null
                    }
                }
            }
        }
    }

    fun acceptIncomingCall() {
        val current = _activeCallSession.value ?: return
        _activeCallSession.value = current.copy(isRingingIncoming = false)
        val myId = currentUser.value?.zallId ?: "ZALL-ME"
        liveCallStreamEngine.startLiveCallStream(
            scope = viewModelScope,
            myZallId = myId,
            peerZallId = current.contact.zallId,
            isMutedProvider = { _activeCallSession.value?.isMuted == true }
        )
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive && _activeCallSession.value != null) {
                delay(1000L)
                _activeCallSession.value = _activeCallSession.value?.let {
                    it.copy(elapsedSeconds = it.elapsedSeconds + 1)
                }
            }
        }
    }

    fun previewGeneratedZallId(fullName: String): String {
        return firebaseManager.generateUniqueZallId(fullName)
    }

    fun refreshCloudDirectory() {
        viewModelScope.launch {
            repository.refreshCloudDirectory()
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openChat(contactId: String) {
        _activeChatContactId.value = contactId
        _replyingToMessage.value = null
        viewModelScope.launch {
            repository.clearUnread(contactId)
        }
    }

    fun closeChat() {
        voiceNoteManager.stopPlayback()
        voiceNoteManager.cancelRecording()
        _activeChatContactId.value = null
        _replyingToMessage.value = null
    }

    fun setReplyMessage(message: MessageEntity?) {
        _replyingToMessage.value = message
    }

    fun registerAccountWithZallId(
        fullName: String,
        username: String,
        email: String,
        password: String,
        bio: String,
        serverUrl: String,
        avatarColorHex: String,
        pickedAvatarUriString: String?,
        pregeneratedZallId: String?
    ) {
        if (fullName.isBlank()) return
        _isRegistering.value = true
        viewModelScope.launch {
            val processedAvatar = pickedAvatarUriString?.let {
                ProfileMediaHelper.copyAndEncodeImage(appContext, it, subFolder = "avatars")
            }
            repository.registerOrLoginAccount(
                scope = viewModelScope,
                fullName = fullName.trim(),
                username = username.trim().removePrefix("@").ifBlank { fullName.trim().lowercase().replace(" ", "_") },
                email = email.trim(),
                password = password,
                bio = bio.trim(),
                serverUrl = serverUrl.trim(),
                avatarColorHex = avatarColorHex,
                avatarLocalPath = processedAvatar?.localFilePath,
                avatarBase64 = processedAvatar?.base64DataUri,
                customZallId = pregeneratedZallId,
                onIncomingCallSignal = ::handleIncomingCallSignal
            )
            _isRegistering.value = false
        }
    }

    fun updateAccountProfileAndPP(
        newName: String,
        newUsername: String,
        newBio: String,
        newServerUrl: String,
        newAvatarColorHex: String,
        newPickedAvatarUri: String?
    ) {
        val acc = currentUser.value ?: return
        viewModelScope.launch {
            val processedAvatar = if (!newPickedAvatarUri.isNullOrBlank() && !newPickedAvatarUri.startsWith("/")) {
                ProfileMediaHelper.copyAndEncodeImage(appContext, newPickedAvatarUri, subFolder = "avatars")
            } else {
                null
            }
            repository.updateProfileAndPicture(
                account = acc,
                newName = newName,
                newUsername = newUsername,
                newBio = newBio,
                newServerUrl = newServerUrl,
                newAvatarColorHex = newAvatarColorHex,
                newAvatarUri = processedAvatar?.localFilePath ?: newPickedAvatarUri ?: acc.avatarUri,
                newAvatarBase64 = processedAvatar?.base64DataUri
            )
        }
    }

    fun logoutAccount() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun connectContactById(
        zallId: String,
        displayName: String,
        username: String,
        bio: String,
        pickedAvatarUri: String?,
        openImmediately: Boolean = true
    ) {
        if (zallId.isBlank() && username.isBlank() && displayName.isBlank()) return
        viewModelScope.launch {
            val processedAvatar = pickedAvatarUri?.let {
                ProfileMediaHelper.copyAndEncodeImage(appContext, it, subFolder = "contact_avatars")
            }
            val created = repository.connectContactByZallId(
                zallIdInput = zallId,
                displayNameInput = displayName,
                usernameInput = username,
                bioInput = bio,
                avatarUriInput = processedAvatar?.localFilePath
            )
            if (openImmediately) {
                openChat(created.contactId)
            }
        }
    }

    fun updateContactCustomPP(contact: ContactEntity, pickedUriString: String) {
        viewModelScope.launch {
            val processed = ProfileMediaHelper.copyAndEncodeImage(
                appContext,
                pickedUriString,
                subFolder = "contact_avatars"
            )
            if (processed != null) {
                repository.updateContactAvatar(contact, processed.localFilePath)
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            if (_activeChatContactId.value == contactId) {
                closeChat()
            }
            repository.deleteContact(contactId)
        }
    }

    fun sendText(text: String) {
        val cid = _activeChatContactId.value ?: return
        if (text.isBlank()) return
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null
        viewModelScope.launch {
            repository.sendTextMessage(
                scope = viewModelScope,
                contactId = cid,
                text = text,
                replyToId = replyMsg?.id,
                replyToPreview = replyMsg?.textContent
            )
        }
    }

    fun sendImageInChat(imageUri: String, caption: String) {
        val cid = _activeChatContactId.value ?: return
        viewModelScope.launch {
            val saved = ProfileMediaHelper.copyAndEncodeImage(
                appContext,
                imageUri,
                subFolder = "chat_images",
                maxDimensionPx = 720
            )
            repository.sendImageMessage(
                scope = viewModelScope,
                contactId = cid,
                localImagePath = saved?.localFilePath ?: imageUri,
                base64Thumb = saved?.base64DataUri,
                caption = caption
            )
        }
    }

    fun startRecordingVoiceNote() {
        voiceNoteManager.startRecording(viewModelScope)
    }

    fun finishAndSendVoiceNote() {
        val cid = _activeChatContactId.value ?: return
        val res = voiceNoteManager.stopRecording()
        viewModelScope.launch {
            repository.sendVoiceNoteMessage(
                scope = viewModelScope,
                contactId = cid,
                filePath = res.filePath,
                audioBase64 = res.audioBase64,
                durationSec = res.durationSec,
                waveformCsv = res.waveformCsv
            )
        }
    }

    fun cancelRecordingVoiceNote() {
        voiceNoteManager.cancelRecording()
    }

    fun playOrPauseVoiceNote(message: MessageEntity) {
        voiceNoteManager.playOrPauseVoiceNote(
            scope = viewModelScope,
            messageId = message.id,
            mediaPath = message.mediaPath,
            durationSec = message.vnDurationSec,
            waveformCsv = message.vnWaveformCsv
        )
    }

    fun togglePlaybackSpeed() {
        voiceNoteManager.togglePlaybackSpeed()
    }

    fun togglePinContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.togglePinContact(contact)
        }
    }

    fun toggleStarMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.toggleStarMessage(message)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    // Status (SW)
    fun createStatusStory(caption: String, bgHex: String, imageUri: String?) {
        if (caption.isBlank() && imageUri == null) return
        val user = currentUser.value
        val authorZallId = user?.zallId ?: "ZALL-ME"
        val authorName = user?.fullName ?: "Saya"
        val authorColor = user?.avatarColorHex ?: "#0A6E5C"
        val authorAvatarUri = user?.avatarUri
        viewModelScope.launch {
            val savedImg = imageUri?.let {
                ProfileMediaHelper.copyAndEncodeImage(
                    appContext,
                    it,
                    subFolder = "stories",
                    maxDimensionPx = 720
                )
            }
            repository.postStatusStory(
                authorZallId = authorZallId,
                authorName = authorName,
                authorColor = authorColor,
                authorAvatarUri = authorAvatarUri,
                caption = caption.trim().ifBlank { "📸 Update Status ZallCall" },
                bgHex = bgHex,
                imageUri = savedImg?.localFilePath ?: imageUri,
                imageBase64 = savedImg?.base64DataUri
            )
        }
    }

    fun openStoryViewer(story: StatusStoryEntity) {
        _activeStoryViewer.value = story
        viewModelScope.launch {
            repository.markStoryViewed(story.id)
        }
    }

    fun closeStoryViewer() {
        _activeStoryViewer.value = null
    }

    fun toggleStoryLike(story: StatusStoryEntity) {
        viewModelScope.launch {
            repository.toggleStoryLike(story)
            _activeStoryViewer.value = story.copy(isLikedByMe = !story.isLikedByMe)
        }
    }

    fun replyToStory(story: StatusStoryEntity, replyText: String) {
        if (replyText.isBlank()) return
        val targetContactId = if (story.authorId == "ME") {
            contacts.value.firstOrNull()?.contactId ?: return
        } else {
            story.authorId
        }
        viewModelScope.launch {
            repository.sendTextMessage(
                scope = viewModelScope,
                contactId = targetContactId,
                text = "💬 Membalas SW [${story.captionText.take(25)}...]: $replyText"
            )
            _activeStoryViewer.value = null
            openChat(targetContactId)
        }
    }

    fun deleteStory(storyId: Long) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
            if (_activeStoryViewer.value?.id == storyId) {
                _activeStoryViewer.value = null
            }
        }
    }

    // Audio & Video Calls
    fun startCall(contact: ContactEntity, isVideoCall: Boolean) {
        voiceNoteManager.stopPlayback()
        callTimerJob?.cancel()
        _activeCallSession.value = ActiveCallSession(
            contact = contact,
            isVideoCall = isVideoCall,
            isIncoming = false,
            isRingingIncoming = false,
            isSpeakerOn = isVideoCall,
            isCameraEnabled = isVideoCall,
            elapsedSeconds = 0
        )
        val myId = currentUser.value?.zallId ?: "ZALL-ME"
        liveCallStreamEngine.startLiveCallStream(
            scope = viewModelScope,
            myZallId = myId,
            peerZallId = contact.zallId,
            isMutedProvider = { _activeCallSession.value?.isMuted == true }
        )
        viewModelScope.launch {
            repository.sendCallSignalToRecipient(
                contact = contact,
                signalType = if (isVideoCall) "CALL_INVITE_VIDEO" else "CALL_INVITE_AUDIO"
            )
        }
        callTimerJob = viewModelScope.launch {
            while (isActive && _activeCallSession.value != null) {
                delay(1000L)
                _activeCallSession.value = _activeCallSession.value?.let {
                    it.copy(elapsedSeconds = it.elapsedSeconds + 1)
                }
            }
        }
    }

    fun pushVideoFrameToPeer(frameBitmap: android.graphics.Bitmap?) {
        val session = _activeCallSession.value ?: return
        if (!session.isVideoCall || !session.isCameraEnabled || session.isRingingIncoming) return
        val myId = currentUser.value?.zallId ?: return
        liveCallStreamEngine.pushLocalVideoFrameIfReady(
            scope = viewModelScope,
            myZallId = myId,
            peerZallId = session.contact.zallId,
            frameBitmap = frameBitmap
        )
    }

    fun toggleCallMute() {
        _activeCallSession.value = _activeCallSession.value?.let {
            it.copy(isMuted = !it.isMuted)
        }
    }

    fun toggleCallSpeaker() {
        _activeCallSession.value = _activeCallSession.value?.let {
            it.copy(isSpeakerOn = !it.isSpeakerOn)
        }
    }

    fun toggleCallCamera() {
        _activeCallSession.value = _activeCallSession.value?.let {
            val nextCam = !it.isCameraEnabled
            it.copy(isCameraEnabled = nextCam, isVideoCall = nextCam || it.isVideoCall)
        }
    }

    fun switchCameraLens() {
        _activeCallSession.value = _activeCallSession.value?.let {
            it.copy(useFrontCamera = !it.useFrontCamera)
        }
    }

    fun endActiveCall() {
        val session = _activeCallSession.value ?: return
        liveCallStreamEngine.stopLiveCallStream()
        callTimerJob?.cancel()
        callTimerJob = null
        _activeCallSession.value = null
        viewModelScope.launch {
            repository.recordCompletedCall(
                contact = session.contact,
                callType = if (session.isVideoCall) "VIDEO" else "AUDIO",
                direction = if (session.isIncoming) "INCOMING" else "OUTGOING",
                durationSec = session.elapsedSeconds.coerceAtLeast(1)
            )
        }
    }

    fun triggerServerPing() {
        viewModelScope.launch {
            repository.triggerManualServerPing()
        }
    }

    fun clearServerLogs() {
        viewModelScope.launch {
            repository.clearServerLogs()
        }
    }
}
