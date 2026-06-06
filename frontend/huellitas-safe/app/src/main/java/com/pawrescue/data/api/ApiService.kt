package com.pawrescue.data.api

import com.pawrescue.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- AUTHENTICATION ---
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<TokenResponse>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    // --- PETS CRUD ---
    @GET("api/v1/pets")
    suspend fun listPets(): Response<List<PetDetailResponse>>

    @GET("api/v1/pets/{id}")
    suspend fun getPetById(
        @Path("id") id: String
    ): Response<PetDetailResponse>

    @Multipart
    @POST("api/v1/pets")
    suspend fun createPet(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): Response<ResponseBody>

    @Multipart
    @PUT("api/v1/pets/{id}")
    suspend fun updatePet(
        @Path("id") id: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): Response<ResponseBody>

    @DELETE("api/v1/pets/{id}")
    suspend fun deletePet(
        @Path("id") id: String
    ): Response<ResponseBody>

    // --- CATALOGS ---
    @GET("api/v1/catalogs/species")
    suspend fun getSpecies(): Response<List<SpeciesDto>>

    @GET("api/v1/catalogs/breeds")
    suspend fun getBreeds(): Response<List<BreedDto>>

    @GET("api/v1/catalogs/species/{id}/breeds")
    suspend fun getBreedsBySpecies(
        @Path("id") speciesId: String
    ): Response<List<BreedDto>>

    @GET("api/v1/catalogs/genders")
    suspend fun getGenders(): Response<List<GenderDto>>

    @GET("api/v1/catalogs/statuses")
    suspend fun getStatuses(): Response<List<StatusDto>>
}
