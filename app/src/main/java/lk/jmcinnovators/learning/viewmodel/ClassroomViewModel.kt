package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lk.jmcinnovators.learning.data.model.SchoolClass
import lk.jmcinnovators.learning.data.repository.AuthRepository
import lk.jmcinnovators.learning.data.repository.ClassroomRepository
import lk.jmcinnovators.learning.data.repository.JoinClassResult

sealed class JoinCodeUiState {
    object Idle : JoinCodeUiState()
    object Joining : JoinCodeUiState()
    data class Joined(val schoolClass: SchoolClass) : JoinCodeUiState()
    object NotFound : JoinCodeUiState()
    object AlreadyMember : JoinCodeUiState()
    data class Error(val message: String) : JoinCodeUiState()
}

class ClassroomViewModel(
    private val authRepository: AuthRepository,
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    private val uid get() = authRepository.currentUser?.uid

    val myClasses: StateFlow<List<SchoolClass>> = run {
        val u = uid
        (if (u == null) flowOf(emptyList()) else classroomRepository.observeMyClasses(u))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _joinState = MutableStateFlow<JoinCodeUiState>(JoinCodeUiState.Idle)
    val joinState: StateFlow<JoinCodeUiState> = _joinState.asStateFlow()

    fun joinClass(code: String) {
        val u = uid ?: return
        if (code.isBlank()) return
        _joinState.value = JoinCodeUiState.Joining
        viewModelScope.launch {
            _joinState.value = when (val result = classroomRepository.joinByCode(code, u)) {
                is JoinClassResult.Success -> JoinCodeUiState.Joined(result.schoolClass)
                JoinClassResult.NotFound -> JoinCodeUiState.NotFound
                JoinClassResult.AlreadyMember -> JoinCodeUiState.AlreadyMember
                is JoinClassResult.Error -> JoinCodeUiState.Error(result.message)
            }
        }
    }

    fun resetJoinState() {
        _joinState.value = JoinCodeUiState.Idle
    }
}
