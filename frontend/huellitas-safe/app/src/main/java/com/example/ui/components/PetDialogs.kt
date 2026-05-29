package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.PetRegistration
import com.example.ui.theme.WarmDialogBackground
import com.example.ui.viewmodel.PetViewModel
import com.example.utils.LocationHelper
import com.example.utils.MediaUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPetDialog(
    viewModel: PetViewModel,
    petToEdit: PetRegistration? = null,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val formPhotos by viewModel.formPhotos.collectAsStateWithLifecycle()
    val formVideoPath by viewModel.formVideoPath.collectAsStateWithLifecycle()
    
    val speciesCatalog by viewModel.speciesCatalog.collectAsStateWithLifecycle()
    val breedsCatalog by viewModel.breedsCatalog.collectAsStateWithLifecycle()
    val gendersCatalog by viewModel.gendersCatalog.collectAsStateWithLifecycle()
    val statusesCatalog by viewModel.statusesCatalog.collectAsStateWithLifecycle()

    var petName by remember(petToEdit) { mutableStateOf(petToEdit?.name ?: "") }
    var petDescription by remember(petToEdit) { mutableStateOf(petToEdit?.description ?: "") }
    
    var selectedSpeciesId by remember(petToEdit) { mutableStateOf("") }
    var selectedBreedId by remember(petToEdit) { mutableStateOf<String?>(null) }
    var selectedGender by remember(petToEdit) { mutableStateOf("") }
    var selectedStatus by remember(petToEdit) { mutableStateOf("") }

    // Coordenadas iniciales por defecto
    var latitude by remember(petToEdit) { mutableDoubleStateOf(petToEdit?.latitude ?: -12.046374) }
    var longitude by remember(petToEdit) { mutableDoubleStateOf(petToEdit?.longitude ?: -77.042793) }
    var locationName by remember(petToEdit) { mutableStateOf(petToEdit?.locationName ?: "Calle de la Esperanza N° 450") }

    LaunchedEffect(petToEdit) {
        viewModel.clearForm()
        viewModel.loadCatalogs()
        if (petToEdit != null) {
            val photos = petToEdit.photosJson.split(",").filter { it.isNotBlank() }
            photos.forEach { viewModel.addPhotoToForm(it) }
            if (petToEdit.videoPath != null) {
                viewModel.setVideoPathInForm(petToEdit.videoPath)
            }
        }
    }

    LaunchedEffect(selectedSpeciesId) {
        if (selectedSpeciesId.isNotEmpty()) {
            viewModel.loadBreedsForSpecies(selectedSpeciesId)
        }
    }

    val locationHelper = remember { LocationHelper(context) }

    // Launchers para multimedia
    var rawPhotoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && rawPhotoFile != null) viewModel.addPhotoToForm(rawPhotoFile!!.absolutePath)
    }

    var rawVideoFile by remember { mutableStateOf<File?>(null) }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && rawVideoFile != null) viewModel.setVideoPathInForm(rawVideoFile!!.absolutePath)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (locationGranted) {
            locationHelper.getLastLocation(
                onSuccess = { lat, lng ->
                    latitude = lat; longitude = lng; locationName = "Registro GPS: [$lat, $lng]"
                },
                onFailure = {}
            )
        }
    }

    fun checkAndRequestPermission(permission: String, onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) onGranted()
        else permissionLauncher.launch(arrayOf(permission))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmDialogBackground),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (petToEdit != null) "Editar Mascota" else "Registrar Mascota", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                // Form Fields
                OutlinedTextField(value = petName, onValueChange = { petName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = petDescription, onValueChange = { petDescription = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp))

                // Selectors
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Clasificación", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Species
                            var speciesExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = speciesCatalog.find { it.id == selectedSpeciesId }?.name ?: "Especie",
                                    onValueChange = {}, readOnly = true, label = { Text("Especie") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { speciesExpanded = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                DropdownMenu(expanded = speciesExpanded, onDismissRequest = { speciesExpanded = false }) {
                                    speciesCatalog.forEach { sp ->
                                        DropdownMenuItem(text = { Text(sp.name) }, onClick = { selectedSpeciesId = sp.id; selectedBreedId = null; speciesExpanded = false })
                                    }
                                }
                            }
                            // Breed
                            var breedExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = breedsCatalog.find { it.id == selectedBreedId }?.name ?: "Raza",
                                    onValueChange = {}, readOnly = true, label = { Text("Raza") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { breedExpanded = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                DropdownMenu(expanded = breedExpanded, onDismissRequest = { breedExpanded = false }) {
                                    breedsCatalog.forEach { br ->
                                        DropdownMenuItem(text = { Text(br.name) }, onClick = { selectedBreedId = br.id; breedExpanded = false })
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Gender
                            var genderExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = gendersCatalog.find { it.id == selectedGender }?.name ?: "Género",
                                    onValueChange = {}, readOnly = true, label = { Text("Género") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { genderExpanded = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                                    gendersCatalog.forEach { g ->
                                        DropdownMenuItem(text = { Text(g.name) }, onClick = { selectedGender = g.id; genderExpanded = false })
                                    }
                                }
                            }
                            // Status
                            var statusExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = statusesCatalog.find { it.id == selectedStatus }?.name ?: "Estado",
                                    onValueChange = {}, readOnly = true, label = { Text("Estado") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { statusExpanded = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                    statusesCatalog.forEach { st ->
                                        DropdownMenuItem(text = { Text(st.name) }, onClick = { selectedStatus = st.id; statusExpanded = false })
                                    }
                                }
                            }
                        }
                    }
                }

                // Location
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Ubicación", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Button(onClick = { checkAndRequestPermission(Manifest.permission.ACCESS_FINE_LOCATION) {
                                locationHelper.getLastLocation({ lat, lng -> latitude = lat; longitude = lng; locationName = "Registro GPS: [$lat, $lng]" }, {})
                            }}, shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.GpsFixed, null, modifier = Modifier.size(16.dp)); Text("GPS", fontSize = 12.sp) }
                        }
                        OutlinedTextField(value = locationName, onValueChange = { locationName = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    }
                }

                // MULTIMEDIA: FOTOS
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column { Text("Fotos (${formPhotos.size}/10)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("Máximo 10", fontSize = 11.sp, color = Color.Gray) }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { checkAndRequestPermission(Manifest.permission.CAMERA) { 
                                    MediaUtils.createPhotoFile(context).let { rawPhotoFile = it; cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)) }
                                } }, shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp)) }
                                Button(onClick = { if (formPhotos.size < 10) viewModel.addPhotoToForm(MediaUtils.getMockPetImage(formPhotos.size)) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        if (formPhotos.isEmpty()) Box(modifier = Modifier.fillMaxWidth().height(60.dp).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("Sin fotos", fontSize = 11.sp, color = Color.Gray) }
                        else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(formPhotos, key = { idx, _ -> "photo_$idx" }) { idx, path ->
                                Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))) {
                                    AsyncImage(model = path, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    IconButton(onClick = { viewModel.removePhotoFromForm(idx) }, modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.6f))) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                }
                            }
                        }
                    }
                }

                // MULTIMEDIA: VIDEO
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column { Text("Video", fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("Opcional", fontSize = 11.sp, color = Color.Gray) }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { checkAndRequestPermission(Manifest.permission.CAMERA) { 
                                    MediaUtils.createVideoFile(context).let { rawVideoFile = it; videoLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)) }
                                } }, shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.Videocam, null, modifier = Modifier.size(16.dp)) }
                                Button(onClick = { viewModel.setVideoPathInForm("/mock_video.mp4") }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        formVideoPath?.let { path ->
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Text(text = "Video listo!", fontSize = 11.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                IconButton(onClick = { viewModel.setVideoPathInForm(null) }, modifier = Modifier.size(16.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                            }
                        } ?: Box(modifier = Modifier.fillMaxWidth().height(40.dp).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("Sin video", fontSize = 11.sp, color = Color.Gray) }
                    }
                }

                // Actions
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(100.dp)) { Text("Cancelar") }
                    Button(onClick = {
                        if (petToEdit != null) viewModel.updatePet(petToEdit.remoteId, petToEdit.id, petName, petDescription, selectedGender, selectedStatus, selectedSpeciesId, selectedBreedId, latitude, longitude, formPhotos, onSaveSuccess)
                        else viewModel.savePetRegistration(petName, petDescription, selectedGender, selectedStatus, selectedSpeciesId, selectedBreedId, latitude, longitude, locationName, onSaveSuccess)
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(100.dp)) { Text("Guardar") }
                }
            }
        }
    }
}

@Composable
fun PetDetailDialog(
    pet: PetRegistration,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val photoUrls = remember(pet.photosJson) { if (pet.photosJson.isBlank()) emptyList() else pet.photosJson.split(",") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Detalle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null) }
                }
                HorizontalDivider()
                // Image Carousel
                if (photoUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(photoUrls, key = { it }) { path -> Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.size(120.dp)) { AsyncImage(model = path, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } }
                    }
                }
                Text(text = pet.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(text = pet.description, color = Color.DarkGray)
                // Info actions
                if (onEdit != null || onDelete != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        onEdit?.let { OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) { Text("Editar") } }
                        onDelete?.let { Button(onClick = it, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") } }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }
}
