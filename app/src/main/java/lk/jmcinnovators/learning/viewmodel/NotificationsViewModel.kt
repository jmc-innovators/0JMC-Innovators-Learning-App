package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lk.jmcinnovators.learning.data.model.AppNotification
import lk.jmcinnovators.learning.data.repository.AuthRepository
import lk.jmcinnovators.learning.data.repository.NotificationsRepository

class NotificationsViewModel(
    private val authRepository: AuthRepository,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val uid get() = authRepository.currentUser?.uid

    val notifications: StateFlow<List<AppNotification>> = run {
        val u = uid
        (if (u == null) flowOf(emptyList()) else notificationsRepository.observeList(u))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun markRead(id: String) {
        val u = uid ?: return
        viewModelScope.launch { notificationsRepository.markRead(u, id) }
    }

    fun remove(id: String) {
        val u = uid ?: return
        viewModelScope.launch { notificationsRepository.remove(u, id) }
    }
}
