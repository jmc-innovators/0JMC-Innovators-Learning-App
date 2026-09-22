package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import lk.jmcinnovators.learning.data.model.Announcement
import lk.jmcinnovators.learning.data.model.Assignment
import lk.jmcinnovators.learning.data.repository.ClassroomRepository

class ClassroomDetailViewModel(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    private val _classId = MutableStateFlow<String?>(null)

    fun setClassId(classId: String) {
        if (_classId.value == classId) return
        _classId.value = classId
    }

    val assignments: StateFlow<List<Assignment>> = _classId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else classroomRepository.observeAssignments(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<Announcement>> = _classId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else classroomRepository.observeAnnouncements(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
