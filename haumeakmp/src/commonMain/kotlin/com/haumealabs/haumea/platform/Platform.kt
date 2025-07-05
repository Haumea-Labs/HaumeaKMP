package com.haumealabs.haumea.platform

/**
 * Represents the platform the library is running on.
 */
enum class PlatformType {
    ANDROID,
    IOS,
    UNKNOWN
}

/**
 * Expect declaration for getting the current platform.
 */
internal expect fun getPlatformType(): PlatformType
