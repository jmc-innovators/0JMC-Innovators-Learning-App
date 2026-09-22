package lk.jmcinnovators.learning.data.repository

import android.content.Context
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import lk.jmcinnovators.learning.data.firebase.ClassroomFirebaseRefs
import lk.jmcinnovators.learning.data.model.Announcement
import lk.jmcinnovators.learning.data.model.Assignment
import lk.jmcinnovators.learning.data.model.SchoolClass

sealed class JoinClassResult {
    data class Success(val schoolClass: SchoolClass) : JoinClassResult()
    object NotFound : JoinClassResult()
    object AlreadyMember : JoinClassResult()
    data class Error(val message: String) : JoinClassResult()
}

/**
 * Talks to the jmc-class Firebase project. Classes/assignments/quizzes are NOT yet in
 * Firestore on the web side (legacy-web keeps them in localStorage) -- this repository
 * is the target schema described in MIGRATION.md, and every read here returns an honest
 * empty list rather than fabricated rows until a class has real Firestore documents.
 */
class ClassroomRepository(private val context: Context) {

    private val db get() = ClassroomFirebaseRefs.firestore(context)
    private val classes get() = db.collection("classes")

    suspend fun joinByCode(code: String, studentUid: String): JoinClassResult {
        return try {
            val query = classes.whereEqualTo("joinCode", code.trim().uppercase()).limit(1).get().await()
            val doc = query.documents.firstOrNull() ?: return JoinClassResult.NotFound
            val schoolClass = doc.toObject<SchoolClass>()?.copy(id = doc.id)
                ?: return JoinClassResult.Error("Malformed classroom record.")

            val membershipRef = classes.document(doc.id).collection("students").document(studentUid)
            if (membershipRef.get().await().exists()) return JoinClassResult.AlreadyMember

            membershipRef.set(mapOf("joinedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                .await()
            JoinClassResult.Success(schoolClass)
        } catch (e: Exception) {
            JoinClassResult.Error(e.message ?: "Could not join classroom.")
        }
    }

    fun observeMyClasses(studentUid: String): Flow<List<SchoolClass>> = callbackFlow {
        // Membership is a subcollection per class, so this listens on a collection-group query.
        val registration = db.collectionGroup("students")
            .whereEqualTo(com.google.firebase.firestore.FieldPath.documentId(), studentUid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Resolve each membership doc's parent class doc.
                val classRefs = snap.documents.mapNotNull { it.reference.parent.parent }
                if (classRefs.isEmpty()) {
                    trySend(emptyList())
                } else {
                    com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.DocumentSnapshot>(
                        classRefs.map { it.get() }
                    ).addOnSuccessListener { docs ->
                        trySend(docs.mapNotNull { d -> d.toObject<SchoolClass>()?.copy(id = d.id) })
                    }.addOnFailureListener { trySend(emptyList()) }
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeAssignments(classId: String): Flow<List<Assignment>> = callbackFlow {
        val registration = classes.document(classId).collection("assignments")
            .orderBy("dueAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects<Assignment>() ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeAnnouncements(classId: String): Flow<List<Announcement>> = callbackFlow {
        val registration = classes.document(classId).collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects<Announcement>() ?: emptyList())
            }
        awaitClose { registration.remove() }
    }
}
