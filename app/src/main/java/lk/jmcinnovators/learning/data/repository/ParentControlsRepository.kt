package lk.jmcinnovators.learning.data.repository

import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import lk.jmcinnovators.learning.data.firebase.FirebaseRefs
import lk.jmcinnovators.learning.data.model.ParentControls

/** parentControls/{studentUid} -- defaults must match legacy-web/js/parent-controls-enforcer.js. */
class ParentControlsRepository {

    private val controls get() = FirebaseRefs.firestore.collection("parentControls")

    fun observeMyControls(uid: String): Flow<ParentControls> = callbackFlow {
        val registration = controls.document(uid).addSnapshotListener { snap, error ->
            if (error != null || snap == null || !snap.exists()) {
                trySend(ParentControls())
                return@addSnapshotListener
            }
            trySend(snap.toObject<ParentControls>() ?: ParentControls())
        }
        awaitClose { registration.remove() }
    }
}
