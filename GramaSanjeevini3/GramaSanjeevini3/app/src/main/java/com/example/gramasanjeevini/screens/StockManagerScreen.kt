package com.example.gramasanjeevini.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.data.StockRepository
import com.example.gramasanjeevini.models.MOCK_SHOPS
import com.example.gramasanjeevini.models.MOCK_STOCK
import com.example.gramasanjeevini.models.Shop
import com.example.gramasanjeevini.models.StockItem
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockManagerScreen(isDarkMode: Boolean) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var stockList    by remember { mutableStateOf(MOCK_STOCK) }
    var allShops     by remember { mutableStateOf(MOCK_SHOPS) }
    var isLoading    by remember { mutableStateOf(true) }
    var editingItem  by remember { mutableStateOf<StockItem?>(null) }
    var showAddForm  by remember { mutableStateOf(false) }
    var showRegShop  by remember { mutableStateOf(false) }
    var filterShop   by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        StockRepository.seedIfEmpty()
        StockRepository.seedShopsIfEmpty()
        launch { StockRepository.shopsFlow().collect { allShops = it } }
        StockRepository.stockFlow().collect { items ->
            stockList = items
            isLoading = false
        }
    }

    val shopNames = listOf("All") + stockList.map { it.shopName }.distinct()
    val filtered  = if (filterShop == "All") stockList
                    else stockList.filter { it.shopName == filterShop }

    editingItem?.let { item ->
        StockEditDialog(
            item = item, isDarkMode = isDarkMode,
            onDismiss = { editingItem = null },
            onSave = { newQty ->
                scope.launch {
                    StockRepository.updateQuantity(item.id, newQty)
                    editingItem = null
                    Toast.makeText(context, "✓ Stock updated", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = {
                scope.launch {
                    StockRepository.deleteStockItem(item.id)
                    editingItem = null
                    Toast.makeText(context, "Item removed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAddForm) {
        AddMedicineSheet(
            isDarkMode = isDarkMode,
            allShops   = allShops,
            onDismiss  = { showAddForm = false },
            onAdd      = { newItem ->
                scope.launch {
                    val result = StockRepository.addStockItem(newItem)
                    showAddForm = false
                    if (result.isSuccess)
                        Toast.makeText(context, "✓ ${newItem.medicineName} added — visible to all villagers!", Toast.LENGTH_LONG).show()
                    else
                        Toast.makeText(context, "Save failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showRegShop) {
        RegisterShopSheet(
            isDarkMode = isDarkMode,
            onDismiss  = { showRegShop = false },
            onRegister = { shop ->
                scope.launch {
                    val result = try {
                        kotlinx.coroutines.withTimeout(10_000L) {
                            StockRepository.registerShop(shop)
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Result.failure(Exception("Registration timed out. Check your internet connection."))
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                    showRegShop = false                 // always close so button resets
                    if (result.isSuccess)
                        Toast.makeText(context, "✓ ${shop.name} registered! Now add your medicines.", Toast.LENGTH_LONG).show()
                    else
                        Toast.makeText(context, "Registration failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Register shop FAB
                SmallFloatingActionButton(
                    onClick        = { showRegShop = true },
                    containerColor = Color(0xFF22C55E),
                    contentColor   = Color.White
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = "Register Shop")
                }
                // Add medicine FAB
                ExtendedFloatingActionButton(
                    onClick        = { showAddForm = true },
                    containerColor = Color(0xFF6366F1),
                    contentColor   = Color.White,
                    icon           = { Icon(Icons.Default.Add, contentDescription = null) },
                    text           = { Text("Add Medicine", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stock Manager", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    Text("${stockList.size} medicines • ${allShops.size} shops in network",
                        fontSize = 12.sp, color = Color.Gray)
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF6366F1))
                } else {
                    Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                        }
                    }
                }
            }

            // Register shop hint banner
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                color    = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF0FDF4),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null,
                        tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tap  to register your shop, then  to add medicines",
                        fontSize = 11.sp, color = Color.Gray)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Shop filter chips
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shopNames) { shop ->
                            val label = if (shop == "All") "All" else shop.split(" ").first()
                            FilterChip(
                                selected = filterShop == shop,
                                onClick  = { filterShop = shop },
                                label    = { Text(label, fontSize = 11.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1),
                                    selectedLabelColor     = Color.White
                                )
                            )
                        }
                    }
                }
                item {
                    Text("${filtered.size} items", fontSize = 12.sp, color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
                }
                items(filtered, key = { it.id }) { item ->
                    StockItemRow(item = item, isDarkMode = isDarkMode, onEdit = { editingItem = item })
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

// ── Register My Shop bottom sheet ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterShopSheet(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onRegister: (Shop) -> Unit
) {
    var shopName   by remember { mutableStateOf("") }
    var village    by remember { mutableStateOf("") }
    var district   by remember { mutableStateOf("") }
    var phone      by remember { mutableStateOf("") }
    var ownerName  by remember { mutableStateOf("") }
    var latText    by remember { mutableStateOf("") }
    var lngText    by remember { mutableStateOf("") }

    var nameErr    by remember { mutableStateOf("") }
    var villageErr by remember { mutableStateOf("") }
    var phoneErr   by remember { mutableStateOf("") }
    var ownerErr   by remember { mutableStateOf("") }

    val districts = listOf("Gadag", "Hassan", "Dakshina Kannada", "Udupi", "Bangalore", "Other")
    var isSaving   by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var ok = true
        nameErr = ""; villageErr = ""; phoneErr = ""; ownerErr = ""
        if (shopName.isBlank())  { nameErr   = "Shop name is required"; ok = false }
        if (village.isBlank())   { villageErr = "Village/Town is required"; ok = false }
        if (phone.isBlank() || phone.length < 10) { phoneErr = "Enter valid 10-digit number"; ok = false }
        if (ownerName.isBlank()) { ownerErr  = "Owner name is required"; ok = false }
        return ok
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF22C55E), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null,
                        tint = Color.White, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Register My Shop", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    Text("Your shop joins the GramaSanjeevini network",
                        fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Info banner
            Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null,
                        tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Once registered, villagers can find your shop and the medicines you add will be visible to everyone in the network.",
                        fontSize = 12.sp, color = Color(0xFF1D4ED8), lineHeight = 18.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            AddFormField(value = shopName, onValueChange = { shopName = it; nameErr = "" },
                label = "Shop Name *", placeholder = "e.g. Shiva Medicals",
                icon = Icons.Default.LocalPharmacy, error = nameErr, isDarkMode = isDarkMode)
            Spacer(Modifier.height(12.dp))

            AddFormField(value = ownerName, onValueChange = { ownerName = it; ownerErr = "" },
                label = "Owner Name *", placeholder = "Your full name",
                icon = Icons.Default.Person, error = ownerErr, isDarkMode = isDarkMode)
            Spacer(Modifier.height(12.dp))

            AddFormField(value = village, onValueChange = { village = it; villageErr = "" },
                label = "Village / Town *", placeholder = "e.g. Mangalore",
                icon = Icons.Default.LocationCity, error = villageErr, isDarkMode = isDarkMode)
            Spacer(Modifier.height(12.dp))

            // District selector
            Text("District *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(districts) { d ->
                    FilterChip(
                        selected = district == d,
                        onClick  = { district = d },
                        label    = { Text(d, fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor     = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            AddFormField(value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10); phoneErr = "" },
                label = "Phone Number *", placeholder = "10-digit mobile",
                icon = Icons.Default.Phone, error = phoneErr,
                keyboardType = KeyboardType.Phone, isDarkMode = isDarkMode)
            Spacer(Modifier.height(12.dp))

            // Optional GPS
            Text("GPS Coordinates (optional — for accurate distance)",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latText, onValueChange = { latText = it },
                    label = { Text("Latitude", fontSize = 12.sp) },
                    placeholder = { Text("e.g. 12.87", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        focusedContainerColor   = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    )
                )
                OutlinedTextField(
                    value = lngText, onValueChange = { lngText = it },
                    label = { Text("Longitude", fontSize = 12.sp) },
                    placeholder = { Text("e.g. 74.84", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        focusedContainerColor   = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    )
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (validate() && !isSaving) {
                        isSaving = true
                        onRegister(
                            Shop(
                                id       = "pharmacist_${UUID.randomUUID()}",
                                name     = shopName.trim(),
                                village  = village.trim(),
                                district = district.ifBlank { "Other" },
                                phone    = phone,
                                ownerName = ownerName.trim(),
                                lat      = latText.toDoubleOrNull() ?: 0.0,
                                lng      = lngText.toDoubleOrNull() ?: 0.0,
                                isRegisteredByPharmacist = true
                            )
                        )
                        // isSaving resets automatically when the sheet is dismissed by the parent
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                enabled  = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Registering… (up to 10s)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Register Shop", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Add Medicine bottom sheet ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineSheet(
    isDarkMode: Boolean,
    allShops: List<com.example.gramasanjeevini.models.Shop>,
    onDismiss: () -> Unit,
    onAdd: (StockItem) -> Unit
) {
    var medicineName by remember { mutableStateOf("") }
    var category     by remember { mutableStateOf("Essential") }
    var selectedShop by remember { mutableStateOf(allShops.firstOrNull()) }
    var quantity     by remember { mutableStateOf("") }
    var expiryDate   by remember { mutableStateOf("") }
    var nameError    by remember { mutableStateOf("") }
    var qtyError     by remember { mutableStateOf("") }
    var expiryError  by remember { mutableStateOf("") }
    var shopError    by remember { mutableStateOf("") }
    val categories   = listOf("Life Saving", "Essential", "General")

    fun validate(): Boolean {
        var ok = true
        nameError = ""; qtyError = ""; expiryError = ""; shopError = ""
        if (medicineName.isBlank()) { nameError = "Medicine name is required"; ok = false }
        if (quantity.isBlank() || quantity.toIntOrNull() == null) { qtyError = "Enter a valid quantity"; ok = false }
        if (expiryDate.isBlank()) { expiryError = "Enter expiry (MM/YYYY)"; ok = false }
        else if (!expiryDate.matches(Regex("\\d{2}/\\d{4}"))) { expiryError = "Format: MM/YYYY"; ok = false }
        if (selectedShop == null) { shopError = "Select a shop"; ok = false }
        return ok
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF6366F1), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = Color.White, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add Medicine to Network", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    Text("Instantly visible to all villagers searching", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(20.dp))

            AddFormField(value = medicineName, onValueChange = { medicineName = it; nameError = "" },
                label = "Medicine Name *", placeholder = "e.g. Azithromycin 500mg",
                icon = Icons.Default.Medication, error = nameError, isDarkMode = isDarkMode)
            Spacer(Modifier.height(14.dp))

            Text("Category *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat, onClick = { category = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (cat) {
                                "Life Saving" -> Color(0xFFEF4444)
                                "Essential"   -> Color(0xFFD97706)
                                else          -> Color(0xFF22C55E)
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // Shop selector — shows ALL shops including pharmacist-registered ones
            Text("Select Shop *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            if (shopError.isNotBlank()) {
                Text(shopError, fontSize = 11.sp, color = Color(0xFFEF4444))
            }
            Spacer(Modifier.height(6.dp))
            allShops.forEach { shop ->
                val selected = selectedShop?.id == shop.id
                OutlinedButton(
                    onClick  = { selectedShop = shop },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color(0xFF6366F1) else Color.Transparent,
                        contentColor   = if (selected) Color.White else Color.Gray
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) Color.Transparent
                        else if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(
                        if (shop.isRegisteredByPharmacist) Icons.Default.Store else Icons.Default.LocalPharmacy,
                        contentDescription = null, modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text("${shop.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${shop.village} • ${shop.district}", fontSize = 11.sp)
                    }
                    if (shop.isRegisteredByPharmacist) {
                        Surface(color = Color(0xFF22C55E).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("MY SHOP", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF22C55E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            AddFormField(value = quantity, onValueChange = { quantity = it.filter { c -> c.isDigit() }; qtyError = "" },
                label = "Quantity in Stock *", placeholder = "e.g. 50",
                icon = Icons.Default.Inventory, error = qtyError,
                keyboardType = KeyboardType.Number, isDarkMode = isDarkMode)
            Spacer(Modifier.height(14.dp))

            AddFormField(value = expiryDate, onValueChange = { expiryDate = it; expiryError = "" },
                label = "Expiry Date *", placeholder = "MM/YYYY  e.g. 12/2026",
                icon = Icons.Default.CalendarMonth, error = expiryError, isDarkMode = isDarkMode)
            Spacer(Modifier.height(28.dp))

            var isSaving by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (validate() && !isSaving) {
                        isSaving = true
                        val shop = selectedShop!!
                        onAdd(
                            StockItem(
                                id               = UUID.randomUUID().toString(),
                                medicineId       = "custom_${System.currentTimeMillis()}",
                                medicineName     = medicineName.trim(),
                                medicineCategory = category,
                                shopId           = shop.id,
                                shopName         = shop.name,
                                shopVillage      = shop.village,
                                shopDistrict     = shop.district,
                                shopLat          = shop.lat,
                                shopLng          = shop.lng,
                                distanceKm       = shop.distanceKm,
                                quantity         = quantity.toInt(),
                                expiryDate       = expiryDate.trim(),
                                isNearExpiry     = isNearExpiry(expiryDate.trim()),
                                discountPercent  = 0
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                enabled  = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Saving…", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Network", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun isNearExpiry(expiryDate: String): Boolean {
    return try {
        val parts = expiryDate.split("/")
        val month = parts[0].toInt(); val year = parts[1].toInt()
        val now = java.util.Calendar.getInstance()
        val nowM = now.get(java.util.Calendar.MONTH) + 1
        val nowY = now.get(java.util.Calendar.YEAR)
        val diff = (year - nowY) * 12 + (month - nowM)
        diff in 0..3
    } catch (_: Exception) { false }
}

@Composable
fun AddFormField(
    value: String, onValueChange: (String) -> Unit, label: String,
    placeholder: String = "", icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String = "", keyboardType: KeyboardType = KeyboardType.Text, isDarkMode: Boolean
) {
    Column {
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            label = { Text(label, fontSize = 13.sp) },
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            singleLine = true, isError = error.isNotBlank(),
            leadingIcon = { Icon(icon, contentDescription = null, tint = if (error.isNotBlank()) Color(0xFFEF4444) else Color(0xFF6366F1)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                focusedContainerColor   = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                focusedBorderColor = Color(0xFF6366F1), errorBorderColor = Color(0xFFEF4444)
            )
        )
        if (error.isNotBlank()) {
            Row(modifier = Modifier.padding(start = 14.dp, top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(error, fontSize = 11.sp, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun StockItemRow(item: StockItem, isDarkMode: Boolean, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(10.dp), shape = RoundedCornerShape(50),
                color = when (item.medicineCategory) {
                    "Life Saving" -> Color(0xFFEF4444)
                    "Essential"   -> Color(0xFFD97706)
                    else          -> Color(0xFF22C55E)
                }
            ) {}
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.medicineName, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                Text("${item.shopName} • ${item.shopVillage} • Exp ${item.expiryDate}",
                    fontSize = 11.sp, color = Color.Gray)
            }
            Surface(
                color = if (item.quantity < 5) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("${item.quantity}", color = if (item.quantity < 5) Color(0xFFEF4444) else Color(0xFF22C55E),
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun StockEditDialog(item: StockItem, isDarkMode: Boolean, onDismiss: () -> Unit, onSave: (Int) -> Unit, onDelete: () -> Unit) {
    var qtyText     by remember { mutableStateOf(item.quantity.toString()) }
    var showConfirm by remember { mutableStateOf(false) }
    val qty = qtyText.toIntOrNull() ?: 0

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor   = if (isDarkMode) Color(0xFF1E293B) else Color.White,
            title = { Text("Delete Item?", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF1E293B)) },
            text  = { Text("Remove ${item.medicineName} from ${item.shopName}?", color = Color.Gray) },
            confirmButton = {
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), shape = RoundedCornerShape(10.dp)) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = Color.Gray) } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = if (isDarkMode) Color(0xFF1E293B) else Color.White,
        title = { Text("Update Stock", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF1E293B)) },
        text = {
            Column {
                Text(item.medicineName, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1), fontSize = 15.sp)
                Text("${item.shopName} • ${item.shopVillage} • Exp ${item.expiryDate}", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = qtyText, onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantity in stock") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (qty >= 0) onSave(qty) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp)) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showConfirm = true }) { Text("Delete", color = Color(0xFFEF4444)) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
            }
        }
    )
}
