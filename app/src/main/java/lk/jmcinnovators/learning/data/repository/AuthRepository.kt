package lk.jmcinnovators.learning.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import lk.jmcinnovators.learning.R
import lk.jmcinnovators.learning.data.firebase.FirebaseRefs

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

/**
 * Google Sign-In via Credential Manager, the SDK Google now recommends over the deprecated
 * GoogleSignInClient. Requires a Web client ID (oauth_client type 3) registered against
 * jmc-home2 -- present in google-services.json -- and the app's release/debug SHA-1 added
 * in the Firebase console (see FIREBASE_SETUP.md; this is currently NOT yet configured).
 */
class AuthRepository(private val context: Context) {

    val currentUser: FirebaseUser? get() = FirebaseRefs.auth.currentUser

    suspend fun signInWithGoogle(): AuthResult {
        val webClientId = context.getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        return try {
            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = FirebaseRefs.auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) AuthResult.Success(user)
                else AuthResult.Error("Sign-in returned no user.")
            } else {
                AuthResult.Error("Unexpected credential type returned.")
            }
        } catch (e: GoogleIdTokenParsingException) {
            AuthResult.Error("Could not parse Google credential: ${e.message}")
        } catch (e: GetCredentialException) {
            if (e.type == "android.credentials.GetCredentialException.TYPE_USER_CANCELED") {
                AuthResult.Cancelled
            } else {
                AuthResult.Error(e.message ?: "Sign-in failed.")
            }
        }
    }

    fun signOut() {
        FirebaseRefs.auth.signOut()
    }
}
