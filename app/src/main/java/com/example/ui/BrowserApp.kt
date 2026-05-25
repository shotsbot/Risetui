package com.example.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.*
import com.example.viewmodel.BrowserViewModel
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserApp(viewModel: BrowserViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "browser") {
        composable(
            route = "browser",
            enterTransition = {
                if (initialState.destination.route == "tabs") {
                    scaleIn(initialScale = 0.7f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                } else {
                    fadeIn(animationSpec = tween(400))
                }
            },
            exitTransition = {
                if (targetState.destination.route == "tabs") {
                    scaleOut(targetScale = 0.7f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                } else {
                    fadeOut(animationSpec = tween(400))
                }
            },
            popEnterTransition = {
                if (initialState.destination.route == "tabs") {
                    scaleIn(initialScale = 0.7f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                } else {
                    fadeIn(animationSpec = tween(400))
                }
            },
            popExitTransition = {
                if (targetState.destination.route == "tabs") {
                    scaleOut(targetScale = 0.7f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                } else {
                    fadeOut(animationSpec = tween(400))
                }
            }
        ) {
            BrowserScreen(
                viewModel = viewModel,
                onNavigateToBookmarks = { navController.navigate("bookmarks") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDashboard = { navController.navigate("dashboard") },
                onNavigateToTabs = { navController.navigate("tabs") },
                onNavigateToDownloads = { navController.navigate("downloads") }
            )
        }
        composable(
            route = "tabs",
            enterTransition = {
                if (initialState.destination.route == "browser") {
                    scaleIn(initialScale = 1.1f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300))
                }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                if (targetState.destination.route == "browser") {
                    scaleOut(targetScale = 1.1f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300))
                }
            }
        ) {
            TabsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable(
            route = "bookmarks",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            BookmarksScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(
            route = "settings",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            SettingsScreen(
                viewModel = viewModel, 
                onBack = { navController.popBackStack() },
                onNavigateToExtensions = { navController.navigate("extensions") }
            )
        }
        composable(
            route = "extensions",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            ExtensionsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(
            route = "dashboard",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300)) }
        ) {
            DashboardScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "downloads",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300)) }
        ) {
            DownloadsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
