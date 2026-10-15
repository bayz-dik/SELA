package id.sela.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Status autentikasi untuk UI. */
data class AuthUiState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userEmail: String? = null,
    val userName: String? = null,
)

/**
 * TUGAS C: Session management untuk Google login.
 * - Cek session saat startup (persisten via Firebase Auth)
 * - Login via Credential Manager
 * - Logout
 * - Error handling: loading, cancel, network error, retry
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInClient: GoogleSignInClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Cek session persisten saat ViewModel dibuat
        refreshSession()
    }

    fun refreshSession() {
        _uiState.value = _uiState.value.copy(
            isSignedIn = signInClient.isSignedIn,
            userEmail = signInClient.currentUserEmail,
            userName = signInClient.currentUserName,
            errorMessage = null,
        )
    }

    /**
     * Login dengan Google. webClientId dari BuildConfig (diisi via local.properties
     * atau BuildConfig field; JANGAN hardcode di repo).
     */
    fun signIn(activity: Activity, webClientId: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = signInClient.signIn(activity, webClientId)
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSignedIn = true,
                        userEmail = signInClient.currentUserEmail,
                        userName = signInClient.currentUserName,
                        errorMessage = null,
                    )
                }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("cancel", ignoreCase = true) == true ->
                            "Login dibatalkan."
                        e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("koneksi", ignoreCase = true) == true ->
                            "Koneksi bermasalah. Coba lagi."
                        e.message?.contains("Web Client ID", ignoreCase = true) == true ->
                            "Konfigurasi Firebase belum lengkap. Hubungi developer."
                        else -> "Gagal masuk: ${e.message ?: "error tidak diketahui"}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = msg,
                    )
                }
        }
    }

    fun signOut() {
        signInClient.signOut()
        _uiState.value = AuthUiState() // reset ke logged out
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
