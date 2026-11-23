package com.pawrescue.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pawrescue.data.model.AdFeedItemDto

/**
 * Carrusel compacto para campañas destacadas (ej. jornadas de vacunación/esterilización
 * gubernamentales o promociones locales en HomeScreen).
 */
@Composable
fun CampaignBannerCarousel(
    ads: List<AdFeedItemDto>,
    onImpression: (String) -> Unit,
    onAdClick: (AdFeedItemDto) -> Unit,
    isGlassTheme: Boolean = false
) {
    if (ads.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Campañas y Aliados",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Patrocinado",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(ads, key = { it.id }) { ad ->
                LaunchedEffect(ad.id) {
                    onImpression(ad.id)
                }

                Card(
                    modifier = Modifier
                        .width(290.dp)
                        .clickable { onAdClick(ad) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGlassTheme) Color.White.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .background(Color.Black.copy(alpha = 0.25f))
                        ) {
                            AsyncImage(
                                model = ad.imageUrl,
                                contentDescription = ad.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                            AdBadgeChip(
                                label = ad.badgeLabel,
                                isGovernment = ad.isGovernment,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            )
                            Text(
                                text = ad.advertiserName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = ad.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!ad.description.isNullOrBlank()) {
                                Text(
                                    text = ad.description,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (ad.isGovernment) {
                                Text(
                                    text = "Aviso informativo independiente • Ver fuente oficial",
                                    color = Color(0xFF90CAF9),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ad.ctaLabel,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta nativa patrocinada para intercalar dentro de SocialFeedLayout sin interrumpir reportes.
 * Incluye etiqueta transparente ("Patrocinado" / "Campaña Pública") y aviso de no afiliación
 * gubernamental cuando aplica, cumpliendo con las políticas de Google Play Store.
 */
@Composable
fun SponsoredAdCard(
    ad: AdFeedItemDto,
    onImpression: (String) -> Unit,
    onAdClick: (AdFeedItemDto) -> Unit
) {
    LaunchedEffect(ad.id) {
        onImpression(ad.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onAdClick(ad) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        border = BorderStroke(
            1.dp,
            if (ad.isGovernment) Color(0xFF64B5F6).copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Column {
            // Header del anunciante
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!ad.advertiserLogoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ad.advertiserLogoUrl,
                        contentDescription = ad.advertiserName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (ad.isGovernment) Icons.Default.Verified else Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ad.advertiserName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = categoryDisplayName(ad.category),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                AdBadgeChip(label = ad.badgeLabel, isGovernment = ad.isGovernment)
            }

            // Creatividad / Banner
            AsyncImage(
                model = ad.imageUrl,
                contentDescription = ad.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            // Cuerpo + CTA
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = ad.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                if (!ad.description.isNullOrBlank()) {
                    Text(
                        text = ad.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (ad.isGovernment) {
                    Text(
                        text = "Huellitas Safe es una app independiente y no representa a ninguna entidad gubernamental. Toca para consultar la fuente oficial.",
                        color = Color(0xFF90CAF9),
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }

                Button(
                    onClick = { onAdClick(ad) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ad.isGovernment) Color(0xFF1976D2)
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = ad.ctaLabel, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdBadgeChip(
    label: String,
    isGovernment: Boolean,
    modifier: Modifier = Modifier
) {
    val chipColor = if (isGovernment) Color(0xFF64B5F6) else Color(0xFFFFB74D)
    Surface(
        modifier = modifier,
        color = chipColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.55f))
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = chipColor
        )
    }
}

private fun categoryDisplayName(category: String): String = when (category.uppercase()) {
    "PET_FOOD" -> "Alimento para mascotas"
    "VETERINARY" -> "Veterinaria y salud"
    "GOVERNMENT" -> "Campaña pública"
    "PET_SHOP" -> "Tienda y cuidados"
    "SHELTER_EVENT" -> "Jornada de adopción"
    else -> "Aliado PAW"
}
