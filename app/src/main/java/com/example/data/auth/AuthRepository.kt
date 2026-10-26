package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

data class GoogleSignInAccountResult(
    val uid: String,
    val email: String,
    val displayName: String,
    val firebaseUser: FirebaseUser? = null
)

sealed interface AuthOperationState {
    data object Idle : AuthOperationState
    data object Loading : AuthOperationState
    data class Authenticated(val uid: String, val email: String?, val displayName: String?) : AuthOperationState
    data class Error(val message: String) : AuthOperationState
}

class AuthRepository(
    private var firebaseAuth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) {
    private val _authState = MutableStateFlow<AuthOperationState>(
        runCatching { firebaseAuth?.currentUser }.getOrNull()?.let {
            AuthOperationState.Authenticated(it.uid, it.email, it.displayName)
        } ?: AuthOperationState.Idle
    )
    val authState: StateFlow<AuthOperationState> = _authState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = runCatching { firebaseAuth?.currentUser }.getOrNull()

    private fun resolveFirebaseAuth(context: Context? = null): FirebaseAuth? {
        firebaseAuth?.let { return it }
        if (context != null) {
            runCatching {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
            }
        }
        val resolved = runCatching { Firebase.auth }.getOrNull()
        if (resolved != null) {
            firebaseAuth = resolved
        }
        return resolved
    }

    fun markGoogleAccountAuthenticated(uid: String, email: String, displayName: String) {
        _authState.value = AuthOperationState.Authenticated(uid, email, displayName)
    }

    fun observeFirebaseUser(): Flow<FirebaseUser?> = callbackFlow {
        val auth = resolveFirebaseAuth()
        if (auth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            val user = fbAuth.currentUser
            if (user != null) {
                _authState.value = AuthOperationState.Authenticated(user.uid, user.email, user.displayName)
            } else if (_authState.value is AuthOperationState.Authenticated) {
                _authState.value = AuthOperationState.Idle
            }
            trySend(user)
        }
        runCatching { auth.addAuthStateListener(listener) }
        awaitClose { runCatching { auth.removeAuthStateListener(listener) } }
    }

    suspend fun attemptAutoSignIn(context: Context): Result<FirebaseUser> {
        val auth = resolveFirebaseAuth(context) ?: return Result.failure(IllegalStateException("Cloud auth not configured"))
        auth.currentUser?.let { return Result.success(it) }
        val hostContext = context.findActivity() ?: context
        val clientId = try {
            hostContext.getString(R.string.default_web_client_id).trim()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (clientId.isBlank()) {
            return Result.failure(IllegalStateException("default_web_client_id is blank"))
        }

        val credentialManager = CredentialManager.create(hostContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(hostContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = checkNotNull(authResult.user) { "Firebase user was null after Google sign-in" }
                _authState.value = AuthOperationState.Authenticated(user.uid, user.email, user.displayName)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleInteractive(activityContext: Context): Result<GoogleSignInAccountResult> {
        _authState.value = AuthOperationState.Loading
        val hostContext = activityContext.findActivity() ?: activityContext
        val auth = resolveFirebaseAuth(hostContext)
        val clientId = try {
            hostContext.getString(R.string.default_web_client_id).trim()
        } catch (e: Exception) {
            ""
        }
        if (clientId.isBlank()) {
            val msg = "Google Sign-In client ID is empty"
            _authState.value = AuthOperationState.Error(msg)
            return Result.failure(IllegalStateException(msg))
        }

        return try {
            val credentialManager = CredentialManager.create(hostContext)
            val result: GetCredentialResponse = try {
                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                val primaryRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
                credentialManager.getCredential(hostContext, primaryRequest)
            } catch (cancelEx: GetCredentialCancellationException) {
                throw cancelEx
            } catch (primaryEx: Exception) {
                Log.w("Auth", "GetSignInWithGoogleOption failed, falling back to GetGoogleIdOption: ${primaryEx.message}")
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(hostContext, fallbackRequest)
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleTokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                val googleEmail = googleTokenCred.id.trim()
                val googleName = googleTokenCred.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: googleEmail.substringBefore("@").ifBlank { "Trader" }

                val fbUser = if (auth != null) {
                    runCatching {
                        val authCredential = GoogleAuthProvider.getCredential(googleTokenCred.idToken, null)
                        val authResult = auth.signInWithCredential(authCredential).await()
                        val signedInUser = authResult.user
                        if (signedInUser != null && signedInUser.displayName.isNullOrBlank() && googleName.isNotBlank()) {
                            runCatching {
                                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                    displayName = googleName
                                }
                                signedInUser.updateProfile(profileUpdates).await()
                            }
                        }
                        signedInUser
                    }.getOrNull()
                } else {
                    null
                }

                val effectiveUid = fbUser?.uid ?: deterministicGoogleUid(googleEmail)
                val effectiveEmail = fbUser?.email?.takeIf { it.isNotBlank() } ?: googleEmail
                val effectiveName = fbUser?.displayName?.takeIf { it.isNotBlank() } ?: googleName

                _authState.value = AuthOperationState.Authenticated(effectiveUid, effectiveEmail, effectiveName)
                Result.success(
                    GoogleSignInAccountResult(
                        uid = effectiveUid,
                        email = effectiveEmail,
                        displayName = effectiveName,
                        firebaseUser = fbUser
                    )
                )
            } else {
                val msg = "Unexpected credential type returned from CredentialManager"
                _authState.value = AuthOperationState.Error(msg)
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            _authState.value = AuthOperationState.Idle
            Result.failure(e)
        } catch (e: CancellationException) {
            _authState.value = AuthOperationState.Idle
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In via CredentialManager failed", e)
            val msg = e.localizedMessage ?: "Google Sign-In failed"
            _authState.value = AuthOperationState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String? = null): Result<FirebaseUser> {
        val auth = resolveFirebaseAuth() ?: return Result.failure(IllegalStateException("Local account registration fallback"))
        _authState.value = AuthOperationState.Loading
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = checkNotNull(authResult.user) { "Firebase user was null after registration" }
            if (!displayName.isNullOrBlank()) {
                try {
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                        this.displayName = displayName.trim()
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to set display name on new user", e)
                }
            }
            _authState.value = AuthOperationState.Authenticated(user.uid, user.email, user.displayName ?: displayName)
            Result.success(user)
        } catch (e: CancellationException) {
            _authState.value = AuthOperationState.Idle
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Registration failed", e)
            val msg = e.localizedMessage ?: "Registration failed"
            _authState.value = AuthOperationState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        val auth = resolveFirebaseAuth() ?: return Result.failure(IllegalStateException("Local account login fallback"))
        _authState.value = AuthOperationState.Loading
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = checkNotNull(authResult.user) { "Firebase user was null after login" }
            _authState.value = AuthOperationState.Authenticated(user.uid, user.email, user.displayName)
            Result.success(user)
        } catch (e: CancellationException) {
            _authState.value = AuthOperationState.Idle
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Email login failed", e)
            val msg = e.localizedMessage ?: "Invalid email or password"
            _authState.value = AuthOperationState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = resolveFirebaseAuth() ?: return Result.failure(IllegalStateException("Password reset service unavailable"))
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Password reset failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(context: Context): Result<Unit> {
        return try {
            val user = firebaseAuth?.currentUser
            if (user != null) {
                user.delete().await()
            }
            signOut(context)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Account deletion failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(context: Context) {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("Auth", "Firebase signOut failed", e)
        }
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            _authState.value = AuthOperationState.Idle
        }
    }

    fun clearError() {
        if (_authState.value is AuthOperationState.Error) {
            _authState.value = AuthOperationState.Idle
        }
    }

    companion object {
        fun deterministicGoogleUid(email: String): String {
            val normalized = email.trim().lowercase()
            val digest = MessageDigest.getInstance("SHA-256").digest("LifeAssetsGoogleUid::$normalized".toByteArray())
            val hex = digest.joinToString("") { "%02x".format(it) }.take(20)
            return "google_$hex"
        }
    }
}
