package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.media.ProfileMediaHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun parseHexColor(hex: String, fallback: Color = Color(0xFF0A6E5C)): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val longVal = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(longVal or 0xFF000000L)
        } else {
            Color(longVal)
        }
    } catch (_: Exception) {
        fallback
    }
}

fun formatTimeShort(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDurationMmSs(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}

@Composable
fun ZallAvatar(
    name: String,
    colorHex: String,
    avatarUri: String? = null,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    hasStoryRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseColor = parseHexColor(colorHex)
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "Z" }

    val decodedBase64Bitmap = remember(avatarUri) {
        if (avatarUri != null && avatarUri.startsWith("data:image")) {
            ProfileMediaHelper.decodeBase64ToBitmap(avatarUri)
        } else {
            null
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .then(
                    if (hasStoryRing) {
                        Modifier.border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF25D366), Color(0xFF0A6E5C), Color(0xFF38BDF8))
                            ),
                            shape = CircleShape
                        )
                    } else Modifier
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(baseColor, baseColor.copy(alpha = 0.78f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                decodedBase64Bitmap != null -> {
                    Image(
                        bitmap = decodedBase64Bitmap.asImageBitmap(),
                        contentDescription = "Foto Profil $name",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                !avatarUri.isNullOrBlank() -> {
                    val modelData: Any = if (avatarUri.startsWith("/")) File(avatarUri) else avatarUri
                    AsyncImage(
                        model = modelData,
                        contentDescription = "Foto Profil $name",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.36f).sp
                    )
                }
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.28f).dp.coerceAtLeast(11.dp))
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
