package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    val accessTokenCamel: String? = null, // Fallback common camelCase
    val refreshTokenCamel: String? = null
)

@JsonClass(generateAdapter = true)
data class SpeciesDto(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class BreedDto(
    val id: String,
    val name: String,
    @Json(name = "species_id") val speciesId: String
)

@JsonClass(generateAdapter = true)
data class GenderDto(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class StatusDto(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class MediaDto(
    val id: String,
    val url: String,
    @Json(name = "public_id") val publicId: String,
    @Json(name = "type") val type: String,
    @Json(name = "pet_id") val petId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String? = null,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class PetDetailResponse(
    val id: String,
    val name: String? = null,
    val gender: String,
    val status: String,
    val description: String? = null,
    @Json(name = "species_id") val speciesId: String,
    @Json(name = "breed_id") val breedId: String? = null,
    @Json(name = "reporter_id") val reporterId: String,
    @Json(name = "shelter_id") val shelterId: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "last_latitude") val lastLatitude: Double? = null,
    @Json(name = "last_longitude") val lastLongitude: Double? = null,
    @Json(name = "last_geohash") val lastGeohash: String? = null,
    val media: List<MediaDto>
)
