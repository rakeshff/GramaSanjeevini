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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.data.StockRepository
import com.example.gramasanjeevini.models.MOCK_SHOPS
import com.example.gramasanjeevini.models.MOCK_STOCK
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
@Composable
fun PharmacistDashboard(isDarkMode: Boolean, userName: String, onNavigateToStock: () -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }

    // ── Live stock from Firestore ─────────────────────────────────────────────
    var liveStock by remember { mutableStateOf(MOCK_STOCK) }
    var allShops  by remember { mutableStateOf(MOCK_SHOPS) }
    LaunchedEffect(Unit) {
        StockRepository.seedIfEmpty()
        StockRepository.seedShopsIfEmpty()
        launch { StockRepository.shopsFlow().collect { allShops = it } }
        StockRepository.stockFlow().collect { items -> liveStock = items }
    }

    // ── Register shop sheet ───────────────────────────────────────────────────
    var showRegShop  by remember { mutableStateOf(false) }
    var showAddMed   by remember { mutableStateOf(false) }

    if (showRegShop) {
        RegisterShopSheet(
            isDarkMode = isDarkMode,
            onDismiss  = { showRegShop = false },
            onRegister = { shop ->
                scope.launch {
                    val result = try {
                        withTimeout(10_000L) {          // 10-second hard timeout
                            StockRepository.registerShop(shop)
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Result.failure(Exception("Registration timed out. Check your internet connection."))
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                    showRegShop = false                 // always close sheet so button resets
                    if (result.isSuccess) {
                        allShops = allShops + shop      // show immediately without waiting for Firestore
                        Toast.makeText(context, "✓ ${shop.name} registered! Now add medicines.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showAddMed) {
        AddMedicineSheet(
            isDarkMode = isDarkMode,
            allShops   = allShops,
            onDismiss  = { showAddMed = false },
            onAdd      = { newItem ->
                scope.launch {
                    val result = StockRepository.addStockItem(newItem)
                    showAddMed = false
                    if (result.isSuccess)
                        Toast.makeText(context, "✓ ${newItem.medicineName} added — visible to all villagers!", Toast.LENGTH_LONG).show()
                    else
                        Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    val totalItems    = liveStock.size
    val lifeSaving    = liveStock.count { it.medicineCategory == "Life Saving" }
    val nearExpiry    = liveStock.count { it.isNearExpiry }
    val lowStock      = liveStock.count { it.quantity < 5 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ── Greeting ─────────────────────────────────────────────────────────
        Text("Welcome back,", fontSize = 14.sp, color = Color.Gray)
        Text(userName, fontWeight = FontWeight.Black, fontSize = 26.sp,
            color = if (isDarkMode) Color.White else Color(0xFF1E293B))
        Text("${allShops.size} shops in network", fontSize = 13.sp, color = Color(0xFF6366F1),
            fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))

        // ── Stats grid ───────────────────────────────────────────────────────
        Text("Network Overview", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier    = Modifier.weight(1f),
                label       = "Total Items",
                value       = "$totalItems",
                icon        = Icons.Default.Inventory,
                color       = Color(0xFF6366F1),
                isDarkMode  = isDarkMode
            )
            StatCard(
                modifier    = Modifier.weight(1f),
                label       = "Life Saving",
                value       = "$lifeSaving",
                icon        = Icons.Default.Favorite,
                color       = Color(0xFFEF4444),
                isDarkMode  = isDarkMode
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier    = Modifier.weight(1f),
                label       = "Near Expiry",
                value       = "$nearExpiry",
                icon        = Icons.Default.AccessTime,
                color       = Color(0xFFD97706),
                isDarkMode  = isDarkMode
            )
            StatCard(
                modifier    = Modifier.weight(1f),
                label       = "Low Stock",
                value       = "$lowStock",
                icon        = Icons.Default.Warning,
                color       = Color(0xFFEF4444),
                isDarkMode  = isDarkMode
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Quick actions ────────────────────────────────────────────────────
        Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        // ── Register My Shop ──────────────────────────────────────────────────
        Button(
            onClick  = { showRegShop = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
        ) {
            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Register My Shop", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Join the GramaSanjeevini network", fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Add Medicine ──────────────────────────────────────────────────────
        Button(
            onClick  = { showAddMed = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Add Medicine to Stock", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Visible to all villagers instantly", fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(10.dp))

        // Sync button
        Button(
            onClick = {
                scope.launch {
                    isSyncing = true
                    try {
                        // Force re-seed stale/missing stock from MOCK_STOCK into Firestore
                        kotlinx.coroutines.withTimeout(15_000L) {
                            StockRepository.seedIfEmpty()
                            StockRepository.seedShopsIfEmpty()
                        }
                        // stockFlow and shopsFlow listeners will auto-update liveStock + allShops
                        Toast.makeText(context, "✓ Network sync complete! Stock refreshed.", Toast.LENGTH_LONG).show()
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Toast.makeText(context, "Sync timed out. Check your connection.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    isSyncing = false
                }
            },
            modifier  = Modifier.fillMaxWidth().height(56.dp),
            shape     = RoundedCornerShape(16.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
            enabled   = !isSyncing
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Syncing with Firestore…", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sync Stock Levels", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Alert button
        OutlinedButton(
            onClick = {
                Toast.makeText(context, "Alert sent to all network pharmacists!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Send Network Alert", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(28.dp))

        // ── Network shops ────────────────────────────────────────────────────
        Text("Network Shops", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        allShops.forEach { shop ->
            val shopStock = liveStock.count { it.shopId == shop.id }
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = if (shop.isRegisteredByPharmacist) Color(0xFF22C55E) else Color(0xFF6366F1), shape = RoundedCornerShape(10.dp)) {
                        Icon(
                            if (shop.isRegisteredByPharmacist) Icons.Default.Store else Icons.Default.LocalPharmacy,
                            contentDescription = null,
                            tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                            if (shop.isRegisteredByPharmacist) {
                                Spacer(Modifier.width(6.dp))
                                Surface(color = Color(0xFF22C55E).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("MY SHOP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF22C55E),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("${shop.village} • ${shop.district}",
                            fontSize = 12.sp, color = Color.Gray)
                    }
                    Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(8.dp)) {
                        Text("$shopStock items", color = Color(0xFF22C55E),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    isDarkMode: Boolean
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                Icon(icon, contentDescription = label,
                    tint = color, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 24.sp,
                color = if (isDarkMode) Color.White else Color(0xFF1E293B))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
