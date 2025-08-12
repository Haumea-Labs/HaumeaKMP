package com.haumealabs.haumea.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Events/Logs payloads for Supabase Edge Functions ingestion API.
 */

@Serializable
data class EventItem(
    val name: String,
    val properties: Map<String, String>? = null,
    @SerialName("created_at") val createdAt: String? = null // ISO 8601, optional
)

@Serializable
data class LogItem(
    val level: String,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null // ISO 8601, optional
)

@Serializable
data class SendEventsRequest(
    val apiKey: String,
    val events: List<EventItem>
)

@Serializable
data class SendLogsRequest(
    val apiKey: String,
    val logs: List<LogItem>
)

@Serializable
data class BaseResponse(
    val success: Boolean,
    val message: String? = null
)