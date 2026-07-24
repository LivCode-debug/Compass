package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val domainMode: String = "COMMUNITY",
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE domainMode = :domain ORDER BY timestamp ASC")
    fun getMessagesByDomain(domain: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE isUser = 1 ORDER BY timestamp DESC LIMIT 20")
    fun getRecentUserMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE domainMode = :domain")
    suspend fun clearHistory(domain: String)
}

@Database(entities = [ChatMessage::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}

class ChatRepository(private val dao: ChatMessageDao) {
    fun getRecentUserMessages(): Flow<List<ChatMessage>> = dao.getRecentUserMessages()

    fun getMessagesForDomain(domain: String): Flow<List<ChatMessage>> = dao.getMessagesByDomain(domain)

    suspend fun insert(message: ChatMessage) = dao.insertMessage(message)

    suspend fun clear(domain: String) = dao.clearHistory(domain)
}
