package com.haumealabs.haumea.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed class RemoteResponse {
    @Serializable
    @SerialName("success")
    data class Success(
        @SerialName("remote-flags") 
        val remoteFlags: Map<String, String> = emptyMap(),
        val data: Map<String, String>? = null,
        val message: String? = null
    ) : RemoteResponse()

    @Serializable
    @SerialName("error")
    data class Error(
        val success: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        @SerialName("app_id") val appId: String? = null,
        val platform: String? = null
    ) : RemoteResponse()

    companion object {
        fun fromJsonString(jsonString: String): RemoteResponse {
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            }
            
            // First try to parse as success response
            return try {
                json.decodeFromString(Success.serializer(), jsonString)
            } catch (e: Exception) {
                // If not a success response, try to parse as error
                try {
                    json.decodeFromString(Error.serializer(), jsonString)
                } catch (e2: Exception) {
                    // If all else fails, return a generic error
                    Error(error = "Invalid response format", message = e2.message)
                }
            }
        }
    }
}

@Serializable
data class RemoteConfigRequest(
    val appId: String,
    val platform: String
)
