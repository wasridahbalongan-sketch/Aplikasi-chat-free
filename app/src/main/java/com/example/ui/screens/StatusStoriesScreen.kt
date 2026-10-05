package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.StatusStoryEntity
import com.example.data.UserAccountEntity
import com.example.ui.components.ZallAvatar
import com.example.ui.components.formatTimeShort
import com.example.ui.components.parseHexColor
import kotlinx.coroutines.delay

@Composable
fun StatusStoriesScreen(
    currentUser: UserAccountEntity?,
    stories: List<StatusStoryEntity>,
    onCreateStory: (caption: String, bgHex: String, imageUri: String?) -> Unit,
    onOpenStory: (StatusStoryEntity) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var pickedPhotoUri by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri.toString()
            showCreateDialog = true
        }
    }

    val myStories = stories.filter { it.authorId == "ME" }
    val otherStories = stories.filter { it.authorId != "ME" }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("status_stories_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // My Status Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStories.isNotEmpty()) {
                                onOpenStory(myStories.first())
                            } else {
                                showCreateDialog = true
                            }
                        }
                        .testTag("my_status_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            ZallAvatar(
                                name = currentUser?.fullName ?: "Saya",
                                colorHex = currentUser?.avatarColorHex ?: "#0A6E5C",
                                avatarUri = currentUser?.avatarUri,
                                size = 58.dp,
                                hasStoryRing = myStories.isNotEmpty()
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366))
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .clickable { showCreateDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah SW",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Status Saya (SW)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (myStories.isNotEmpty()) {
                                    "${myStories.size} status aktif • Ketuk untuk melihat (${formatTimeShort(myStories.first().createdAt)})"
                                } else {
                                    "Ketuk untuk membuat Status (SW) teks atau foto"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showCreateDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("create_sw_header_button")
                        ) {
                            Text("+ Buat SW")
                        }
                    }
                }
            }

            if (myStories.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SW SAYA YANG AKTIF",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(myStories, key = { it.id }) { story ->
                    StoryRowCard(
                        story = story,
                        onClick = { onOpenStory(story) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PEMBARUAN STATUS KONTAK (${otherStories.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(otherStories, key = { it.id }) { story ->
                StoryRowCard(
                    story = story,
                    onClick = { onOpenStory(story) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // Dual FABs for Photo SW and Text SW
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.testTag("fab_photo_sw")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "SW Foto")
            }

            FloatingActionButton(
                onClick = {
                    pickedPhotoUri = null
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_text_sw")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Buat Status Teks")
            }
        }
    }

    if (showCreateDialog) {
        CreateStatusDialog(
            initialImageUri = pickedPhotoUri,
            onPickImage = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = {
                showCreateDialog = false
                pickedPhotoUri = null
            },
            onPost = { caption, bgHex, imgUri ->
                onCreateStory(caption, bgHex, imgUri)
                showCreateDialog = false
                pickedPhotoUri = null
            }
        )
    }
}

@Composable
private fun StoryRowCard(
    story: StatusStoryEntity,
    onClick: () -> Unit
) {
    val bgColor = parseHexColor(story.backgroundHex)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("story_item_${story.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Story preview circle with gradient ring
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(
                        width = if (story.isViewedByMe) 1.5.dp else 3.dp,
                        color = if (story.isViewedByMe) MaterialTheme.colorScheme.outline
                        else Color(0xFF25D366),
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (!story.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = story.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = story.captionText.take(6),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = story.authorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = story.captionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${story.viewsCount} tayangan • ${formatTimeShort(story.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateStatusDialog(
    initialImageUri: String?,
    onPickImage: () -> Unit,
    onDismiss: () -> Unit,
    onPost: (caption: String, bgHex: String, imageUri: String?) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    val bgPalette = listOf("#0A6E5C", "#5B21B6", "#0369A1", "#BE123C", "#B45309", "#1E293B")
    var selectedBg by remember { mutableStateOf(bgPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Status (SW) ZallCall", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Live Canvas Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(parseHexColor(selectedBg)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!initialImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = initialImageUri,
                            contentDescription = "Foto SW",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                    Text(
                        text = caption.ifBlank { "Ketik status Anda di bawah..." },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Background color selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bgPalette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .then(
                                        if (selectedBg == hex) Modifier.border(2.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedBg = hex }
                            )
                        }
                    }

                    TextButton(onClick = onPickImage) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Foto")
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Teks / Caption Status (SW)") },
                    placeholder = { Text("Bagikan momen hari ini...") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sw_caption_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caption.isNotBlank() || !initialImageUri.isNullOrBlank()) {
                        onPost(caption, selectedBg, initialImageUri)
                    }
                },
                modifier = Modifier.testTag("sw_publish_button")
            ) {
                Text("Tayangkan ke Server")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun FullScreenStoryViewerModal(
    story: StatusStoryEntity,
    onClose: () -> Unit,
    onToggleLike: () -> Unit,
    onReplyStory: (String) -> Unit,
    onDeleteStory: () -> Unit
) {
    BackHandler(onBack = onClose)

    var replyText by remember { mutableStateOf("") }
    var targetProgress by remember(story.id) { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 6500, easing = LinearEasing),
        label = "story_progress"
    )

    LaunchedEffect(story.id) {
        targetProgress = 1f
        delay(6800L)
        if (replyText.isBlank()) {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        parseHexColor(story.backgroundHex),
                        Color(0xFF090D10)
                    )
                )
            )
            .imePadding()
            .testTag("fullscreen_story_viewer")
    ) {
        if (!story.imageUri.isNullOrBlank()) {
            AsyncImage(
                model = story.imageUri,
                contentDescription = "Gambar Status",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }

        // Top Bar with Progress & Author
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = Color(0xFF25D366),
                trackColor = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZallAvatar(
                    name = story.authorName,
                    colorHex = story.authorAvatarColor,
                    avatarUri = story.authorAvatarUri,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.authorName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Hari ini, ${formatTimeShort(story.createdAt)} • ${story.viewsCount} tayangan",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                if (story.authorId == "ME") {
                    IconButton(onClick = onDeleteStory) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus SW",
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_story_viewer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup SW",
                        tint = Color.White
                    )
                }
            }
        }

        // Centered Story Text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = story.captionText,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
        }

        // Bottom Reply & Like Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text("Balas Status (SW)...", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.16f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = if (story.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Suka SW",
                    tint = if (story.isLikedByMe) Color(0xFFEF4444) else Color.White
                )
            }

            if (replyText.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onReplyStory(replyText) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim Balasan SW",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
