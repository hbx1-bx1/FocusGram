package com.focusgram.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class FocusGramApp : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedTheme()
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(AppPreferences.FILE, MODE_PRIVATE)
        val mode = when (prefs.getString(AppPreferences.KEY_THEME, AppPreferences.THEME_SYSTEM)) {
            AppPreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppPreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
