package com.haumealabs.haumea.models

import kotlinx.serialization.Serializable

/**
 * Supabase Edge Function response for remote flags.
 */
@Serializable
data class RemoteFlag(
    val key: String,
    val value: String
)

@Serializable
data class RemoteFlagsResponse(
    val flags: List<RemoteFlag> = emptyList()
)
