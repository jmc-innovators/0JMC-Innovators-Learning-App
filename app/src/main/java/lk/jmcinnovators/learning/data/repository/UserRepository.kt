package lk.jmcinnovators.learning.data.repository

import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import lk.jmcinnovators.learning.data.firebase.FirebaseRefs
import lk.jmcinnovators.learning.data.model.UserProfile

/** users/{uid} -- see legacy-web/js/profile.page.js for the write side this mirrors. */
class UserRepository {

    private val users get() = FirebaseRefs.firestore.collection("users")

    suspend fun getProfile(uid: String): UserProfile? =
        users.document(uid).get().await().toObject<UserProfile>()

    fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.toObject<UserProfile>())
        }
        awaitClose { registration.remove() }
    }

    suspend fun createOrUpdateProfile(profile: UserProfile) {
        users.document(profile.uid).set(profile.copy(uid = profile.uid)).await()
    }
}
