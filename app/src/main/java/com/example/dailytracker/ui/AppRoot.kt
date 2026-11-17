package com.example.dailytracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dailytracker.ui.ledger.LedgerScreen
import com.example.dailytracker.ui.settings.SettingsScreen
import com.example.dailytracker.ui.stats.StatsScreen
import com.example.dailytracker.ui.theme.DailyTrackerTheme
import com.example.dailytracker.ui.today.TodayScreen

@Composable
fun AppRoot() {
    val vm: MainViewModel = viewModel()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    DailyTrackerTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    // 顺序：打卡、账本、统计、设置
                    listOf(
                        Dest.Today,
                        Dest.Ledger,
                        Dest.Stats,
                        Dest.Settings
                    ).forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Dest.Today.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Dest.Today.route) { TodayScreen(vm) }
                composable(Dest.Ledger.route) { LedgerScreen(vm) }
                composable(Dest.Stats.route) { StatsScreen(vm) }
                composable(Dest.Settings.route) { SettingsScreen(vm) }
            }
        }
    }
}