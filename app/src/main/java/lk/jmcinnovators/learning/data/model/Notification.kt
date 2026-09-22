package lk.jmcinnovators.learning.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** notifications/{uid}/items/{id} — same path legacy-web/js/notifications.js writes to. */
data class AppNotification(
    @DocumentId val id: String = "",
    val category: String = "system", // assignment | result | announcement | ai | security | parent | system
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null
)
