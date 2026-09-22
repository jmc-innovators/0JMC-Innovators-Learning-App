package lk.jmcinnovators.learning.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jmc_prefs")

/** Local-only flags: onboarding completion and the theme choice, before any user is signed in. */
class UserPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME_PREF = stringPreferencesKey("theme_pref") // light | dark | system
    }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    val themePreference: Flow<String> =
        context.dataStore.data.map { it[Keys.THEME_PREF] ?: "dark" } // dark matches legacy-web default

    suspend fun setThemePreference(pref: String) {
        context.dataStore.edit { it[Keys.THEME_PREF] = pref }
    }
}
