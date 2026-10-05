package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainTab
import com.example.ui.ZallCallViewModel
import com.example.ui.components.ZallAvatar
import com.example.ui.screens.ActiveCallFullScreenOverlay
import com.example.ui.screens.AuthRegistrationScreen
import com.example.ui.screens.CallsTabScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.FullScreenStoryViewerModal
import com.example.ui.screens.ServerAndProfileScreen
import com.example.ui.screens.StatusStoriesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ZallCallAppRoot()
            }
        }
    }
}

@Composable
fun ZallCallAppRoot(
    vm: ZallCallViewModel = viewModel()
) {
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val cloudDirectoryUsers by vm.cloudDirectoryUsers.collectAsStateWithLifecycle()
    val allMessages by vm.allMessages.collectAsStateWithLifecycle()
    val chatItems by vm.chatPreviewItems.collectAsStateWithLifecycle()
    val stories by vm.stories.collectAsStateWithLifecycle()
    val callLogs by vm.callLogs.collectAsStateWithLifecycle()
    val serverLogs by vm.serverLogs.collectAsStateWithLifecycle()

    val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val activeChatContactId by vm.activeChatContactId.collectAsStateWithLifecycle()
    val replyingTo by vm.replyingToMessage.collectAsStateWithLifecycle()
    val activeStoryViewer by vm.activeStoryViewer.collectAsStateWithLifecycle()
    val activeCallSession by vm.activeCallSession.collectAsStateWithLifecycle()
    val isRegistering by vm.isRegistering.collectAsStateWithLifecycle()

    val isRecordingVn by vm.voiceNoteManager.isRecording.collectAsStateWithLifecycle()
    val recordingElapsedSec by vm.voiceNoteManager.recordingElapsedSec.collectAsStateWithLifecycle()
    val liveAmplitudes by vm.voiceNoteManager.liveAmplitudes.collectAsStateWithLifecycle()
    val activePlayingMsgId by vm.voiceNoteManager.activePlayingMsgId.collectAsStateWithLifecycle()
    val playbackProgress by vm.voiceNoteManager.playbackProgress.collectAsStateWithLifecycle()
    val playbackSpeed by vm.voiceNoteManager.playbackSpeed.collectAsStateWithLifecycle()
    val remoteVideoBitmap by vm.liveCallStreamEngine.remoteVideoFrame.collectAsStateWithLifecycle()
    val callPacketsCount by vm.liveCallStreamEngine.packetsTransferred.collectAsStateWithLifecycle()

    // 1. If not registered / logged in yet, show the Registration & Zall ID Generation Screen
    if (currentUser == null) {
        AuthRegistrationScreen(
            isRegistering = isRegistering,
            onGenerateZallId = vm::previewGeneratedZallId,
            onRegisterSubmit = { fullName, username, email, password, bio, serverUrl, avatarColor, pickedAvatarUri, generatedZallId ->
                vm.registerAccountWithZallId(
                    fullName = fullName,
                    username = username,
                    email = email,
                    password = password,
                    bio = bio,
                    serverUrl = serverUrl,
                    avatarColorHex = avatarColor,
                    pickedAvatarUriString = pickedAvatarUri,
                    pregeneratedZallId = generatedZallId
                )
            }
        )
        return
    }

    // 2. If an active Voice or Video Call is running, display full-screen WebRTC Call Overlay
    if (activeCallSession != null) {
        ActiveCallFullScreenOverlay(
            session = activeCallSession!!,
            remoteVideoBitmap = remoteVideoBitmap,
            packetsTransferred = callPacketsCount,
            onPushLocalVideoFrame = vm::pushVideoFrameToPeer,
            onAcceptCall = vm::acceptIncomingCall,
            onToggleMute = vm::toggleCallMute,
            onToggleSpeaker = vm::toggleCallSpeaker,
            onToggleCamera = vm::toggleCallCamera,
            onSwitchCamera = vm::switchCameraLens,
            onEndCall = vm::endActiveCall
        )
        return
    }

    // 3. If a Status (SW) Story is open in full-screen viewer
    if (activeStoryViewer != null) {
        FullScreenStoryViewerModal(
            story = activeStoryViewer!!,
            onClose = vm::closeStoryViewer,
            onToggleLike = { vm.toggleStoryLike(activeStoryViewer!!) },
            onReplyStory = { replyText -> vm.replyToStory(activeStoryViewer!!, replyText) },
            onDeleteStory = { vm.deleteStory(activeStoryViewer!!.id) }
        )
        return
    }

    // 4. If a specific Chat Conversation is open
    val activeContact = contacts.firstOrNull { it.contactId == activeChatContactId }
    if (activeContact != null) {
        val chatMessages = allMessages
            .filter { it.contactId == activeContact.contactId }
            .sortedBy { it.timestamp }

        ChatDetailScreen(
            contact = activeContact,
            messages = chatMessages,
            replyingTo = replyingTo,
            isRecordingVn = isRecordingVn,
            recordingSeconds = recordingElapsedSec,
            liveAmplitudes = liveAmplitudes,
            activePlayingMsgId = activePlayingMsgId,
            playbackProgress = playbackProgress,
            playbackSpeed = playbackSpeed,
            onBack = vm::closeChat,
            onSendText = vm::sendText,
            onSendImage = vm::sendImageInChat,
            onStartVnRecording = vm::startRecordingVoiceNote,
            onFinishVnRecording = vm::finishAndSendVoiceNote,
            onCancelVnRecording = vm::cancelRecordingVoiceNote,
            onPlayPauseVn = vm::playOrPauseVoiceNote,
            onToggleSpeed = vm::togglePlaybackSpeed,
            onSelectReply = vm::setReplyMessage,
            onToggleStar = vm::toggleStarMessage,
            onDeleteMessage = vm::deleteMessage,
            onUpdateContactPP = { newUri -> vm.updateContactCustomPP(activeContact, newUri) },
            onStartAudioCall = { vm.startCall(activeContact, isVideoCall = false) },
            onStartVideoCall = { vm.startCall(activeContact, isVideoCall = true) }
        )
        return
    }

    // 5. Main ZallCall Hub with Top Server Status Bar & Bottom Navigation
    val totalUnread = contacts.sumOf { it.unreadCount }
    val unviewedStories = stories.count { !it.isViewedByMe && it.authorId != "ME" }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ZallCall",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF25D366).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4ADE80))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = currentUser?.zallId ?: "FIREBASE SYNC",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${currentUser?.fullName} • @${currentUser?.username}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            vm.selectTab(MainTab.SERVER_PROFILE)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Firebase & Server Terhubung",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(22.dp)
                        )
                        ZallAvatar(
                            name = currentUser?.fullName ?: "Z",
                            colorHex = currentUser?.avatarColorHex ?: "#0A6E5C",
                            avatarUri = currentUser?.avatarUri,
                            size = 40.dp,
                            isOnline = true
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.CHATS,
                    onClick = { vm.selectTab(MainTab.CHATS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnread > 0) {
                                    Badge(containerColor = Color(0xFF25D366), contentColor = Color.White) {
                                        Text(totalUnread.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == MainTab.CHATS) Icons.Filled.Chat else Icons.Outlined.Chat,
                                contentDescription = "Chat"
                            )
                        }
                    },
                    label = { Text("Chat", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_tab_chats")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.STATUS_SW,
                    onClick = { vm.selectTab(MainTab.STATUS_SW) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unviewedStories > 0) {
                                    Badge(containerColor = Color(0xFF25D366), contentColor = Color.White) {
                                        Text(unviewedStories.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == MainTab.STATUS_SW) Icons.Filled.AutoAwesomeMotion else Icons.Outlined.AutoAwesomeMotion,
                                contentDescription = "Status (SW)"
                            )
                        }
                    },
                    label = { Text("Status (SW)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_tab_status")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.CALLS,
                    onClick = { vm.selectTab(MainTab.CALLS) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.CALLS) Icons.Filled.Call else Icons.Outlined.Call,
                            contentDescription = "Panggilan"
                        )
                    },
                    label = { Text("Panggilan", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_tab_calls")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.SERVER_PROFILE,
                    onClick = { vm.selectTab(MainTab.SERVER_PROFILE) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.SERVER_PROFILE) Icons.Filled.Dns else Icons.Outlined.Dns,
                            contentDescription = "Profil & DB"
                        )
                    },
                    label = { Text("Profil & DB", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_tab_server")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.CHATS -> {
                    ChatsTabScreen(
                        currentUser = currentUser,
                        chatItems = chatItems,
                        cloudDirectoryUsers = cloudDirectoryUsers,
                        searchQuery = searchQuery,
                        onSearchChange = vm::updateSearchQuery,
                        onRefreshDirectory = vm::refreshCloudDirectory,
                        onOpenChat = vm::openChat,
                        onStartAudioCall = { contact -> vm.startCall(contact, isVideoCall = false) },
                        onStartVideoCall = { contact -> vm.startCall(contact, isVideoCall = true) },
                        onTogglePin = vm::togglePinContact,
                        onConnectContactById = { zallId, name, username, bio, avatarUri ->
                            vm.connectContactById(
                                zallId = zallId,
                                displayName = name,
                                username = username,
                                bio = bio,
                                pickedAvatarUri = avatarUri,
                                openImmediately = true
                            )
                        }
                    )
                }
                MainTab.STATUS_SW -> {
                    StatusStoriesScreen(
                        currentUser = currentUser,
                        stories = stories,
                        onCreateStory = vm::createStatusStory,
                        onOpenStory = vm::openStoryViewer
                    )
                }
                MainTab.CALLS -> {
                    CallsTabScreen(
                        callLogs = callLogs,
                        contacts = contacts,
                        onStartAudioCall = { contact -> vm.startCall(contact, isVideoCall = false) },
                        onStartVideoCall = { contact -> vm.startCall(contact, isVideoCall = true) }
                    )
                }
                MainTab.SERVER_PROFILE -> {
                    ServerAndProfileScreen(
                        currentUser = currentUser,
                        firebaseStatusLabel = vm.firebaseManager.statusLabel,
                        totalContacts = contacts.size,
                        totalMessages = allMessages.size,
                        totalStories = stories.size,
                        totalCalls = callLogs.size,
                        serverLogs = serverLogs,
                        onSaveProfileAndPP = vm::updateAccountProfileAndPP,
                        onPingServer = vm::triggerServerPing,
                        onClearLogs = vm::clearServerLogs,
                        onLogout = vm::logoutAccount
                    )
                }
            }
        }
    }
}
