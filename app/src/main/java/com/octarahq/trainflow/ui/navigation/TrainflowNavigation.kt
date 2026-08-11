package com.octarahq.trainflow.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home?select={select}&lat={lat}&lon={lon}", "Accueil") {
        fun createRoute(selectTrainId: String? = null, lat: Double? = null, lon: Double? = null): String {
            val params = mutableListOf<String>()
            if (selectTrainId != null) params.add("select=$selectTrainId")
            if (lat != null && lon != null) {
                params.add("lat=$lat")
                params.add("lon=$lon")
            }
            return if (params.isNotEmpty()) "home?${params.joinToString("&")}" else "home"
        }
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
    object StationInfo : Screen("station-info/{stationName}?uic={uic}&lat={lat}&lon={lon}", "Gare Info") {
        fun createRoute(stationName: String, uic: String? = null, lat: Double? = null, lon: Double? = null): String {
            val encodedName = java.net.URLEncoder.encode(stationName, "UTF-8")
            val params = mutableListOf<String>()
            if (!uic.isNullOrEmpty()) params.add("uic=${java.net.URLEncoder.encode(uic, "UTF-8")}")
            if (lat != null && lon != null) {
                params.add("lat=$lat")
                params.add("lon=$lon")
            }
            return if (params.isNotEmpty()) "station-info/$encodedName?${params.joinToString("&")}" else "station-info/$encodedName"
        }
    }
}
