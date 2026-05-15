package com.example.gramasanjeevini.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(val label: String, val icon: ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    userName: String,
    startRole: String,
    language: com.example.gramasanjeevini.models.AppLanguage =
        com.example.gramasanjeevini.models.AppLanguage.ENGLISH,
    isDarkMode: Boolean,
    onToggleDark: () -> Unit,
    onLogout: () -> Unit
) {
    // Role is FIXED to what was chosen at login — no in-app switching
    val role = startRole

    var currentRoute by remember {
        mutableStateOf(if (role == "Villager") "search" else "dashboard")
    }

    // ── Navigation items per role ─────────────────────────────────────────────
    val navItems = if (role == "Villager") {
        listOf(
            NavItem("Search",    Icons.Default.Search,   "search"),
            NavItem("Emergency", Icons.Default.Favorite, "emergency"),
            NavItem("Expiry",    Icons.Default.Warning,  "expiry"),
            NavItem("Map",       Icons.Default.Map,      "map")
        )
    } else {
        listOf(
            NavItem("Dashboard", Icons.Default.Dashboard, "dashboard"),
            NavItem("Stock",     Icons.Default.Inventory, "stock"),
            NavItem("Expiry",    Icons.Default.Warning,   "expiry_mgr"),
            NavItem("Network",   Icons.Default.Hub,       "network")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "GramaSanjeevini",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 18.sp
                        )
                        Text(
                            "RURAL PHARMACY NETWORK",
                            fontSize      = 9.sp,
                            color         = Color.Gray,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    // ── Role badge (display only — not clickable) ─────────────
                    Surface(
                        color = if (role == "Villager") Color(0xFFEDE9FE) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (role == "Villager") Icons.Default.Person
                                else Icons.Default.LocalPharmacy,
                                contentDescription = null,
                                tint     = if (role == "Villager") Color(0xFF6366F1)
                                           else Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                role,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (role == "Villager") Color(0xFF6366F1)
                                             else Color(0xFFD97706)
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // ── Dark mode toggle ──────────────────────────────────────
                    IconButton(onClick = onToggleDark) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle dark mode"
                        )
                    }

                    // ── Logout ────────────────────────────────────────────────
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick  = { currentRoute = item.route },
                        icon     = { Icon(item.icon, contentDescription = item.label) },
                        label    = { Text(item.label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (role == "Villager") {
                when (currentRoute) {
                    "search"    -> SearchScreen(isDarkMode = isDarkMode)
                    "emergency" -> EmergencyScreen(isDarkMode = isDarkMode)
                    "expiry"    -> ExpiryWatchScreen(isDarkMode = isDarkMode, isPharmacist = false)
                    "map"       -> MapScreen(isDarkMode = isDarkMode)
                }
            } else {
                when (currentRoute) {
                    "dashboard"  -> PharmacistDashboard(isDarkMode = isDarkMode, userName = userName)
                    "stock"      -> StockManagerScreen(isDarkMode = isDarkMode)
                    "expiry_mgr" -> ExpiryWatchScreen(isDarkMode = isDarkMode, isPharmacist = true)
                    "network"    -> NetworkScreen(isDarkMode = isDarkMode)
                }
            }
        }
    }
}
