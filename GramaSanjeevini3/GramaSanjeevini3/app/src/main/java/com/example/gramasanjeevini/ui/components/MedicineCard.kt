package com.example.gramasanjeevini.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.models.StockItem

@Composable
fun MedicineCard(
    item: StockItem,
    isDarkMode: Boolean,
    onCallShop: (String) -> Unit,
    onAiGuide: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val isLifeSaving = item.medicineCategory == "Life Saving"
    val isLowStock   = item.quantity < 5
    val hasDistance  = item.distanceKm > 0.0

    val cardBorder = if (isLifeSaving)
        Modifier.border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
    else Modifier

    // Distance label — friendly text
    val distanceLabel = when {
        !hasDistance          -> "Distance unavailable"
        item.distanceKm < 1.0 -> "Less than 1 km away"
        item.distanceKm < 5.0 -> "%.1f km away — very close".format(item.distanceKm)
        item.distanceKm < 20.0 -> "%.1f km away — nearby".format(item.distanceKm)
        item.distanceKm < 50.0 -> "%.0f km away".format(item.distanceKm)
        else                  -> "%.0f km away".format(item.distanceKm)
    }

    // Availability banner color — green if close, amber if far
    val bannerColor = when {
        !hasDistance           -> if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        item.distanceKm < 10.0 -> if (isDarkMode) Color(0xFF14532D) else Color(0xFFF0FDF4)
        item.distanceKm < 30.0 -> if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFEFF6FF)
        else                   -> if (isDarkMode) Color(0xFF451A03) else Color(0xFFFFFBEB)
    }
    val bannerTextColor = when {
        !hasDistance           -> Color.Gray
        item.distanceKm < 10.0 -> if (isDarkMode) Color(0xFF86EFAC) else Color(0xFF166534)
        item.distanceKm < 30.0 -> if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
        else                   -> if (isDarkMode) Color(0xFFFCD34D) else Color(0xFF92400E)
    }
    val pillColor = when {
        !hasDistance           -> Color(0xFF94A3B8)
        item.distanceKm < 10.0 -> Color(0xFF22C55E)
        item.distanceKm < 30.0 -> Color(0xFF6366F1)
        else                   -> Color(0xFFD97706)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(cardBorder)
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors    = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape     = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Row 1: Medicine name + Life Saving badge ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = item.medicineName,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        text     = item.medicineCategory,
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
                if (isLifeSaving) {
                    Surface(color = Color(0xFFEF4444), shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("LIFE SAVING", color = Color.White,
                                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Availability banner — THE core villager message ───────────────
            Surface(
                color    = bannerColor,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint     = pillColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Available in ${item.shopVillage}",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = bannerTextColor
                        )
                        Text(
                            item.shopName,
                            fontSize = 12.sp,
                            color    = bannerTextColor.copy(alpha = 0.75f)
                        )
                    }
                    // Distance pill
                    Surface(color = pillColor, shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NearMe, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (hasDistance) "%.1f km".format(item.distanceKm) else "? km",
                                color      = Color.White,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Distance context line — plain language
            if (hasDistance) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null,
                        tint = pillColor.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        distanceLabel,
                        fontSize = 12.sp,
                        color    = pillColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Stock + expiry badges ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (isLowStock) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isLowStock) Icons.Default.Warning else Icons.Default.Inventory,
                            contentDescription = null,
                            tint     = if (isLowStock) Color(0xFFEF4444) else Color(0xFF22C55E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${item.quantity} in stock",
                            color      = if (isLowStock) Color(0xFFEF4444) else Color(0xFF22C55E),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (item.isNearExpiry) {
                    Surface(color = Color(0xFFFFFBEB), shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null,
                                tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Exp ${item.expiryDate}",
                                color = Color(0xFFD97706), fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                            if (item.discountPercent > 0) {
                                Spacer(Modifier.width(4.dp))
                                Surface(color = Color(0xFFEF4444), shape = RoundedCornerShape(4.dp)) {
                                    Text("${item.discountPercent}% OFF",
                                        color = Color.White, fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Expanded: call + AI guide ────────────────────────────────────
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                Spacer(Modifier.height(12.dp))

                // District + expiry row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("District", fontSize = 11.sp, color = Color.Gray)
                        Text(item.shopDistrict.ifBlank { "—" }, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expiry", fontSize = 11.sp, color = Color.Gray)
                        Text(item.expiryDate, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Call shop button
                Button(
                    onClick = { onCallShop(item.shopName) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call ${item.shopName}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                // AI Guide button
                OutlinedButton(
                    onClick  = onAiGuide,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Health Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
