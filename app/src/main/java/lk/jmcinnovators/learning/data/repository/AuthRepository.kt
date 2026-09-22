package lk.jmcinnovators.learning.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuthException
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
 * Google Sign-In using modern Android Credential Manager + Google Identity Services + Firebase Auth.
 *
 * Resiliency:
 * 1. Tries quick sign-in using previously authorized accounts first.
 * 2. If NoCredentialException is thrown (i.e. no previously authorized account exists on this device),
 *    it automatically retries with authorized-account filtering disabled (filterByAuthorizedAccounts = false)
 *    and optionally with GetSignInWithGoogleOption, presenting the full Google account chooser UI.
 * 3. Handles cancellation, interruptedException, and missing account states cleanly.
 * 4. Authenticates the retrieved Google ID token against Firebase Authentication.
 */
class AuthRepository(private val context: Context) {

    private val tag = "AuthRepository"

    val currentUser: FirebaseUser? get() = FirebaseRefs.auth.currentUser

    suspend fun signInWithGoogle(activityContext: Context? = null): AuthResult {
        val targetContext = resolveActivity(activityContext ?: context) ?: activityContext ?: context
        val webClientId = targetContext.getString(R.string.default_web_client_id)

        if (webClientId.isBlank()) {
            Log.e(tag, "Google Web Client ID is missing in strings.xml")
            return AuthResult.Error("Configuration error: Google Client ID is missing.")
        }

        val credentialManager = CredentialManager.create(targetContext)

        // Step 1: Attempt sign-in with authorized accounts filter
        val authorizedAccountsOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val initialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(authorizedAccountsOption)
            .build()

        val response: GetCredentialResponse = try {
            Log.d(tag, "Attempting Google Sign-In with authorized accounts filter...")
            credentialManager.getCredential(targetContext, initialRequest)
        } catch (e: Exception) {
            if (isCancellation(e)) {
                Log.d(tag, "User cancelled initial sign-in.")
                return AuthResult.Cancelled
            }

            if (isNoCredentialsError(e)) {
                Log.d(tag, "No previously authorized account (${e.javaClass.simpleName}). Retrying with full account chooser...")
                // Step 2: Retry with authorized-account filtering disabled to show account chooser
                val unfilteredOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val retryRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(unfilteredOption)
                    .build()

                try {
                    credentialManager.getCredential(targetContext, retryRequest)
                } catch (retryEx: Exception) {
                    if (isCancellation(retryEx)) {
                        Log.d(tag, "User cancelled account selection.")
                        return AuthResult.Cancelled
                    }

                    if (isNoCredentialsError(retryEx)) {
                        Log.d(tag, "Retrying with GetSignInWithGoogleOption fallback...")
                        val siwgOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                        val siwgRequest = GetCredentialRequest.Builder()
                            .addCredentialOption(siwgOption)
                            .build()

                        try {
                            credentialManager.getCredential(targetContext, siwgRequest)
                        } catch (siwgEx: Exception) {
                            return handleException(siwgEx)
                        }
                    } else {
                        return handleException(retryEx)
                    }
                }
            } else {
                return handleException(e)
            }
        }

        return processCredentialResponse(response)
    }

    private suspend fun processCredentialResponse(response: GetCredentialResponse): AuthResult {
        val credential = response.credential
        return try {
            val idToken = when {
                credential is CustomCredential &&
                    (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                     credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL) -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdTokenCredential.idToken
                }
                credential is CustomCredential -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdTokenCredential.idToken
                }
                else -> {
                    Log.e(tag, "Unexpected credential type: ${credential::class.java.name}")
                    return AuthResult.Error("Unexpected credential type returned from Google.")
                }
            }

            if (idToken.isBlank()) {
                return AuthResult.Error("Failed to obtain Google ID token.")
            }

            Log.d(tag, "Obtained Google ID token, authenticating with Firebase...")
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = FirebaseRefs.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            if (user != null) {
                Log.d(tag, "Firebase sign-in successful. UID: ${user.uid}")
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Sign-in succeeded, but no user profile was returned.")
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(tag, "Failed to parse Google ID token: ${e.message}", e)
            AuthResult.Error("Could not process Google credentials: ${e.message}")
        } catch (e: FirebaseAuthException) {
            Log.e(tag, "Firebase authentication failed: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Firebase authentication failed.")
        } catch (e: Exception) {
            Log.e(tag, "Error processing Google credential: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Sign-in failed.")
        }
    }

    private fun handleException(e: Exception): AuthResult {
        if (isCancellation(e)) {
            return AuthResult.Cancelled
        }
        if (isNoCredentialsError(e)) {
            Log.w(tag, "No Google accounts available on device.")
            return AuthResult.Error("No Google account found on this device. Please add a Google account in Settings and try again.")
        }
        Log.e(tag, "Credential Manager error: ${e.javaClass.simpleName} - ${e.message}", e)
        return AuthResult.Error(e.localizedMessage ?: "Sign-in failed. Please try again.")
    }

    private fun isNoCredentialsError(e: Throwable): Boolean {
        if (e is NoCredentialException) return true
        val type = (e as? GetCredentialException)?.type ?: ""
        val msg = e.message.orEmpty()
        return type.contains("TYPE_NO_CREDENTIAL", ignoreCase = true) ||
               type.contains("NoCredential", ignoreCase = true) ||
               msg.contains("No credentials available", ignoreCase = true) ||
               msg.contains("no credential", ignoreCase = true)
    }

    private fun isCancellation(e: Throwable): Boolean {
        if (e is GetCredentialCancellationException || e is GetCredentialInterruptedException) return true
        val type = (e as? GetCredentialException)?.type ?: ""
        val msg = e.message.orEmpty()
        return type.contains("CANCELED", ignoreCase = true) ||
               type.contains("Cancellation", ignoreCase = true) ||
               msg.contains("cancel", ignoreCase = true)
    }

    private fun resolveActivity(ctx: Context): Activity? {
        var current = ctx
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun signOut() {
        FirebaseRefs.auth.signOut()
    }
}
