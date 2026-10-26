package com.focusgram.app

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.focusgram.app.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { getSharedPreferences(AppPreferences.FILE, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.versionValue.text = BuildConfig.VERSION_NAME

        updateThemeValue()
        updateLanguageValue()

        binding.logoutRow.setOnClickListener { confirmLogout() }
        binding.themeRow.setOnClickListener { showThemeDialog() }
        binding.languageRow.setOnClickListener { showLanguageDialog() }
        binding.privacyRow.setOnClickListener { showPrivacyDialog() }
        binding.sourceRow.setOnClickListener { openSource() }
    }

    private fun updateThemeValue() {
        val value = when (prefs.getString(AppPreferences.KEY_THEME, AppPreferences.THEME_SYSTEM)) {
            AppPreferences.THEME_LIGHT -> R.string.theme_light
            AppPreferences.THEME_DARK -> R.string.theme_dark
            else -> R.string.theme_system
        }
        binding.themeValue.setText(value)
    }

    private fun updateLanguageValue() {
        val locales = AppCompatDelegate.getApplicationLocales()
        val language = if (locales.isEmpty) {
            null
        } else {
            locales.toLanguageTags().substringBefore(',').substringBefore('-')
        }
        val value = when (language) {
            "ar" -> R.string.language_arabic
            "en" -> R.string.language_english
            else -> R.string.language_system
        }
        binding.languageValue.setText(value)
    }

    private fun showThemeDialog() {
        val values = arrayOf(
            AppPreferences.THEME_SYSTEM,
            AppPreferences.THEME_LIGHT,
            AppPreferences.THEME_DARK
        )
        val labels = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        val current = prefs.getString(AppPreferences.KEY_THEME, AppPreferences.THEME_SYSTEM)
        val checked = values.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.theme)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val value = values[which]
                prefs.edit().putString(AppPreferences.KEY_THEME, value).apply()
                AppCompatDelegate.setDefaultNightMode(
                    when (value) {
                        AppPreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        AppPreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val labels = arrayOf(
            getString(R.string.language_system),
            getString(R.string.language_arabic),
            getString(R.string.language_english)
        )
        val locales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage = if (locales.isEmpty) {
            null
        } else {
            locales.toLanguageTags().substringBefore(',').substringBefore('-')
        }
        val checked = when (currentLanguage) {
            "ar" -> 1
            "en" -> 2
            else -> 0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val selected = when (which) {
                    1 -> LocaleListCompat.forLanguageTags("ar")
                    2 -> LocaleListCompat.forLanguageTags("en")
                    else -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(selected)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmLogout() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout_confirm_action) { _, _ ->
                SessionManager.logout {
                    Toast.makeText(this, R.string.logout_complete, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                ?.setTextColor(ContextCompat.getColor(this, R.color.brand_clay))
        }
        dialog.show()
    }

    private fun showPrivacyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.privacy_title)
            .setMessage(R.string.privacy_body)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openSource() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/hbx1-bx1/FocusGram")
        )
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }
}
