package com.example.gramasanjeevini.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.models.MOCK_MEDICINES
import com.example.gramasanjeevini.models.StockItem
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

// ── Gemini config ─────────────────────────────────────────────────────────────
private const val GEMINI_API_KEY = "AIzaSyApoqeFefV-_-BJTjBUyIfT6weI1AuZXHw"
private const val GEMINI_MODEL   = "gemini-1.5-flash"

// Single shared model instance — avoids recreating on every call
private val geminiModel by lazy {
    GenerativeModel(
        modelName = GEMINI_MODEL,
        apiKey    = GEMINI_API_KEY
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGuideSheet(
    item: StockItem,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val scope       = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Find full medicine details from catalogue (null for custom pharmacist-added items)
    val medicine = remember(item.medicineId) {
        MOCK_MEDICINES.find { it.id == item.medicineId }
    }

    // State
    var aiResponse        by remember { mutableStateOf("") }
    var isLoading         by remember { mutableStateOf(false) }
    var isFollowUpLoading by remember { mutableStateOf(false) }
    var errorMsg          by remember { mutableStateOf<String?>(null) }
    var followUpQ         by remember { mutableStateOf("") }
    var chatHistory       by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // Auto-scroll to bottom whenever chat grows
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Load initial guide when sheet opens
    LaunchedEffect(item.medicineId) {
        isLoading  = true
        errorMsg   = null
        aiResponse = ""
        try {
            aiResponse = callGemini(buildInitialPrompt(item, medicine?.uses, medicine?.sideEffects))
        } catch (e: Exception) {
            errorMsg = e.message ?: "Unknown error"
        }
        isLoading = false
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
                .padding(horizontal = 20.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Color(0xFF6366F1), shape = RoundedCornerShape(14.dp)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Health Guide",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 17.sp,
                        color      = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        item.medicineName,
                        fontSize   = 13.sp,
                        color      = Color(0xFF6366F1),
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Info chips ────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val catColor = when (item.medicineCategory) {
                    "Life Saving" -> Color(0xFFEF4444)
                    "Essential"   -> Color(0xFFD97706)
                    else          -> Color(0xFF22C55E)
                }
                Surface(color = catColor, shape = RoundedCornerShape(50)) {
                    Text(
                        item.medicineCategory,
                        color      = Color.White,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE0E7FF),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "${item.shopVillage} • ${"%.1f".format(item.distanceKm)} km",
                        color      = Color(0xFF6366F1),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            Spacer(Modifier.height(12.dp))

            // ── Scrollable body ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {

                // ── Loading spinner ───────────────────────────────────────────
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF6366F1))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Generating health guide…",
                                fontSize = 13.sp,
                                color    = Color.Gray
                            )
                        }
                    }
                }

                // ── Error state ───────────────────────────────────────────────
                if (!isLoading && errorMsg != null) {
                    // Show local data as fallback
                    LocalMedicineGuide(item = item, medicine = medicine, isDarkMode = isDarkMode)
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WifiOff, contentDescription = null,
                                    tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "AI unavailable — showing local data",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color(0xFFEF4444)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                errorMsg ?: "",
                                fontSize = 11.sp,
                                color    = Color(0xFFEF4444)
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isLoading  = true
                                        errorMsg   = null
                                        aiResponse = ""
                                        try {
                                            aiResponse = callGemini(
                                                buildInitialPrompt(item, medicine?.uses, medicine?.sideEffects)
                                            )
                                        } catch (e: Exception) {
                                            errorMsg = e.message ?: "Unknown error"
                                        }
                                        isLoading = false
                                    }
                                },
                                shape  = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null,
                                    modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry AI Guide", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // ── AI response ───────────────────────────────────────────────
                if (!isLoading && errorMsg == null && aiResponse.isNotBlank()) {
                    AiResponseCard(text = aiResponse, isDarkMode = isDarkMode)
                }

                // ── Chat history ──────────────────────────────────────────────
                chatHistory.forEach { (question, answer) ->
                    Spacer(Modifier.height(12.dp))
                    // User bubble (right-aligned)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            color = Color(0xFF6366F1),
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 4.dp,
                                bottomStart = 16.dp, bottomEnd = 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                question,
                                color    = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // AI bubble (left-aligned)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEDE9FE),
                            shape = RoundedCornerShape(
                                topStart = 4.dp, topEnd = 16.dp,
                                bottomStart = 16.dp, bottomEnd = 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                answer,
                                color    = if (isDarkMode) Color.White else Color(0xFF1E293B),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // ── Follow-up loading indicator ───────────────────────────────
                if (isFollowUpLoading) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = Color(0xFF6366F1)
                        )
                        Text("Getting answer…", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // ── Quick question chips ──────────────────────────────────────
                if (!isLoading && errorMsg == null && aiResponse.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Ask more:",
                        fontSize   = 12.sp,
                        color      = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "What is the correct dosage?",
                        "Are there side effects?",
                        "Can children take this?",
                        "What foods to avoid?",
                        "Is it safe during pregnancy?"
                    ).forEach { q ->
                        SuggestionChip(
                            onClick  = {
                                if (!isFollowUpLoading) {
                                    scope.launch {
                                        isFollowUpLoading = true
                                        try {
                                            val ans = callGemini(
                                                "About ${item.medicineName}: $q\n" +
                                                "Answer in simple language for a rural Indian villager. " +
                                                "Keep it under 80 words."
                                            )
                                            chatHistory = chatHistory + Pair(q, ans)
                                        } catch (e: Exception) {
                                            chatHistory = chatHistory + Pair(q, "Sorry, could not get an answer right now.")
                                        }
                                        isFollowUpLoading = false
                                    }
                                }
                            },
                            label    = { Text(q, fontSize = 12.sp) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── Follow-up text input (only when AI is working) ────────────────
            if (!isLoading && errorMsg == null && aiResponse.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = followUpQ,
                        onValueChange = { followUpQ = it },
                        placeholder   = { Text("Ask anything about this medicine…", fontSize = 12.sp) },
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(14.dp),
                        singleLine    = true,
                        enabled       = !isFollowUpLoading,
                        colors        = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                            focusedContainerColor   = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                            focusedBorderColor      = Color(0xFF6366F1)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isFollowUpLoading) Color(0xFF6366F1).copy(alpha = 0.5f)
                                        else Color(0xFF6366F1),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                val q = followUpQ.trim()
                                if (q.isNotBlank() && !isFollowUpLoading) {
                                    followUpQ = ""
                                    scope.launch {
                                        isFollowUpLoading = true
                                        try {
                                            val ans = callGemini(
                                                "About ${item.medicineName}: $q\n" +
                                                "Answer in simple language for a rural Indian villager. " +
                                                "Keep it under 100 words."
                                            )
                                            chatHistory = chatHistory + Pair(q, ans)
                                        } catch (e: Exception) {
                                            chatHistory = chatHistory + Pair(q, "Sorry, could not get an answer right now.")
                                        }
                                        isFollowUpLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isFollowUpLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    color       = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── AI response card ──────────────────────────────────────────────────────────
@Composable
private fun AiResponseCard(text: String, isDarkMode: Boolean) {
    Surface(
        color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEDE9FE),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint     = Color(0xFF6366F1),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Gemini AI Guide",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF6366F1)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text,
                fontSize   = 14.sp,
                lineHeight = 22.sp,
                color      = if (isDarkMode) Color.White else Color(0xFF1E293B)
            )
        }
    }
}

// ── Local fallback guide ──────────────────────────────────────────────────────
@Composable
private fun LocalMedicineGuide(
    item: StockItem,
    medicine: com.example.gramasanjeevini.models.Medicine?,
    isDarkMode: Boolean
) {
    Surface(
        color    = if (isDarkMode) Color(0xFF1E293B) else Color.White,
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MedicalServices, contentDescription = null,
                    tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Medicine Info", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1))
            }
            Spacer(Modifier.height(10.dp))
            InfoRow("Medicine",     item.medicineName,                                          isDarkMode)
            InfoRow("Category",     item.medicineCategory,                                      isDarkMode)
            InfoRow("Uses",         medicine?.uses       ?: "General use — consult a doctor",   isDarkMode)
            InfoRow("Side Effects", medicine?.sideEffects ?: "Consult a doctor for details",    isDarkMode)
            InfoRow("Available at", "${item.shopName}, ${item.shopVillage}",                    isDarkMode)
            InfoRow("Stock",        "${item.quantity} units",                                   isDarkMode)
            InfoRow("Expiry",       item.expiryDate,                                            isDarkMode)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray,
            modifier = Modifier.weight(0.38f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color    = if (isDarkMode) Color.White else Color(0xFF1E293B),
            modifier = Modifier.weight(0.62f))
    }
    HorizontalDivider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9))
}

// ── Gemini API call ───────────────────────────────────────────────────────────
private suspend fun callGemini(prompt: String): String {
    val response = geminiModel.generateContent(prompt)
    val result = response.text?.trim()
    if (result.isNullOrBlank()) throw Exception("Empty response from Gemini")
    return result
}

// ── Initial prompt ────────────────────────────────────────────────────────────
private fun buildInitialPrompt(
    item: StockItem,
    uses: String?,
    sideEffects: String?
): String {
    val usesLine = if (!uses.isNullOrBlank()) "Known uses: $uses" else ""
    val seLine   = if (!sideEffects.isNullOrBlank()) "Known side effects: $sideEffects" else ""
    val context  = listOf(usesLine, seLine).filter { it.isNotBlank() }.joinToString("\n")

    return buildString {
        appendLine("You are a simple health guide for rural Indian villagers with low digital literacy.")
        appendLine("Explain the medicine \"${item.medicineName}\" in very simple, friendly language.")
        if (context.isNotBlank()) {
            appendLine()
            appendLine("Context:")
            appendLine(context)
        }
        appendLine()
        appendLine("Include:")
        appendLine("1. What this medicine is used for (1-2 simple sentences)")
        appendLine("2. How to take it (general dosage guidance — not a prescription)")
        appendLine("3. Important warnings or side effects (keep it simple)")
        appendLine("4. When to see a doctor immediately")
        appendLine()
        appendLine("Keep the total response under 150 words. Use simple English.")
        append("Do NOT use medical jargon. Write as if explaining to a farmer in a village.")
    }
}
