package lk.jmcinnovators.learning.data.repository

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import lk.jmcinnovators.learning.data.firebase.FirebaseRefs
import lk.jmcinnovators.learning.data.model.Note
import java.util.Date

/** notes/{noteId}, one document per note, scoped by ownerUid (see firebase/firestore.rules). */
class NotesRepository {

    private val notes get() = FirebaseRefs.firestore.collection("notes")

    fun observeNotes(ownerUid: String): Flow<List<Note>> = callbackFlow {
        val registration = notes
            .whereEqualTo("ownerUid", ownerUid)
            .orderBy("pinned", Query.Direction.DESCENDING)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects<Note>() ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveNote(note: Note): String {
        return if (note.id.isBlank()) {
            val ref = notes.document()
            ref.set(note.copy(id = ref.id)).await()
            ref.id
        } else {
            notes.document(note.id).set(note).await()
            note.id
        }
    }

    suspend fun deleteNote(noteId: String) {
        notes.document(noteId).delete().await()
    }

    suspend fun getNote(noteId: String): Note? =
        notes.document(noteId).get().await().toObject<Note>()
}
