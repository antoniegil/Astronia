package com.antoniegil.astronia

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.antoniegil.astronia.viewmodel.MainViewModel
import com.antoniegil.astronia.ui.common.Route
import com.antoniegil.astronia.ui.common.animatedComposable
import com.antoniegil.astronia.ui.page.*
import com.antoniegil.astronia.ui.page.settings.*
import com.antoniegil.astronia.ui.page.settings.about.*
import com.antoniegil.astronia.ui.page.settings.appearance.*
import com.antoniegil.astronia.ui.page.settings.data.*
import com.antoniegil.astronia.ui.page.settings.network.*
import com.antoniegil.astronia.ui.page.settings.player.*
import com.antoniegil.astronia.ui.page.settings.video.*
import com.antoniegil.astronia.ui.theme.SettingsProvider
import com.antoniegil.astronia.util.LanguageContextWrapper
import com.antoniegil.astronia.util.SettingsManager
import com.antoniegil.astronia.util.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = SettingsManager.getLocaleFromPreference(this)
            com.antoniegil.astronia.util.setLanguage(locale)
        }
        
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            
            LaunchedEffect(Unit) {
                SettingsManager.initializeThemeSettings(context)
            }
            
            val pendingNav = remember { SettingsManager.getPendingNavigation(context) }
            
            LaunchedEffect(Unit) {
                if (pendingNav.isNotEmpty()) {
                    SettingsManager.clearPendingNavigation(context)
                }
            }
            
            SettingsProvider {
                MainScreen(
                    initialNavigation = pendingNav
                )
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context?) {
        val wrappedContext = if (newBase != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = SettingsManager.getLocaleFromPreference(newBase)
            LanguageContextWrapper.wrap(newBase, locale)
        } else {
            newBase
        }
        super.attachBaseContext(wrappedContext)
    }
}

@Composable
fun MainScreen(
    initialNavigation: String = "",
    viewModel: MainViewModel = viewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf(UpdateUtil.LatestRelease()) }

    val navController = rememberNavController()
    
    LaunchedEffect(Unit) {
        if (SettingsManager.getAutoUpdate(context)) {
            scope.launch(Dispatchers.IO) {
                runCatching<Unit> {
                    UpdateUtil.checkForUpdate(context)?.let {
                        latestRelease = it
                        showUpdateDialog = true
                    }
                }
            }
        }
    }
    
    LaunchedEffect(initialNavigation) {
        if (initialNavigation.isNotEmpty()) {
            when (initialNavigation) {
                "settings" -> navController.navigate(Route.SETTINGS)
                "language" -> navController.navigate(Route.LANGUAGE)
                "appearance" -> navController.navigate(Route.APPEARANCE)
            }
        }
    }

    val onNavigateBack: () -> Unit = {
        with(navController) {
            val state = currentBackStackEntry?.lifecycle?.currentState
            if (state != null && state.isAtLeast(Lifecycle.State.STARTED)) {
                popBackStack()
            }
        }
    }

    val shouldShowPlayer = playbackState.playingUrl != null

    if (shouldShowPlayer) {
        key(playbackState.playingUrl) {
            PlayerPage(
                url = playbackState.playingUrl!!,
                initialChannelUrl = playbackState.initialChannelUrl,
                initialVideoTitle = playbackState.initialVideoTitle,
                initialChannelId = playbackState.initialChannelId,
                onBack = { viewModel.stopPlayback() }
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = Route.HOME
            ) {
                animatedComposable(Route.HOME) {
                    HomePage(
                        onNavigateToSettings = {
                            navController.navigate(Route.SETTINGS) {
                                launchSingleTop = true
                                popUpTo(Route.HOME)
                            }
                        },
                        onNavigateToHistory = {
                            navController.navigate(Route.HISTORY) {
                                launchSingleTop = true
                                popUpTo(Route.HOME)
                            }
                        },
                        onPlayUrl = { url -> viewModel.startPlayback(url) },
                        onPlayHistoryItem = { historyItem ->
                            viewModel.startPlaybackFromHistory(historyItem)
                        }
                    )
                }
                animatedComposable(Route.HISTORY) {
                    HistoryPage(
                        onBack = onNavigateBack,
                        onPlay = { historyItem ->
                            viewModel.startPlaybackFromHistory(historyItem)
                        }
                    )
                }
                
                animatedComposable(Route.SETTINGS) {
                    SettingsPage(
                        onNavigateBack = onNavigateBack,
                        onNavigateToPlayer = { navController.navigate(Route.PLAYER_SETTINGS) { launchSingleTop = true } },
                        onNavigateToVideo = { navController.navigate(Route.VIDEO_SETTINGS) { launchSingleTop = true } },
                        onNavigateToAppearance = { navController.navigate(Route.APPEARANCE) { launchSingleTop = true } },
                        onNavigateToDataManagement = { navController.navigate(Route.DATA_MANAGEMENT) { launchSingleTop = true } },
                        onNavigateToProxy = { navController.navigate(Route.NETWORK) { launchSingleTop = true } },
                        onNavigateToAbout = { navController.navigate(Route.ABOUT) { launchSingleTop = true } }
                    )
                }
                animatedComposable(Route.PLAYER_SETTINGS) {
                    PlayerSettingsPage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.VIDEO_SETTINGS) {
                    VideoSettingsPage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.APPEARANCE) {
                    AppearancePage(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDarkTheme = { navController.navigate(Route.DARK_THEME) { launchSingleTop = true } },
                        onNavigateToLanguage = { navController.navigate(Route.LANGUAGE) { launchSingleTop = true } }
                    )
                }
                animatedComposable(Route.DARK_THEME) {
                    DarkThemePage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.LANGUAGE) {
                    LanguagePage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.DATA_MANAGEMENT) {
                    DataManagementPage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.NETWORK) {
                    NetworkPage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.ABOUT) {
                    AboutPage(
                        onNavigateBack = onNavigateBack,
                        onNavigateToLicense = { navController.navigate(Route.LICENSE) { launchSingleTop = true } },
                        onNavigateToUpdate = { navController.navigate(Route.UPDATE) { launchSingleTop = true } }
                    )
                }
                animatedComposable(Route.LICENSE) {
                    LicensePage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.UPDATE) {
                    UpdatePage(onNavigateBack = onNavigateBack)
                }
            }
        }
    }
    
    if (showUpdateDialog) {
        UpdateDialog(
            onDismissRequest = { showUpdateDialog = false },
            latestRelease = latestRelease
        )
    }
}
