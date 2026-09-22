package lk.jmcinnovators.learning.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lk.jmcinnovators.learning.data.local.UserPreferences
import lk.jmcinnovators.learning.data.repository.AuthRepository

enum class StartDestination { ONBOARDING, LOGIN, PROFILE_SETUP, HOME }

/** Decides the app's start destination once: onboarding -> login -> profile-setup gap -> home. */
class SessionViewModel(
    context: Context,
    private val userPreferences: UserPreferences = UserPreferences(context.applicationContext),
    private val authRepository: AuthRepository = AuthRepository(context.applicationContext)
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _startDestination = MutableStateFlow(StartDestination.ONBOARDING)
    val startDestination: StateFlow<StartDestination> = _startDestination.asStateFlow()

    val themePreference: StateFlow<String> = userPreferences.themePreference
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    init {
        viewModelScope.launch {
            val onboardingDone = userPreferences.onboardingDone.first()
            _startDestination.value = when {
                !onboardingDone -> StartDestination.ONBOARDING
                authRepository.currentUser == null -> StartDestination.LOGIN
                // ProfileSetup vs Home is resolved inside AppNavGraph once the Firestore profile loads.
                else -> StartDestination.HOME
            }
            _isReady.value = true
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { userPreferences.setOnboardingDone(true) }
    }
}
