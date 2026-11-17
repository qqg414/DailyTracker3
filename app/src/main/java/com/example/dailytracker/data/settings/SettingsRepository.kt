package com.example.dailytracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val KEY_INITIAL_BALANCE = doublePreferencesKey("initial_balance")

    val initialBalance: Flow<Double> =
        context.dataStore.data.map { it[KEY_INITIAL_BALANCE] ?: 0.0 }

    suspend fun setInitialBalance(value: Double) {
        context.dataStore.edit { it[KEY_INITIAL_BALANCE] = value }
    }
}