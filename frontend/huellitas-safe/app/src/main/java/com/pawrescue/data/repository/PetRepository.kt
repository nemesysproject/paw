package com.pawrescue.data.repository

import com.pawrescue.data.api.ApiService
import com.pawrescue.data.local.AdDao
import com.pawrescue.data.local.PetRegistrationDao
import com.pawrescue.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class PetRepository(
    private val dao: PetRegistrationDao,
    private val themeDao: com.pawrescue.data.local.ThemeDao,
    private val catalogDao: com.pawrescue.data.local.CatalogDao,
    private val apiService: ApiService,
    private val adDao: AdDao? = null
) {

    val allRegistrations: Flow<List<PetRegistration>> = dao.getAllRegistrationsFlow()
    val selectedTheme: Flow<AppTheme?> = themeDao.getSelectedTheme()

    val speciesCatalog: Flow<List<SpeciesDto>> = catalogDao.getAllSpeciesFlow()
    val gendersCatalog: Flow<List<GenderDto>> = catalogDao.getAllGendersFlow()
    val statusesCatalog: Flow<List<StatusDto>> = catalogDao.getAllStatusesFlow()
    val allBreedsCatalog: Flow<List<BreedDto>> = catalogDao.getAllBreedsFlow()
    val activeAds: Flow<List<AdFeedItemDto>> = adDao?.getAllAdsFlow() ?: emptyFlow()

    fun getBreedsBySpecies(speciesId: String): Flow<List<BreedDto>> = catalogDao.getBreedsBySpeciesFlow(speciesId)

    suspend fun login(email: String, password: String) = apiService.login(LoginRequest(email, password))

    suspend fun register(name: String, email: String, password: String) = apiService.register(RegisterRequest(email, password, name))

    suspend fun insertRegistration(registration: PetRegistration): Long {
        return dao.insertRegistration(registration)
    }

    suspend fun deleteRegistration(id: Int) {
        val pet = dao.getAllRegistrationsFlow().toString() // Check if we can find it
        // We will query to delete on local DB
        dao.deleteRegistration(id)
    }

    suspend fun deleteRemotePet(remoteId: String) {
        try {
            apiService.deletePet(remoteId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAsSynced(id: Int) {
        dao.markAsSynced(id)
    }

    suspend fun updateRemoteIdAndSync(id: Int, remoteId: String) {
        dao.updateRemoteIdAndSync(id, remoteId)
    }

    // --- CATALOGS (Sync Logic) ---
    suspend fun syncCatalogs() {
        try {
            // Species
            val remoteSpecies = apiService.getSpecies().body() ?: emptyList()
            catalogDao.insertSpecies(remoteSpecies)

            // Breeds
            val remoteBreeds = apiService.getBreeds().body() ?: emptyList()
            catalogDao.insertBreeds(remoteBreeds)

            // Genders
            val remoteGenders = apiService.getGenders().body() ?: emptyList()
            catalogDao.insertGenders(remoteGenders)

            // Statuses
            val remoteStatuses = apiService.getStatuses().body() ?: emptyList()
            catalogDao.insertStatuses(remoteStatuses)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FETCH REMOTE PETS ---
    suspend fun fetchRemotePets(): List<PetDetailResponse> {
        val response = apiService.listPets()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        }
        return emptyList()
    }

    // --- SYNC PENDING PETS WITH BACKEND ---
    suspend fun syncPendingRegistrations(reporterId: String, onProgress: (String) -> Unit): Int {
        val unsynced = dao.getUnsyncedRegistrations()
        if (unsynced.isEmpty()) {
            return 0
        }

        var successCount = 0
        onProgress("Iniciando sincronización de ${unsynced.size} registros...")

        for (pet in unsynced) {
            onProgress("Sincronizando: ${pet.name}...")
            try {
                val fields = mutableMapOf<String, RequestBody>()
                
                fun toRequestBody(text: String): RequestBody {
                    return text.toRequestBody("text/plain".toMediaTypeOrNull())
                }

                // Map local properties to API fields
                fields["name"] = toRequestBody(pet.name)
                fields["description"] = toRequestBody(pet.description)
                
                // Usar los catálogos reales guardados en Room (con normalización defensiva para registros antiguos)
                val defaultDogSpeciesId = "5fd52847-6b3b-5cc5-aa38-0849117cb69a"
                val defaultCatSpeciesId = "cf8bc1e1-43c2-5bf0-a8fb-1eea7221dcc3"
                val speciesId = pet.speciesId?.takeIf { it.isNotBlank() } ?: run {
                    val isGato = pet.description.contains("gato", ignoreCase = true) ||
                        pet.name.contains("gato", ignoreCase = true)
                    if (isGato) defaultCatSpeciesId else defaultDogSpeciesId
                }

                val gender = when (pet.gender?.trim()?.uppercase()) {
                    "MALE", "MACHO" -> "MALE"
                    "FEMALE", "HEMBRA" -> "FEMALE"
                    else -> "UNKNOWN"
                }

                val status = when (pet.status?.trim()?.uppercase()) {
                    "LOST", "PERDIDO", "PERDIDA" -> "LOST"
                    "ADOPTION", "ADOPCIÓN", "ADOPCION" -> "ADOPTION"
                    "STREET", "CALLE" -> "STREET"
                    "AT_RISK", "IN_DANGER", "EN RIESGO" -> "AT_RISK"
                    "SAFE", "FOUND", "A SALVO", "ENCONTRADO" -> "SAFE"
                    else -> "LOST"
                }

                fields["gender"] = toRequestBody(gender)
                fields["status"] = toRequestBody(status)
                fields["species_id"] = toRequestBody(speciesId)
                if (!pet.breedId.isNullOrBlank()) {
                    fields["breed_id"] = toRequestBody(pet.breedId)
                }
                fields["reporter_id"] = toRequestBody(reporterId.ifBlank { "admin-id" })
                fields["last_latitude"] = toRequestBody(pet.latitude.toString())
                fields["last_longitude"] = toRequestBody(pet.longitude.toString())

                // Process photos
                val parts = mutableListOf<MultipartBody.Part>()
                val photoPaths = pet.photosJson.split(",").filter { it.isNotBlank() }
                for (path in photoPaths) {
                    val file = File(path)
                    if (file.exists()) {
                        val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("file", file.name, reqFile)
                        parts.add(part)
                    }
                }

                // Process video
                if (!pet.videoPath.isNullOrBlank()) {
                    val file = File(pet.videoPath)
                    if (file.exists()) {
                        val reqFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("file", file.name, reqFile)
                        parts.add(part)
                    }
                }

                // Send to backend
                val response = apiService.createPet(fields, parts)
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()
                    if (bodyString != null) {
                        val json = JSONObject(bodyString)
                        val remoteId = json.getString("id")
                        dao.updateRemoteIdAndSync(pet.id, remoteId)
                        successCount++
                        onProgress("Mascota '${pet.name}' sincronizada exitosamente.")
                    } else {
                        onProgress("Error al sincronizar '${pet.name}': Respuesta vacía.")
                    }
                } else {
                    val errString = response.errorBody()?.string() ?: "Error desconocido"
                    onProgress("Error al sincronizar '${pet.name}': $errString")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onProgress("Fallo al sincronizar '${pet.name}': ${e.localizedMessage}")
            }
        }

        onProgress("Sincronización finalizada. $successCount registros sincronizados.")
        return successCount
    }

    // --- INSTANT CREATION/UPDATE VIA API REST ---
    suspend fun createPetRemote(
        name: String,
        description: String,
        gender: String,
        status: String,
        speciesId: String,
        breedId: String?,
        reporterId: String,
        latitude: Double,
        longitude: Double,
        photoPaths: List<String>,
        videoPath: String?
    ): Boolean {
        return try {
            val fields = mutableMapOf<String, RequestBody>()
            fun toRequestBody(text: String): RequestBody {
                return text.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            fields["name"] = toRequestBody(name)
            fields["description"] = toRequestBody(description)
            fields["gender"] = toRequestBody(gender)
            fields["status"] = toRequestBody(status)
            fields["species_id"] = toRequestBody(speciesId)
            if (!breedId.isNullOrBlank()) {
                fields["breed_id"] = toRequestBody(breedId)
            }
            fields["reporter_id"] = toRequestBody(reporterId)
            fields["last_latitude"] = toRequestBody(latitude.toString())
            fields["last_longitude"] = toRequestBody(longitude.toString())

            val parts = mutableListOf<MultipartBody.Part>()
            for (path in photoPaths) {
                val file = File(path)
                if (file.exists()) {
                    val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    parts.add(MultipartBody.Part.createFormData("file", file.name, reqFile))
                }
            }

            if (!videoPath.isNullOrBlank()) {
                val file = File(videoPath)
                if (file.exists()) {
                    val reqFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                    parts.add(MultipartBody.Part.createFormData("file", file.name, reqFile))
                }
            }

            val response = apiService.createPet(fields, parts)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updatePetRemote(
        remoteId: String,
        name: String,
        description: String,
        gender: String,
        status: String,
        speciesId: String,
        breedId: String?,
        latitude: Double,
        longitude: Double,
        newPhotoPaths: List<String>
    ): Boolean {
        return try {
            val fields = mutableMapOf<String, RequestBody>()
            fun toRequestBody(text: String): RequestBody {
                return text.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            fields["name"] = toRequestBody(name)
            fields["description"] = toRequestBody(description)
            fields["gender"] = toRequestBody(gender)
            fields["status"] = toRequestBody(status)
            fields["species_id"] = toRequestBody(speciesId)
            if (!breedId.isNullOrBlank()) {
                fields["breed_id"] = toRequestBody(breedId)
            }
            fields["last_latitude"] = toRequestBody(latitude.toString())
            fields["last_longitude"] = toRequestBody(longitude.toString())

            val parts = mutableListOf<MultipartBody.Part>()
            for (path in newPhotoPaths) {
                val file = File(path)
                if (file.exists()) {
                    val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    parts.add(MultipartBody.Part.createFormData("file", file.name, reqFile))
                }
            }

            val response = apiService.updatePet(remoteId, fields, parts)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletePetRemote(remoteId: String): Boolean {
        return try {
            val response = apiService.deletePet(remoteId)
            if (response.isSuccessful) {
                dao.deleteByRemoteId(remoteId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- THEME ---
    suspend fun setTheme(themeName: String) {
        themeDao.setTheme(AppTheme(themeName = themeName))
    }

    // --- ADS & CAMPAIGNS ---
    suspend fun syncAds(
        lat: Double? = null,
        lon: Double? = null,
        radiusKm: Double? = 15.0,
        limit: Int = 20
    ): List<AdFeedItemDto> {
        return try {
            val response = apiService.getActiveAds(
                lat = lat,
                lon = lon,
                radiusKm = radiusKm,
                limit = limit
            )
            if (response.isSuccessful) {
                val ads = response.body() ?: emptyList()
                adDao?.replaceAllAds(ads)
                ads
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun recordAdEvent(
        campaignId: String,
        eventType: String,
        userId: String? = null,
        lat: Double? = null,
        lon: Double? = null
    ) {
        try {
            apiService.recordAdEvent(
                campaignId = campaignId,
                request = RecordAdEventRequest(
                    eventType = eventType,
                    userId = userId,
                    latitude = lat,
                    longitude = lon
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
