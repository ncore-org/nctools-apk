package eu.nctools.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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

/** Root navigation graph. Routes mirror the app's information architecture. */
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    Surface(modifier = Modifier.fillMaxSize()) {
        ConsentGate {
            AuthNavFlow(navController = navController, authViewModel = authViewModel)
        }
    }
}

@Composable
private fun AuthNavFlow(
    navController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel,
) {
    val user = authViewModel.state.value.user

    NavHost(
        navController = navController,
        startDestination = if (user != null) "home" else "landing",
    ) {
            composable("landing") {
                LandingScreen(
                    onLogin = { navController.navigate("login") },
                    onRegister = { navController.navigate("register") },
                )
            }
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onBack = { navController.navigate("landing") },
                    onLoggedIn = { navController.navigate("home") { popUpTo("landing") { inclusive = true } } },
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onBack = { navController.navigate("landing") },
                    onRegistered = { navController.navigate("home") { popUpTo("landing") { inclusive = true } } },
                )
            }
            composable("home") {
                DashboardScreen(
                    viewModel = authViewModel,
                    onOpenTool = { navController.navigate("tool/$it") },
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