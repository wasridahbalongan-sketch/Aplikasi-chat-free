package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val zallId: String, // Unique ID e.g. ZALL-PRA84K92 (no phone numbers!)
    val fullName: String,
    val username: String,
    val email: String = "",
    val bio: String,
    val avatarColorHex: String,
    val avatarUri: String? = null, // Local file path or Base64 data URI for Profile Picture (PP)
    val authToken: String,
    val serverEndpoint: String,
    val registeredAt: Long,
    val isOnline: Boolean = true
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val contactId: String, // Matches target user's zallId
    val zallId: String, // e.g., ZALL-9284XK
    val username: String, // e.g., @rizky_dev
    val displayName: String,
    val bio: String,
    val avatarColorHex: String,
    val avatarUri: String? = null, // Custom PP uri or Base64
    val isOnline: Boolean,
    val lastSeenText: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isVerifiedServerUser: Boolean = true
)

/**
 * Message types:
 * "TEXT", "VN" (Voice Note), "IMAGE", "CALL_EVENT"
 * Delivery statuses:
 * "SENDING", "SENT", "DELIVERED", "READ"
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packetId: String,
    val contactId: String, // Zall ID of the conversation partner
    val senderId: String, // "ME" or contactId (Zall ID)
    val messageType: String, // "TEXT", "VN", "IMAGE", "CALL_EVENT"
    val textContent: String,
    val mediaPath: String? = null,
    val vnDurationSec: Int = 0,
    val vnWaveformCsv: String = "", // comma-separated amplitudes 0..100
    val replyToMessageId: Long? = null,
    val replyToPreview: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryStatus: String = "READ", // SENDING, SENT, DELIVERED, READ
    val isStarred: Boolean = false
)

@Entity(tableName = "status_stories")
data class StatusStoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: String, // "ME" or contact's Zall ID
    val authorZallId: String = "",
    val authorName: String,
    val authorAvatarColor: String,
    val authorAvatarUri: String? = null,
    val captionText: String,
    val backgroundHex: String,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val viewsCount: Int = 0,
    val isViewedByMe: Boolean = false,
    val isLikedByMe: Boolean = false
)

/**
 * Call types: "AUDIO", "VIDEO"
 * Call directions: "OUTGOING", "INCOMING", "MISSED"
 */
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: String,
    val contactName: String,
    val contactZallId: String,
    val avatarColorHex: String,
    val avatarUri: String? = null,
    val callType: String, // "AUDIO" or "VIDEO"
    val direction: String, // "OUTGOING", "INCOMING", "MISSED"
    val durationSec: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val serverSessionId: String
)

@Entity(tableName = "server_sync_logs")
data class ServerSyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // "FIREBASE_AUTH", "FIRESTORE_SYNC", "WS_MESSAGE_PUSH", "VN_UPLOAD", "SW_BROADCAST", "WEBRTC_SIGNAL"
    val endpoint: String,
    val statusCode: Int,
    val payloadSummary: String,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
