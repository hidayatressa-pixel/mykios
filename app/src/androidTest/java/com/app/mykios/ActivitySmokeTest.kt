package com.app.mykios

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivitySmokeTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun prepareRegisteredSession() {
        val session = SessionManager(context)
        session.logout()
        session.saveTokoInfo("Smoke Test Kios", "Tester", "Sembako / Warung", "1234")
        session.setIntroDone(true)
        session.setOnboardingFinished(true)
        session.setLanguage("in")
        // Runtime smoke tests verify activity startup, not Android permission UI.
        // Pre-grant notifications so MainActivity can remain RESUMED on API 33+.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            ).close()
        }
    }

    private fun <T : androidx.activity.ComponentActivity> launch(clazz: Class<T>) {
        ActivityScenario.launch(clazz).use { scenario ->
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        }
    }

    @Test fun mainActivityLaunches() = launch(MainActivity::class.java)
    @Test fun transactionActivityLaunches() = launch(TransaksiActivity::class.java)
    @Test fun stockActivityLaunches() = launch(StokActivity::class.java)
    @Test fun debtActivityLaunches() = launch(HutangActivity::class.java)
    @Test fun reportActivityLaunches() = launch(LaporanActivity::class.java)
    @Test fun settingsActivityLaunches() = launch(SettingsActivity::class.java)
    @Test fun assetsActivityLaunches() = launch(AssetsActivity::class.java)
    @Test fun expensesActivityLaunches() = launch(PengeluaranActivity::class.java)
    @Test fun walletActivityLaunches() = launch(DompetActivity::class.java)
    @Test fun upgradeActivityLaunches() = launch(UpgradeProActivity::class.java)
}
