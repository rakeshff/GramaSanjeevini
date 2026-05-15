package com.example.gramasanjeevini.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.data.StockRepository
import com.example.gramasanjeevini.models.MOCK_STOCK
import com.example.gramasanjeevini.ui.components.MedicineCard

@Composable
fun EmergencyScreen(isDarkMode: Boolean) {
    val context = LocalContext.current

    // Only Life Saving items from Firestore, sorted by distance
    var allStock by remember { mutableStateOf(com.example.gramasanjeevini.models.MOCK_STOCK) }
    LaunchedEffect(Unit) {
        com.example.gramasanjeevini.data.StockRepository.stockFlow().collect { items ->
            // Apply static distances as fallback so 0.0 is never shown
            val staticDistMap = com.example.gramasanjeevini.models.MOCK_STOCK.associate { it.id to it.distanceKm }
            val shopDistMap   = com.example.gramasanjeevini.models.MOCK_SHOPS.associate { it.id to it.distanceKm }
            allStock = items.map { item ->
                if (item.distanceKm == 0.0) {
                    val d = staticDistMap[item.id]
                        ?: shopDistMap[item.shopId]
                        ?: 0.0
                    item.copy(distanceKm = d)
                } else item
            }
        }
    }
    val emergencyItems = remember(allStock) {
        allStock
            .filter { it.medicineCategory == "Life Saving" }
            .sortedBy { it.distanceKm }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Alert banner ─────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = if (isDarkMode) Color(0xFF450A0A) else Color(0xFFFEF2F2),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Color(0xFFEF4444), shape = RoundedCornerShape(10.dp)) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Emergency",
                        tint     = Color.White,
                        modifier = Modifier.padding(8.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Life Saving Drugs",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 15.sp,
                        color      = if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                    )
                    Text(
                        "${emergencyItems.map { it.medicineId }.distinct().size} critical medicines " +
                        "across ${emergencyItems.map { it.shopId }.distinct().size} nearby shops",
                        fontSize = 12.sp,
                        color    = if (isDarkMode) Color(0xFFF87171) else Color(0xFFB91C1C)
                    )
                }
            }
        }

        // ── Unique medicine summary cards ────────────────────────────────────
        val uniqueMeds = emergencyItems.map { it.medicineName }.distinct()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uniqueMeds.forEach { medName ->
                val count = emergencyItems.count { it.medicineName == medName }
                Surface(
                    color = Color(0xFFEF4444),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "$medName ($count shop${if (count != 1) "s" else ""})",
                        color    = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Emergency stock list ─────────────────────────────────────────────
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(emergencyItems, key = { it.id }) { item ->
                MedicineCard(
                    item       = item,
                    isDarkMode = isDarkMode,
                    onCallShop = { shopName ->
                        Toast.makeText(context, "Calling $shopName…", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
