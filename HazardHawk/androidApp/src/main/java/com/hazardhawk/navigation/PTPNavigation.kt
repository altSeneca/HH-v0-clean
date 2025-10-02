package com.hazardhawk.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hazardhawk.ui.safety.ptp.PTPCreationScreen
import com.hazardhawk.ui.safety.ptp.PTPDocumentEditor
import com.hazardhawk.ui.safety.ptp.PTPListScreen
import org.koin.androidx.compose.koinViewModel

/**
 * Navigation graph for Pre-Task Plan (PTP) feature.
 * Defines navigation flow between PTP screens:
 * - PTPListScreen: View all PTPs
 * - PTPCreationScreen: Create new PTP via questionnaire
 * - PTPDocumentEditor: Review and edit AI-generated PTP
 */
fun NavGraphBuilder.ptpNavGraph(navController: NavHostController) {

    // PTP List Screen - Shows all PTPs with filtering
    composable(route = PTPRoute.PTPList.route) {
        PTPListScreen(
            viewModel = koinViewModel(),
            onNavigateToCreate = {
                navController.navigate(PTPRoute.PTPCreate.route)
            },
            onNavigateToPTP = { ptpId ->
                navController.navigate(PTPRoute.PTPEdit.createRoute(ptpId))
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    // PTP Creation Screen - Questionnaire for new PTP
    composable(route = PTPRoute.PTPCreate.route) {
        PTPCreationScreen(
            onNavigateToEditor = { ptpId ->
                // Navigate to editor and remove creation screen from back stack
                navController.navigate(PTPRoute.PTPEdit.createRoute(ptpId)) {
                    popUpTo(PTPRoute.PTPCreate.route) { inclusive = true }
                }
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    // PTP Editor Screen - Review and edit AI-generated content
    composable(
        route = PTPRoute.PTPEdit.ROUTE,
        arguments = listOf(
            navArgument(PTPRoute.PTPEdit.ARG_PTP_ID) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val ptpId = backStackEntry.arguments?.getString(PTPRoute.PTPEdit.ARG_PTP_ID)
            ?: run {
                // If no ID, navigate back
                navController.popBackStack()
                return@composable
            }

        PTPDocumentEditor(
            ptpId = ptpId,
            onNavigateBack = {
                navController.popBackStack()
            },
            onExportComplete = { filePath ->
                // Success - navigate back to list
                navController.navigate(PTPRoute.PTPList.route) {
                    // Clear the back stack up to list
                    popUpTo(PTPRoute.PTPList.route) { inclusive = false }
                }
            }
        )
    }

    // PTP View Screen (Read-only) - For approved/submitted PTPs
    composable(
        route = PTPRoute.PTPView.ROUTE,
        arguments = listOf(
            navArgument(PTPRoute.PTPView.ARG_PTP_ID) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val ptpId = backStackEntry.arguments?.getString(PTPRoute.PTPView.ARG_PTP_ID)
            ?: run {
                navController.popBackStack()
                return@composable
            }

        // For now, use the same editor component in read-only mode
        // In Phase 1 implementation, you can add a viewOnly parameter
        PTPDocumentEditor(
            ptpId = ptpId,
            onNavigateBack = {
                navController.popBackStack()
            },
            onExportComplete = { filePath ->
                navController.popBackStack()
            }
        )
    }
}

/**
 * Helper extension function to navigate to PTP List from any screen
 */
fun NavHostController.navigateToPTPList() {
    navigate(PTPRoute.PTPList.route) {
        // Single top to avoid multiple instances
        launchSingleTop = true
    }
}

/**
 * Helper extension function to navigate to PTP creation
 */
fun NavHostController.navigateToPTPCreate() {
    navigate(PTPRoute.PTPCreate.route)
}

/**
 * Helper extension function to navigate to PTP editor
 */
fun NavHostController.navigateToPTPEdit(ptpId: String) {
    navigate(PTPRoute.PTPEdit.createRoute(ptpId))
}

/**
 * Helper extension function to navigate to PTP viewer
 */
fun NavHostController.navigateToPTPView(ptpId: String) {
    navigate(PTPRoute.PTPView.createRoute(ptpId))
}
