package com.example.gramasanjeevini.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.gramasanjeevini.models.MOCK_SHOPS
import com.example.gramasanjeevini.models.MOCK_STOCK
import com.example.gramasanjeevini.models.Shop
import kotlinx.coroutines.launch

@Composable
fun NetworkScreen(isDarkMode: Boolean) {
    val context = LocalContext.current

    var liveStock by remember { mutableStateOf(MOCK_STOCK) }
    var allShops  by remember { mutableStateOf(MOCK_SHOPS) }

    LaunchedEffect(Unit) {
        launch { StockRepository.shopsFlow().collect { allShops = it } }
        StockRepository.stockFlow().collect { liveStock = it }
    }

    // Group shops by district
    val shopsByDistrict = allShops.groupBy { it.district.ifBlank { "Other" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Pharmacy Network", fontWeight = FontWeight.Black, fontSize = 22.sp,
            color = if (isDarkMode) Color.White else Color(0xFF1E293B))
        Text("Karnataka — ${allShops.size} Registered Shops", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(20.dp))

        // Network health card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = Color(0xFF6366F1))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Network Health", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    NetworkStat("${allShops.size}", "Shops", Color.White)
                    NetworkStat("${liveStock.size}", "Items", Color.White)
                    NetworkStat("${liveStock.count { it.medicineCategory == "Life Saving" }}", "Critical", Color(0xFFFCA5A5))
                    NetworkStat("${liveStock.count { it.isNearExpiry }}", "Expiring", Color(0xFFFCD34D))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Pharmacist-registered shops highlight
        val myShops = allShops.filter { it.isRegisteredByPharmacist }
        if (myShops.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color    = if (isDarkMode) Color(0xFF14532D) else Color(0xFFF0FDF4),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null,
                            tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pharmacist-Registered Shops", fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = if (isDarkMode) Color(0xFF86EFAC) else Color(0xFF166534))
                    }
                    Spacer(Modifier.height(8.dp))
                    myShops.forEach { shop ->
                        Text("• ${shop.name} — ${shop.village}, ${shop.district}",
                            fontSize = 13.sp, color = if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF15803D),
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        // Per-district breakdown
        shopsByDistrict.forEach { (district, shops) ->
            Text(district, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                color = Color(0xFF6366F1), modifier = Modifier.padding(bottom = 8.dp))

            shops.forEach { shop ->
                val shopItems  = liveStock.filter { it.shopId == shop.id }
                val lifeSaving = shopItems.count { it.medicineCategory == "Life Saving" }
                val nearExpiry = shopItems.count { it.isNearExpiry }
                val lowStock   = shopItems.count { it.quantity < 5 }

                Card(
                    modifier  = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (shop.isRegisteredByPharmacist) Color(0xFF22C55E) else Color(0xFF6366F1),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (shop.isRegisteredByPharmacist) Icons.Default.Store else Icons.Default.LocalPharmacy,
                                    contentDescription = null, tint = Color.White,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                                    if (shop.isRegisteredByPharmacist) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(color = Color(0xFF22C55E), shape = RoundedCornerShape(4.dp)) {
                                            Text("MY SHOP", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("${shop.village} • ${shop.district}",
                                    fontSize = 12.sp, color = Color.Gray)
                            }
                            Surface(color = Color(0xFF22C55E), shape = RoundedCornerShape(50)) {
                                Text("ONLINE", color = Color.White, fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        if (shopItems.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                            Spacer(Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                ShopStat("${shopItems.size}", "Total", Color(0xFF6366F1), isDarkMode)
                                ShopStat("$lifeSaving", "Critical", Color(0xFFEF4444), isDarkMode)
                                ShopStat("$nearExpiry", "Expiring", Color(0xFFD97706), isDarkMode)
                                ShopStat("$lowStock", "Low Stock", Color(0xFFEF4444), isDarkMode)
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text("No stock added yet — tap 'Add Medicine' to add",
                                fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick  = { Toast.makeText(context, "Calling ${shop.name}…", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1))
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick  = { Toast.makeText(context, "Alert sent to ${shop.name}", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Alert", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun NetworkStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.8f))
    }
}

@Composable
fun ShopStat(value: String, label: String, color: Color, isDarkMode: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp,
            color = if (isDarkMode) Color.White else Color(0xFF1E293B))
        Text(label, fontSize = 10.sp, color = color)
    }
}
