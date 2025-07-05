package com.haumealabs.haumea

import com.haumealabs.haumea.models.AddEventRequest
import com.haumealabs.haumea.models.AddLogRequest
import com.haumealabs.haumea.models.BaseResponse
import com.haumealabs.haumea.models.RemoteResponse
import com.haumealabs.haumea.platform.getPlatformType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Main client for interacting with the Haumea Labs Remote Config API.
 *
 * @property apiKey The API key for authentication
 * @property appId The unique identifier for your application
 * @property platform The platform the app is running on (e.g., "android", "ios")
 * @property baseUrl The base URL of the Haumea Labs API (defaults to production)
 */
class HaumeaClient internal constructor(
    private val apiKey: String,
    private val appId: String,
    /**
     * The platform this client is configured for ("android" or "ios")
     */
    val platform: String,
    private val baseUrl: String,
    private val client: HttpClient = createHttpClient(apiKey, appId, platform),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    /**
     * Creates a new HaumeaClient with automatic platform detection.
     *
     * @param apiKey The API key for authentication
     * @param appId The unique identifier for your application
     * @param baseUrl The base URL of the Haumea Labs API (defaults to production)
     */
    constructor(
        apiKey: String,
        appId: String,
        baseUrl: String = "https://api.haumealabs.com"
    ) : this(
        apiKey = apiKey,
        appId = appId,
        platform = getPlatformType().name.lowercase().lowercase().takeIf { it in setOf("android", "ios") }
            ?: throw IllegalArgumentException("Platform must be either 'android' or 'ios'"),
        baseUrl = baseUrl
    )

    private val _configState = MutableStateFlow<Map<String, String>?>(null)


    private fun generateRandomString(length: Int = 16): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    var userId: String = generateRandomString()
    
    /**
     * A [StateFlow] that emits the current remote flags.
     * Will be null until [fetchConfig] is called successfully.
     */
    val configState: StateFlow<Map<String, String>?> = _configState.asStateFlow()

    /**
     * Fetches the remote configuration from the Haumea Labs API.
     * Updates the [configState] with the received configuration.
     *
     * @return A Result containing either the remote flags or an error message
     */
    /**
     * Fetches the remote configuration from the Haumea Labs API.
     * @return A Result containing either the remote flags or an error message
     */
    suspend fun fetchConfig(): Result<Map<String, String>> {
        return try {
            val response = client.get("$baseUrl/remote-config") {
                defaultHeaders()
            }
            
            val responseString = response.body<String>()
            println("Raw response ($response): $responseString")
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val remoteResponse = RemoteResponse.fromJsonString(responseString)
                    when (remoteResponse) {
                        is RemoteResponse.Success -> {
                            val flags = remoteResponse.remoteFlags.ifEmpty {
                                remoteResponse.data ?: emptyMap()
                            }
                            _configState.value = flags
                            Result.success(flags)
                        }
                        is RemoteResponse.Error -> {
                            val errorMsg = buildString {
                                append(remoteResponse.error ?: "Unknown error")
                                remoteResponse.message?.let { append(": $it") }
                                remoteResponse.appId?.let { append(" (app_id: $it)") }
                                remoteResponse.platform?.let { append(", platform: $it") }
                            }
                            Result.failure(Exception(errorMsg))
                        }
                    }
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Invalid or missing API key"))
                }
                HttpStatusCode.NotFound -> {
                    Result.failure(Exception("Application not found (app_id: $appId, platform: $platform)"))
                }
                HttpStatusCode.BadRequest -> {
                    Result.failure(Exception("Bad request: ${response.status.description}"))
                }
                else -> {
                    Result.failure(Exception("Server error: ${response.status.description}"))
                }
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is kotlinx.serialization.SerializationException -> "Failed to parse server response: ${e.message}"
                is ClientRequestException -> "Request failed: ${e.response.status.description}"
                is ServerResponseException -> "Server error: ${e.message}"
                else -> "Failed to fetch remote config: ${e.message}"
            }
            println("Error: $errorMsg")
            Result.failure(Exception(errorMsg, e))
        }
    }

    /**
     * Fetches the remote configuration asynchronously and updates the state.
     */
    suspend fun fetchConfigAsync() {
        coroutineScope.launch {
            fetchConfig().onSuccess { flags ->
                // State is already updated in fetchConfig()
                println("Successfully updated ${flags.size} flags")
            }.onFailure { error ->
                _configState.value = null
                println("Failed to fetch config: ${error.message}")
            }
        }
    }

    fun addEvent(
        eventName: String,
        params: Map<String, String> = emptyMap(),
        onSuccess: (BaseResponse) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        coroutineScope.launch {
            try {
                val request = AddEventRequest(
                    name = eventName,
                    params = params
                )

                val response = client.post("$baseUrl/addEvent") {
                    userIdHeaders(userId!!)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<BaseResponse>()

                onSuccess(response)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun addLog(
        severity: String,
        message: String,
        onSuccess: (BaseResponse) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        require(severity in listOf("debug", "info", "warn", "error")) {
            "Invalid severity level. Must be one of: debug, info, warn, error"
        }

        coroutineScope.launch {
            try {
                val request = AddLogRequest(
                    severity = severity,
                    message = message
                )

                val response = client.post("$baseUrl/addLog") {
                    userIdHeaders(userId!!)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<BaseResponse>()

                onSuccess(response)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }


    private fun HttpRequestBuilder.defaultHeaders() {
        headers {
            append("x-api-key", apiKey)
            append("app-id", appId)
            append("platform", platform)
        }
    }

    private fun HttpRequestBuilder.userIdHeaders(userId: String) {
        headers {
            append("x-api-key", apiKey)
            append("app-id", appId)
            append("platform", platform)
            append("userid", userId)
        }
    }

    /**
     * Cleans up resources used by the client.
     * Should be called when the client is no longer needed.
     */
    fun close() {
        coroutineScope.cancel()
        client.close()
    }
}
