package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lk.jmcinnovators.learning.data.local.UserPreferences
import lk.jmcinnovators.learning.data.model.UserProfile
import lk.jmcinnovators.learning.data.repository.AuthRepository
import lk.jmcinnovators.learning.data.repository.UserRepository

class ProfileViewModel(
    private val authRepository: AuthRepository,
    userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = run {
        val uid = authRepository.currentUser?.uid
        (if (uid == null) flowOf(null) else userRepository.observeProfile(uid))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    val themePreference: StateFlow<String> = userPreferences.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    fun setThemePreference(pref: String) {
        viewModelScope.launch { userPreferences.setThemePreference(pref) }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
