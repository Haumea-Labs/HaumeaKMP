package com.haumealabs.haumea

import io.ktor.client.HttpClient

expect fun createHttpClient(
    apiKey: String,
    appId: String,
    platform: String
): HttpClient