package com.subhanismayil.budget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shared_budget_prefs")

object Prefs {
    private val LAST_WHO = stringPreferencesKey("last_who")

    fun lastWho(context: Context): Flow<String?> =
        context.dataStore.data.map { it[LAST_WHO] }

    suspend fun setLastWho(context: Context, who: String) {
        context.dataStore.edit { it[LAST_WHO] = who }
    }
}
