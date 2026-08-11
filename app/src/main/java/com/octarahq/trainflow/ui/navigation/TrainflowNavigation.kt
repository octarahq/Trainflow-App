package com.octarahq.trainflow.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home?select={select}", "Accueil") {
        fun createRoute(selectTrainId: String? = null) = if (selectTrainId != null) "home?select=$selectTrainId" else "home"
    }
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
    object Update : Screen("update?tag={tag}&notes={notes}&apkUrl={apkUrl}", "Mise à jour") {
        fun createRoute(tag: String, notes: String, apkUrl: String): String {
            val encodedTag = java.net.URLEncoder.encode(tag, "UTF-8")
            val encodedNotes = java.net.URLEncoder.encode(notes, "UTF-8")
            val encodedUrl = java.net.URLEncoder.encode(apkUrl, "UTF-8")
            return "update?tag=$encodedTag&notes=$encodedNotes&apkUrl=$encodedUrl"
        }
    }
}
