package com.example.gramasanjeevini.data

import android.content.Context
import android.content.SharedPreferences
import com.example.gramasanjeevini.UserProfile
import com.example.gramasanjeevini.models.AppLanguage

/**
 * Persists the logged-in user profile across app restarts using SharedPreferences.
 * Call [save] on login/signup, [clear] on logout, [load] on app start.
 */
object SessionManager {

    private const val PREFS_NAME = "gramasanjeevini_session"
    private const val KEY_NAME     = "name"
    private const val KEY_PHONE    = "phone"
    private const val KEY_EMAIL    = "email"
    private const val KEY_ROLE     = "role"
    private const val KEY_SHOP_NAME    = "shopName"
    private const val KEY_SHOP_VILLAGE = "shopVillage"
    private const val KEY_SHOP_ID      = "shopId"
    private const val KEY_LANGUAGE     = "language"
    private const val KEY_LOGGED_IN    = "loggedIn"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Save profile to disk — call right after successful login or sign-up. */
    fun save(context: Context, profile: UserProfile) {
        prefs(context).edit().apply {
            putBoolean(KEY_LOGGED_IN,    true)
            putString(KEY_NAME,          profile.name)
            putString(KEY_PHONE,         profile.phone)
            putString(KEY_EMAIL,         profile.email)
            putString(KEY_ROLE,          profile.role)
            putString(KEY_SHOP_NAME,     profile.shopName)
            putString(KEY_SHOP_VILLAGE,  profile.shopVillage)
            putString(KEY_SHOP_ID,       profile.shopId)
            putString(KEY_LANGUAGE,      profile.language.name)
            apply()
        }
    }

    /** Load the saved profile. Returns null if no session exists. */
    fun load(context: Context): UserProfile? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_LOGGED_IN, false)) return null
        return UserProfile(
            name        = p.getString(KEY_NAME,  "") ?: "",
            phone       = p.getString(KEY_PHONE, "") ?: "",
            email       = p.getString(KEY_EMAIL, "") ?: "",
            role        = p.getString(KEY_ROLE,  "Villager") ?: "Villager",
            shopName    = p.getString(KEY_SHOP_NAME,    "") ?: "",
            shopVillage = p.getString(KEY_SHOP_VILLAGE, "") ?: "",
            shopId      = p.getString(KEY_SHOP_ID,      "") ?: "",
            language    = runCatching {
                AppLanguage.valueOf(p.getString(KEY_LANGUAGE, "ENGLISH") ?: "ENGLISH")
            }.getOrDefault(AppLanguage.ENGLISH)
        )
    }

    /** Clear the session — call on logout. */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
