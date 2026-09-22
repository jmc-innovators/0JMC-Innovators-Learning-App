package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogle() {
        if (_uiState.value == LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle()) {
                is AuthResult.Success -> {
                    val profile = userRepository.getProfile(result.user.uid)
                    _uiState.value = LoginUiState.SignedIn(hasProfile = profile != null)
                }
                is AuthResult.Error -> _uiState.value = LoginUiState.Error(result.message)
                AuthResult.Cancelled -> _uiState.value = LoginUiState.Idle
            }
        }
    }

    fun resetError() {
        _uiState.value = LoginUiState.Idle
    }
}
