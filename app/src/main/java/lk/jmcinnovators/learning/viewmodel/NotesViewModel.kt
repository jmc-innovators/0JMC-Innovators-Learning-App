package lk.jmcinnovators.learning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lk.jmcinnovators.learning.data.model.Note
import lk.jmcinnovators.learning.data.repository.AuthRepository
import lk.jmcinnovators.learning.data.repository.NotesRepository

class NotesViewModel(
    private val authRepository: AuthRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val uid get() = authRepository.currentUser?.uid

    val notes: StateFlow<List<Note>> = run {
        val u = uid
        (if (u == null) flowOf(emptyList()) else notesRepository.observeNotes(u))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveNote(note: Note, onSaved: (String) -> Unit) {
        val u = uid ?: return
        viewModelScope.launch {
            val id = notesRepository.saveNote(note.copy(ownerUid = u))
            onSaved(id)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { notesRepository.deleteNote(noteId) }
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch { notesRepository.saveNote(note.copy(pinned = !note.pinned)) }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch { notesRepository.saveNote(note.copy(favorite = !note.favorite)) }
    }
}
