package com.haumealabs.haumea.models

import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Add these data classes to your models package
@Serializable
data class AddEventRequest(
    val name: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val params: Map<String, String> = emptyMap()
)

@Serializable
data class AddLogRequest(
    @SerialName("severity")
    val severity: String,
    @SerialName("message")
    val message: String,
    @SerialName("timestamp")
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class BaseResponse(
    val success: Boolean,
    val message: String? = null
)