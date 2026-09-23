package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lk.jmcinnovators.learning.data.model.UserProfile
import lk.jmcinnovators.learning.data.repository.UserRepository

sealed class ProfileSetupUiState {
    object Idle : ProfileSetupUiState()
    object Saving : ProfileSetupUiState()
    object Saved : ProfileSetupUiState()
    data class Error(val message: String) : ProfileSetupUiState()
}

/** Backs the native equivalent of legacy-web/profile.html's role/grade/language form. */
class ProfileSetupViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Idle)
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun submit(profile: UserProfile) {
        _uiState.value = ProfileSetupUiState.Saving
        viewModelScope.launch {
            try {
                userRepository.createOrUpdateProfile(profile)
                _uiState.value = ProfileSetupUiState.Saved
            } catch (e: Exception) {
                val isPermissionDenied = e is com.google.firebase.firestore.FirebaseFirestoreException &&
                    e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true

                val userMessage = if (isPermissionDenied) {
                    "Your profile could not be saved. Please check your account permissions and try again."
                } else {
                    e.localizedMessage ?: "Could not save your profile. Please try again."
                }
                _uiState.value = ProfileSetupUiState.Error(userMessage)
            }
        }
    }
}
