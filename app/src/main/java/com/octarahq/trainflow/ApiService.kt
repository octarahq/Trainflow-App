package com.octarahq.trainflow

import retrofit2.http.Query
import retrofit2.http.GET
data class NetworkStatus(
    val stats: NetworkStatusStats,
    val delayedTrains: List<DelayedTrain>
)

data class NetworkStatusStats(
    val delays: Int,
    val inTransit: Int,
    val incidents: Int,
    val lastUpdated: String,
    val punctuality: Int,
    val total: Int,
    val trainCounts: Map<String, Int>? = null
)

data class DelayedTrain(
    val number: String,
    val type: String,
    val origin: String,
    val destination: String,
    val delayMinutes: Int,
    val delay: String
)

interface ApiService {
    @GET("/network/status")
    suspend fun getNetworkStatus(
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("showDelaysTrains") showDelaysTrains: Int? = null
    ): NetworkStatus

    @GET("/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("result") result: String
    ): SearchResponse
    @GET("/live/vehicles")
    suspend fun getLiveVehicles(): LiveVehiclesResponse

    @retrofit2.http.GET("/live/vehicle/{id}")
    suspend fun getSingleVehicle(@retrofit2.http.Path("id") id: String): SingleVehicleResponse
}

data class SingleVehicleResponse(
    val vehicle: InterpolatedJourney
)

data class LiveVehiclesResponse(
    val count: Int,
    val vehicles: List<InterpolatedJourney>
)

data class InterpolatedJourney(
    val journey: TrainJourney,
    val status: String,
    val delayMinutes: Int? = null,
    val lastStopId: String,
    val nextStopId: String,
    val ratio: Double,
    val lat: Double,
    val lon: Double,
    val speed: Int? = null
)

data class FramedVehicleJourneyRef(
    val DatedVehicleJourneyRef: String
)

data class TrainNumbers(
    val TrainNumberRef: String
)

data class RecordedCall(
    val StopPointRef: String,
    val StopPointName: String,
    val AimedArrivalTime: String?,
    val ExpectedArrivalTime: String?,
    val ArrivalPlatformName: String?,
    val AimedDepartureTime: String?,
    val ExpectedDepartureTime: String?,
    val DeparturePlatformName: String?
)

data class RecordedCalls(
    val RecordedCall: List<RecordedCall>?
)

data class EstimatedCall(
    val StopPointRef: String,
    val StopPointName: String,
    val AimedArrivalTime: String?,
    val ExpectedArrivalTime: String?,
    val ArrivalPlatformName: String?,
    val AimedDepartureTime: String?,
    val ExpectedDepartureTime: String?
)

data class EstimatedCalls(
    val EstimatedCall: List<EstimatedCall>?
)

data class TrainJourney(
    val PublishedLineName: String,
    val ProductCategoryRef: String,
    val VehicleMode: String,
    val VehicleJourneyRef: String,
    val OriginName: String,
    val DestinationName: String,
    val FramedVehicleJourneyRef: FramedVehicleJourneyRef?,
    val TrainNumbers: TrainNumbers?,
    val RecordedCalls: RecordedCalls?,
    val EstimatedCalls: EstimatedCalls?
)

data class SearchResponse(
    val gares: List<GareSearchResult>? = null,
    val trains: List<TrainSearchResult>? = null
)

data class GareSearchResult(
    val obj: GareObj,
    val distance: Int
)

data class GareObj(
    val name: String,
    val uic: String? = null
)

data class TrainSearchResult(
    val obj: TrainObj,
    val distance: Int
)

data class TrainObj(
    val name: String,
    val type: String,
    val delayMinutes: Int,
    val origin: String,
    val destination: String,
    val passingByUIC: String? = null
)