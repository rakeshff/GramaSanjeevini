package com.example.gramasanjeevini.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gramasanjeevini.data.StockRepository
import com.example.gramasanjeevini.data.withRealDistance
import com.example.gramasanjeevini.models.MOCK_SHOPS
import com.example.gramasanjeevini.models.MOCK_STOCK
import com.example.gramasanjeevini.models.SYMPTOM_MAP
import com.example.gramasanjeevini.models.StockItem
import com.example.gramasanjeevini.ui.components.MedicineCard
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val GEMINI_KEY   = "AIzaSyApoqeFefV-_-BJTjBUyIfT6weI1AuZXHw"
private const val GEMINI_MODEL = "gemini-1.5-flash"

// Shared model instance — avoids recreating on every search
private val searchGeminiModel by lazy {
    GenerativeModel(modelName = GEMINI_MODEL, apiKey = GEMINI_KEY)
}

// ── Search mode ───────────────────────────────────────────────────────────────
private enum class SearchMode { MEDICINE, SYMPTOM }

// ── AI symptom result ─────────────────────────────────────────────────────────
data class AiSymptomResult(
    val medicineNames: List<String>,   // names Gemini suggested
    val aiExplanation: String,         // short explanation to show user
    val stockMatches: List<StockItem>  // items from MOCK_STOCK that match
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(isDarkMode: Boolean) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var query     by remember { mutableStateOf("") }
    var mode      by remember { mutableStateOf(SearchMode.MEDICINE) }
    var filterCat by remember { mutableStateOf("All") }
    var maxDistKm by remember { mutableStateOf(500f) }  // default 500km to show all shops

    // ── GPS location for real distance calculation ────────────────────────────
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    var locationLabel by remember { mutableStateOf("Static distances (no GPS)") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                try {
                    val fused = LocationServices.getFusedLocationProviderClient(context)
                    val loc = fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                        ?: fused.lastLocation.await()
                    if (loc != null) {
                        userLat = loc.latitude
                        userLng = loc.longitude
                        locationLabel = "Your location"
                    } else {
                        locationLabel = "Static distances (no GPS)"
                    }
                } catch (_: Exception) {
                    locationLabel = "Static distances (no GPS)"
                }
            }
        } else {
            locationLabel = "Static distances (no GPS)"
        }
    }

    // ── Live stock from Firestore with real GPS distances ─────────────────────
    var allStock by remember { mutableStateOf(MOCK_STOCK) }
    LaunchedEffect(Unit) {
        StockRepository.seedIfEmpty()
        // Try to get GPS first
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                val loc = fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                    ?: fused.lastLocation.await()
                if (loc != null) {
                    userLat = loc.latitude
                    userLng = loc.longitude
                    locationLabel = "Your location"
                }
            } catch (_: Exception) { /* use fallback */ }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Subscribe to Firestore — recalculate distances whenever GPS is known
        StockRepository.stockFlow().collect { items ->
            val lat = userLat; val lng = userLng
            allStock = if (lat != null && lng != null) {
                // GPS available — compute real distances
                items.withRealDistance(lat, lng)
            } else {
                // No GPS — merge Firestore items with MOCK_STOCK static distances
                // so we never show 0.0 km
                val staticDistMap = MOCK_STOCK.associate { it.id to it.distanceKm }
                items.map { item ->
                    val staticDist = staticDistMap[item.id]
                    if (staticDist != null && staticDist > 0.0)
                        item.copy(distanceKm = staticDist)
                    else if (item.distanceKm == 0.0) {
                        // Look up by shopId from MOCK_SHOPS
                        val shopDist = MOCK_SHOPS.find { it.id == item.shopId }?.distanceKm ?: 0.0
                        item.copy(distanceKm = shopDist)
                    } else item
                }
            }
        }
    }

    // Recompute distances when GPS becomes available
    LaunchedEffect(userLat, userLng) {
        val lat = userLat ?: return@LaunchedEffect
        val lng = userLng ?: return@LaunchedEffect
        allStock = allStock.withRealDistance(lat, lng)
        locationLabel = "Your location"
    }

    // AI Guide sheet
    var showAiGuide by remember { mutableStateOf(false) }
    var aiGuideItem by remember { mutableStateOf<StockItem?>(null) }

    // Symptom AI state
    var aiSymptomResult  by remember { mutableStateOf<AiSymptomResult?>(null) }
    var isAiSearching    by remember { mutableStateOf(false) }
    var aiSearchError    by remember { mutableStateOf<String?>(null) }
    var lastAiQuery      by remember { mutableStateOf("") }
    var debounceJob      by remember { mutableStateOf<Job?>(null) }

    val categories = listOf("All", "Life Saving", "Essential", "General")

    // ── Local search results (instant, from Firestore / fallback) ────────────
    val localResults: List<StockItem> = remember(query, mode, filterCat, allStock, maxDistKm) {
        val q = query.trim()
        if (q.isBlank()) {
            allStock
                .filter { it.distanceKm <= maxDistKm }
                .sortedWith(compareByDescending<StockItem> { if (it.medicineCategory == "Life Saving") 1 else 0 }
                    .thenBy { it.distanceKm })
                .filter { filterCat == "All" || it.medicineCategory == filterCat }
        } else if (mode == SearchMode.MEDICINE) {
            // Search by medicine name, shop, village — AND also symptom keywords
            val lq = q.lowercase()
            val symptomMatched = SYMPTOM_MAP.entries
                .filter { (sym, _) -> lq.contains(sym) || sym.contains(lq) }
                .flatMap { it.value }.distinct()

            allStock.filter { item ->
                val nameMatch = item.medicineName.contains(q, ignoreCase = true) ||
                                item.shopName.contains(q, ignoreCase = true) ||
                                item.shopVillage.contains(q, ignoreCase = true)
                val symptomMatch = symptomMatched.any {
                    item.medicineName.contains(it, ignoreCase = true)
                }
                (nameMatch || symptomMatch) &&
                (filterCat == "All" || item.medicineCategory == filterCat) &&
                item.distanceKm <= maxDistKm
            }.sortedWith(compareByDescending<StockItem> { if (it.medicineCategory == "Life Saving") 1 else 0 }
                .thenBy { it.distanceKm })
        } else {
            // Symptom mode — local map first
            val lq = q.lowercase()
            val matched = SYMPTOM_MAP.entries
                .filter { (sym, _) -> lq.contains(sym) || sym.contains(lq) }
                .flatMap { it.value }.distinct()
            allStock.filter { item ->
                matched.any { item.medicineName.contains(it, ignoreCase = true) } &&
                (filterCat == "All" || item.medicineCategory == filterCat) &&
                item.distanceKm <= maxDistKm
            }.sortedWith(compareByDescending<StockItem> { if (it.medicineCategory == "Life Saving") 1 else 0 }
                .thenBy { it.distanceKm })
        }
    }

    // ── AI symptom search: fires when local results are empty ────────────────
    LaunchedEffect(query, mode, localResults.size) {
        if (query.isBlank()) {
            aiSymptomResult = null
            aiSearchError   = null
            return@LaunchedEffect
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(800)
            val q = query.trim()
            if (q == lastAiQuery && aiSymptomResult != null) return@launch
            if (localResults.isNotEmpty()) {
                // Local results found — clear any stale AI result
                aiSymptomResult = null
                aiSearchError   = null
                return@launch
            }
            // No local match → ask Gemini regardless of mode
            isAiSearching = true
            aiSearchError = null
            aiSymptomResult = null
            lastAiQuery = q
            try {
                val result = askGeminiForMedicines(q, allStock)
                aiSymptomResult = result
            } catch (e: Exception) {
                aiSearchError = "AI search failed. Check your connection."
            }
            isAiSearching = false
        }
    }

    // Combined display list
    val displayResults: List<StockItem> = remember(localResults, aiSymptomResult, filterCat, allStock) {
        val aiMatches = aiSymptomResult?.stockMatches
            ?.filter { filterCat == "All" || it.medicineCategory == filterCat }
            ?: emptyList()
        // Merge: local first, then AI matches not already in local
        val localIds = localResults.map { it.id }.toSet()
        localResults + aiMatches.filter { it.id !in localIds }
    }

    // ── AI Guide sheet ────────────────────────────────────────────────────────
    if (showAiGuide && aiGuideItem != null) {
        AiGuideSheet(
            item       = aiGuideItem!!,
            isDarkMode = isDarkMode,
            onDismiss  = { showAiGuide = false; aiGuideItem = null }
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Network info banner ───────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Hub, contentDescription = null,
                    tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Registered Network: ${MOCK_SHOPS.size} shops • Gadag, Hassan, Mangalore, Bangalore",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF1D4ED8)
                    )
                    Text(
                        "Distances from: $locationLabel",
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF3B82F6)
                    )
                }
                if (userLat == null) {
                    Icon(Icons.Default.LocationSearching, contentDescription = null,
                        tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                }
            }
        }

        // ── Mode toggle ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .background(
                    if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    RoundedCornerShape(14.dp)
                )
                .padding(4.dp)
        ) {
            listOf(
                SearchMode.MEDICINE to ("Medicine Name" to Icons.Default.Medication),
                SearchMode.SYMPTOM  to ("By Symptom"   to Icons.Default.Psychology)
            ).forEach { (m, pair) ->
                val (label, icon) = pair
                Button(
                    onClick  = { mode = m; query = ""; aiSymptomResult = null },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (mode == m) Color(0xFF6366F1) else Color.Transparent,
                        contentColor   = if (mode == m) Color.White else Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp), elevation = null
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Search bar ────────────────────────────────────────────────────────
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            placeholder   = {
                Text(
                    if (mode == SearchMode.MEDICINE)
                        "Search any medicine, shop or village…"
                    else
                        "Any symptom: typhoid, malaria, chest pain…",
                    fontSize = 13.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            leadingIcon = {
                if (isAiSearching)
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp).padding(2.dp),
                        strokeWidth = 2.dp,
                        color       = Color(0xFF6366F1)
                    )
                else
                    Icon(
                        if (mode == SearchMode.MEDICINE) Icons.Default.Search
                        else Icons.Default.Healing,
                        contentDescription = null,
                        tint = Color(0xFF6366F1)
                    )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; aiSymptomResult = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    IconButton(onClick = {
                        if (displayResults.isNotEmpty()) {
                            aiGuideItem = displayResults.first()
                            showAiGuide = true
                        } else {
                            Toast.makeText(context, "Search a medicine first", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Guide",
                            tint = Color(0xFF6366F1))
                    }
                }
            },
            shape  = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor   = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        // ── AI symptom explanation banner ─────────────────────────────────────
        AnimatedVisibility(
            visible = mode == SearchMode.SYMPTOM &&
                      aiSymptomResult != null &&
                      aiSymptomResult!!.aiExplanation.isNotBlank()
        ) {
            aiSymptomResult?.let { result ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEDE9FE),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null,
                            tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AI Symptom Analysis", fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                            Spacer(Modifier.height(3.dp))
                            Text(result.aiExplanation, fontSize = 12.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                                lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // ── AI searching indicator ────────────────────────────────────────────
        AnimatedVisibility(visible = isAiSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF6366F1)
                )
                Text("Asking AI for medicines that treat \"$query\"…",
                    fontSize = 12.sp, color = Color.Gray)
            }
        }

        // ── Category filter chips ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = filterCat == cat,
                    onClick  = { filterCat = cat },
                    label    = { Text(cat, fontSize = 11.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6366F1),
                        selectedLabelColor     = Color.White
                    )
                )
            }
        }

        // ── Distance radius filter ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NearMe, contentDescription = null,
                        tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Distance: up to", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(
                    color = Color(0xFFEDE9FE),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "${maxDistKm.toInt()} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Slider(
                value         = maxDistKm,
                onValueChange = { maxDistKm = it },
                valueRange    = 5f..500f,
                steps         = 9,   // 5,50,100,150,200,250,300,350,400,450,500 km
                modifier      = Modifier.fillMaxWidth(),
                colors        = SliderDefaults.colors(
                    thumbColor       = Color(0xFF6366F1),
                    activeTrackColor = Color(0xFF6366F1)
                )
            )
        }

        // ── Result count ──────────────────────────────────────────────────────
        if (query.isNotBlank() && !isAiSearching) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (mode == SearchMode.SYMPTOM && displayResults.isNotEmpty()) {
                    Surface(color = Color(0xFFEDE9FE), shape = RoundedCornerShape(50)) {
                        Text(
                            if (aiSymptomResult != null) "AI + Local match" else "Symptom match",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    "${displayResults.size} result${if (displayResults.size != 1) "s" else ""} " +
                    "across ${displayResults.map { it.shopId }.distinct().size} shop${if (displayResults.map { it.shopId }.distinct().size != 1) "s" else ""}",
                    fontSize = 12.sp, color = Color.Gray
                )
            }
        }

        // ── Results list ──────────────────────────────────────────────────────
        if (displayResults.isEmpty() && query.isNotBlank() && !isAiSearching) {
            EmptyState(
                mode       = mode,
                query      = query,
                isDarkMode = isDarkMode,
                aiError    = aiSearchError,
                onRetry    = {
                    scope.launch {
                        isAiSearching = true
                        aiSearchError = null
                        try {
                            aiSymptomResult = askGeminiForMedicines(query.trim(), allStock)
                        } catch (e: Exception) {
                            aiSearchError = "AI search failed. Check your connection."
                        }
                        isAiSearching = false
                    }
                }
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(displayResults, key = { it.id }) { item ->
                    MedicineCard(
                        item       = item,
                        isDarkMode = isDarkMode,
                        onCallShop = { shopName ->
                            Toast.makeText(context, "Calling $shopName…", Toast.LENGTH_SHORT).show()
                        },
                        onAiGuide  = {
                            aiGuideItem = item
                            showAiGuide = true
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(
    mode: SearchMode,
    query: String,
    isDarkMode: Boolean,
    aiError: String?,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                if (mode == SearchMode.SYMPTOM) Icons.Default.Psychology
                else Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF6366F1).copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No stock found for \"$query\"",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            if (aiError != null) {
                Text(aiError, fontSize = 12.sp, color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape   = RoundedCornerShape(12.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry AI Search", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Not found in the registered network (${MOCK_SHOPS.size} shops).\nTry the Map tab to find any real pharmacy near you.",
                    fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Gemini: identify medicines for any symptom ────────────────────────────────
private suspend fun askGeminiForMedicines(symptom: String, allStock: List<StockItem>): AiSymptomResult {
    val prompt = """
You are a pharmacy assistant in rural India.
A villager says they have: "$symptom"

List the common over-the-counter medicines used for this condition.
Reply in this EXACT format (nothing else):
MEDICINES: Medicine1, Medicine2, Medicine3
EXPLANATION: One simple sentence explaining what these medicines do.

Use generic medicine names only (e.g. Paracetamol, ORS, Cetirizine).
Maximum 4 medicines.
""".trimIndent()

    val response = searchGeminiModel.generateContent(prompt)
    val text     = response.text?.trim() ?: ""

    // Parse response
    val medicinesLine   = text.lines().find { it.startsWith("MEDICINES:") }
    val explanationLine = text.lines().find { it.startsWith("EXPLANATION:") }

    val medicineNames = medicinesLine
        ?.removePrefix("MEDICINES:")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    val explanation = explanationLine
        ?.removePrefix("EXPLANATION:")
        ?.trim()
        ?: "AI suggested medicines for your symptom."

    // Match against live stock (not just MOCK_STOCK)
    val stockMatches = allStock.filter { item ->
        medicineNames.any { aiName ->
            item.medicineName.contains(aiName, ignoreCase = true) ||
            aiName.contains(item.medicineName.split(" ").first(), ignoreCase = true)
        }
    }.sortedWith(
        compareByDescending<StockItem> { if (it.medicineCategory == "Life Saving") 1 else 0 }
            .thenBy { it.distanceKm }
    )

    return AiSymptomResult(
        medicineNames  = medicineNames,
        aiExplanation  = explanation,
        stockMatches   = stockMatches
    )
}
