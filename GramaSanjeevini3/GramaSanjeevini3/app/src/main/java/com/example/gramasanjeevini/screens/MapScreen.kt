package com.example.gramasanjeevini.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gramasanjeevini.data.StockRepository
import com.example.gramasanjeevini.models.MOCK_SHOPS
import com.example.gramasanjeevini.models.MOCK_STOCK
import com.example.gramasanjeevini.models.Shop
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val MAPS_API_KEY = "AIzaSyCo-ZjJi-hp0BWt7vTqBbEhM03HDG0H1NY"

data class NearbyPharmacy(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng,
    val rating: Float?,
    val isOpen: Boolean?,
    val phoneNumber: String?
)

data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

// Represents a GramaSanjeevini-registered shop shown as a purple pin
data class NetworkShopPin(
    val shop: Shop,
    val latLng: LatLng,
    val stockCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(isDarkMode: Boolean) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var userLocation     by remember { mutableStateOf<LatLng?>(null) }
    var pharmacies       by remember { mutableStateOf<List<NearbyPharmacy>>(emptyList()) }
    var selectedPharmacy by remember { mutableStateOf<NearbyPharmacy?>(null) }
    var isLoading        by remember { mutableStateOf(false) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }
    var hasPermission    by remember { mutableStateOf(false) }

    // ── GramaSanjeevini network shops (from Firestore) ────────────────────────
    var networkShops     by remember { mutableStateOf(MOCK_SHOPS) }
    var networkStock     by remember { mutableStateOf(MOCK_STOCK) }
    var selectedNetShop  by remember { mutableStateOf<NetworkShopPin?>(null) }

    // Load network shops + stock from Firestore
    LaunchedEffect(Unit) {
        StockRepository.seedShopsIfEmpty()
        launch { StockRepository.shopsFlow().collect  { networkShops = it } }
        launch { StockRepository.stockFlow().collect  { networkStock = it } }
    }

    // Build pin list — only shops that have GPS coordinates
    val networkPins: List<NetworkShopPin> = remember(networkShops, networkStock) {
        networkShops.mapNotNull { shop ->
            if (shop.lat == 0.0 && shop.lng == 0.0) return@mapNotNull null
            val count = networkStock.count { it.shopId == shop.id }
            NetworkShopPin(shop, LatLng(shop.lat, shop.lng), count)
        }
    }

    // Search state
    var searchQuery      by remember { mutableStateOf("") }
    var suggestions      by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var showSuggestions  by remember { mutableStateOf(false) }
    var searchJob        by remember { mutableStateOf<Job?>(null) }
    var searchAreaVisible by remember { mutableStateOf(false) }

    val defaultCenter = LatLng(20.5937, 78.9629)
    val cameraState   = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 5f)
    }

    // Init Places
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) Places.initialize(context, MAPS_API_KEY)
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Check permission
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Get location + search pharmacies
    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        isLoading = true
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            val loc = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                      ?: fused.lastLocation.await()
            if (loc != null) {
                val ll = LatLng(loc.latitude, loc.longitude)
                userLocation = ll
                cameraState.position = CameraPosition.fromLatLngZoom(ll, 14f)
                pharmacies = searchNearbyPharmacies(context, ll)
                if (pharmacies.isEmpty()) errorMsg = "No pharmacies found nearby. Try searching a location."
            } else {
                errorMsg = "GPS unavailable. Search a location manually."
            }
        } catch (e: Exception) {
            errorMsg = "Location error: ${e.localizedMessage}"
        }
        isLoading = false
    }

    // Autocomplete as user types
    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 2) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(400)
            try {
                val client = Places.createClient(context)
                val token  = AutocompleteSessionToken.newInstance()
                val req    = FindAutocompletePredictionsRequest.builder()
                    .setQuery(searchQuery)
                    .setSessionToken(token)
                    .build()
                val resp = client.findAutocompletePredictions(req).await()
                suggestions = resp.autocompletePredictions.map { pred ->
                    PlaceSuggestion(
                        placeId       = pred.placeId,
                        primaryText   = pred.getPrimaryText(null).toString(),
                        secondaryText = pred.getSecondaryText(null).toString()
                    )
                }
                showSuggestions = suggestions.isNotEmpty()
            } catch (_: Exception) {
                suggestions = emptyList()
            }
        }
    }

    // Go to selected suggestion
    fun goToPlace(suggestion: PlaceSuggestion) {
        showSuggestions = false
        searchQuery = suggestion.primaryText
        scope.launch {
            isLoading = true
            try {
                val client = Places.createClient(context)
                val req    = FetchPlaceRequest.newInstance(
                    suggestion.placeId,
                    listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                )
                val resp = client.fetchPlace(req).await()
                val ll   = resp.place.latLng
                if (ll != null) {
                    // Update userLocation so FAB + distance comparisons use the searched location
                    userLocation = ll
                    cameraState.position = CameraPosition.fromLatLngZoom(ll, 14f)
                    pharmacies = searchNearbyPharmacies(context, ll)
                    errorMsg = if (pharmacies.isEmpty())
                        "No pharmacies found near ${suggestion.primaryText}"
                    else null
                }
            } catch (e: Exception) {
                errorMsg = "Could not find location: ${e.localizedMessage}"
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Google Map ────────────────────────────────────────────────────────
        GoogleMap(
            modifier            = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties          = MapProperties(isMyLocationEnabled = hasPermission),
            uiSettings          = MapUiSettings(
                zoomControlsEnabled     = true,
                myLocationButtonEnabled = false
            ),
            onMapClick = {
                selectedPharmacy = null
                selectedNetShop  = null
                showSuggestions  = false
            }
        ) {
            // ── Red pins: Google Places pharmacies ────────────────────────────
            pharmacies.forEach { pharmacy ->
                Marker(
                    state   = MarkerState(position = pharmacy.latLng),
                    title   = pharmacy.name,
                    snippet = pharmacy.address,
                    icon    = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_RED
                    ),
                    onClick = {
                        selectedNetShop  = null
                        selectedPharmacy = pharmacy
                        false
                    }
                )
            }

            // ── Purple pins: GramaSanjeevini registered shops ─────────────────
            networkPins.forEach { pin ->
                Marker(
                    state   = MarkerState(position = pin.latLng),
                    title   = pin.shop.name,
                    snippet = "${pin.shop.village} • ${pin.stockCount} medicines",
                    icon    = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_VIOLET
                    ),
                    onClick = {
                        selectedPharmacy = null
                        selectedNetShop  = pin
                        false
                    }
                )
            }
        }

        // ── Search bar + suggestions ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Search field
            Surface(
                shape         = RoundedCornerShape(16.dp),
                color         = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                shadowElevation = 6.dp,
                modifier      = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier      = Modifier.weight(1f).padding(vertical = 12.dp),
                        singleLine    = true,
                        textStyle     = androidx.compose.ui.text.TextStyle(
                            color    = if (isDarkMode) Color.White else Color(0xFF1E293B),
                            fontSize = 15.sp
                        ),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search location, village, city…",
                                    color = Color.Gray, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery     = ""
                            suggestions     = emptyList()
                            showSuggestions = false
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear",
                                tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Autocomplete suggestions dropdown
            AnimatedVisibility(visible = showSuggestions) {
                Surface(
                    shape         = RoundedCornerShape(16.dp),
                    color         = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                    shadowElevation = 8.dp,
                    modifier      = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(suggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { goToPlace(suggestion) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(suggestion.primaryText,
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                                    if (suggestion.secondaryText.isNotBlank()) {
                                        Text(suggestion.secondaryText,
                                            fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                            if (suggestions.last() != suggestion) {
                                HorizontalDivider(
                                    color = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)
                                )
                            }
                        }
                    }
                }
            }

            // Pharmacy count chip + network legend + Search This Area button
            if ((pharmacies.isNotEmpty() || networkPins.isNotEmpty()) && !showSuggestions) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Google Places count
                    if (pharmacies.isNotEmpty()) {
                        Surface(
                            color           = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                            shape           = RoundedCornerShape(50),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(10.dp),
                                    shape    = RoundedCornerShape(50),
                                    color    = Color(0xFFEF4444)
                                ) {}
                                Text(
                                    "${pharmacies.size} nearby",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                    // GramaSanjeevini network count
                    if (networkPins.isNotEmpty()) {
                        Surface(
                            color           = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                            shape           = RoundedCornerShape(50),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(10.dp),
                                    shape    = RoundedCornerShape(50),
                                    color    = Color(0xFF8B5CF6)
                                ) {}
                                Text(
                                    "${networkPins.size} in network",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }

            // "Search this area" button — shown when user pans away from current results
            AnimatedVisibility(visible = searchAreaVisible && !isLoading && !showSuggestions) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        searchAreaVisible = false
                        val target = cameraState.position.target
                        scope.launch {
                            isLoading  = true
                            userLocation = target          // treat panned location as new base
                            pharmacies = searchNearbyPharmacies(context, target)
                            errorMsg   = if (pharmacies.isEmpty())
                                "No pharmacies found in this area" else null
                            isLoading  = false
                        }
                    },
                    shape  = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Search this area", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── My Location FAB ───────────────────────────────────────────────────
        FloatingActionButton(
            onClick = {
                userLocation?.let {
                    cameraState.position = CameraPosition.fromLatLngZoom(it, 14f)
                    scope.launch {
                        isLoading  = true
                        pharmacies = searchNearbyPharmacies(context, it)
                        isLoading  = false
                    }
                } ?: permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp),
            containerColor = Color(0xFF6366F1),
            contentColor   = Color.White
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My location")
        }

        // ── Loading ───────────────────────────────────────────────────────────
        if (isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        if (isDarkMode) Color(0xCC1E293B) else Color(0xCCFFFFFF),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                    Spacer(Modifier.height(10.dp))
                    Text("Finding medical shops…", fontSize = 13.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                }
            }
        }

        // ── Error ─────────────────────────────────────────────────────────────
        errorMsg?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                action = { TextButton(onClick = { errorMsg = null }) {
                    Text("OK", color = Color(0xFF818CF8)) }
                },
                containerColor = Color(0xFF1E293B)
            ) { Text(msg, color = Color.White, fontSize = 13.sp) }
        }

        // ── No permission ─────────────────────────────────────────────────────
        if (!hasPermission && !isLoading) {
            Card(
                modifier  = Modifier.align(Alignment.Center).padding(32.dp),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null,
                        modifier = Modifier.size(52.dp), tint = Color(0xFF6366F1))
                    Spacer(Modifier.height(14.dp))
                    Text("Location Access Needed", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    Spacer(Modifier.height(8.dp))
                    Text("Allow location to find nearby medical shops, or search any location manually.",
                        fontSize = 13.sp, color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) },
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Pharmacy detail card (Google Places) ─────────────────────────────
        AnimatedVisibility(
            visible  = selectedPharmacy != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPharmacy?.let { pharmacy ->
                PharmacyDetailCard(
                    pharmacy   = pharmacy,
                    isDarkMode = isDarkMode,
                    onClose    = { selectedPharmacy = null },
                    onNavigate = {
                        val uri    = Uri.parse("google.navigation:q=${pharmacy.latLng.latitude},${pharmacy.latLng.longitude}")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${pharmacy.latLng.latitude},${pharmacy.latLng.longitude}")))
                        }
                    },
                    onCall = {
                        pharmacy.phoneNumber?.let { phone ->
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                    }
                )
            }
        }

        // ── Network shop detail card (GramaSanjeevini) ────────────────────────
        AnimatedVisibility(
            visible  = selectedNetShop != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedNetShop?.let { pin ->
                NetworkShopDetailCard(
                    pin        = pin,
                    isDarkMode = isDarkMode,
                    onClose    = { selectedNetShop = null },
                    onNavigate = {
                        val uri    = Uri.parse("google.navigation:q=${pin.latLng.latitude},${pin.latLng.longitude}")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${pin.latLng.latitude},${pin.latLng.longitude}")))
                        }
                    },
                    onCall = {
                        if (pin.shop.phone.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${pin.shop.phone}")))
                        }
                    }
                )
            }
        }
    }

    // Show "search this area" when map panned far from current results
    LaunchedEffect(cameraState.isMoving) {
        if (!cameraState.isMoving) {
            val currentCenter = cameraState.position.target
            val base = userLocation
            if (base != null) {
                val dist = distanceMeters(currentCenter, base)
                searchAreaVisible = dist > 2000
            } else if (pharmacies.isNotEmpty()) {
                val dist = distanceMeters(currentCenter, pharmacies.first().latLng)
                searchAreaVisible = dist > 2000
            }
        }
    }
}

// ── Network shop detail card (GramaSanjeevini registered) ────────────────────
@Composable
fun NetworkShopDetailCard(
    pin: NetworkShopPin,
    isDarkMode: Boolean,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    onCall: () -> Unit
) {
    val shop = pin.shop
    Card(
        modifier  = Modifier.fillMaxWidth().padding(12.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(color = Color(0xFF8B5CF6), shape = RoundedCornerShape(14.dp)) {
                    Icon(
                        if (shop.isRegisteredByPharmacist) Icons.Default.Store
                        else Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(12.dp).size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(shop.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                        if (shop.isRegisteredByPharmacist) {
                            Spacer(Modifier.width(6.dp))
                            Surface(color = Color(0xFF22C55E).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text("REGISTERED", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF22C55E),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${shop.village}, ${shop.district}", fontSize = 12.sp, color = Color.Gray)
                    if (shop.ownerName.isNotBlank()) {
                        Text("Owner: ${shop.ownerName}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // GramaSanjeevini badge
                Surface(color = Color(0xFFEDE9FE), shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Hub, contentDescription = null,
                            tint = Color(0xFF8B5CF6), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("GramaSanjeevini Network", color = Color(0xFF8B5CF6),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Stock count badge
                Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null,
                            tint = Color(0xFF22C55E), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${pin.stockCount} medicines", color = Color(0xFF22C55E),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (shop.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick  = onCall,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B5CF6))
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call", fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick  = onNavigate,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Navigate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Pharmacy detail card (Google Places) ─────────────────────────────────────
@Composable
fun PharmacyDetailCard(
    pharmacy: NearbyPharmacy,
    isDarkMode: Boolean,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(12.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(color = Color(0xFF6366F1), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.LocalPharmacy, contentDescription = null,
                        tint = Color.White, modifier = Modifier.padding(12.dp).size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pharmacy.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B))
                    Text(pharmacy.address, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pharmacy.rating?.let { rating ->
                    Surface(color = Color(0xFFFFFBEB), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null,
                                tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("%.1f".format(rating), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }
                Surface(color = Color(0xFFEDE9FE), shape = RoundedCornerShape(8.dp)) {
                    Text("Medical Shop", color = Color(0xFF6366F1), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pharmacy.phoneNumber != null) {
                    OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1))) {
                        Icon(Icons.Default.Phone, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call", fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = onNavigate, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) {
                    Icon(Icons.Default.Navigation, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Navigate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Search nearby pharmacies ──────────────────────────────────────────────────
private suspend fun searchNearbyPharmacies(
    context: android.content.Context,
    centre: LatLng
): List<NearbyPharmacy> {
    if (!Places.isInitialized()) Places.initialize(context, MAPS_API_KEY)
    val client = Places.createClient(context)

    val placeFields = listOf(
        Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS,
        Place.Field.LAT_LNG, Place.Field.RATING, Place.Field.PHONE_NUMBER
    )
    val bounds = CircularBounds.newInstance(centre, 5000.0)
    val allResults = mutableListOf<NearbyPharmacy>()

    try {
        val req = SearchNearbyRequest.builder(bounds, placeFields)
            .setIncludedPrimaryTypes(listOf("pharmacy", "drugstore"))
            .setMaxResultCount(20).build()
        client.searchNearby(req).await().places.mapNotNull { place ->
            val ll = place.latLng ?: return@mapNotNull null
            NearbyPharmacy(place.id ?: "", place.name ?: "Medical Shop",
                place.address ?: "", ll, place.rating?.toFloat(), null, place.phoneNumber)
        }.also { allResults.addAll(it) }
    } catch (_: Exception) {}

    try {
        val req2 = SearchNearbyRequest.builder(bounds, placeFields)
            .setIncludedTypes(listOf("pharmacy", "drugstore", "hospital", "medical_lab"))
            .setMaxResultCount(20).build()
        val existingIds = allResults.map { it.placeId }.toSet()
        client.searchNearby(req2).await().places.mapNotNull { place ->
            val ll = place.latLng ?: return@mapNotNull null
            if (place.id in existingIds) return@mapNotNull null
            NearbyPharmacy(place.id ?: "", place.name ?: "Medical Shop",
                place.address ?: "", ll, place.rating?.toFloat(), null, place.phoneNumber)
        }.also { allResults.addAll(it) }
    } catch (_: Exception) {}

    return allResults.sortedBy { distanceMeters(centre, it.latLng) }
}

private fun distanceMeters(a: LatLng, b: LatLng): Double {
    val r    = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val sinA = Math.sin(dLat / 2)
    val sinB = Math.sin(dLon / 2)
    val c    = sinA * sinA + Math.cos(Math.toRadians(a.latitude)) *
               Math.cos(Math.toRadians(b.latitude)) * sinB * sinB
    return r * 2 * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c))
}
