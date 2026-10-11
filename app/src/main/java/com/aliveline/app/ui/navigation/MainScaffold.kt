package com.aliveline.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aliveline.app.audio.LocalSoundPlayer
import com.aliveline.app.audio.Sfx
import com.aliveline.app.feature.bot.BotDetailScreen
import com.aliveline.app.feature.create.CreateTaskScreen
import com.aliveline.app.feature.home.HomeScreen
import com.aliveline.app.feature.notes.NoteEditorScreen
import com.aliveline.app.feature.notes.NotesScreen
import com.aliveline.app.feature.task.TaskDetailScreen
import com.aliveline.app.ui.theme.AliveColors

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home", Icons.Default.Home),
    TabItem(Routes.MAP, "Map", Icons.Default.DateRange),
    TabItem(Routes.NOTES, "Notes", Icons.Default.Edit),
    TabItem(Routes.REMINDERS, "Reminders", Icons.Default.Notifications),
)

@Composable
fun MainScaffold(onReplayOnboarding: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    // A "bloop" every time the screen changes (tabs, opening a screen, going back).
    val sound = LocalSoundPlayer.current
    var firstRouteSeen by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        if (firstRouteSeen) {
            sound?.play(Sfx.NAV)
        } else if (currentRoute != null) {
            firstRouteSeen = true
        }
    }

    Scaffold(
        containerColor = AliveColors.Background,
        contentColor = AliveColors.TextPrimary,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (showBottomBar) AliveBottomBar(currentRoute, navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onBotClick = { navController.navigate(Routes.bot(it)) },
                    onTaskClick = { navController.navigate(Routes.task(it)) },
                )
            }
            composable(Routes.MAP) {
                PlaceholderScreen("Daily Map", "Your day as a timeline. Coming soon.")
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onNoteClick = { navController.navigate(Routes.note(id = it)) },
                    onNewNote = { navController.navigate(Routes.note()) },
                )
            }
            composable(Routes.REMINDERS) {
                RemindersScreen(onReplayOnboarding)
            }
            composable(
                Routes.BOT_PATTERN,
                arguments = listOf(navArgument("botType") { type = NavType.StringType }),
            ) {
                BotDetailScreen(
                    onBack = { navController.popBackStack() },
                    onTaskClick = { navController.navigate(Routes.task(it)) },
                    onAddTask = { navController.navigate(Routes.create(it)) },
                )
            }
            composable(
                Routes.TASK_PATTERN,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) {
                TaskDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNote = { navController.navigate(Routes.note(id = it)) },
                    onAddNote = { taskId -> navController.navigate(Routes.note(taskId = taskId)) },
                )
            }
            composable(
                Routes.CREATE_PATTERN,
                arguments = listOf(
                    navArgument("bot") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                CreateTaskScreen(onClose = { navController.popBackStack() })
            }
            composable(
                Routes.NOTE_PATTERN,
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("taskId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) {
                NoteEditorScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AliveBottomBar(currentRoute: String?, navController: NavController) {
    fun goTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavigationBar(containerColor = AliveColors.Surface) {
        tabs.take(2).forEach { TabButton(it, currentRoute == it.route) { goTo(it.route) } }

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate(Routes.create()) },
            icon = {
                Box(
                    modifier = Modifier.size(44.dp).background(AliveColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add task", tint = Color.White)
                }
            },
        )

        tabs.drop(2).forEach { TabButton(it, currentRoute == it.route) { goTo(it.route) } }
    }
}

@Composable
private fun RowScope.TabButton(item: TabItem, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label) },
    )
}