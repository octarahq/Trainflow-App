package com.octarahq.trainflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.datetime.Clock
import com.octarahq.trainflow.ui.utils.SavedJourney
import com.octarahq.trainflow.ui.utils.getSavedJourneys
import com.octarahq.trainflow.ui.utils.deleteSavedJourney
import com.octarahq.trainflow.ui.utils.getTrainCategoryDisplay
import com.octarahq.trainflow.ui.utils.saveJourney
import com.octarahq.trainflow.ui.utils.showToast
import com.octarahq.trainflow.ui.utils.currentPlatformContext
import com.octarahq.trainflow.ui.utils.TicketParser

private object TripsPalette {
    val background = Color(0xFF0F1115)
    val panel = Color(0xFF1D2026)
    val surface = Color(0xFF262B35)
    val border = Color(0xFF2A303C)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val activeGreen = Color(0xFF4ADE80)
    val delayAmber = Color(0xFFFBBF24)
    val blue = Color(0xFF3B82F6)
    val teal = Color(0xFF0D9488)
    val purple = Color(0xFF7C3AED)
}

class TripsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = com.octarahq.trainflow.ui.utils.currentPlatformContext
        var journeysList by remember { mutableStateOf<List<SavedJourney>>(getSavedJourneys()) }
        var showAddDialog by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TripsPalette.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TripsTopBar(
                    onBack = { navigator.pop() },
                    onSearch = { navigator.push(SearchScreen()) }
                )

                if (journeysList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun voyage actif.\nAppuyez sur le bouton + pour en ajouter un.",
                            color = TripsPalette.textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(journeysList.size) { index ->
                            val item = journeysList[index]
                            TripCard(
                                journey = item,
                                onDelete = {
                                    deleteSavedJourney(item.pnr, item.trainNumber)
                                    journeysList = getSavedJourneys()
                                    context.showToast("Voyage supprimé")
                                },
                                onClick = {
                                    val targetId = item.vehicleJourneyRef?.ifBlank { null } ?: item.trainNumber
                                    navigator.push(TrainInfoScreen(trainId = targetId))
                                }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TripsPalette.teal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .padding(bottom = 80.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter un voyage",
                    modifier = Modifier.size(28.dp)
                )
            }

            if (showAddDialog) {
                AddTripDialog(
                    onDismiss = { showAddDialog = false },
                    onJourneyAdded = {
                        journeysList = getSavedJourneys()
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddTripDialog(
    onDismiss: () -> Unit,
    onJourneyAdded: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = com.octarahq.trainflow.ui.utils.currentPlatformContext

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TripsPalette.panel,
            border = BorderStroke(1.dp, TripsPalette.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ajouter un trajet",
                    color = TripsPalette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = TripsPalette.textPrimary,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = TripsPalette.teal
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Code / Billet", fontSize = 14.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manuel", fontSize = 14.sp) }
                    )
                }

                if (selectedTab == 0) {
                    TicketImportTab(onJourneyAdded = onJourneyAdded)
                } else {
                    ManualTripTab(onJourneyAdded = onJourneyAdded)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = TripsPalette.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketImportTab(onJourneyAdded: () -> Unit) {
    var rawText by remember { mutableStateOf("") }
    val context = com.octarahq.trainflow.ui.utils.currentPlatformContext

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Collez les détails bruts du billet ou le texte de scan :",
            color = TripsPalette.textSecondary,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            placeholder = { Text("ocr_parsed:...", color = TripsPalette.textSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TripsPalette.surface,
                unfocusedContainerColor = TripsPalette.surface,
                focusedIndicatorColor = TripsPalette.teal,
                unfocusedIndicatorColor = TripsPalette.border,
                focusedTextColor = TripsPalette.textPrimary,
                unfocusedTextColor = TripsPalette.textPrimary
            )
        )

        Button(
            onClick = {
                val parsed = TicketParser.parse(rawText)
                if (parsed != null) {
                    val saved = SavedJourney(
                        pnr = parsed.pnr,
                        passengerName = parsed.passengerName,
                        departureStationCode = parsed.departureStationCode,
                        arrivalStationCode = parsed.arrivalStationCode,
                        trainNumber = parsed.trainNumber,
                        travelDate = parsed.travelDate,
                        departureTime = parsed.departureTime,
                        arrivalTime = parsed.arrivalTime,
                        addedAt = Clock.System.now().toEpochMilliseconds()
                    )
                    saveJourney(saved)
                    context.showToast("Billet importé avec succès !")
                    onJourneyAdded()
                } else {
                    context.showToast("Format de billet invalide")
                }
            },
            enabled = rawText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TripsPalette.teal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Importer", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Billet de test (Simulateurs) :", color = TripsPalette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    rawText = "ocr_parsed:Paris Gare de Lyon|Lyon Part Dieu|30/12/2026|6613|JEAN DUPONT|PNR8Y8|09:12|11:08"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TripsPalette.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("TGV Paris-Lyon", fontSize = 11.sp, color = TripsPalette.textPrimary)
            }
            Button(
                onClick = {
                    rawText = "ocr_parsed:Paris Montparnasse|Bordeaux St-Jean|31/12/2026|8513|JEAN DUPONT|PNR4B4|08:45|10:55"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TripsPalette.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("TGV Bordeaux", fontSize = 11.sp, color = TripsPalette.textPrimary)
            }
        }
    }
}

@Composable
private fun ManualTripTab(onJourneyAdded: () -> Unit) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val context = com.octarahq.trainflow.ui.utils.currentPlatformContext

    var trainNum by remember { mutableStateOf("") }
    var departure by remember { mutableStateOf("") }
    var arrival by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var depTime by remember { mutableStateOf("") }
    var arrTime by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var pnr by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = trainNum,
            onValueChange = { trainNum = it },
            label = { Text("Numéro de Train *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TripsPalette.surface,
                unfocusedContainerColor = TripsPalette.surface,
                focusedIndicatorColor = TripsPalette.teal,
                unfocusedTextColor = TripsPalette.textPrimary,
                focusedTextColor = TripsPalette.textPrimary
            )
        )
        OutlinedTextField(
            value = departure,
            onValueChange = { departure = it },
            label = { Text("Gare de Départ *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TripsPalette.surface,
                unfocusedContainerColor = TripsPalette.surface,
                focusedIndicatorColor = TripsPalette.teal,
                unfocusedTextColor = TripsPalette.textPrimary,
                focusedTextColor = TripsPalette.textPrimary
            )
        )
        OutlinedTextField(
            value = arrival,
            onValueChange = { arrival = it },
            label = { Text("Gare d'Arrivée *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TripsPalette.surface,
                unfocusedContainerColor = TripsPalette.surface,
                focusedIndicatorColor = TripsPalette.teal,
                unfocusedTextColor = TripsPalette.textPrimary,
                focusedTextColor = TripsPalette.textPrimary
            )
        )
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (JJ/MM/AAAA) *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TripsPalette.surface,
                unfocusedContainerColor = TripsPalette.surface,
                focusedIndicatorColor = TripsPalette.teal,
                unfocusedTextColor = TripsPalette.textPrimary,
                focusedTextColor = TripsPalette.textPrimary
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = depTime,
                onValueChange = { depTime = it },
                label = { Text("Départ (HH:MM)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = TripsPalette.surface,
                    unfocusedContainerColor = TripsPalette.surface,
                    focusedIndicatorColor = TripsPalette.teal,
                    unfocusedTextColor = TripsPalette.textPrimary,
                    focusedTextColor = TripsPalette.textPrimary
                )
            )
            OutlinedTextField(
                value = arrTime,
                onValueChange = { arrTime = it },
                label = { Text("Arrivée (HH:MM)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = TripsPalette.surface,
                    unfocusedContainerColor = TripsPalette.surface,
                    focusedIndicatorColor = TripsPalette.teal,
                    unfocusedTextColor = TripsPalette.textPrimary,
                    focusedTextColor = TripsPalette.textPrimary
                )
            )
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom du Passager") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TripsPalette.surface,
                unfocusedContainerColor = TripsPalette.surface,
                focusedIndicatorColor = TripsPalette.teal,
                unfocusedTextColor = TripsPalette.textPrimary,
                focusedTextColor = TripsPalette.textPrimary
            )
        )
        OutlinedTextField(
            value = pnr,
            onValueChange = { pnr = it },
            label = { Text("PNR / Référence") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TripsPalette.surface,
                unfocusedContainerColor = TripsPalette.surface,
                focusedIndicatorColor = TripsPalette.teal,
                unfocusedTextColor = TripsPalette.textPrimary,
                focusedTextColor = TripsPalette.textPrimary
            )
        )

        Button(
            onClick = {
                val now = Clock.System.now().toEpochMilliseconds()
                val saved = SavedJourney(
                    pnr = pnr.ifBlank { "MANUAL_${now}" },
                    passengerName = name,
                    departureStationCode = departure,
                    arrivalStationCode = arrival,
                    trainNumber = trainNum,
                    travelDate = date,
                    departureTime = depTime.ifBlank { "--:--" },
                    arrivalTime = arrTime.ifBlank { "--:--" },
                    addedAt = now
                )
                saveJourney(saved)
                context.showToast("Trajet enregistré !")
                onJourneyAdded()
            },
            enabled = trainNum.isNotBlank() && departure.isNotBlank() && arrival.isNotBlank() && date.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TripsPalette.teal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Enregistrer", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TripsTopBar(onBack: () -> Unit, onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = TripsPalette.textPrimary
            )
        }
        Text(
            text = "Mes Trajets",
            color = TripsPalette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
        IconButton(onClick = onSearch, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Rechercher",
                tint = TripsPalette.textPrimary
            )
        }
    }
}

@Composable
private fun TripCard(
    journey: SavedJourney,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = TripsPalette.panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TripsPalette.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = journey.travelDate,
                        color = TripsPalette.textSecondary,
                        fontSize = 13.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val catRef = journey.categoryRef ?: ""
                    val categoryDisplay = getTrainCategoryDisplay(catRef.ifEmpty { "train" })
                    Surface(color = categoryDisplay.color, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = categoryDisplay.label.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "n°${journey.trainNumber}",
                        color = TripsPalette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(50.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = journey.departureTime, color = TripsPalette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = journey.arrivalTime, color = TripsPalette.textSecondary, fontSize = 14.sp)
                }

                TimelineRail(active = true)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = journey.departureStationCode,
                        color = TripsPalette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = journey.arrivalStationCode,
                        color = TripsPalette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRail(active: Boolean) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(TripsPalette.blue, CircleShape)
                    .border(2.dp, Color(0xFF0F1115), CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
                    .background(if (active) TripsPalette.blue else TripsPalette.border)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(2.dp, Color(0xFF94A3B8), CircleShape)
            )
        }
    }
}
