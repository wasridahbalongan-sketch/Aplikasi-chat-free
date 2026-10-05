package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZallCallDao {
    // Account
    @Query("SELECT * FROM user_account LIMIT 1")
    fun observeCurrentUser(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_account LIMIT 1")
    suspend fun getCurrentUserOnce(): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserAccount(user: UserAccountEntity)

    @Query("DELETE FROM user_account")
    suspend fun logoutUser()

    // Contacts (Pure real user-added or Firestore-connected contacts, no dummy seed!)
    @Query("SELECT * FROM contacts ORDER BY isPinned DESC, displayName ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE contactId = :contactId OR zallId = :contactId LIMIT 1")
    suspend fun getContactById(contactId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE contactId = :contactId")
    suspend fun deleteContact(contactId: String)

    @Query("UPDATE contacts SET unreadCount = 0 WHERE contactId = :contactId")
    suspend fun clearUnreadCount(contactId: String)

    @Query("UPDATE contacts SET isPinned = :pinned WHERE contactId = :contactId")
    suspend fun setContactPinned(contactId: String, pinned: Boolean)

    // Messages
    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun observeMessagesForContact(contactId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE packetId = :packetId LIMIT 1")
    suspend fun getMessageByPacketId(packetId: String): MessageEntity?

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :msgId")
    suspend fun updateMessageStatus(msgId: Long, status: String)

    @Query("UPDATE messages SET isStarred = :starred WHERE id = :msgId")
    suspend fun toggleMessageStarred(msgId: Long, starred: Boolean)

    @Query("DELETE FROM messages WHERE id = :msgId")
    suspend fun deleteMessageById(msgId: Long)

    // Status Stories (SW)
    @Query("SELECT * FROM status_stories ORDER BY createdAt DESC")
    fun observeAllStories(): Flow<List<StatusStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StatusStoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StatusStoryEntity>)

    @Query("UPDATE status_stories SET isViewedByMe = 1, viewsCount = viewsCount + 1 WHERE id = :storyId AND isViewedByMe = 0")
    suspend fun markStoryViewed(storyId: Long)

    @Query("UPDATE status_stories SET isLikedByMe = :liked WHERE id = :storyId")
    suspend fun setStoryLiked(storyId: Long, liked: Boolean)

    @Query("DELETE FROM status_stories WHERE id = :storyId")
    suspend fun deleteStory(storyId: Long)

    // Call Logs
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun observeCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(callLogs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()

    // Server Sync Logs
    @Query("SELECT * FROM server_sync_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeServerLogs(): Flow<List<ServerSyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerLog(log: ServerSyncLogEntity)

    @Query("DELETE FROM server_sync_logs")
    suspend fun clearServerLogs()
}

@Database(
    entities = [
        UserAccountEntity::class,
        ContactEntity::class,
        MessageEntity::class,
        StatusStoryEntity::class,
        CallLogEntity::class,
        ServerSyncLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ZallCallDatabase : RoomDatabase() {
    abstract fun dao(): ZallCallDao

    companion object {
        @Volatile
        private var INSTANCE: ZallCallDatabase? = null

        fun getInstance(context: Context): ZallCallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZallCallDatabase::class.java,
                    "zallcall_v2_id.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
