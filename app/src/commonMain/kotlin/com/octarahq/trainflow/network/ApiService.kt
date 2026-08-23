package com.octarahq.trainflow.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class NetworkStatus(
    val stats: NetworkStatusStats = NetworkStatusStats(),
    val delayedTrains: List<DelayedTrain> = emptyList()
)

@Serializable
data class NetworkStatusStats(
    val delays: Int = 0,
    val inTransit: Int = 0,
    val incidents: Int = 0,
    val lastUpdated: String = "",
    val punctuality: Int = 0,
    val total: Int = 0,
    val trainCounts: Map<String, Int>? = null
)

@Serializable
data class DelayedTrain(
    val number: String = "",
    val type: String = "",
    val origin: String = "",
    val destination: String = "",
    val delayMinutes: Int = 0,
    val delay: String = ""
)

@Serializable
data class SingleVehicleResponse(
    val vehicle: InterpolatedJourney? = null
)

@Serializable
data class LiveVehiclesResponse(
    val count: Int = 0,
    val vehicles: List<InterpolatedJourney> = emptyList()
)

@Serializable
data class InterpolatedJourney(
    val journey: TrainJourney = TrainJourney(),
    val status: String = "",
    val delayMinutes: Int? = null,
    val lastStopId: String = "",
    val nextStopId: String = "",
    val ratio: Double = 0.0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val speed: Int? = null
)

@Serializable
data class FramedVehicleJourneyRef(
    val DatedVehicleJourneyRef: String = ""
)

@Serializable
data class TrainNumbers(
    val TrainNumberRef: String = ""
)

@Serializable
data class RecordedCall(
    val StopPointRef: String = "",
    val StopPointName: String = "",
    val AimedArrivalTime: String? = null,
    val ExpectedArrivalTime: String? = null,
    val ArrivalPlatformName: String? = null,
    val AimedDepartureTime: String? = null,
    val ExpectedDepartureTime: String? = null,
    val DeparturePlatformName: String? = null
)

@Serializable
data class RecordedCalls(
    val RecordedCall: List<RecordedCall>? = null
)

@Serializable
data class EstimatedCall(
    val StopPointRef: String = "",
    val StopPointName: String = "",
    val AimedArrivalTime: String? = null,
    val ExpectedArrivalTime: String? = null,
    val ArrivalPlatformName: String? = null,
    val AimedDepartureTime: String? = null,
    val ExpectedDepartureTime: String? = null
)

@Serializable
data class EstimatedCalls(
    val EstimatedCall: List<EstimatedCall>? = null
)

@Serializable
data class TrainJourney(
    val PublishedLineName: String = "",
    val ProductCategoryRef: String = "",
    val VehicleMode: String = "",
    val VehicleJourneyRef: String = "",
    val OriginName: String = "",
    val DestinationName: String = "",
    val FramedVehicleJourneyRef: FramedVehicleJourneyRef? = null,
    val TrainNumbers: TrainNumbers? = null,
    val RecordedCalls: RecordedCalls? = null,
    val EstimatedCalls: EstimatedCalls? = null
)

@Serializable
data class SearchResponse(
    val gares: List<GareSearchResult>? = null,
    val trains: List<TrainSearchResult>? = null
)

@Serializable
data class GareSearchResult(
    val obj: GareObj = GareObj(),
    val distance: Int? = null
)

@Serializable
data class GareObj(
    val name: String = "",
    val uic: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
)

@Serializable
data class TrainSearchResult(
    val obj: TrainObj = TrainObj(),
    val distance: Int? = null
)

@Serializable
data class TrainObj(
    val name: String = "",
    val type: String = "",
    val delayMinutes: Int = 0,
    val origin: String = "",
    val destination: String = "",
    val passingByUIC: String? = null
)

class ApiService(private val client: HttpClient) {
    val baseUrl = "https://apitrainflow.orionhost.app"

    suspend fun getNetworkStatus(
        page: Int? = null,
        pageSize: Int? = null,
        showDelaysTrains: Int? = null
    ): NetworkStatus {
        return client.get("$baseUrl/network/status") {
            page?.let { parameter("page", it) }
            pageSize?.let { parameter("pageSize", it) }
            showDelaysTrains?.let { parameter("showDelaysTrains", it) }
        }.body()
    }

    suspend fun search(query: String, result: String): SearchResponse {
        return client.get("$baseUrl/search") {
            parameter("q", query)
            parameter("result", result)
        }.body()
    }

    suspend fun getLiveVehicles(): LiveVehiclesResponse {
        return client.get("$baseUrl/live/vehicles").body()
    }

    suspend fun getSingleVehicle(id: String): SingleVehicleResponse {
        return client.get("$baseUrl/live/vehicle/$id").body()
    }
}

val apiService = ApiService(KtorClient)
