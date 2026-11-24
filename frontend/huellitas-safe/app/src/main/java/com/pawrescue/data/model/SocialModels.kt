package com.pawrescue.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Avistamiento colaborativo ("¡Vi a tu mascota aquí!") con ubicación GPS y foto opcional.
 */
@Entity(
    tableName = "pet_sightings",
    indices = [Index(value = ["petId"])]
)
@JsonClass(generateAdapter = true)
data class PetSightingEntity(
    @PrimaryKey val id: String,
    @Json(name = "pet_id") val petId: String,
    @Json(name = "reporter_id") val reporterId: String,
    @Json(name = "reporter_name") val reporterName: String? = null,
    val notes: String? = null,
    val latitude: Double,
    val longitude: Double,
    val geohash: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "sighted_at") val sightedAt: String,
    @Json(name = "is_confirmed_by_owner") val isConfirmedByOwner: Boolean = false,
    @Json(name = "created_at") val createdAt: String,
    val isSynced: Boolean = true
)

/**
 * Conversación contextual entre usuarios, refugios y veterinarias.
 */
@Entity(
    tableName = "conversations",
    indices = [Index(value = ["petId"]), Index(value = ["lastMessageAt"])]
)
@JsonClass(generateAdapter = true)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String, // PET_SIGHTING, ADOPTION_INQUIRY, SHELTER_SUPPORT, VET_CONSULT
    val status: String = "OPEN", // OPEN, CLOSED, ARCHIVED
    @Json(name = "pet_id") val petId: String? = null,
    @Json(name = "pet_name") val petName: String? = null,
    @Json(name = "shelter_id") val shelterId: String? = null,
    @Json(name = "veterinary_id") val veterinaryId: String? = null,
    @Json(name = "counterpart_name") val counterpartName: String? = null,
    @Json(name = "last_message_preview") val lastMessagePreview: String? = null,
    @Json(name = "last_message_at") val lastMessageAt: String? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

/**
 * Mensaje individual (texto, imagen o pin de ubicación GPS) dentro de una conversación.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "createdAt"])]
)
@JsonClass(generateAdapter = true)
data class MessageEntity(
    @PrimaryKey val id: String,
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String? = null,
    val type: String = "TEXT", // TEXT, IMAGE, LOCATION, SYSTEM
    val content: String? = null,
    @Json(name = "media_url") val mediaUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    @Json(name = "location_label") val locationLabel: String? = null,
    @Json(name = "created_at") val createdAt: String,
    val isSynced: Boolean = true
)

/**
 * Solicitud formal de adopción vinculada a una mascota y a su conversación.
 */
@Entity(
    tableName = "adoption_requests",
    indices = [Index(value = ["petId"]), Index(value = ["status"])]
)
@JsonClass(generateAdapter = true)
data class AdoptionRequestEntity(
    @PrimaryKey val id: String,
    @Json(name = "pet_id") val petId: String,
    @Json(name = "pet_name") val petName: String? = null,
    @Json(name = "applicant_id") val applicantId: String,
    @Json(name = "applicant_name") val applicantName: String? = null,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "shelter_id") val shelterId: String? = null,
    @Json(name = "conversation_id") val conversationId: String? = null,
    val status: String = "PENDING", // PENDING, IN_REVIEW, APPROVED, REJECTED, CANCELLED
    val notes: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

// --- Request DTOs para futuros endpoints sociales ---

@JsonClass(generateAdapter = true)
data class CreateSightingRequest(
    @Json(name = "pet_id") val petId: String,
    val notes: String? = null,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "image_public_id") val imagePublicId: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateAdoptionStatusRequest(
    val status: String
)

@JsonClass(generateAdapter = true)
data class CreateConversationRequest(
    val type: String,
    @Json(name = "pet_id") val petId: String? = null,
    @Json(name = "shelter_id") val shelterId: String? = null,
    @Json(name = "veterinary_id") val veterinaryId: String? = null,
    @Json(name = "target_user_id") val targetUserId: String? = null,
    @Json(name = "initial_message") val initialMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    val type: String = "TEXT",
    val content: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "location_label") val locationLabel: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateAdoptionRequestDto(
    @Json(name = "pet_id") val petId: String,
    @Json(name = "shelter_id") val shelterId: String? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterDeviceTokenRequest(
    @Json(name = "fcm_token") val fcmToken: String,
    val platform: String = "ANDROID"
)

@JsonClass(generateAdapter = true)
data class BlockUserRequest(
    @Json(name = "blocked_user_id") val blockedUserId: String,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentReportRequest(
    @Json(name = "target_type") val targetType: String, // USER, MESSAGE, SIGHTING, PET
    @Json(name = "target_id") val targetId: String,
    val reason: String
)
