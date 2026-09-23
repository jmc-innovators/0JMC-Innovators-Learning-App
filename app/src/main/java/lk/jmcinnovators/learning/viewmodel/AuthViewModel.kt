package lk.jmcinnovators.learning.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lk.jmcinnovators.learning.data.model.UserProfile
import lk.jmcinnovators.learning.data.repository.AuthRepository
import lk.jmcinnovators.learning.data.repository.AuthResult
import lk.jmcinnovators.learning.data.repository.UserRepository

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class SignedIn(val hasProfile: Boolean) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val tag = "AuthViewModel"

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(context: Context? = null) {
        if (_uiState.value == LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle(context)) {
                is AuthResult.Success -> {
                    val uid = result.user.uid
                    Log.d(tag, "Google sign-in successful for user: $uid")

                    try {
                        val profile = userRepository.getProfile(uid)
                        if (profile != null) {
                            Log.d(tag, "Existing profile found in users/$uid with role: ${profile.role}")
                            _uiState.value = LoginUiState.SignedIn(hasProfile = true)
                        } else {
                            Log.d(tag, "No profile document exists for $uid in users/{uid}. Proceeding to Profile Setup.")
                            _uiState.value = LoginUiState.SignedIn(hasProfile = false)
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Error reading users/$uid from Firestore: ${e.message}", e)
                        _uiState.value = LoginUiState.Error(
                            "Unable to verify profile. Please check your network connection and try again."
                        )
                    }
                }
                is AuthResult.Error -> {
                    Log.w(tag, "Sign-in failed with error: ${result.message}")
                    _uiState.value = LoginUiState.Error(result.message)
                }
                AuthResult.Cancelled -> {
                    Log.d(tag, "Sign-in was cancelled by user. Resetting to Idle state.")
                    _uiState.value = LoginUiState.Idle
                }
            }
        }
    }

    fun resetError() {
        _uiState.value = LoginUiState.Idle
    }
}
