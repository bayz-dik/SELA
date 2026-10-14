package id.sela.feature.settings
import android.content.Context
import androidx.lifecycle.viewModelScope
import id.sela.database.TransitDao
import id.sela.network.BackupManager
import id.sela.network.DataUsage
import id.sela.network.ProfileManager
import id.sela.network.SelaPrefs
import id.sela.network.SelaSettings
import id.sela.network.TransitPreferences
import id.sela.network.WeatherAdvisor
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private var vm: SettingsViewModel? = null
    private fun settings(): SelaSettings = mockk(relaxed = true) {
        every { tripHistory } returns flowOf(emptyList())
        every { notifications } returns flowOf(emptyList())
        every { offlineMode } returns flowOf(false)
        every { arrivalAlerts } returns flowOf(false)
        every { gtfsVersion } returns flowOf("bundled-2026-10-14")
        every { gtfsUpdatedAt } returns flowOf(0L)
        every { gtfsUpdateUrl } returns flowOf("")
    }
    private fun preferences(): TransitPreferences = mockk(relaxed = true) {
        every { amoledDark } returns flowOf(false)
        every { materialYou } returns flowOf(false)
    }
    private fun buildVm(s: SelaSettings = settings(), p: TransitPreferences = preferences()): SettingsViewModel =
        SettingsViewModel(
            s, p,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    @After fun teardown() { vm?.viewModelScope?.cancel(); Dispatchers.resetMain() }
    @Test fun viewModelInitializes() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val model = buildVm().also { vm = it }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        assertNull(model.state.value.message)
        assertFalse(model.state.value.offlineMode)
        assertEquals("bundled-2026-10-14", model.state.value.gtfsVersion)
    }
    @Test fun togglesDelegateToSettings() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val s = settings()
        val model = buildVm(s = s).also { vm = it }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        model.toggleOffline(); model.toggleAlerts(); runCurrent()
        coVerify { s.toggleOfflineMode() }
        coVerify { s.toggleArrivalAlerts() }
    }
    @Test fun themeTogglesDelegateToPreferences() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val p = preferences()
        val model = buildVm(p = p).also { vm = it }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        model.toggleAmoled(); model.toggleMaterialYou(); runCurrent()
        coVerify { p.setAmoledDark(true) }
        coVerify { p.setMaterialYou(true) }
    }
}
