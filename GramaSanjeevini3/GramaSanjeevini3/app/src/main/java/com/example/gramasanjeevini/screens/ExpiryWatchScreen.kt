package com.example.gramasanjeevini.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.data.StockRepository
import com.example.gramasanjeevini.models.MOCK_SHOPS
import com.example.gramasanjeevini.models.MOCK_STOCK
import com.example.gramasanjeevini.models.StockItem
import kotlinx.coroutines.launch

@Composable
fun ExpiryWatchScreen(isDarkMode: Boolean, isPharmacist: Boolean) {
    val context = LocalContext.current

    var allStock by remember { mutableStateOf(MOCK_STOCK) }
    LaunchedEffect(Unit) {
        StockRepository.stockFlow().collect { items ->
            val staticDistMap = MOCK_STOCK.associate { it.id to it.distanceKm }
            val shopDistMap   = MOCK_SHOPS.associate { it.id to it.distanceKm }
            allStock = items.map { item ->
                if (item.distanceKm == 0.0) {
                    val d = staticDistMap[item.id] ?: shopDistMap[item.shopId] ?: 0.0
                    item.copy(distanceKm = d)
                } else item
            }
        }
    }
    val nearExpiryItems = remember(allStock) {
        allStock
            .filter { it.isNearExpiry }
            .sortedByDescending { it.discountPercent }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Header banner ────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = if (isDarkMode) Color(0xFF451A03) else Color(0xFFFFFBEB),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(10.dp)) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = "Expiry",
                        tint     = Color.White,
                        modifier = Modifier.padding(8.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (isPharmacist) "Expiry Alerts — Action Required"
                        else "Near-Expiry Discounts",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 15.sp,
                        color      = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFF92400E)
                    )
                    Text(
                        "${nearExpiryItems.size} item${if (nearExpiryItems.size != 1) "s" else ""} " +
                        "expiring within 3 months — buy at discount",
                        fontSize = 12.sp,
                        color    = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309)
                    )
                }
            }
        }

        if (nearExpiryItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        modifier = Modifier.size(64.dp), tint = Color(0xFF22C55E))
                    Spacer(Modifier.height(12.dp))
                    Text("All stock is fresh!", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Pharmacist sees a "Mark as Discounted" action; villager sees buy info
                if (isPharmacist) {
                    item {
                        Text(
                            "Tap a card to expand and take action",
                            fontSize = 12.sp,
                            color    = Color.Gray,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }
                items(nearExpiryItems, key = { it.id }) { item ->
                    ExpiryItemCard(
                        item         = item,
                        isDarkMode   = isDarkMode,
                        isPharmacist = isPharmacist,
                        onAction     = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun ExpiryItemCard(
    item: StockItem,
    isDarkMode: Boolean,
    isPharmacist: Boolean,
    onAction: (String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Discount dialog state
    var showDiscountDialog by remember { mutableStateOf(false) }
    var discountText       by remember { mutableStateOf(if (item.discountPercent > 0) item.discountPercent.toString() else "") }
    var isSaving           by remember { mutableStateOf(false) }
    var discountError      by remember { mutableStateOf("") }

    // Live discount value (updates after save)
    var currentDiscount by remember { mutableStateOf(item.discountPercent) }

    // ── Set Discount dialog ───────────────────────────────────────────────────
    if (showDiscountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showDiscountDialog = false },
            containerColor   = if (isDarkMode) Color(0xFF1E293B) else Color.White,
            title = {
                Text("Set Discount", fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF1E293B))
            },
            text = {
                Column {
                    Text(item.medicineName, fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706), fontSize = 15.sp)
                    Text("${item.shopName} • Exp ${item.expiryDate}",
                        fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value         = discountText,
                        onValueChange = { v ->
                            discountText  = v.filter { it.isDigit() }.take(2)
                            discountError = ""
                        },
                        label         = { Text("Discount %  (1–99)") },
                        placeholder   = { Text("e.g. 20") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        isError       = discountError.isNotBlank(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon   = {
                            Icon(Icons.Default.Percent, contentDescription = null,
                                tint = Color(0xFFD97706))
                        }
                    )
                    if (discountError.isNotBlank()) {
                        Text(discountError, fontSize = 11.sp, color = Color(0xFFEF4444),
                            modifier = Modifier.padding(start = 4.dp, top = 3.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("This marks the item as near-expiry and shows the discount badge to all villagers.",
                        fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pct = discountText.toIntOrNull()
                        if (pct == null || pct < 1 || pct > 99) {
                            discountError = "Enter a value between 1 and 99"
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            val result = try {
                                kotlinx.coroutines.withTimeout(10_000L) {
                                    StockRepository.updateDiscount(item.id, pct)
                                }
                            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                                Result.failure(Exception("Timed out. Check connection."))
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                            isSaving = false
                            showDiscountDialog = false
                            if (result.isSuccess) {
                                currentDiscount = pct
                                onAction("✓ ${item.medicineName} — ${pct}% discount set. Visible to all villagers!")
                            } else {
                                onAction("Failed: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Saving…")
                    } else {
                        Text("Apply Discount", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }, enabled = !isSaving) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp),
        shape     = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.medicineName, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    Text(item.shopName + " • " + item.shopVillage,
                        fontSize = 12.sp, color = Color.Gray)
                }
                Surface(color = Color(0xFFFFFBEB), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "Exp ${item.expiryDate}",
                        color    = Color(0xFFD97706),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Qty
                Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(8.dp)) {
                    Text("${item.quantity} units",
                        color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                // Discount badge — shows live value
                if (currentDiscount > 0) {
                    Surface(color = Color(0xFFEF4444), shape = RoundedCornerShape(8.dp)) {
                        Text("${currentDiscount}% OFF",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                // Action button
                Button(
                    onClick = {
                        if (isPharmacist) {
                            discountText = if (currentDiscount > 0) currentDiscount.toString() else ""
                            discountError = ""
                            showDiscountDialog = true
                        } else {
                            onAction("Calling ${item.shopName}…")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPharmacist) Color(0xFFD97706) else Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (isPharmacist) Icons.Default.Percent else Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isPharmacist) {
                            if (currentDiscount > 0) "Update Discount" else "Set Discount"
                        } else "Call Shop",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
