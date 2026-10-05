package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.CallLogEntity
import com.example.data.ContactEntity
import com.example.ui.ActiveCallSession
import com.example.ui.components.ZallAvatar
import com.example.ui.components.formatDurationMmSs
import com.example.ui.components.formatTimeShort
import com.example.ui.theme.CallRed

@Composable
fun CallsTabScreen(
    callLogs: List<CallLogEntity>,
    contacts: List<ContactEntity>,
    onStartAudioCall: (ContactEntity) -> Unit,
    onStartVideoCall: (ContactEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calls_history_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (contacts.isNotEmpty()) {
            item {
                Text(
                    text = "PANGGILAN CEPAT KE ZALL ID TERHUBUNG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        contacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ZallAvatar(
                                    name = contact.displayName,
                                    colorHex = contact.avatarColorHex,
                                    avatarUri = contact.avatarUri,
                                    size = 42.dp,
                                    isOnline = contact.isOnline
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${contact.zallId} • @${contact.username}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onStartAudioCall(contact) },
                                    modifier = Modifier.testTag("call_tab_audio_${contact.contactId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Telfon Suara",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onStartVideoCall(contact) },
                                    modifier = Modifier.testTag("call_tab_video_${contact.contactId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video Call",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "RIWAYAT PANGGILAN DATABASE (${callLogs.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (callLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Riwayat Panggilan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hubungkan Zall ID teman Anda di tab Chat lalu mulai panggilan suara atau Video Call HD.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(callLogs, key = { it.id }) { log ->
                val matchedContact = contacts.firstOrNull { it.contactId == log.contactId }
                    ?: ContactEntity(
                        contactId = log.contactId,
                        zallId = log.contactZallId,
                        username = log.contactZallId.lowercase(),
                        displayName = log.contactName,
                        bio = "",
                        avatarColorHex = log.avatarColorHex,
                        avatarUri = log.avatarUri,
                        isOnline = true,
                        lastSeenText = "Online"
                    )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZallAvatar(
                            name = log.contactName,
                            colorHex = log.avatarColorHex,
                            avatarUri = log.avatarUri,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${log.contactName} (${log.contactZallId})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (log.direction == "MISSED") MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dirIcon = when (log.direction) {
                                    "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived
                                    "MISSED" -> Icons.AutoMirrored.Filled.CallMissed
                                    else -> Icons.AutoMirrored.Filled.CallMade
                                }
                                val dirTint = if (log.direction == "MISSED") MaterialTheme.colorScheme.error
                                else Color(0xFF25D366)

                                Icon(
                                    imageVector = dirIcon,
                                    contentDescription = log.direction,
                                    tint = dirTint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (log.callType == "VIDEO") "Video Call" else "Suara"} • ${formatDurationMmSs(log.durationSec)} • ${formatTimeShort(log.timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (log.callType == "VIDEO") {
                                    onStartVideoCall(matchedContact)
                                } else {
                                    onStartAudioCall(matchedContact)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (log.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Panggil Ulang",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ActiveCallFullScreenOverlay(
    session: ActiveCallSession,
    remoteVideoBitmap: android.graphics.Bitmap? = null,
    packetsTransferred: Int = 0,
    onPushLocalVideoFrame: (android.graphics.Bitmap?) -> Unit = {},
    onAcceptCall: () -> Unit = {},
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    BackHandler(onBack = onEndCall)

    val context = LocalContext.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPerm = result[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(session.isVideoCall) {
        if (session.isVideoCall && !hasCameraPerm) {
            permLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF06483C), Color(0xFF0B141A), Color(0xFF050A0E))
                )
            )
            .testTag("active_call_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "End-to-End Encrypted • ${session.contact.zallId} • Pkt: $packetsTransferred",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = session.contact.displayName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: ${session.contact.zallId} • @${session.contact.username}",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    session.isRingingIncoming -> "📲 Panggilan Masuk dari ${session.contact.zallId}..."
                    session.elapsedSeconds < 2 -> "Menghubungkan ke Zall ID..."
                    else -> "${if (session.isVideoCall) "Video Call HD" else "Panggilan Suara"} • ${formatDurationMmSs(session.elapsedSeconds)}"
                },
                color = Color(0xFF4ADE80),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (remoteVideoBitmap != null && session.isVideoCall) {
                androidx.compose.foundation.Image(
                    bitmap = remoteVideoBitmap.asImageBitmap(),
                    contentDescription = "Video Lawan Bicara",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366).copy(alpha = 0.16f))
                )
                ZallAvatar(
                    name = session.contact.displayName,
                    colorHex = session.contact.avatarColorHex,
                    avatarUri = session.contact.avatarUri,
                    size = 124.dp,
                    isOnline = true
                )
            }
        }

        if (session.isVideoCall && session.isCameraEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 145.dp, end = 16.dp)
                    .size(width = 115.dp, height = 165.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, Color(0xFF25D366), RoundedCornerShape(18.dp))
                    .background(Color(0xFF1F2C34))
            ) {
                if (hasCameraPerm) {
                    CameraXLocalPreview(
                        useFrontCamera = session.useFrontCamera,
                        onFrameCaptured = onPushLocalVideoFrame
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kamera Anda Aktif",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Surface(
            color = Color(0xFF1F2C34).copy(alpha = 0.92f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onToggleSpeaker,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (session.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.16f),
                        contentColor = if (session.isSpeakerOn) Color.Black else Color.White
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Speaker")
                }

                FilledIconButton(
                    onClick = onToggleCamera,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (session.isCameraEnabled) Color.White else Color.White.copy(alpha = 0.16f),
                        contentColor = if (session.isCameraEnabled) Color.Black else Color.White
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (session.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Kamera Video"
                    )
                }

                FilledIconButton(
                    onClick = onToggleMute,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (session.isMuted) Color.White else Color.White.copy(alpha = 0.16f),
                        contentColor = if (session.isMuted) Color.Black else Color.White
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute Mikrofon"
                    )
                }

                if (session.isVideoCall) {
                    FilledIconButton(
                        onClick = onSwitchCamera,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Putar Kamera")
                    }
                }

                if (session.isRingingIncoming) {
                    FilledIconButton(
                        onClick = onAcceptCall,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(62.dp)
                            .testTag("accept_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Terima Panggilan",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                FilledIconButton(
                    onClick = onEndCall,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CallRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(62.dp)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Akhiri Panggilan",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraXLocalPreview(
    useFrontCamera: Boolean,
    onFrameCaptured: (android.graphics.Bitmap?) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(useFrontCamera) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val selector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(previewView) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            try {
                val bmp = previewView.bitmap
                if (bmp != null) {
                    onFrameCaptured(bmp)
                }
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
