package id.sela.feature.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Credential Manager -> Google ID token -> Firebase Authentication.
 *
 * webClientId: OAuth client ID tipe WEB dari Firebase Console.
 * Untuk debug APK (id.sela.debug): daftarkan package + SHA-1/SHA-256 debug ke Firebase.
 * Untuk release (id.sela): daftarkan package + SHA-1/SHA-256 release ke Firebase.
 *
 * TIDAK ada token palsu — jika Firebase belum dikonfigurasi, signIn() akan
 * gagal dengan error yang jelas (bukan crash, bukan login palsu).
 */
@Singleton
class GoogleSignInClient @Inject constructor() {
    /**
     * Anti-crash: FirebaseAuth.getInstance() throw IllegalStateException jika
     * FirebaseApp belum ter-init (mis. google-services.json bermasalah).
     * Jangan biarkan ini crash saat startup — anggap belum login.
     */
    private val auth: FirebaseAuth? by lazy {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }

    val isSignedIn: Boolean get() = auth?.currentUser != null
    val currentUserEmail: String? get() = auth?.currentUser?.email
    val currentUserName: String? get() = auth?.currentUser?.displayName

    suspend fun signIn(activity: Activity, webClientId: String): Result<Unit> = runCatching {
        val firebaseAuth = auth ?: error("Firebase belum terinisialisasi. Cek google-services.json.")
        require(webClientId.isNotBlank()) {
            "Web Client ID belum dikonfigurasi. Lihat README_FIREBASE_SETUP.md"
        }
        val manager = CredentialManager.create(activity)
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
        val result = manager.getCredential(context = activity, request = request)
        val credential = result.credential
        require(credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Credential yang diterima bukan Google ID token"
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        check(firebaseAuth.currentUser != null) { "Google login tidak mengembalikan pengguna" }
    }

    fun signOut() { auth?.signOut() }
}
