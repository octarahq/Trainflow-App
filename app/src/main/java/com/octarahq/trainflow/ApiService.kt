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
    val total: Int
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
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int = 10
    ): NetworkStatus
}