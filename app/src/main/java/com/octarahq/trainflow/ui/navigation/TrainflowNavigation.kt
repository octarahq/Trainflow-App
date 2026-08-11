package com.octarahq.trainflow.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Accueil")
    object Trips : Screen("trips", "Mes trajets")
    object TrainInfo : Screen("train-info/{trainId}?speed={speed}", "Train Info") {
        fun createRoute(trainId: String, speed: Int? = null) = if (speed != null) "train-info/$trainId?speed=$speed" else "train-info/$trainId"
    }
    object Search : Screen("search", "Rechercher")
    object Alerts : Screen("alerts", "Afficher les alertes")
    object TicketResult : Screen("ticket-result/{rawData}", "Résultat Scan") {
        fun createRoute(rawData: String): String {
            val encoded = java.net.URLEncoder.encode(rawData, "UTF-8")
            return "ticket-result/$encoded"
        }
    }
}
