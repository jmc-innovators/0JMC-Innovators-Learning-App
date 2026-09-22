package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import lk.jmcinnovators.learning.data.model.UserProfile
import lk.jmcinnovators.learning.data.repository.AuthRepository
import lk.jmcinnovators.learning.data.repository.NotificationsRepository
import lk.jmcinnovators.learning.data.repository.UserRepository

data class HomeUiState(
    val profile: UserProfile? = null,
    val unreadNotifications: Int = 0,
    val loading: Boolean = true
)

/** The Home route is only reachable once SessionViewModel confirms a signed-in user, so a
 *  null uid here just resolves to an empty, non-loading state rather than crashing. */
class HomeViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    notificationsRepository: NotificationsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = run {
        val uid = authRepository.currentUser?.uid
        val flow = if (uid == null) {
            flowOf(HomeUiState(loading = false))
        } else {
            userRepository.observeProfile(uid).combine(
                notificationsRepository.observeUnreadCount(uid)
            ) { profile, unread ->
                HomeUiState(profile = profile, unreadNotifications = unread, loading = false)
            }
        }
        flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
    }
}
