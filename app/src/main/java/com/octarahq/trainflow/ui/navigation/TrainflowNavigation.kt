package com.octarahq.trainflow.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Accueil")
    object Trips : Screen("trips", "Mes trajets")
    object TrainInfo : Screen("train-info", "Train Info")
    object Search : Screen("search", "Rechercher")
    object Alerts : Screen("alerts", "Afficher les alertes")
}
