package com.hazardhawk.navigation

/**
 * Navigation routes for Pre-Task Plan (PTP) feature.
 * Defines all PTP-related screens and their parameters.
 */
sealed class PTPRoute(val route: String) {
    /**
     * PTP list screen showing all PTPs
     */
    object PTPList : PTPRoute("ptp/list")

    /**
     * PTP creation questionnaire screen
     */
    object PTPCreate : PTPRoute("ptp/create")

    /**
     * PTP document editor screen for reviewing and editing AI-generated content
     */
    data class PTPEdit(val ptpId: String) : PTPRoute("ptp/edit/{ptpId}") {
        companion object {
            const val ROUTE = "ptp/edit/{ptpId}"
            const val ARG_PTP_ID = "ptpId"

            fun createRoute(ptpId: String) = "ptp/edit/$ptpId"
        }
    }

    /**
     * PTP view-only screen for approved/submitted PTPs
     */
    data class PTPView(val ptpId: String) : PTPRoute("ptp/view/{ptpId}") {
        companion object {
            const val ROUTE = "ptp/view/{ptpId}"
            const val ARG_PTP_ID = "ptpId"

            fun createRoute(ptpId: String) = "ptp/view/$ptpId"
        }
    }
}
