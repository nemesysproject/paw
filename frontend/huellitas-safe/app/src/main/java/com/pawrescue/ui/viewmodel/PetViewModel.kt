package com.pawrescue.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pawrescue.data.local.SessionManager
import com.pawrescue.data.model.*
import com.pawrescue.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    val currentTheme: StateFlow<String> = repository.selectedTheme
        .map { it?.themeName ?: "Glass" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Glass"
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
    val currentUserRole: String get() = sessionManager.getUserRole()
    val canUseBiometricQuickLogin: Boolean
        get() = !sessionManager.getUserEmail().isNullOrBlank() && sessionManager.isLoggedIn()

    // --- SYNC STATUS ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // --- CATALOG STATES ---
    val speciesCatalog: StateFlow<List<SpeciesDto>> = repository.speciesCatalog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gendersCatalog: StateFlow<List<GenderDto>> = repository.gendersCatalog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusesCatalog: StateFlow<List<StatusDto>> = repository.statusesCatalog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSpeciesId = MutableStateFlow<String?>(null)
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val breedsCatalog: StateFlow<List<BreedDto>> = _selectedSpeciesId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) repository.allBreedsCatalog
            else repository.getBreedsBySpecies(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- REMOTE PETS STATE ---
    private val _remotePets = MutableStateFlow<List<PetDetailResponse>>(emptyList())
    val remotePets: StateFlow<List<PetDetailResponse>> = _remotePets.asStateFlow()

    // --- ADS & CAMPAIGNS STATE ---
    val activeAds: StateFlow<List<AdFeedItemDto>> = repository.activeAds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recordedImpressions = mutableSetOf<String>()

    // --- SOCIAL INTERACTIONS STATE ---
    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adoptionRequests: StateFlow<List<AdoptionRequestEntity>> = repository.adoptionRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<MessageEntity>> = _selectedConversationId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getMessagesFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- FORM TEMPORARY FIELDS ---
    private val _formPhotos = MutableStateFlow<List<String>>(emptyList())
    val formPhotos: StateFlow<List<String>> = _formPhotos.asStateFlow()

    private val _formVideoPath = MutableStateFlow<String?>(null)
    val formVideoPath: StateFlow<String?> = _formVideoPath.asStateFlow()

    init {
        if (_isLoggedIn.value) {
            loadCatalogs()
            fetchRemotePets()
            loadAds()
            triggerSync()
        }
    }

    // --- ACTIONS ---
    fun setAuthenticated(auth: Boolean) {
        _isAuthenticated.value = auth
    }

    fun setTheme(themeName: String) {
        viewModelScope.launch {
            repository.setTheme(themeName)
        }
    }

    fun toggleNetwork(online: Boolean) {
        _isOnline.value = online
        if (online) {
            triggerSync()
            loadCatalogs()
            fetchRemotePets()
            loadAds()
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
    private fun extractServerError(errorBody: String?, fallback: String): String {
        val raw = try {
            org.json.JSONObject(errorBody ?: "").optString("error").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } ?: return fallback
        return when {
            raw.contains("Email already exists", ignoreCase = true) ->
                "Este correo ya está registrado. Inicia sesión o utiliza otro correo."
            raw.contains("Invalid credentials", ignoreCase = true) ->
                "Correo o contraseña incorrectos."
            else -> raw
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _authError.value = "Los campos de correo y contraseña son requeridos."
            return
        }
        viewModelScope.launch {
            _isAuthProcessing.value = true
            _authError.value = null
            try {
                val response = repository.login(cleanEmail, password)
                if (response.isSuccessful) {
                    val tokenResp = response.body()
                    val finalAccessToken = tokenResp?.accessToken ?: tokenResp?.accessTokenCamel
                    val finalRefreshToken = tokenResp?.refreshToken ?: tokenResp?.refreshTokenCamel

                    if (!finalAccessToken.isNullOrBlank()) {
                        val resolvedEmail = tokenResp?.email ?: cleanEmail
                        val resolvedName = tokenResp?.name ?: resolvedEmail.substringBefore("@")
                        sessionManager.saveSession(
                            accessToken = finalAccessToken,
                            refreshToken = finalRefreshToken ?: "",
                            email = resolvedEmail,
                            name = resolvedName,
                            userId = "user-${resolvedEmail.hashCode()}",
                            avatarUrl = tokenResp?.avatarUrl
                        )
                        _isLoggedIn.value = true
                        _isAuthenticated.value = true
                        loadCatalogs()
                        fetchRemotePets()
                        loadAds()
                        loadSocialData()
                        triggerSync()
                        onSuccess()
                    } else {
                        _authError.value = "Error al recibir el token de acceso."
                    }
                } else {
                    _authError.value = extractServerError(
                        response.errorBody()?.string(),
                        "Credenciales incorrectas o error en el servidor."
                    )
                }
            } catch (e: Exception) {
                _authError.value = "Fallo de conexión: ${e.localizedMessage}"
            } finally {
                _isAuthProcessing.value = false
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String = password,
        onSuccess: () -> Unit
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        when {
            cleanName.isBlank() || cleanEmail.isBlank() || password.isBlank() -> {
                _authError.value = "Todos los campos son obligatorios para el registro."
                return
            }
            !emailRegex.matches(cleanEmail) -> {
                _authError.value = "Ingresa un correo electrónico válido."
                return
            }
            password.length < 8 -> {
                _authError.value = "La contraseña debe tener al menos 8 caracteres."
                return
            }
            password != confirmPassword -> {
                _authError.value = "Las contraseñas no coinciden."
                return
            }
        }

        viewModelScope.launch {
            _isAuthProcessing.value = true
            _authError.value = null
            try {
                val registerResp = repository.register(cleanName, cleanEmail, password)
                if (registerResp.isSuccessful) {
                    // El backend responde 201 Created sin cuerpo; hacemos login automático para obtener JWT
                    val loginResp = repository.login(cleanEmail, password)
                    if (loginResp.isSuccessful) {
                        val tokenResp = loginResp.body()
                        val finalAccessToken = tokenResp?.accessToken ?: tokenResp?.accessTokenCamel
                        val finalRefreshToken = tokenResp?.refreshToken ?: tokenResp?.refreshTokenCamel
                        if (!finalAccessToken.isNullOrBlank()) {
                            sessionManager.saveSession(
                                accessToken = finalAccessToken,
                                refreshToken = finalRefreshToken ?: "",
                                email = tokenResp?.email ?: cleanEmail,
                                name = tokenResp?.name ?: cleanName,
                                userId = "user-${cleanEmail.hashCode()}",
                                avatarUrl = tokenResp?.avatarUrl
                            )
                            _isLoggedIn.value = true
                            _isAuthenticated.value = true
                            loadCatalogs()
                            fetchRemotePets()
                            loadAds()
                            loadSocialData()
                            triggerSync()
                            onSuccess()
                        } else {
                            _authError.value = "Cuenta creada, pero no se recibió el token. Inicia sesión manualmente."
                        }
                    } else {
                        _authError.value = "Cuenta creada con éxito. Ahora inicia sesión con tu correo y contraseña."
                    }
                } else {
                    _authError.value = extractServerError(
                        registerResp.errorBody()?.string(),
                        "El correo ya está registrado o los datos son inválidos."
                    )
                }
            } catch (e: Exception) {
                _authError.value = "Fallo de conexión: ${e.localizedMessage}"
            } finally {
                _isAuthProcessing.value = false
            }
        }
    }

    fun setAuthError(message: String?) {
        _authError.value = message
    }

    fun loginWithGoogle(
        idToken: String,
        nonce: String?,
        fallbackEmail: String? = null,
        fallbackName: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isAuthProcessing.value = true
            _authError.value = null
            try {
                val response = repository.socialLogin(
                    provider = "GOOGLE",
                    idToken = idToken,
                    nonce = nonce
                )
                if (response.isSuccessful) {
                    val tokenResp = response.body()
                    val finalAccessToken = tokenResp?.accessToken ?: tokenResp?.accessTokenCamel
                    val finalRefreshToken = tokenResp?.refreshToken ?: tokenResp?.refreshTokenCamel
                    if (!finalAccessToken.isNullOrBlank()) {
                        val resolvedEmail = tokenResp?.email ?: fallbackEmail ?: "google-user@pawrescue.app"
                        val resolvedName = tokenResp?.name ?: fallbackName ?: resolvedEmail.substringBefore("@")
                        sessionManager.saveSession(
                            accessToken = finalAccessToken,
                            refreshToken = finalRefreshToken ?: "",
                            email = resolvedEmail,
                            name = resolvedName,
                            userId = "user-${resolvedEmail.hashCode()}"
                        )
                        _isLoggedIn.value = true
                        _isAuthenticated.value = true
                        loadCatalogs()
                        fetchRemotePets()
                        loadAds()
                        loadSocialData()
                        triggerSync()
                        onSuccess()
                    } else {
                        _authError.value = "Error al recibir el token de acceso desde el servidor."
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    val serverMsg = try {
                        org.json.JSONObject(errBody ?: "").optString("error").takeIf { it.isNotBlank() }
                    } catch (_: Exception) { null }
                    _authError.value = serverMsg ?: "No se pudo validar tu cuenta de Google en el servidor."
                }
            } catch (e: Exception) {
                _authError.value = "Fallo de conexión: ${e.localizedMessage}"
            } finally {
                _isAuthProcessing.value = false
            }
        }
    }

    fun logout() {
        sessionManager.clearTokens()
        _isLoggedIn.value = false
        _isAuthenticated.value = false
        _remotePets.value = emptyList()
    }

    fun clearStoredCredentials() {
        sessionManager.clearSession()
        _isLoggedIn.value = false
        _isAuthenticated.value = false
    }

    fun biometricLogin() {
        viewModelScope.launch {
            if (sessionManager.isAccessTokenValid()) {
                _isLoggedIn.value = true
                _isAuthenticated.value = true
                loadCatalogs()
                fetchRemotePets()
                loadAds()
                loadSocialData()
                triggerSync()
                return@launch
            }

            val refresh = sessionManager.getRefreshToken()
            if (!refresh.isNullOrBlank()) {
                _isAuthProcessing.value = true
                _authError.value = null
                try {
                    val resp = repository.refreshToken(refresh)
                    val body = resp.body()
                    val newAccess = body?.accessToken ?: body?.accessTokenCamel
                    val newRefresh = body?.refreshToken ?: body?.refreshTokenCamel ?: refresh
                    if (resp.isSuccessful && !newAccess.isNullOrBlank()) {
                        sessionManager.updateTokens(newAccess, newRefresh)
                        _isLoggedIn.value = true
                        _isAuthenticated.value = true
                        loadCatalogs()
                        fetchRemotePets()
                        loadAds()
                        loadSocialData()
                        triggerSync()
                    } else {
                        sessionManager.clearTokens()
                        _isLoggedIn.value = false
                        _isAuthenticated.value = false
                        _authError.value = "Tu sesión expiró. Inicia sesión nuevamente."
                    }
                } catch (e: Exception) {
                    _authError.value = "No se pudo renovar tu sesión: ${e.localizedMessage}"
                } finally {
                    _isAuthProcessing.value = false
                }
            } else {
                _isLoggedIn.value = false
                _isAuthenticated.value = false
                _authError.value = "Inicia sesión para activar el acceso biométrico."
            }
        }
    }

    // --- CATALOG LOADER ---
    fun loadCatalogs() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            repository.syncCatalogs()
        }
    }

    fun loadBreedsForSpecies(speciesId: String) {
        _selectedSpeciesId.value = speciesId
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

    // --- ADS & CAMPAIGNS LOADER & METRICS ---
    fun loadAds(lat: Double? = null, lon: Double? = null) {
        if (!_isOnline.value) return
        viewModelScope.launch {
            repository.syncAds(lat = lat, lon = lon)
        }
    }

    fun trackAdImpression(campaignId: String, lat: Double? = null, lon: Double? = null) {
        if (!_isOnline.value || !recordedImpressions.add(campaignId)) return
        viewModelScope.launch {
            repository.recordAdEvent(
                campaignId = campaignId,
                eventType = "IMPRESSION",
                userId = currentUserId,
                lat = lat,
                lon = lon
            )
        }
    }

    fun trackAdClick(campaignId: String, lat: Double? = null, lon: Double? = null) {
        if (!_isOnline.value) return
        viewModelScope.launch {
            repository.recordAdEvent(
                campaignId = campaignId,
                eventType = "CLICK",
                userId = currentUserId,
                lat = lat,
                lon = lon
            )
        }
    }

    // --- SOCIAL INTERACTIONS ACTIONS ---
    fun loadSocialData() {
        if (!_isOnline.value || !_isLoggedIn.value) return
        viewModelScope.launch {
            repository.syncConversations()
            repository.syncAdoptions()
        }
    }

    fun openConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
        if (!conversationId.isNullOrBlank() && _isOnline.value) {
            viewModelScope.launch {
                repository.syncMessages(conversationId)
            }
        }
    }

    fun sendChatText(conversationId: String, text: String, onDone: (Boolean) -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val ok = repository.sendMessage(
                conversationId = conversationId,
                type = "TEXT",
                content = text.trim()
            )
            onDone(ok)
        }
    }

    fun sendChatLocation(
        conversationId: String,
        latitude: Double,
        longitude: Double,
        label: String = "Ubicación compartida",
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val ok = repository.sendMessage(
                conversationId = conversationId,
                type = "LOCATION",
                content = "Ubicación GPS: %.5f, %.5f".format(latitude, longitude),
                latitude = latitude,
                longitude = longitude,
                locationLabel = label
            )
            onDone(ok)
        }
    }

    fun reportPetSighting(
        petId: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val ok = repository.reportSighting(
                petId = petId,
                notes = notes.ifBlank { "Avistamiento reportado desde Huellitas Safe" },
                latitude = latitude,
                longitude = longitude
            )
            onResult(ok)
        }
    }

    fun startConversationForPet(
        petId: String,
        type: String,
        initialMessage: String,
        onCreated: (ConversationEntity?) -> Unit
    ) {
        viewModelScope.launch {
            val conv = repository.createConversation(
                type = type,
                petId = petId,
                initialMessage = initialMessage
            )
            if (conv != null) {
                openConversation(conv.id)
            }
            onCreated(conv)
        }
    }

    fun requestPetAdoption(
        petId: String,
        notes: String,
        onCreated: (AdoptionRequestEntity?) -> Unit
    ) {
        viewModelScope.launch {
            val req = repository.requestAdoption(petId = petId, notes = notes)
            req?.conversationId?.let { openConversation(it) }
            onCreated(req)
        }
    }

    fun updateAdoptionStatus(adoptionId: String, status: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.updateAdoptionStatus(adoptionId, status)
            onResult(ok)
        }
    }

    fun reportContentItem(targetType: String, targetId: String, reason: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.reportContent(targetType, targetId, reason)
            onResult(ok)
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
            _isProcessing.value = true
            try {
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
                    gender = gender,
                    status = status,
                    speciesId = speciesId,
                    breedId = breedId,
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
            } finally {
                _isProcessing.value = false
            }
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
            _isProcessing.value = true
            try {
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
                    gender = gender,
                    status = status,
                    speciesId = speciesId,
                    breedId = breedId,
                    photosJson = newPhotos.joinToString(","),
                    videoPath = null,
                    latitude = latitude,
                    longitude = longitude,
                    locationName = "Ubicación Modificada",
                    isSynced = false
                )
                repository.insertRegistration(updated) // REPLACE conflict strategy will update it
                onSuccess()
            } finally {
                _isProcessing.value = false
            }
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
