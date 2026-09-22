package lk.jmcinnovators.learning.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import lk.jmcinnovators.learning.data.local.UserPreferences
import lk.jmcinnovators.learning.data.repository.AuthRepository
import lk.jmcinnovators.learning.data.repository.ClassroomRepository
import lk.jmcinnovators.learning.data.repository.NotesRepository
import lk.jmcinnovators.learning.data.repository.NotificationsRepository
import lk.jmcinnovators.learning.data.repository.ParentControlsRepository
import lk.jmcinnovators.learning.data.repository.UserRepository

/** Plain manual DI: no Hilt in the supplied project, so this keeps the dependency graph explicit. */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val userPreferences by lazy { UserPreferences(appContext) }
    private val authRepository by lazy { AuthRepository(appContext) }
    private val userRepository by lazy { UserRepository() }
    private val notesRepository by lazy { NotesRepository() }
    private val notificationsRepository by lazy { NotificationsRepository() }
    private val parentControlsRepository by lazy { ParentControlsRepository() }
    private val classroomRepository by lazy { ClassroomRepository(appContext) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        SessionViewModel::class.java -> SessionViewModel(appContext, userPreferences, authRepository) as T
        AuthViewModel::class.java -> AuthViewModel(authRepository, userRepository) as T
        ProfileSetupViewModel::class.java -> ProfileSetupViewModel(userRepository) as T
        HomeViewModel::class.java -> HomeViewModel(authRepository, userRepository, notificationsRepository) as T
        NotesViewModel::class.java -> NotesViewModel(authRepository, notesRepository) as T
        ClassroomViewModel::class.java -> ClassroomViewModel(authRepository, classroomRepository) as T
        ClassroomDetailViewModel::class.java -> ClassroomDetailViewModel(classroomRepository) as T
        ProfileViewModel::class.java ->
            ProfileViewModel(authRepository, userRepository, userPreferences) as T
        NotificationsViewModel::class.java ->
            NotificationsViewModel(authRepository, notificationsRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
