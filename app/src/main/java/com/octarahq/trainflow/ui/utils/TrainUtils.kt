package com.octarahq.trainflow.ui.utils

import androidx.compose.ui.graphics.Color

data class TrainCategoryDisplay(val label: String, val color: Color)

fun getTrainCategoryDisplay(key: String): TrainCategoryDisplay {
    val activeGreen = Color(0xFF4ADE80)
    val delayAmber = Color(0xFFFBBF24)
    val blue = Color(0xFF3B82F6)
    val teal = Color(0xFF0D9488)
    val pink = Color(0xFFDB2777)
    val purple = Color(0xFF7C3AED)
    val textSecondary = Color(0xFF94A3B8)
    
    return when (key.lowercase()) {
        "fr:typeofproductcategory::highspeedrail::", "tgv" -> TrainCategoryDisplay("TGV", purple)
        "fr:typeofproductcategory::regionalrail::", "ter" -> TrainCategoryDisplay("TER", teal)
        "fr:typeofproductcategory::interregionalrail::", "intercités" -> TrainCategoryDisplay("Intercités", blue)
        "fr:typeofproductcategory::suburbanrailway::", "rer" -> TrainCategoryDisplay("RER", pink)
        "fr:typeofproductcategory::local::", "transilien" -> TrainCategoryDisplay("Transilien", activeGreen)
        "fr:typeofproductcategory::tramtrain::", "tram" -> TrainCategoryDisplay("Tram-Train", delayAmber)
        "fr:typeofproductcategory::longdistance::" -> TrainCategoryDisplay("Grandes Lignes", Color(0xFF6366F1))
        "fr:typeofproductcategory::crosscountryrail::" -> TrainCategoryDisplay("Transversal", Color(0xFFF59E0B))
        "fr:typeofproductcategory::railshuttle::", "navette" -> TrainCategoryDisplay("Navette", Color(0xFF14B8A6))
        else -> {
            val shortName = key.removePrefix("fr:typeofproductcategory::").removeSuffix("::").replaceFirstChar { it.uppercase() }
            TrainCategoryDisplay(shortName.ifEmpty { "Train" }, textSecondary)
        }
    }
}
