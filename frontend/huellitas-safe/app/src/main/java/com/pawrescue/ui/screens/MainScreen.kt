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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pawrescue.data.model.PetRegistration
import com.pawrescue.ui.components.AddPetDialog
import com.pawrescue.ui.components.PetDetailDialog
import com.pawrescue.ui.viewmodel.PetViewModel
import com.pawrescue.utils.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PetViewModel) {
    val context = LocalContext.current
    val registrations by viewModel.registrations.collectAsStateWithLifecycle()
    val remotePets by viewModel.remotePets.collectAsStateWithLifecycle()
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

    LaunchedEffect(isLoggedIn, isAuthenticated) {
        if (isLoggedIn && !isAuthenticated) triggerBiometricAuth()
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
                TopAppBar(
                    title = {
                        Column {
                            Text("Huellitas Safe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleNetwork(!isOnline) }) {
                                Icon(if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff, null, modifier = Modifier.size(14.dp), tint = if (isOnline) Color(0xFF5F9E81) else Color.Gray)
                                Text(if (isOnline) "Modo Online" else "Modo Offline", fontSize = 10.sp, color = if (isOnline) Color(0xFF5F9E81) else Color.Gray)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setAuthenticated(false) }) { Icon(Icons.Default.Fingerprint, "BIO LOCK", tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (currentThemeName == "Glass") Color.White.copy(alpha = 0.05f) else Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                val currentThemeName by viewModel.currentTheme.collectAsStateWithLifecycle()
                NavigationBar(
                    containerColor = if (currentThemeName == "Glass") Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "inicio",
                        onClick = { currentTab = "inicio" },
                        icon = { Icon(if (currentTab == "inicio") Icons.Filled.Home else Icons.Outlined.Home, null) },
                        label = if (currentTab == "inicio") { { Text("INICIO") } } else null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "feed",
                        onClick = { currentTab = "feed" },
                        icon = { Icon(if (currentTab == "feed") Icons.Filled.Pets else Icons.Outlined.Pets, null) },
                        label = if (currentTab == "feed") { { Text("DESCUBRIR") } } else null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "perfil",
                        onClick = { currentTab = "perfil" },
                        icon = { Icon(if (currentTab == "perfil") Icons.Filled.Person else Icons.Outlined.Person, null) },
                        label = if (currentTab == "perfil") { { Text("MI PERFIL") } } else null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Reportar") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
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
                    // Immersive background image like the login screen
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?q=80&w=1000&auto=format&fit=crop",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dark overlay for contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }

                // Background blurred circles logic
                if (currentThemeName != "Glass") {
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
                        unifiedPets, 
                        notificationList, 
                        { notificationList = notificationList.map { it.copy(isRead = true) } }, 
                        { id -> notificationList = notificationList.filter { it.id != id } }, 
                        { nearbyAlertsEnabled = !nearbyAlertsEnabled }, 
                        nearbyAlertsEnabled,
                        isGlassTheme = currentThemeName == "Glass"
                    )
                    "feed" -> SocialFeedLayout(unifiedPets, isSyncing, { selectedPetForDetail = it }, { /* share logic */ }, { viewModel.triggerSync() })
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
        PetDetailDialog(pet, { selectedPetForDetail = null }, { petToEdit = pet; selectedPetForDetail = null; showAddDialog = true }, { viewModel.deleteRegistration(pet.id, pet.remoteId); selectedPetForDetail = null })
    }
}
