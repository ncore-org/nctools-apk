package eu.nctools.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.nctools.app.ui.auth.AuthViewModel
import eu.nctools.app.ui.auth.LoginScreen
import eu.nctools.app.ui.auth.RegisterScreen
import eu.nctools.app.ui.consent.ConsentGate
import eu.nctools.app.ui.dashboard.DashboardScreen
import eu.nctools.app.ui.tools.ToolDetailScreen
import eu.nctools.app.ui.tools.ToolsViewModel

/**
 * Root of the app. Two top-level flows:
 *  - Auth flow (landing → login / register), and
 *  - Main flow (dashboard + tools), reachable either signed-in OR in guest mode.
 *
 * Which flow is active is derived from state, so signing in, choosing guest, or
 * logging out switches screens automatically with no manual navigation calls.
 */
@Composable
fun AppRoot() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val state by authViewModel.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        ConsentGate {
            val entered = state.user != null || state.guestMode
            if (entered) {
                MainNavFlow(
                    authViewModel = authViewModel,
                    isGuest = state.user == null,
                )
            } else {
                AuthNavFlow(authViewModel = authViewModel)
            }
        }
    }
}

/** Signed-in or guest experience: dashboard → tools. */
@Composable
private fun MainNavFlow(
    authViewModel: AuthViewModel,
    isGuest: Boolean,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            DashboardScreen(
                viewModel = authViewModel,
                isGuest = isGuest,
                onOpenTool = { navController.navigate("tool/$it") },
                onSignIn = { authViewModel.exitGuest() },
            )
        }
        composable("tool/{slug}") { entry ->
            val slug = entry.arguments?.getString("slug") ?: return@composable
            val toolsViewModel: ToolsViewModel = hiltViewModel()
            ToolDetailScreen(
                slug = slug,
                viewModel = toolsViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Unauthenticated flow: landing → login / register, with a guest escape hatch. */
@Composable
private fun AuthNavFlow(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "landing") {
        composable("landing") {
            LandingScreen(
                onLogin = { navController.navigate("login") },
                onRegister = { navController.navigate("register") },
                onGuest = { authViewModel.continueAsGuest() },
            )
        }
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}