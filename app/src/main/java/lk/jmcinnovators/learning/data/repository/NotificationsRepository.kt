package lk.jmcinnovators.learning.data.repository

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import lk.jmcinnovators.learning.data.firebase.FirebaseRefs
import lk.jmcinnovators.learning.data.model.AppNotification

/** notifications/{uid}/items/{id} -- matches legacy-web/js/notifications.js exactly. */
class NotificationsRepository {

    private fun items(uid: String) =
        FirebaseRefs.firestore.collection("notifications").document(uid).collection("items")

    fun observeList(uid: String, max: Long = 30): Flow<List<AppNotification>> = callbackFlow {
        val registration = items(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(max)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects<AppNotification>() ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeUnreadCount(uid: String): Flow<Int> =
        observeList(uid, max = 50).map { list -> list.count { !it.read } }

    suspend fun markRead(uid: String, id: String) {
        items(uid).document(id).update("read", true).await()
    }

    suspend fun remove(uid: String, id: String) {
        items(uid).document(id).delete().await()
    }
}
