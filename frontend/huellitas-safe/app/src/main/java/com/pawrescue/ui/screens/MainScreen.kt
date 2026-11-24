package com.pawrescue.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pawrescue.data.model.AdFeedItemDto
import com.pawrescue.data.model.PetRegistration
import com.pawrescue.ui.components.AddPetDialog
import com.pawrescue.ui.components.PetDetailDialog
import com.pawrescue.ui.theme.GlassGradientBottom
import com.pawrescue.ui.theme.GlassGradientMid1
import com.pawrescue.ui.theme.GlassGradientMid2
import com.pawrescue.ui.theme.GlassGradientTop
import com.pawrescue.ui.theme.IceGradientBottom
import com.pawrescue.ui.theme.IceGradientMid
import com.pawrescue.ui.theme.IceGradientTop
import com.pawrescue.ui.theme.IceInk
import com.pawrescue.ui.theme.IceInkSecondary
import com.pawrescue.ui.theme.IceTeal
import com.pawrescue.ui.theme.PremiumGold
import com.pawrescue.ui.theme.PremiumGradientBottom
import com.pawrescue.ui.theme.PremiumGradientMid
import com.pawrescue.ui.theme.PremiumGradientTop
import com.pawrescue.ui.theme.PremiumObsidian
import com.pawrescue.ui.theme.PremiumTaupe
import com.pawrescue.ui.theme.isIceCleanTheme
import com.pawrescue.ui.theme.isPremiumTheme
import com.pawrescue.ui.viewmodel.PetViewModel
import com.pawrescue.utils.BiometricHelper
import com.pawrescue.utils.LocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PetViewModel) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val registrations by viewModel.registrations.collectAsStateWithLifecycle()
    val remotePets by viewModel.remotePets.collectAsStateWithLifecycle()
    val activeAds by viewModel.activeAds.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val unifiedPets = remember(registrations, remotePets) {
        // Mapping remote pets to PetRegistration structure for UI consistency
        val mappedRemote = remotePets.map { remote ->
            PetRegistration(
                id = 0,
                remoteId = remote.id,
                name = remote.name ?: "Mascota sin Nombre",
                description = remote.description ?: "Sin descripción",
                gender = remote.gender,
                status = remote.status,
                speciesId = remote.speciesId,
                breedId = remote.breedId,
                photosJson = remote.media.joinToString(",") { it.url },
                videoPath = remote.media.find { it.type == "SightingVideo" }?.url,
                latitude = remote.lastLatitude ?: 0.0,
                longitude = remote.lastLongitude ?: 0.0,
                locationName = "Reporte Remoto",
                timestamp = System.currentTimeMillis(),
                isSynced = true
            )
        }
        val remoteIds = mappedRemote.mapNotNull { it.remoteId }.toSet()
        val uniqueLocal = registrations.filter { it.remoteId == null || it.remoteId !in remoteIds }
        uniqueLocal + mappedRemote
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var petToEdit by remember { mutableStateOf<PetRegistration?>(null) }
    var selectedPetForDetail by remember { mutableStateOf<PetRegistration?>(null) }
    var currentTab by remember { mutableStateOf("inicio") }

    // Mock notifications for UI demonstration
    var notificationList by remember { mutableStateOf(listOf(
        AppNotification("1", "¡Sincronización!", "Tus reportes se sincronizaron con éxito.", "Hace 5 min", false, "sync"),
        AppNotification("2", "Actualización v2.1", "Mejoras en el rendimiento de mapas.", "Ayer", true, "system")
    )) }
    var nearbyAlertsEnabled by remember { mutableStateOf(true) }

    val triggerBiometricAuth = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            BiometricHelper.showPrompt(
                activity = activity,
                onSuccess = {
                    viewModel.setAuthenticated(true)
                    viewModel.biometricLogin()
                },
                onError = { error ->
                    Toast.makeText(context, "Acceso denegado: $error", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            viewModel.setAuthenticated(true)
            viewModel.biometricLogin()
        }
    }

    val bannerAds = remember(activeAds) {
        val top = activeAds.filter { it.placement == "BANNER_TOP" || it.isGovernment }
        top.ifEmpty { activeAds.take(5) }
    }
    val feedAds = remember(activeAds) {
        val feed = activeAds.filter { it.placement == "HOME_FEED" }
        feed.ifEmpty { activeAds }
    }
    val handleAdClick: (AdFeedItemDto) -> Unit = { ad ->
        viewModel.trackAdClick(ad.id)
        val destination = ad.targetUrl?.takeIf { it.isNotBlank() }
            ?: ad.officialSourceUrl?.takeIf { it.isNotBlank() }
        if (!destination.isNullOrBlank()) {
            try {
                uriHandler.openUri(destination)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isLoggedIn, isAuthenticated) {
        if (isLoggedIn && !isAuthenticated) {
            triggerBiometricAuth()
        } else if (isLoggedIn && isAuthenticated) {
            viewModel.loadAds()
            viewModel.loadSocialData()
        }
    }

    if (!isLoggedIn) {
        LoginRegisterRedesignScreen(
            viewModel = viewModel,
            onBiometricAuthRequested = { triggerBiometricAuth() }
        )
    } else if (!isAuthenticated) {
        BiometricLockScreen(isBiometricAvailable = BiometricHelper.isBiometricAvailable(context), onUnlockRequested = { triggerBiometricAuth() })
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val currentThemeName by viewModel.currentTheme.collectAsStateWithLifecycle()
                val iceActive = isIceCleanTheme(currentThemeName)
                val barTitleColor = if (iceActive) IceInk else Color.White
                TopAppBar(
                    title = {
                        Column {
                            Text("Huellitas Safe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = barTitleColor)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleNetwork(!isOnline) }) {
                                val onlineColor = if (iceActive) IceTeal else Color(0xFF5F9E81)
                                Icon(if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff, null, modifier = Modifier.size(14.dp), tint = if (isOnline) onlineColor else Color.Gray)
                                Text(if (isOnline) "Modo Online" else "Modo Offline", fontSize = 10.sp, color = if (isOnline) onlineColor else Color.Gray)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setAuthenticated(false) }) { Icon(Icons.Default.Fingerprint, "BIO LOCK", tint = barTitleColor) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = when {
                            currentThemeName == "Glass" -> Color.White.copy(alpha = 0.14f)
                            isPremiumTheme(currentThemeName) -> Color(0xFF241D17).copy(alpha = 0.55f)
                            iceActive -> Color.White.copy(alpha = 0.72f)
                            else -> Color.Transparent
                        },
                        titleContentColor = barTitleColor
                    )
                )
            },
            bottomBar = {
                val currentThemeName by viewModel.currentTheme.collectAsStateWithLifecycle()
                val premiumActive = isPremiumTheme(currentThemeName)
                val iceActive = isIceCleanTheme(currentThemeName)
                NavigationBar(
                    containerColor = when {
                        currentThemeName == "Glass" -> Color(0xFF141B38).copy(alpha = 0.72f)
                        premiumActive -> Color(0xFF14100D).copy(alpha = 0.90f)
                        iceActive -> Color.White.copy(alpha = 0.94f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    },
                    tonalElevation = 0.dp
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = when {
                            premiumActive -> PremiumObsidian
                            iceActive -> Color.White
                            else -> MaterialTheme.colorScheme.primary
                        },
                        selectedTextColor = when {
                            premiumActive -> PremiumGold
                            iceActive -> IceInk
                            else -> MaterialTheme.colorScheme.primary
                        },
                        unselectedIconColor = when {
                            premiumActive -> PremiumTaupe.copy(alpha = 0.85f)
                            iceActive -> IceInkSecondary
                            else -> Color.Gray
                        },
                        indicatorColor = when {
                            premiumActive -> Color.White
                            iceActive -> IceInk
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        }
                    )
                    NavigationBarItem(
                        selected = currentTab == "inicio",
                        onClick = { currentTab = "inicio" },
                        icon = { Icon(if (currentTab == "inicio") Icons.Filled.Home else Icons.Outlined.Home, null) },
                        label = if (currentTab == "inicio") { { Text("INICIO") } } else null,
                        alwaysShowLabel = false,
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentTab == "feed",
                        onClick = { currentTab = "feed" },
                        icon = { Icon(if (currentTab == "feed") Icons.Filled.Pets else Icons.Outlined.Pets, null) },
                        label = if (currentTab == "feed") { { Text("DESCUBRIR") } } else null,
                        alwaysShowLabel = false,
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentTab == "mensajes",
                        onClick = { currentTab = "mensajes" },
                        icon = { Icon(if (currentTab == "mensajes") Icons.Filled.Chat else Icons.Outlined.Chat, null) },
                        label = if (currentTab == "mensajes") { { Text("MENSAJES") } } else null,
                        alwaysShowLabel = false,
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentTab == "perfil",
                        onClick = { currentTab = "perfil" },
                        icon = { Icon(if (currentTab == "perfil") Icons.Filled.Person else Icons.Outlined.Person, null) },
                        label = if (currentTab == "perfil") { { Text("MI PERFIL") } } else null,
                        alwaysShowLabel = false,
                        colors = navItemColors
                    )
                }
            },
            floatingActionButton = {
                val currentThemeName by viewModel.currentTheme.collectAsStateWithLifecycle()
                val premiumActive = isPremiumTheme(currentThemeName)
                val iceActive = isIceCleanTheme(currentThemeName)
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Reportar", fontWeight = FontWeight.SemiBold) },
                    containerColor = when {
                        premiumActive -> Color.White
                        iceActive -> IceInk
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (premiumActive) PremiumObsidian else Color.White,
                    shape = RoundedCornerShape(100.dp)
                )
            }
        ) { innerPadding ->
            val currentThemeName by viewModel.currentTheme.collectAsStateWithLifecycle()
            
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (currentThemeName == "Glass") {
                    // Fondo Liquid Glass: degradado lavanda luminoso -> índigo -> azul noche profundo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to GlassGradientTop,
                                        0.32f to GlassGradientMid1,
                                        0.66f to GlassGradientMid2,
                                        1.0f to GlassGradientBottom
                                    )
                                )
                            )
                    )
                    // Brillo ambiental superior estilo cristal líquido
                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .offset(x = (-60).dp, y = (-80).dp)
                            .blur(90.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.42f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                } else if (isPremiumTheme(currentThemeName)) {
                    // Fondo Premium Warm Glass: degradado taupe cálido (#AA907A) -> moka ahumado -> obsidiana (#0F0F0F)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to PremiumGradientTop,
                                        0.38f to PremiumGradientMid,
                                        0.78f to Color(0xFF1A1410),
                                        1.0f to PremiumGradientBottom
                                    )
                                )
                            )
                    )
                    // Halo cálido ámbar (#FFDA9F) superior
                    Box(
                        modifier = Modifier
                            .size(320.dp)
                            .offset(x = 80.dp, y = (-60).dp)
                            .blur(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        PremiumGold.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                } else if (isIceCleanTheme(currentThemeName)) {
                    // Fondo Ice Clean (CleanPro): degradado azul hielo limpio -> neblina glaciar -> blanco escarcha
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to IceGradientTop,
                                        0.45f to IceGradientMid,
                                        1.0f to IceGradientBottom
                                    )
                                )
                            )
                    )
                    // Brillo suave blanco superior
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .offset(x = (-60).dp, y = (-70).dp)
                            .blur(90.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }

                // Background blurred circles logic (legacy)
                if (false) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .offset(x = (-100).dp, y = (-50).dp)
                            .blur(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .align(Alignment.CenterEnd)
                            .offset(x = 100.dp, y = (-150).dp)
                            .blur(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                when (currentTab) {
                    "inicio" -> HomeScreen(
                        pets = unifiedPets,
                        notifications = notificationList,
                        onMarkAllRead = { notificationList = notificationList.map { it.copy(isRead = true) } },
                        onDeleteNotification = { id -> notificationList = notificationList.filter { it.id != id } },
                        onToggleNearbyAlerts = { nearbyAlertsEnabled = !nearbyAlertsEnabled },
                        nearbyAlertsEnabled = nearbyAlertsEnabled,
                        isGlassTheme = currentThemeName == "Glass" || isPremiumTheme(currentThemeName),
                        bannerAds = bannerAds,
                        onAdImpression = { viewModel.trackAdImpression(it) },
                        onAdClick = handleAdClick
                    )
                    "feed" -> SocialFeedLayout(
                        registrations = unifiedPets,
                        isSyncing = isSyncing,
                        onPetClicked = { selectedPetForDetail = it },
                        onShareRequested = { /* share logic */ },
                        onManualSyncRequested = { viewModel.triggerSync() },
                        feedAds = feedAds,
                        onAdImpression = { viewModel.trackAdImpression(it) },
                        onAdClick = handleAdClick
                    )
                    "mensajes" -> MessagesScreen(
                        viewModel = viewModel,
                        isGlassTheme = currentThemeName == "Glass"
                    )
                    "perfil" -> {
                        val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
                        ProfileLayout(
                            registrations = unifiedPets,
                            name = viewModel.currentUserName ?: "",
                            email = viewModel.currentUserEmail ?: "",
                            profilePhotoUrl = null,
                            currentTheme = currentTheme,
                            onThemeChanged = { viewModel.setTheme(it) },
                            onNameChanged = {},
                            onEmailChanged = {},
                            onPetClicked = { selectedPetForDetail = it },
                            onSaveProfile = {},
                            onLogout = { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPetDialog(viewModel, petToEdit, { showAddDialog = false; petToEdit = null }, { showAddDialog = false; petToEdit = null })
    }

    selectedPetForDetail?.let { pet ->
        PetDetailDialog(
            pet = pet,
            onDismiss = { selectedPetForDetail = null },
            onEdit = { petToEdit = pet; selectedPetForDetail = null; showAddDialog = true },
            onDelete = { viewModel.deleteRegistration(pet.id, pet.remoteId); selectedPetForDetail = null },
            onReportSighting = { note ->
                val remoteId = pet.remoteId ?: return@PetDetailDialog
                LocationHelper(context).getLastLocation(
                    onSuccess = { lat, lon ->
                        viewModel.reportPetSighting(remoteId, note, lat, lon) { ok ->
                            Toast.makeText(
                                context,
                                if (ok) "Avistamiento enviado al dueño" else "No se pudo enviar el avistamiento",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (ok) selectedPetForDetail = null
                        }
                    },
                    onFailure = {
                        Toast.makeText(context, "Activa el GPS para reportar el avistamiento", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onStartChat = {
                val remoteId = pet.remoteId ?: return@PetDetailDialog
                val convType = if (pet.status.equals("ADOPTION", ignoreCase = true)) "ADOPTION_INQUIRY" else "PET_SIGHTING"
                viewModel.startConversationForPet(
                    petId = remoteId,
                    type = convType,
                    initialMessage = "Hola, me comunico respecto a ${pet.name}"
                ) { conv ->
                    if (conv != null) {
                        selectedPetForDetail = null
                        currentTab = "mensajes"
                    } else {
                        Toast.makeText(context, "No se pudo abrir la conversación", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRequestAdoption = {
                val remoteId = pet.remoteId ?: return@PetDetailDialog
                viewModel.requestPetAdoption(
                    petId = remoteId,
                    notes = "Hola, estoy interesado en adoptar a ${pet.name}."
                ) { req ->
                    if (req != null) {
                        Toast.makeText(context, "Solicitud de adopción enviada", Toast.LENGTH_SHORT).show()
                        selectedPetForDetail = null
                        currentTab = "mensajes"
                    } else {
                        Toast.makeText(context, "No se pudo enviar la solicitud", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onReportContent = {
                val remoteId = pet.remoteId ?: return@PetDetailDialog
                viewModel.reportContentItem("PET", remoteId, "Reporte desde la app móvil") { ok ->
                    Toast.makeText(
                        context,
                        if (ok) "Reporte enviado para revisión" else "No se pudo enviar el reporte",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
