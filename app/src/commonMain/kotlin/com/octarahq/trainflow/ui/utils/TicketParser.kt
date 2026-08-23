package com.octarahq.trainflow.ui.utils

data class ParsedTicket(
    val pnr: String,
    val passengerName: String,
    val departureStationCode: String,
    val arrivalStationCode: String,
    val trainNumber: String,
    val travelDate: String,
    val departureTime: String = "--:--",
    val arrivalTime: String = "--:--",
    val isSecureBinary: Boolean = false,
    val hexDump: String = ""
)

object TicketParser {

    fun parse(raw: String): ParsedTicket? {
        try {
            if (raw.startsWith("ocr_parsed:")) {
                val parts = raw.removePrefix("ocr_parsed:").split("|")
                if (parts.size >= 8) {
                    return ParsedTicket(
                        departureStationCode = parts[0].ifEmpty { "Inconnue" },
                        arrivalStationCode = parts[1].ifEmpty { "Inconnue" },
                        travelDate = parts[2].ifEmpty { "--/--" },
                        trainNumber = parts[3].ifEmpty { "N/A" },
                        passengerName = parts[4].ifEmpty { "Voyageur inconnu" },
                        pnr = parts[5].ifEmpty { "Aucun" },
                        departureTime = parts[6].ifEmpty { "--:--" },
                        arrivalTime = parts[7].ifEmpty { "--:--" },
                        isSecureBinary = false
                    )
                }
            }

            if (raw.startsWith("\u0001UcP") || raw.contains("UcP")) {
                val bytes = raw.encodeToByteArray()
                return ParsedTicket(
                    pnr = "Sécurisé (UIC)",
                    passengerName = "Données chiffrées",
                    departureStationCode = "Billet Virtuel",
                    arrivalStationCode = "SNCF Connect",
                    trainNumber = "Non décodable",
                    travelDate = "--/--",
                    isSecureBinary = true,
                    hexDump = generateHexDump(bytes)
                )
            }

            if (raw.startsWith("i0C") && raw.length > 74) {
                val pnr = raw.substring(4, 10).trim()
                val departure = raw.substring(35, 38).trim()
                val arrival = raw.substring(40, 43).trim()
                val trainNumber = raw.substring(43, 48).trim().trimStart('0')
                val dateStr = raw.substring(48, 53).trim()
                
                val remaining = raw.substring(74)
                val nameRegex = Regex("([A-Z\\-]+)\\s+([A-Z\\-]+)")
                val match = nameRegex.find(remaining)
                
                val passenger = if (match != null) {
                    "${match.groupValues[1]} ${match.groupValues[2]}"
                } else {
                    "Voyageur inconnu"
                }

                return ParsedTicket(
                    pnr = pnr,
                    passengerName = passenger,
                    departureStationCode = mapStationCode(departure),
                    arrivalStationCode = mapStationCode(arrival),
                    trainNumber = trainNumber,
                    travelDate = dateStr
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }

    private fun generateHexDump(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices step 16) {
            val chunk = bytes.slice(i until minOf(i + 16, bytes.size))
            sb.append(i.toString().padStart(3, '0')).append(": ")
            val hexString = chunk.joinToString(" ") { 
                it.toUByte().toString(16).uppercase().padStart(2, '0')
            }
            sb.append(hexString.padEnd(47))
            sb.append("  |")
            for (b in chunk) {
                val c = b.toInt().toChar()
                if (b in 32..126) {
                    sb.append(c)
                } else {
                    sb.append(".")
                }
            }
            sb.append("|\n")
        }
        return sb.toString()
    }
    
    private fun mapStationCode(code: String): String {
        return when (code) {
            "PMO" -> "Paris Montparnasse"
            "LSO" -> "Les Sables d'Olonne"
            "PGA" -> "Paris Gare de Lyon"
            "PNO" -> "Paris Nord"
            "PES" -> "Paris Est"
            "PSL" -> "Paris St-Lazare"
            "PAB" -> "Paris Bercy"
            "LYD" -> "Lyon Part-Dieu"
            "LYP" -> "Lyon Perrache"
            "MRS" -> "Marseille St-Charles"
            "BDX" -> "Bordeaux St-Jean"
            "LIL" -> "Lille Europe"
            "LIF" -> "Lille Flandres"
            "NTE" -> "Nantes"
            "RNS" -> "Rennes"
            "SXB" -> "Strasbourg"
            "TLS" -> "Toulouse Matabiau"
            "FPO" -> "Montpellier St-Roch"
            "FPM" -> "Montpellier Sud de France"
            else -> code
        }
    }
}
