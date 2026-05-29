package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.local.PetRegistrationDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class PetRepository(
    private val dao: PetRegistrationDao,
    private val themeDao: com.example.data.local.ThemeDao,
    private val catalogDao: com.example.data.local.CatalogDao,
    private val apiService: ApiService
) {

    val allRegistrations: Flow<List<PetRegistration>> = dao.getAllRegistrationsFlow()
    val selectedTheme: Flow<AppTheme?> = themeDao.getSelectedTheme()

    val speciesCatalog: Flow<List<SpeciesDto>> = catalogDao.getAllSpeciesFlow()
    val gendersCatalog: Flow<List<GenderDto>> = catalogDao.getAllGendersFlow()
    val statusesCatalog: Flow<List<StatusDto>> = catalogDao.getAllStatusesFlow()
    val allBreedsCatalog: Flow<List<BreedDto>> = catalogDao.getAllBreedsFlow()

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
                
                // Rust seeder default IDs
                // Perro default ID: 5fd52847-6b3b-5cc5-aa38-0849117cb69a
                // Gato default ID: cf8bc1e1-43c2-5bf0-a8fb-1eea7221dcc3
                val isGato = pet.description.contains("gato", ignoreCase = true) || 
                             pet.name.contains("gato", ignoreCase = true)
                val speciesId = if (isGato) "cf8bc1e1-43c2-5bf0-a8fb-1eea7221dcc3" else "5fd52847-6b3b-5cc5-aa38-0849117cb69a"
                
                // Mapear el género y el estado
                val isMacho = pet.description.contains("macho", ignoreCase = true) || 
                              pet.description.contains("perrito", ignoreCase = true)
                val gender = if (isMacho) "MACHO" else "HEMBRA"
                
                // Mapear estado
                val isLost = pet.description.contains("perdido", ignoreCase = true)
                val status = if (isLost) "LOST" else "FOUND"

                fields["gender"] = toRequestBody(gender)
                fields["status"] = toRequestBody(status)
                fields["species_id"] = toRequestBody(speciesId)
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
}
