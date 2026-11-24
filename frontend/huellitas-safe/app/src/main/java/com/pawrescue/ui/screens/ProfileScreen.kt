package com.pawrescue.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pawrescue.data.model.PetRegistration
import com.pawrescue.ui.theme.IceAmber
import com.pawrescue.ui.theme.IceGradientTop
import com.pawrescue.ui.theme.IceInk
import com.pawrescue.ui.theme.IceInkSecondary
import com.pawrescue.ui.theme.IceSurfaceMuted
import com.pawrescue.ui.theme.IceTeal
import com.pawrescue.ui.theme.PremiumGold
import com.pawrescue.ui.theme.PremiumObsidian
import com.pawrescue.ui.theme.PremiumTaupe
import com.pawrescue.ui.theme.isIceCleanTheme
import com.pawrescue.ui.theme.isPremiumTheme

@Composable
fun ProfileLayout(
    registrations: List<PetRegistration>,
    name: String,
    email: String,
    profilePhotoUrl: String? = null,
    currentTheme: String = "Glass",
    onThemeChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPetClicked: (PetRegistration) -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val premiumActive = isPremiumTheme(currentTheme)
        val iceActive = isIceCleanTheme(currentTheme)
        val textPrimary = if (iceActive) IceInk else Color.White
        val textSecondary = if (iceActive) IceInkSecondary else Color.Gray
        val cardBg = when {
            premiumActive -> Color(0xFF2A211B).copy(alpha = 0.65f)
            iceActive -> Color.White.copy(alpha = 0.90f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        }
        val cardBorder = when {
            premiumActive -> Color.White.copy(alpha = 0.22f)
            iceActive -> Color.White
            else -> Color.White.copy(alpha = 0.08f)
        }

        // --- SELECCIÓN DE TEMA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tema de la Aplicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                    if (premiumActive || iceActive) {
                        val swatches = if (premiumActive) {
                            listOf(PremiumTaupe, PremiumGold, Color.White, PremiumObsidian)
                        } else {
                            listOf(IceGradientTop, IceTeal, IceAmber, IceInk)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            swatches.forEach { swatch ->
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Glass" to "Glass",
                        "Premium" to "Premium",
                        "Ice Clean" to "Ice Clean"
                    ).forEach { (themeKey, label) ->
                        val isSelected = when (themeKey) {
                            "Premium" -> premiumActive
                            "Ice Clean" -> iceActive
                            else -> currentTheme == themeKey
                        }
                        val pillContainer = when {
                            isSelected && premiumActive -> Color.White
                            isSelected && iceActive -> IceInk
                            isSelected -> MaterialTheme.colorScheme.primary
                            premiumActive -> Color.Transparent
                            iceActive -> IceSurfaceMuted
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val pillContent = when {
                            isSelected && premiumActive -> PremiumObsidian
                            isSelected -> Color.White
                            premiumActive -> Color.White.copy(alpha = 0.85f)
                            iceActive -> IceInkSecondary
                            else -> Color.Gray
                        }
                        val pillBorder = when {
                            !isSelected && premiumActive -> BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            !isSelected && iceActive -> BorderStroke(1.dp, IceGradientTop)
                            else -> null
                        }

                        Button(
                            onClick = { onThemeChanged(themeKey) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(100.dp),
                            border = pillBorder,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pillContainer,
                                contentColor = pillContent
                            )
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Tarjeta de Identidad
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profilePhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profilePhotoUrl,
                            contentDescription = "Foto de Perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Iniciales con display tipográfico si no hay foto
                        Text(
                            text = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = textPrimary)
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (iceActive) IceGradientTop else Color.White.copy(alpha = 0.1f)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text("Nombre Completo", color = textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (iceActive) IceGradientTop else Color.White.copy(alpha = 0.1f),
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text("Dirección De Email", color = textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (iceActive) IceGradientTop else Color.White.copy(alpha = 0.1f),
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onSaveProfile,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).background(
                        if (iceActive) Brush.horizontalGradient(listOf(IceInk, Color(0xFF243447)))
                        else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                        RoundedCornerShape(100.dp)
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    val saveBtnContent = if (premiumActive) PremiumObsidian else Color.White
                    Icon(Icons.Default.Check, "Guardar", modifier = Modifier.size(16.dp), tint = saveBtnContent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Datos De Perfil", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = saveBtnContent)
                }

                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEC6A7C)),
                    border = BorderStroke(1.dp, Color(0xFFEC6A7C).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar Sesión", tint = Color(0xFFEC6A7C), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Métricas de Rescates Realizados por el Usuario
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = registrations.size.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text("Total Rescates", fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(if (iceActive) IceGradientTop else Color.White.copy(alpha = 0.1f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val syncedCount = registrations.count { it.isSynced }
                    Text(
                        text = syncedCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (iceActive) IceTeal else Color(0xFF81C784)
                    )
                    Text("Sincronizados", fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(if (iceActive) IceGradientTop else Color.White.copy(alpha = 0.1f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pendingCount = registrations.count { !it.isSynced }
                    Text(
                        text = pendingCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (iceActive) IceAmber else Color(0xFFEC6A7C)
                    )
                    Text("Pendientes", fontSize = 10.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sección del Historial
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Historial De Mascotas (${registrations.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (registrations.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no has registrado ninguna mascota.",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    registrations.forEach { pet ->
                        ProfilePetItem(
                            pet = pet,
                            onClick = { onPetClicked(pet) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilePetItem(
    pet: PetRegistration,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = pet.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = pet.locationName,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (pet.isSynced) Color(0xFF81C784).copy(alpha = 0.1f) else Color(0xFFEC6A7C).copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (pet.isSynced) "Sincro" else "Pendiente",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pet.isSynced) Color(0xFF81C784) else Color(0xFFEC6A7C)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver detalles",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
