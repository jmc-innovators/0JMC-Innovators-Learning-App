package lk.jmcinnovators.learning.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import lk.jmcinnovators.learning.ui.components.FullScreenLoading
import lk.jmcinnovators.learning.ui.screens.auth.LoginScreen
import lk.jmcinnovators.learning.ui.screens.auth.ProfileSetupScreen
import lk.jmcinnovators.learning.ui.screens.classroom.ClassroomDetailScreen
import lk.jmcinnovators.learning.ui.screens.classroom.ClassroomScreen
import lk.jmcinnovators.learning.ui.screens.home.HomeScreen
import lk.jmcinnovators.learning.ui.screens.notes.NoteEditorScreen
import lk.jmcinnovators.learning.ui.screens.notes.NotesScreen
import lk.jmcinnovators.learning.ui.screens.notifications.NotificationsScreen
import lk.jmcinnovators.learning.ui.screens.onboarding.OnboardingScreen
import lk.jmcinnovators.learning.ui.screens.profile.ProfileScreen
import lk.jmcinnovators.learning.ui.screens.tools.DictionaryScreen
import lk.jmcinnovators.learning.ui.screens.tools.PendingToolScreen
import lk.jmcinnovators.learning.ui.screens.tools.ToolsScreen
import lk.jmcinnovators.learning.viewmodel.SessionViewModel
import lk.jmcinnovators.learning.viewmodel.StartDestination
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Filled.Home),
    BottomTab(Routes.CLASSROOM, "Classroom", Icons.Filled.School),
    BottomTab(Routes.TOOLS, "Tools", Icons.Filled.MenuBook),
    BottomTab(Routes.NOTES, "Notes", Icons.Filled.StickyNote2),
    BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

@Composable
fun JmcNavGraph(sessionViewModel: SessionViewModel, factory: ViewModelFactory) {
    val isReady by sessionViewModel.isReady.collectAsState()
    if (!isReady) {
        FullScreenLoading()
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    val start = when (sessionViewModel.startDestination.collectAsState().value) {
        StartDestination.ONBOARDING -> Routes.ONBOARDING
        StartDestination.LOGIN -> Routes.LOGIN
        StartDestination.PROFILE_SETUP -> Routes.PROFILE_SETUP
        StartDestination.HOME -> Routes.HOME
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = start,
                enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 6 } },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { it / 6 } }
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onGetStarted = {
                        sessionViewModel.completeOnboarding()
                        navController.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.LOGIN) {
                    LoginScreen(factory = factory, onSignedIn = { hasProfile ->
                        val dest = if (hasProfile) Routes.HOME else Routes.PROFILE_SETUP
                        navController.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    })
                }
                composable(Routes.PROFILE_SETUP) {
                    ProfileSetupScreen(factory = factory, onSaved = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.PROFILE_SETUP) { inclusive = true } }
                    })
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        factory = factory,
                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                        onOpenAction = { route -> navController.navigate(route) }
                    )
                }
                composable(Routes.CLASSROOM) {
                    ClassroomScreen(factory = factory, onOpenClass = { id ->
                        navController.navigate(Routes.classroomDetail(id))
                    })
                }
                composable(
                    Routes.CLASSROOM_DETAIL_ROUTE,
                    arguments = listOf(navArgument(Routes.CLASSROOM_ID_ARG) { })
                ) { entry ->
                    val classId = entry.arguments?.getString(Routes.CLASSROOM_ID_ARG).orEmpty()
                    ClassroomDetailScreen(classId = classId, factory = factory, onBack = { navController.popBackStack() })
                }
                composable(Routes.TOOLS) {
                    ToolsScreen(onOpenTool = { route -> navController.navigate(route) })
                }
                composable(Routes.DICTIONARY) {
                    DictionaryScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.MATHS_LAB) {
                    PendingToolScreen("Maths Lab", onBack = { navController.popBackStack() })
                }
                composable(Routes.SCIENCE_WORLD) {
                    PendingToolScreen("Science World", onBack = { navController.popBackStack() })
                }
                composable(Routes.NOTES) {
                    NotesScreen(factory = factory, onOpenNote = { id -> navController.navigate(Routes.noteEditor(id)) })
                }
                composable(
                    Routes.NOTE_EDITOR_ROUTE,
                    arguments = listOf(navArgument(Routes.NOTE_EDITOR_ARG) { })
                ) { entry ->
                    val noteId = entry.arguments?.getString(Routes.NOTE_EDITOR_ARG) ?: "new"
                    NoteEditorScreen(noteId = noteId, factory = factory, onBack = { navController.popBackStack() })
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(factory = factory, onSignedOut = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    })
                }
                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(factory = factory, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
