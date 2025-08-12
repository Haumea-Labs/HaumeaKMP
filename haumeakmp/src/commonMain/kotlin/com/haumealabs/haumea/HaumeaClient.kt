package com.haumealabs.haumea

import com.haumealabs.haumea.models.BaseResponse
import com.haumealabs.haumea.models.EventItem
import com.haumealabs.haumea.models.LogItem
import com.haumealabs.haumea.models.RemoteFlagsResponse
import com.haumealabs.haumea.models.SendEventsRequest
import com.haumealabs.haumea.models.SendLogsRequest
import com.haumealabs.haumea.platform.getPlatformType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.statement.*
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
     * @property baseUrl The base URL of the Haumea Labs API (defaults to Supabase Edge Functions)
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
        baseUrl: String = "https://tgkcqvkyxpephsbhrkqm.functions.supabase.co"
    ) : this(
        apiKey = apiKey,
        appId = appId,
        platform = getPlatformType().name.lowercase().takeIf { it in setOf("android", "ios") }
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
    suspend fun fetchConfig(): Result<Map<String, String>> {
        return try {
            val response = client.get("$baseUrl/sdk-flags") {
                header("x-api-key", apiKey)
                accept(ContentType.Application.Json)
            }

            if (response.status == HttpStatusCode.OK) {
                val body = response.body<RemoteFlagsResponse>()
                val flags = body.flags.associate { it.key to it.value }
                _configState.value = flags
                Result.success(flags)
            } else {
                val text = response.bodyAsText()
                Result.failure(Exception("${response.status.value} ${response.status.description}: $text"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch remote config: ${e.message}", e))
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
                val payload = SendEventsRequest(
                    apiKey = apiKey,
                    events = listOf(
                        EventItem(name = eventName, properties = if (params.isEmpty()) null else params)
                    )
                )

                val response = client.post("$baseUrl/sdk-events") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

                if (response.status == HttpStatusCode.OK) {
                    val text = response.bodyAsText()
                    onSuccess(BaseResponse(success = true, message = text))
                } else {
                    val text = response.bodyAsText()
                    onError(Exception("${response.status.value} ${response.status.description}: $text"))
                }
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
        coroutineScope.launch {
            try {
                val payload = SendLogsRequest(
                    apiKey = apiKey,
                    logs = listOf(
                        LogItem(level = severity, message = message)
                    )
                )

                val response = client.post("$baseUrl/sdk-logs") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

                if (response.status == HttpStatusCode.OK) {
                    val text = response.bodyAsText()
                    onSuccess(BaseResponse(success = true, message = text))
                } else {
                    val text = response.bodyAsText()
                    onError(Exception("${response.status.value} ${response.status.description}: $text"))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // No default headers; each endpoint has distinct requirements in the new backend.

    /**
     * Cleans up resources used by the client.
     * Should be called when the client is no longer needed.
     */
    fun close() {
        coroutineScope.cancel()
        client.close()
    }
}
