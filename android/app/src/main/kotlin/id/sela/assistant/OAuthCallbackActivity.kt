package id.sela.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Activity transparan penangkap redirect OAuth ChatGPT (sela://oauth/callback).
 * Menukar code jadi token lalu langsung finish.
 */
class OAuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            // ChatGptAuth.handleCallback mengurus validasi state + tukar token.
            // Hasilnya disiarkan lewat ChatGptAuth.authState untuk UI.
            runCatching { ChatGptAuth.handleCallback(uri) }
            finish()
        }
    }
}
