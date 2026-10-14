package id.sela.location
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
class ForegroundLocationTest {
    private val context = mockk<Context>()
    @Before fun setup() { mockkStatic(ContextCompat::class) }
    @After fun teardown() { unmockkStatic(ContextCompat::class) }
    @Test fun deniedPermissionDoesNotContactLocationService() = runTest {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_DENIED
        val location = ForegroundLocation(context)
        assertFalse(location.hasPermission()); assertNull(location.current())
        verify(exactly = 0) { context.getSystemService(any<String>()) }
    }
    @Test fun approximatePermissionIsEnough() {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        assertTrue(ForegroundLocation(context).hasPermission())
    }
}
