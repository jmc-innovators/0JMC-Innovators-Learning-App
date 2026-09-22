package lk.jmcinnovators.learning.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

/**
 * The default FirebaseApp is the jmc-home2 project (app/google-services.json). Every
 * repository except the classroom one talks to this instance, matching legacy-web/js/firebase-init.js.
 */
object FirebaseRefs {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage get() = FirebaseStorage.getInstance()
    val functions: FirebaseFunctions get() = FirebaseFunctions.getInstance()

    fun currentUid(): String? = auth.currentUser?.uid
}
