package lk.jmcinnovators.learning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.jmcinnovators.learning.navigation.JmcNavGraph
import lk.jmcinnovators.learning.ui.theme.JmcTheme
import lk.jmcinnovators.learning.viewmodel.SessionViewModel
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = ViewModelFactory(applicationContext)
        val sessionViewModel = ViewModelProvider(this, factory)[SessionViewModel::class.java]

        // Keep the splash on screen until we know whether onboarding/auth are needed.
        splash.setKeepOnScreenCondition { !sessionViewModel.isReady.value }

        setContent {
            val themePref by sessionViewModel.themePreference.collectAsState()
            JmcTheme(themePreference = themePref) {
                JmcNavGraph(sessionViewModel = sessionViewModel, factory = factory)
            }
        }
    }
}
