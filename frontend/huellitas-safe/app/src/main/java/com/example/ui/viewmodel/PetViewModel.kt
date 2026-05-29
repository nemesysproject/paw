package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.model.*
import com.example.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetViewModel(
    private val repository: PetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- SQLite LOCAL REGISTRATIONS ---
    val registrations: StateFlow<List<PetRegistration>> = repository.allRegistrations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- NETWORK & ONLINE STATUS ---
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // --- BIOMETRIC STATE ---
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // --- AUTH BACKEND STATE ---
    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthProcessing = MutableStateFlow(false)
    val isAuthProcessing: StateFlow<Boolean> = _isAuthProcessing.asStateFlow()

    val currentUserEmail: String? get() = sessionManager.getUserEmail()
    val currentUserName: String? get() = sessionManager.getUserName()
    val currentUserId: String? get() = sessionManager.getUserId()

    // --- SYNC STATUS ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // --- CATALOG STATES ---
    private val _speciesCatalog = MutableStateFlow<List<SpeciesDto>>(emptyList())
    val speciesCatalog: StateFlow<List<SpeciesDto>> = _speciesCatalog.asStateFlow()

    private val _gendersCatalog = MutableStateFlow<List<GenderDto>>(emptyList())
    val gendersCatalog: StateFlow<List<GenderDto>> = _gendersCatalog.asStateFlow()

    private val _statusesCatalog = MutableStateFlow<List<StatusDto>>(emptyList())
    val statusesCatalog: StateFlow<List<StatusDto>> = _statusesCatalog.asStateFlow()

    private val _breedsCatalog = MutableStateFlow<List<BreedDto>>(emptyList())
    val breedsCatalog: StateFlow<List<BreedDto>> = _breedsCatalog.asStateFlow()

    // --- REMOTE PETS STATE ---
    private val _remotePets = MutableStateFlow<List<PetDetailResponse>>(emptyList())
    val remotePets: StateFlow<List<PetDetailResponse>> = _remotePets.asStateFlow()

    // --- FORM TEMPORARY FIELDS ---
    private val _formPhotos = MutableStateFlow<List<String>>(emptyList())
    val formPhotos: StateFlow<List<String>> = _formPhotos.asStateFlow()

    private val _formVideoPath = MutableStateFlow<String?>(null)
    val formVideoPath: StateFlow<String?> = _formVideoPath.asStateFlow()

    init {
        if (_isLoggedIn.value) {
            loadCatalogs()
            fetchRemotePets()
        }
    }

    // --- ACTIONS ---
    fun setAuthenticated(auth: Boolean) {
        _isAuthenticated.value = auth
    }

    fun toggleNetwork(online: Boolean) {
        _isOnline.value = online
        if (online) {
            triggerSync()
            loadCatalogs()
            fetchRemotePets()
        }
    }

    fun addPhotoToForm(photoPath: String) {
        val current = _formPhotos.value.toMutableList()
        if (current.size < 10) {
            current.add(photoPath)
            _formPhotos.value = current
        }
    }

    fun removePhotoFromForm(index: Int) {
        val current = _formPhotos.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _formPhotos.value = current
        }
    }

    fun setVideoPathInForm(path: String?) {
        _formVideoPath.value = path
    }

    fun clearForm() {
        _formPhotos.value = emptyList()
        _formVideoPath.value = null
    }

    // --- AUTHENTICATION ACTIONS ---
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Los campos de correo y contraseña son requeridos."
            return
        }
        viewModelScope.launch {
            _isAuthProcessing.value = true
            _authError.value = null
            try {
                val response = repository.login(email, password)
                if (response.isSuccessful) {
                    val tokenResp = response.body()
                    val finalAccessToken = tokenResp?.accessToken ?: tokenResp?.accessTokenCamel
                    val finalRefreshToken = tokenResp?.refreshToken ?: tokenResp?.refreshTokenCamel
                    
                    if (!finalAccessToken.isNullOrBlank()) {
                        // Guardar sesión (el reporter id se puede simular o extraer, en este caso guardamos el email como userId)
                        sessionManager.saveSession(
                            accessToken = finalAccessToken,
                            refreshToken = finalRefreshToken ?: "",
                            email = email,
                            name = email.substringBefore("@"),
                            userId = "user-${email.hashCode()}"
                        )
                        _isLoggedIn.value = true
                        _isAuthenticated.value = true // Bypass biometric on login
                        loadCatalogs()
                        fetchRemotePets()
                        onSuccess()
                    } else {
                        _authError.value = "Error al recibir el token de acceso."
                    }
                } else {
                    _authError.value = "Credenciales incorrectas o error en el servidor."
                }
            } catch (e: Exception) {
                _authError.value = "Fallo de conexión: ${e.localizedMessage}"
            } finally {
                _isAuthProcessing.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authError.value = "Todos los campos son obligatorios para el registro."
            return
        }
        viewModelScope.launch {
            _isAuthProcessing.value = true
            _authError.value = null
            try {
                val response = repository.register(name, email, password)
                if (response.isSuccessful) {
                    val tokenResp = response.body()
                    val finalAccessToken = tokenResp?.accessToken ?: tokenResp?.accessTokenCamel
                    val finalRefreshToken = tokenResp?.refreshToken ?: tokenResp?.refreshTokenCamel
                    
                    if (!finalAccessToken.isNullOrBlank()) {
                        sessionManager.saveSession(
                            accessToken = finalAccessToken,
                            refreshToken = finalRefreshToken ?: "",
                            email = email,
                            name = name,
                            userId = "user-${email.hashCode()}"
                        )
                        _isLoggedIn.value = true
                        _isAuthenticated.value = true
                        loadCatalogs()
                        fetchRemotePets()
                        onSuccess()
                    } else {
                        _authError.value = "Cuenta creada pero falló el inicio de sesión automático."
                    }
                } else {
                    _authError.value = "El correo ya está registrado o datos inválidos."
                }
            } catch (e: Exception) {
                _authError.value = "Fallo de conexión: ${e.localizedMessage}"
            } finally {
                _isAuthProcessing.value = false
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _isLoggedIn.value = false
        _isAuthenticated.value = false
        _remotePets.value = emptyList()
    }

    // --- CATALOG LOADER ---
    fun loadCatalogs() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                val species = repository.fetchSpecies()
                if (species.isNotEmpty()) _speciesCatalog.value = species

                val genders = repository.fetchGenders()
                if (genders.isNotEmpty()) _gendersCatalog.value = genders

                val statuses = repository.fetchStatuses()
                if (statuses.isNotEmpty()) _statusesCatalog.value = statuses

                val breeds = repository.fetchAllBreeds()
                if (breeds.isNotEmpty()) _breedsCatalog.value = breeds
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadBreedsForSpecies(speciesId: String) {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                val breeds = repository.fetchBreedsBySpecies(speciesId)
                _breedsCatalog.value = breeds
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- REMOTE PETS LOADER ---
    fun fetchRemotePets() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            try {
                val pets = repository.fetchRemotePets()
                _remotePets.value = pets
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- CREATE PET ACTION ---
    fun savePetRegistration(
        name: String,
        description: String,
        gender: String,
        status: String,
        speciesId: String,
        breedId: String?,
        latitude: Double,
        longitude: Double,
        locationName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val photoPathsString = _formPhotos.value.joinToString(",")
            val reporterId = currentUserId ?: "admin-id"

            if (_isOnline.value) {
                // Sincronización instantánea si estamos online
                _isSyncing.value = true
                val success = repository.createPetRemote(
                    name = name.ifBlank { "Mascota sin Nombre" },
                    description = description.ifBlank { "Sin descripción detallada" },
                    gender = gender,
                    status = status,
                    speciesId = speciesId,
                    breedId = breedId,
                    reporterId = reporterId,
                    latitude = latitude,
                    longitude = longitude,
                    photoPaths = _formPhotos.value,
                    videoPath = _formVideoPath.value
                )
                _isSyncing.value = false

                if (success) {
                    clearForm()
                    fetchRemotePets()
                    onSuccess()
                    return@launch
                }
            }

            // Fallback: Guardar en base de datos local SQLite si falló la red o estamos offline
            val pet = PetRegistration(
                name = name.ifBlank { "Mascota sin Nombre" },
                description = description.ifBlank { "Sin descripción detallada" },
                photosJson = photoPathsString,
                videoPath = _formVideoPath.value,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName.ifBlank { "Ubicación Desconocida" },
                isSynced = false
            )
            repository.insertRegistration(pet)
            clearForm()
            onSuccess()
        }
    }

    // --- UPDATE PET ACTION ---
    fun updatePet(
        remoteId: String?,
        localId: Int,
        name: String,
        description: String,
        gender: String,
        status: String,
        speciesId: String,
        breedId: String?,
        latitude: Double,
        longitude: Double,
        newPhotos: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (!remoteId.isNullOrBlank() && _isOnline.value) {
                _isSyncing.value = true
                val success = repository.updatePetRemote(
                    remoteId = remoteId,
                    name = name,
                    description = description,
                    gender = gender,
                    status = status,
                    speciesId = speciesId,
                    breedId = breedId,
                    latitude = latitude,
                    longitude = longitude,
                    newPhotoPaths = newPhotos
                )
                _isSyncing.value = false
                if (success) {
                    fetchRemotePets()
                    onSuccess()
                    return@launch
                }
            }

            // Si es local o falló la sincronización online, actualizamos localmente
            val updated = PetRegistration(
                id = localId,
                remoteId = remoteId,
                name = name,
                description = description,
                photosJson = newPhotos.joinToString(","),
                videoPath = null,
                latitude = latitude,
                longitude = longitude,
                locationName = "Ubicación Modificada",
                isSynced = false
            )
            repository.insertRegistration(updated) // REPLACE conflict strategy will update it
            onSuccess()
        }
    }

    // --- DELETE PET ACTION ---
    fun deleteRegistration(id: Int, remoteId: String?) {
        viewModelScope.launch {
            if (!remoteId.isNullOrBlank() && _isOnline.value) {
                repository.deletePetRemote(remoteId)
            }
            repository.deleteRegistration(id)
            fetchRemotePets()
        }
    }

    // --- SYNC ACTION ---
    fun triggerSync() {
        if (_isSyncing.value || !_isOnline.value || !_isLoggedIn.value) return
        val reporterId = currentUserId ?: "admin-id"

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncPendingRegistrations(reporterId) { message ->
                    _syncMessage.value = message
                }
                fetchRemotePets()
            } catch (e: Exception) {
                _syncMessage.value = "Error al sincronizar: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

class PetViewModelFactory(
    private val repository: PetRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PetViewModel(repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
