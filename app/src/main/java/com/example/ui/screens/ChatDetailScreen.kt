package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.ContactEntity
import com.example.data.MessageEntity
import com.example.ui.components.ZallAvatar
import com.example.ui.components.formatDurationMmSs
import com.example.ui.components.formatTimeShort
import com.example.ui.theme.IncomingBubbleDark
import com.example.ui.theme.IncomingBubbleLight
import com.example.ui.theme.OutgoingBubbleDark
import com.example.ui.theme.OutgoingBubbleLight
import com.example.ui.theme.ZallChatBgDark
import com.example.ui.theme.ZallChatBgLight

@Composable
fun ChatDetailScreen(
    contact: ContactEntity,
    messages: List<MessageEntity>,
    replyingTo: MessageEntity?,
    isRecordingVn: Boolean,
    recordingSeconds: Int,
    liveAmplitudes: List<Int>,
    activePlayingMsgId: Long?,
    playbackProgress: Float,
    playbackSpeed: Float,
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    onSendImage: (String, String) -> Unit,
    onStartVnRecording: () -> Unit,
    onFinishVnRecording: () -> Unit,
    onCancelVnRecording: () -> Unit,
    onPlayPauseVn: (MessageEntity) -> Unit,
    onToggleSpeed: () -> Unit,
    onSelectReply: (MessageEntity?) -> Unit,
    onToggleStar: (MessageEntity) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onUpdateContactPP: (String) -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Even if denied on an emulator without mic, startRecording handles acoustic synthesis gracefully
        onStartVnRecording()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendImage(uri.toString(), inputText.trim())
            inputText = ""
        }
    }

    val contactPpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateContactPP(uri.toString())
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) ZallChatBgDark else ZallChatBgLight)
            .imePadding()
    ) {
        // Top Chat Header Bar
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                ZallAvatar(
                    name = contact.displayName,
                    colorHex = contact.avatarColorHex,
                    avatarUri = contact.avatarUri,
                    size = 42.dp,
                    isOnline = contact.isOnline,
                    modifier = Modifier.clickable {
                        contactPpPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        text = "${contact.zallId} • @${contact.username}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onStartVideoCall,
                    modifier = Modifier.testTag("chat_header_video_call")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onStartAudioCall,
                    modifier = Modifier.testTag("chat_header_audio_call")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Panggilan Suara",
                        tint = Color.White
                    )
                }
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // Encryption & Server Sync Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🔐 Pesan, Voice Note (VN), dan Panggilan dienkripsi End-to-End & tersimpan di Database ZallCall",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                MessageBubbleItem(
                    message = msg,
                    isDark = isDark,
                    isPlaying = activePlayingMsgId == msg.id,
                    playbackProgress = if (activePlayingMsgId == msg.id) playbackProgress else 0f,
                    playbackSpeed = playbackSpeed,
                    onPlayPauseVn = { onPlayPauseVn(msg) },
                    onToggleSpeed = onToggleSpeed,
                    onReply = { onSelectReply(msg) },
                    onToggleStar = { onToggleStar(msg) },
                    onDelete = { onDeleteMessage(msg.id) }
                )
            }
        }

        // Reply Preview Banner
        AnimatedVisibility(visible = replyingTo != null) {
            if (replyingTo != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (replyingTo.senderId == "ME") "Membalas pesan Anda" else "Membalas ${contact.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = replyingTo.textContent,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onSelectReply(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal Balas")
                        }
                    }
                }
            }
        }

        // Bottom Input Bar or Active Voice Note Recorder Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (isRecordingVn) {
                // Active Voice Note Recording Studio Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCancelVnRecording,
                        modifier = Modifier.testTag("cancel_vn_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Batalkan VN",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDurationMmSs(recordingSeconds),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Live Acoustic Waveform Canvas
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        val barCount = 24
                        val spacing = 4.dp.toPx()
                        val totalWidth = size.width
                        val barWidth = ((totalWidth - spacing * (barCount - 1)) / barCount).coerceAtLeast(3f)
                        val amps = if (liveAmplitudes.isEmpty()) List(barCount) { 30 } else liveAmplitudes.takeLast(barCount)

                        for (i in 0 until barCount) {
                            val amp = amps.getOrElse(i) { 25 }
                            val ratio = (amp / 100f).coerceIn(0.15f, 1f)
                            val barHeight = size.height * ratio
                            val top = (size.height - barHeight) / 2f
                            drawRoundRect(
                                color = Color(0xFF25D366),
                                topLeft = Offset(i * (barWidth + spacing), top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    FilledIconButton(
                        onClick = onFinishVnRecording,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_vn_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Voice Note")
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("attach_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Kirim Foto",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ketik pesan ZallCall...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (inputText.isNotBlank()) {
                        FilledIconButton(
                            onClick = {
                                onSendText(inputText)
                                inputText = ""
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_text_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Pesan")
                        }
                    } else {
                        FilledIconButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    onStartVnRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("record_vn_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Rekam Voice Note (VN)")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleItem(
    message: MessageEntity,
    isDark: Boolean,
    isPlaying: Boolean,
    playbackProgress: Float,
    playbackSpeed: Float,
    onPlayPauseVn: () -> Unit,
    onToggleSpeed: () -> Unit,
    onReply: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    val isMe = message.senderId == "ME"
    var showContextMenu by remember { mutableStateOf(false) }

    if (message.messageType == "CALL_EVENT") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "${message.textContent} • ${formatTimeShort(message.timestamp)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    val bubbleColor = when {
        isMe && isDark -> OutgoingBubbleDark
        isMe && !isDark -> OutgoingBubbleLight
        !isMe && isDark -> IncomingBubbleDark
        else -> IncomingBubbleLight
    }

    val textColor = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(min = 120.dp, max = 310.dp)
                .combinedClickable(
                    onClick = {
                        if (message.messageType == "VN") onPlayPauseVn()
                    },
                    onLongClick = { showContextMenu = true }
                )
                .testTag("message_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Replied preview if any
                if (!message.replyToPreview.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "↩ ${message.replyToPreview}",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                when (message.messageType) {
                    "VN" -> {
                        VoiceNoteBubbleContent(
                            message = message,
                            isPlaying = isPlaying,
                            progress = playbackProgress,
                            speed = playbackSpeed,
                            textColor = textColor,
                            onPlayPause = onPlayPauseVn,
                            onToggleSpeed = onToggleSpeed
                        )
                    }
                    "IMAGE" -> {
                        if (!message.mediaPath.isNullOrBlank()) {
                            val b64Bmp = remember(message.mediaPath) {
                                if (message.mediaPath.startsWith("data:image")) {
                                    com.example.media.ProfileMediaHelper.decodeBase64ToBitmap(message.mediaPath)
                                } else null
                            }
                            if (b64Bmp != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = b64Bmp.asImageBitmap(),
                                    contentDescription = "Foto Chat",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                AsyncImage(
                                    model = message.mediaPath,
                                    contentDescription = "Foto Chat",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = message.textContent,
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                    else -> {
                        Text(
                            text = message.textContent,
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp + Star + Delivery Checkmarks
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Berbintang",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier
                                .size(13.dp)
                                .padding(end = 3.dp)
                        )
                    }
                    Text(
                        text = formatTimeShort(message.timestamp),
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.65f)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusIcon = when (message.deliveryStatus) {
                            "SENDING" -> Icons.Default.Schedule
                            "SENT" -> Icons.Default.Done
                            else -> Icons.Default.DoneAll
                        }
                        val statusTint = if (message.deliveryStatus == "READ") {
                            Color(0xFF38BDF8)
                        } else {
                            textColor.copy(alpha = 0.65f)
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = message.deliveryStatus,
                            tint = statusTint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Balas Pesan") },
                    onClick = {
                        showContextMenu = false
                        onReply()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (message.isStarred) "Hapus Bintang" else "Beri Bintang ⭐") },
                    onClick = {
                        showContextMenu = false
                        onToggleStar()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hapus dari Database") },
                    onClick = {
                        showContextMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceNoteBubbleContent(
    message: MessageEntity,
    isPlaying: Boolean,
    progress: Float,
    speed: Float,
    textColor: Color,
    onPlayPause: () -> Unit,
    onToggleSpeed: () -> Unit
) {
    val amplitudes = remember(message.vnWaveformCsv) {
        message.vnWaveformCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(35, 65, 80, 45, 70, 90, 55, 40, 75, 85, 60, 48, 72, 88, 50, 38) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        FilledIconButton(
            onClick = onPlayPause,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFF25D366),
                contentColor = Color.White
            ),
            modifier = Modifier
                .size(42.dp)
                .testTag("play_vn_button_${message.id}")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause VN" else "Play VN"
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val count = amplitudes.size.coerceAtLeast(1)
                val spacing = 3.dp.toPx()
                val barW = ((size.width - spacing * (count - 1)) / count).coerceAtLeast(2.5f)

                for (i in 0 until count) {
                    val ratio = (amplitudes[i] / 100f).coerceIn(0.18f, 1f)
                    val barH = size.height * ratio
                    val top = (size.height - barH) / 2f
                    val isPlayedPart = (i.toFloat() / count.toFloat()) <= progress && isPlaying
                    drawRoundRect(
                        color = if (isPlayedPart) Color(0xFF25D366) else textColor.copy(alpha = 0.38f),
                        topLeft = Offset(i * (barW + spacing), top),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentSec = if (isPlaying) {
                    (message.vnDurationSec * progress).toInt()
                } else {
                    message.vnDurationSec
                }
                Text(
                    text = "🎙️ VN • ${formatDurationMmSs(currentSec)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.8f)
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onToggleSpeed() }
                ) {
                    Text(
                        text = "${speed}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
