package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.platform.BiometricHelper
import com.example.ui.navigation.AnalyticsRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.DisciplineRoute
import com.example.ui.navigation.FinanceRoute
import com.example.ui.navigation.FitnessRoute
import com.example.ui.navigation.FocusRoute
import com.example.ui.navigation.GoalsRoute
import com.example.ui.navigation.HabitsRoute
import com.example.ui.navigation.JournalRoute
import com.example.ui.navigation.LifeOsRoute
import com.example.ui.navigation.ModulesRoute
import com.example.ui.navigation.ReadingRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DisciplineScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.FitnessScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.ModulesHubScreen
import com.example.ui.screens.ReadingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LifeOsTheme
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FitnessViewModel
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.GoalsViewModel
import com.example.ui.viewmodel.HabitsViewModel
import com.example.ui.viewmodel.JournalViewModel
import com.example.ui.viewmodel.LifeOsViewModelFactory
import com.example.ui.viewmodel.ReadingViewModel
import com.example.ui.viewmodel.SettingsViewModel

private data class TopLevelTab(
    val route: LifeOsRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LifeOsApplication
        val factory = LifeOsViewModelFactory(app)

        setContent {
            val appViewModel: AppViewModel = viewModel(factory = factory)
            val prefs by appViewModel.preferences.collectAsStateWithLifecycle()
            val isUnlocked by appViewModel.isUnlocked.collectAsStateWithLifecycle()
            val lockInitialized by appViewModel.lockInitialized.collectAsStateWithLifecycle()
            var authError by remember { mutableStateOf<String?>(null) }

            LifeOsTheme(themePreference = prefs.themeEnum) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (!lockInitialized) {
                        Box(modifier = Modifier.fillMaxSize())
                    } else if (prefs.biometricLockEnabled && !isUnlocked) {
                        LaunchedEffect(Unit) {
                            BiometricHelper.promptUnlock(
                                activity = this@MainActivity,
                                onSuccess = { appViewModel.markUnlocked() },
                                onError = { msg -> authError = msg },
                            )
                        }
                        BiometricLockGate(
                            errorMessage = authError,
                            onUnlockClick = {
                                authError = null
                                BiometricHelper.promptUnlock(
                                    activity = this@MainActivity,
                                    onSuccess = { appViewModel.markUnlocked() },
                                    onError = { msg -> authError = msg },
                                )
                            },
                        )
                    } else {
                        LifeOsMainShell(factory = factory)
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricLockGate(
    errorMessage: String?,
    onUnlockClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Biometric Lock",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("LIFE OS 365 Locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorMessage ?: "Verify your identity to access your private offline vault.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onUnlockClick, modifier = Modifier.testTag("unlock_app_button")) {
            Text("Unlock")
        }
    }
}

@Composable
fun LifeOsMainShell(factory: LifeOsViewModelFactory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val topLevelTabs = remember {
        listOf(
            TopLevelTab(DashboardRoute, "Today", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
            TopLevelTab(HabitsRoute, "Habits", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "nav_habits"),
            TopLevelTab(FocusRoute, "Focus", Icons.Filled.Timer, Icons.Outlined.Timer, "nav_focus"),
            TopLevelTab(AnalyticsRoute, "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_analytics"),
            TopLevelTab(ModulesRoute, "Modules", Icons.Filled.Apps, Icons.Outlined.Apps, "nav_modules"),
        )
    }

    val isTopLevel = currentDest == null ||
        topLevelTabs.any { tab -> currentDest.hasRoute(tab.route::class) }

    fun navigateTopTab(route: LifeOsRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && isTopLevel) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    topLevelTabs.forEach { tab ->
                        val selected = currentDest?.hasRoute(tab.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.testTag(tab.tag),
                        )
                    }
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail && isTopLevel) {
                        Column {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = LifeOsThemeTokens.colors.border,
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                windowInsets = NavigationBarDefaults.windowInsets,
                            ) {
                                topLevelTabs.forEach { tab ->
                                    val selected = currentDest?.hasRoute(tab.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label,
                                            )
                                        },
                                        label = { Text(tab.label, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        modifier = Modifier.testTag(tab.tag),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = DashboardRoute,
                        enterTransition = { fadeIn(tween(180)) },
                        exitTransition = { fadeOut(tween(150)) },
                        popEnterTransition = { fadeIn(tween(180)) },
                        popExitTransition = { fadeOut(tween(150)) },
                    ) {
                        composable<DashboardRoute> {
                            val vm: DashboardViewModel = viewModel(factory = factory)
                            DashboardScreen(
                                viewModel = vm,
                                onNavigateHabits = { navigateTopTab(HabitsRoute) },
                                onNavigateFocus = { navigateTopTab(FocusRoute) },
                                onNavigateFitness = { navController.navigate(FitnessRoute) },
                                onNavigateFinance = { navController.navigate(FinanceRoute) },
                                onNavigateGoals = { navController.navigate(GoalsRoute) },
                                onNavigateJournal = { navController.navigate(JournalRoute) },
                                onNavigateAnalytics = { navigateTopTab(AnalyticsRoute) },
                            )
                        }
                        composable<HabitsRoute> {
                            val vm: HabitsViewModel = viewModel(factory = factory)
                            HabitsScreen(viewModel = vm)
                        }
                        composable<FocusRoute> {
                            val vm: FocusViewModel = viewModel(factory = factory)
                            FocusScreen(viewModel = vm)
                        }
                        composable<AnalyticsRoute> {
                            val vm: AnalyticsViewModel = viewModel(factory = factory)
                            AnalyticsScreen(viewModel = vm)
                        }
                        composable<ModulesRoute> {
                            ModulesHubScreen(
                                onOpenFitness = { navController.navigate(FitnessRoute) },
                                onOpenFinance = { navController.navigate(FinanceRoute) },
                                onOpenGoals = { navController.navigate(GoalsRoute) },
                                onOpenJournal = { navController.navigate(JournalRoute) },
                                onOpenReading = { navController.navigate(ReadingRoute) },
                                onOpenDiscipline = { navController.navigate(DisciplineRoute) },
                                onOpenSettings = { navController.navigate(SettingsRoute) },
                            )
                        }
                        composable<FitnessRoute> {
                            val vm: FitnessViewModel = viewModel(factory = factory)
                            FitnessScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                        composable<FinanceRoute> {
                            val vm: FinanceViewModel = viewModel(factory = factory)
                            FinanceScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                        composable<GoalsRoute> {
                            val vm: GoalsViewModel = viewModel(factory = factory)
                            GoalsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                        composable<JournalRoute> {
                            val vm: JournalViewModel = viewModel(factory = factory)
                            JournalScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                        composable<ReadingRoute> {
                            val vm: ReadingViewModel = viewModel(factory = factory)
                            ReadingScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                        composable<DisciplineRoute> {
                            val vm: DisciplineViewModel = viewModel(factory = factory)
                            DisciplineScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                        composable<SettingsRoute> {
                            val vm: SettingsViewModel = viewModel(factory = factory)
                            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
