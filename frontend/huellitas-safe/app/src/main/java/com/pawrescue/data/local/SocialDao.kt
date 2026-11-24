package com.pawrescue.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pawrescue.data.model.AdoptionRequestEntity
import com.pawrescue.data.model.ConversationEntity
import com.pawrescue.data.model.MessageEntity
import com.pawrescue.data.model.PetSightingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {
    // --- Avistamientos ---
    @Query("SELECT * FROM pet_sightings WHERE petId = :petId ORDER BY createdAt DESC")
    fun getSightingsForPetFlow(petId: String): Flow<List<PetSightingEntity>>

    @Query("SELECT * FROM pet_sightings WHERE isSynced = 0")
    suspend fun getUnsyncedSightings(): List<PetSightingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSightings(sightings: List<PetSightingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSighting(sighting: PetSightingEntity)

    // --- Conversaciones ---
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC, updatedAt DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    // --- Mensajes ---
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversationFlow(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // --- Solicitudes de Adopción ---
    @Query("SELECT * FROM adoption_requests ORDER BY updatedAt DESC")
    fun getAllAdoptionRequestsFlow(): Flow<List<AdoptionRequestEntity>>

    @Query("SELECT * FROM adoption_requests WHERE petId = :petId ORDER BY createdAt DESC")
    fun getAdoptionRequestsForPetFlow(petId: String): Flow<List<AdoptionRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdoptionRequests(requests: List<AdoptionRequestEntity>)
}
