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
}

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