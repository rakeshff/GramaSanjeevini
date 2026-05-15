package com.example.gramasanjeevini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.data.SessionManager
import com.example.gramasanjeevini.screens.*
import com.example.gramasanjeevini.models.AppLanguage
import com.example.gramasanjeevini.models.AppStrings
import com.example.gramasanjeevini.models.getStrings
import com.example.gramasanjeevini.ui.components.LanguageSelector

// ── User profile passed through the app ──────────────────────────────────────
data class UserProfile(
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val shopName: String = "",
    val shopVillage: String = "",
    val shopId: String = "",
    val language: AppLanguage = AppLanguage.ENGLISH
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context     = LocalContext.current
            val systemDark  = isSystemInDarkTheme()
            var isDarkMode  by remember { mutableStateOf(systemDark) }

            // ── Restore session from disk on every app start ──────────────────
            var userProfile by remember {
                mutableStateOf(SessionManager.load(context))
            }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme(
                    primary    = Color(0xFF818CF8),
                    background = Color(0xFF0F172A),
                    surface    = Color(0xFF1E293B)
                ) else lightColorScheme(
                    primary    = Color(0xFF6366F1),
                    background = Color(0xFFF8FAFC),
                    surface    = Color.White
                )
            ) {
                if (userProfile == null) {
                    AuthScreen(
                        isDarkMode = isDarkMode,
                        onAuth     = { profile ->
                            SessionManager.save(context, profile)   // persist to disk
                            userProfile = profile
                        }
                    )
                } else {
                    MainApp(
                        userName     = userProfile!!.name,
                        startRole    = userProfile!!.role,
                        language     = userProfile!!.language,
                        isDarkMode   = isDarkMode,
                        onToggleDark = { isDarkMode = !isDarkMode },
                        onLogout     = {
                            SessionManager.clear(context)           // wipe from disk
                            userProfile = null
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTH SCREEN  (Login / Sign Up toggled)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AuthScreen(isDarkMode: Boolean, onAuth: (UserProfile) -> Unit) {
    var isLogin  by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf(AppLanguage.ENGLISH) }
    val strings  = getStrings(language)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkMode)
                        listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                    else
                        listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFC))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── App logo + title ──────────────────────────────────────────────
            Surface(
                modifier = Modifier.size(80.dp),
                shape    = RoundedCornerShape(24.dp),
                color    = Color(0xFF6366F1),
                shadowElevation = 12.dp
            ) {
                Icon(
                    Icons.Default.HealthAndSafety,
                    contentDescription = "Logo",
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    tint     = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "GramaSanjeevini",
                fontWeight = FontWeight.Black,
                fontSize   = 26.sp,
                color      = if (isDarkMode) Color.White else Color(0xFF1E293B)
            )
            Text(
                "RURAL PHARMACY NETWORK",
                fontSize   = 10.sp,
                color      = Color(0xFF6366F1),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(16.dp))

            // ── Language selector ─────────────────────────────────────────────
            LanguageSelector(
                selected   = language,
                onSelect   = { selected -> language = selected },
                isDarkMode = isDarkMode
            )

            Spacer(Modifier.height(16.dp))

            // ── Tab switcher ──────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                color    = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE0E7FF)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf(true to "Sign In", false to "Create Account").forEach { (login, label) ->
                        Button(
                            onClick  = { isLogin = login },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (isLogin == login) Color(0xFF6366F1)
                                                 else Color.Transparent,
                                contentColor   = if (isLogin == login) Color.White
                                                 else Color.Gray
                            ),
                            shape     = RoundedCornerShape(12.dp),
                            elevation = null
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Form card ─────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                if (isLogin) {
                    LoginForm(
                        isDarkMode = isDarkMode,
                        strings    = strings,
                        onLogin    = onAuth,
                        onSwitch   = { isLogin = false },
                        language   = language
                    )
                } else {
                    SignUpForm(
                        isDarkMode = isDarkMode,
                        strings    = strings,
                        onSignUp   = onAuth,
                        onSwitch   = { isLogin = true },
                        language   = language
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOGIN FORM
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoginForm(
    isDarkMode: Boolean,
    strings: AppStrings,
    onLogin: (UserProfile) -> Unit,
    onSwitch: () -> Unit,
    language: AppLanguage
) {
    var name         by remember { mutableStateOf("") }
    var phone        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var role         by remember { mutableStateOf("Villager") }

    // Validation errors
    var nameError     by remember { mutableStateOf("") }
    var phoneError    by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    fun validate(): Boolean {
        var ok = true
        nameError     = ""
        phoneError    = ""
        passwordError = ""
        if (name.isBlank()) { nameError = "Full name is required"; ok = false }
        if (phone.isBlank()) { phoneError = "Phone number is required"; ok = false }
        else if (phone.length < 10) { phoneError = "Enter a valid 10-digit number"; ok = false }
        if (password.isBlank()) { passwordError = "Password is required"; ok = false }
        else if (password.length < 6) { passwordError = "Password must be at least 6 characters"; ok = false }
        return ok
    }

    Column(modifier = Modifier.padding(28.dp)) {

        Text(
            strings.signIn,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 22.sp,
            color      = if (isDarkMode) Color.White else Color(0xFF1E293B)
        )
        Text("Sign in to continue", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        // Name
        AuthField(
            value         = name,
            onValueChange = { name = it; nameError = "" },
            label         = strings.fullName,
            icon          = Icons.Default.Person,
            error         = nameError,
            isDarkMode    = isDarkMode
        )

        Spacer(Modifier.height(14.dp))

        // Phone
        AuthField(
            value         = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10); phoneError = "" },
            label         = "Phone Number",
            icon          = Icons.Default.Phone,
            error         = phoneError,
            keyboardType  = KeyboardType.Phone,
            isDarkMode    = isDarkMode
        )

        Spacer(Modifier.height(14.dp))

        // Password
        AuthField(
            value             = password,
            onValueChange     = { password = it; passwordError = "" },
            label             = "Password",
            icon              = Icons.Default.Lock,
            error             = passwordError,
            keyboardType      = KeyboardType.Password,
            visualTransform   = if (showPassword) VisualTransformation.None
                                else PasswordVisualTransformation(),
            trailingIcon      = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = Color.Gray
                    )
                }
            },
            isDarkMode = isDarkMode
        )

        Spacer(Modifier.height(16.dp))

        // Role selector
        RoleSelector(role = role, onRoleChange = { role = it }, isDarkMode = isDarkMode)

        Spacer(Modifier.height(28.dp))

        // Sign In button
        Button(
            onClick = {
                if (validate()) {
                    // In a real app this would verify against Firebase Auth
                    // For now, accept any valid input
                    onLogin(
                        UserProfile(
                            name     = name.trim(),
                            phone    = phone,
                            email    = "",
                            role     = role,
                            language = language
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign In", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(20.dp))

        // Switch to sign up
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", fontSize = 13.sp, color = Color.Gray)
            TextButton(onClick = onSwitch, contentPadding = PaddingValues(0.dp)) {
                Text(
                    "Create Account",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF6366F1)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SIGN UP FORM
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SignUpForm(
    isDarkMode: Boolean,
    strings: AppStrings,
    onSignUp: (UserProfile) -> Unit,
    onSwitch: () -> Unit,
    language: AppLanguage
) {
    var name         by remember { mutableStateOf("") }
    var phone        by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var confirmPass  by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm  by remember { mutableStateOf(false) }
    var role         by remember { mutableStateOf("Villager") }
    var agreedTerms  by remember { mutableStateOf(false) }

    // Validation errors
    var nameError    by remember { mutableStateOf("") }
    var phoneError   by remember { mutableStateOf("") }
    var emailError   by remember { mutableStateOf("") }
    var passError    by remember { mutableStateOf("") }
    var confirmError by remember { mutableStateOf("") }
    var termsError   by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var ok = true
        nameError = ""; phoneError = ""; emailError = ""
        passError = ""; confirmError = ""; termsError = false

        if (name.isBlank()) { nameError = "Full name is required"; ok = false }
        else if (name.trim().length < 2) { nameError = "Enter your full name"; ok = false }

        if (phone.isBlank()) { phoneError = "Phone number is required"; ok = false }
        else if (phone.length < 10) { phoneError = "Enter a valid 10-digit number"; ok = false }

        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"; ok = false
        }

        if (password.isBlank()) { passError = "Password is required"; ok = false }
        else if (password.length < 6) { passError = "Minimum 6 characters"; ok = false }

        if (confirmPass.isBlank()) { confirmError = "Please confirm your password"; ok = false }
        else if (confirmPass != password) { confirmError = "Passwords do not match"; ok = false }

        if (!agreedTerms) { termsError = true; ok = false }

        return ok
    }

    Column(modifier = Modifier.padding(28.dp)) {

        Text(
            "Create Account",
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 22.sp,
            color      = if (isDarkMode) Color.White else Color(0xFF1E293B)
        )
        Text(
            "Join the rural pharmacy network",
            fontSize = 13.sp,
            color    = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        // Full Name (required)
        AuthField(
            value         = name,
            onValueChange = { name = it; nameError = "" },
            label         = "Full Name *",
            icon          = Icons.Default.Person,
            error         = nameError,
            isDarkMode    = isDarkMode
        )

        Spacer(Modifier.height(14.dp))

        // Phone (required)
        AuthField(
            value         = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10); phoneError = "" },
            label         = "Phone Number *",
            icon          = Icons.Default.Phone,
            error         = phoneError,
            keyboardType  = KeyboardType.Phone,
            isDarkMode    = isDarkMode,
            placeholder   = "10-digit mobile number"
        )

        Spacer(Modifier.height(14.dp))

        // Email (optional)
        AuthField(
            value         = email,
            onValueChange = { email = it; emailError = "" },
            label         = "Email Address (optional)",
            icon          = Icons.Default.Email,
            error         = emailError,
            keyboardType  = KeyboardType.Email,
            isDarkMode    = isDarkMode,
            placeholder   = "you@example.com"
        )

        Spacer(Modifier.height(14.dp))

        // Password
        AuthField(
            value           = password,
            onValueChange   = { password = it; passError = "" },
            label           = "Password *",
            icon            = Icons.Default.Lock,
            error           = passError,
            keyboardType    = KeyboardType.Password,
            visualTransform = if (showPassword) VisualTransformation.None
                              else PasswordVisualTransformation(),
            trailingIcon    = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
                        tint = Color.Gray
                    )
                }
            },
            isDarkMode  = isDarkMode,
            placeholder = "Minimum 6 characters"
        )

        Spacer(Modifier.height(14.dp))

        // Confirm Password
        AuthField(
            value           = confirmPass,
            onValueChange   = { confirmPass = it; confirmError = "" },
            label           = "Confirm Password *",
            icon            = Icons.Default.LockOpen,
            error           = confirmError,
            keyboardType    = KeyboardType.Password,
            visualTransform = if (showConfirm) VisualTransformation.None
                              else PasswordVisualTransformation(),
            trailingIcon    = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(
                        if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
                        tint = Color.Gray
                    )
                }
            },
            isDarkMode = isDarkMode
        )

        Spacer(Modifier.height(16.dp))

        // Role selector
        RoleSelector(role = role, onRoleChange = { role = it }, isDarkMode = isDarkMode)

        Spacer(Modifier.height(16.dp))

        // Terms checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked         = agreedTerms,
                onCheckedChange = { agreedTerms = it; termsError = false },
                colors          = CheckboxDefaults.colors(
                    checkedColor   = Color(0xFF6366F1),
                    checkmarkColor = Color.White
                )
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "I agree to the Terms of Service and Privacy Policy",
                fontSize = 12.sp,
                color    = if (termsError) Color(0xFFEF4444) else Color.Gray
            )
        }
        if (termsError) {
            Text(
                "Please accept the terms to continue",
                fontSize = 11.sp,
                color    = Color(0xFFEF4444),
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Create Account button
        Button(
            onClick = {
                if (validate()) {
                    onSignUp(
                        UserProfile(
                            name     = name.trim(),
                            phone    = phone,
                            email    = email.trim(),
                            role     = role,
                            language = language
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create Account", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(20.dp))

        // Switch to login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", fontSize = 13.sp, color = Color.Gray)
            TextButton(onClick = onSwitch, contentPadding = PaddingValues(0.dp)) {
                Text(
                    "Sign In",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF6366F1)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE: Role Selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RoleSelector(role: String, onRoleChange: (String) -> Unit, isDarkMode: Boolean) {
    Column {
        Text(
            "I am a",
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                "Villager"   to Icons.Default.Person,
                "Pharmacist" to Icons.Default.LocalPharmacy
            ).forEach { (r, icon) ->
                val selected = role == r
                OutlinedButton(
                    onClick  = { onRoleChange(r) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color(0xFF6366F1) else Color.Transparent,
                        contentColor   = if (selected) Color.White else Color.Gray
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 0.dp else 1.dp,
                        color = if (selected) Color.Transparent
                                else if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(r, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE: Auth text field
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransform: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    isDarkMode: Boolean,
    placeholder: String = ""
) {
    Column {
        OutlinedTextField(
            value             = value,
            onValueChange     = onValueChange,
            label             = { Text(label, fontSize = 13.sp) },
            placeholder       = if (placeholder.isNotBlank()) ({ Text(placeholder, fontSize = 12.sp) }) else null,
            modifier          = Modifier.fillMaxWidth(),
            shape             = RoundedCornerShape(14.dp),
            singleLine        = true,
            isError           = error.isNotBlank(),
            leadingIcon       = {
                Icon(icon, contentDescription = null,
                    tint = if (error.isNotBlank()) Color(0xFFEF4444) else Color(0xFF6366F1))
            },
            trailingIcon      = trailingIcon,
            keyboardOptions   = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransform,
            colors            = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                focusedContainerColor   = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                focusedBorderColor      = Color(0xFF6366F1),
                errorBorderColor        = Color(0xFFEF4444)
            )
        )
        if (error.isNotBlank()) {
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null,
                    tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(error, fontSize = 11.sp, color = Color(0xFFEF4444))
            }
        }
    }
}
