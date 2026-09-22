package lk.jmcinnovators.learning.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** notes/{noteId}, scoped to the owning uid by a Firestore query + security rule, not by path. */
data class Note(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val title: String = "",
    val body: String = "",
    val folder: String = "General",
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)
