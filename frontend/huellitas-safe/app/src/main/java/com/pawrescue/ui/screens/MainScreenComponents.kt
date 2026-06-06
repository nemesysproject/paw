package com.pawrescue.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pawrescue.data.model.PetRegistration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialFeedLayout(
    registrations: List<PetRegistration>,
    isSyncing: Boolean,
    onPetClicked: (PetRegistration) -> Unit,
    onShareRequested: (PetRegistration) -> Unit,
    onManualSyncRequested: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (registrations.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Pets, 
                    null, 
                    modifier = Modifier.size(64.dp), 
                    tint = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No hay reportes cerca", 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(registrations, key = { it.remoteId ?: it.id }) { pet ->
                    SocialPetCard(
                        pet = pet, 
                        onClicked = { onPetClicked(pet) },
                        onShare = { onShareRequested(pet) }
                    )
                }
            }
        }
        
        // Mini sync indicator
        if (isSyncing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocialPetCard(
    pet: PetRegistration,
    onClicked: () -> Unit,
    onShare: () -> Unit
) {
    val photoUrls = remember(pet.photosJson) { 
        if (pet.photosJson.isBlank()) emptyList() 
        else pet.photosJson.split(",").filter { it.isNotBlank() } 
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { onClicked() },
        shape = RoundedCornerShape(0.dp), // Full width look like social media
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            // Header: User / Location
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountCircle, null, tint = Color.White.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        pet.name.ifEmpty { "Mascota sin nombre" },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        pet.locationName,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val statusColor = when {
                    pet.status.contains("riesgo", true) || pet.status.contains("danger", true) -> Color(0xFFEC6A7C)
                    pet.status.contains("adopción", true) || pet.status.contains("adoption", true) -> Color(0xFF81C784)
                    else -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        pet.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            // Media Carousel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square like Instagram
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                if (photoUrls.isEmpty()) {
                    Icon(
                        Icons.Default.Pets, 
                        null, 
                        modifier = Modifier.size(80.dp).align(Alignment.Center), 
                        tint = Color.White.copy(alpha = 0.1f)
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { photoUrls.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = photoUrls[page],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Indicators
                    if (photoUrls.size > 1) {
                        Row(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(photoUrls.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape).background(color)
                                )
                            }
                        }
                    }
                }
                
                // Overlay text at the bottom of the image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = pet.description,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Interaction Bar
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.FavoriteBorder, null, tint = Color.White)
                }
                IconButton(onClick = onClicked) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Color.White)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClicked) {
                    Icon(Icons.Outlined.Info, null, tint = Color.White)
                }
            }
            
            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
        }
    }
}
