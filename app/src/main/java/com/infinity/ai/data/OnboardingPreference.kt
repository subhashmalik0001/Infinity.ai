package com.infinity.ai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingPreference(private val context: Context) {
    companion object {
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED_KEY] = completed }
    }
}
