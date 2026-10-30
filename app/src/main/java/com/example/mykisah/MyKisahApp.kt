package com.example.mykisah

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyKisahApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Terapkan pilihan tema yang tersimpan begitu app diluncurkan.
        applySavedNightMode()
    }

    private fun applySavedNightMode() {
        val mode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_NIGHT_MODE, VALUE_SYSTEM)
        val delegateMode = when (mode) {
            VALUE_YES -> AppCompatDelegate.MODE_NIGHT_YES
            VALUE_NO -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(delegateMode)
    }

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_NIGHT_MODE = "night_mode"
        const val VALUE_SYSTEM = "system"
        const val VALUE_YES = "yes"
        const val VALUE_NO = "no"
    }
}