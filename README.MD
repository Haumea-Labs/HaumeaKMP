# Haumea KMP Library

A Kotlin Multiplatform library for managing remote configuration, event tracking, and logging in your mobile applications.

## Features

- 📱 **Cross-Platform**: Works on Android, iOS, and other Kotlin Multiplatform targets
- ⚡ **Remote Configuration**: Fetch and manage feature flags and remote settings
- 📊 **Event Tracking**: Track user events with custom parameters
- 📝 **Structured Logging**: Send logs with different severity levels
- 🔄 **Reactive Updates**: Observe configuration changes with StateFlow
- 🎯 **Automatic User Identification**: Random user ID generation with override capability

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
// For multiplatform projects
sourceSets {
    commonMain {
        dependencies {
            implementation("com.haumealabs:haumea:1.0.1")
        }
    }
}

// For Android projects
implementation("com.haumealabs:haumea:1.0.1")

// For iOS projects
pod("Haumea", "com.haumealabs:haumea:1.0.1")
```

## Usage

### Initialization

```kotlin
import com.haumealabs.haumea.HaumeaClient

// Initialize the client with required parameters
val haumeaClient = HaumeaClient(
    apiKey = "your-api-key",
    appId = "your-app-id" // e.g., "com.yourcompany.app"
)

// Optionally set a custom user ID
haumeaClient.userId = "custom-user-id"
```

### Remote Configuration

```kotlin
// Fetch configuration
haumeaClient.fetchConfig(
    onSuccess = { config ->
        // Use the configuration
        val featureEnabled = config["feature_flag"]?.toBoolean() ?: false
    },
    onError = { error ->
        // Handle error
    }
)

// Or observe configuration changes
haumeaClient.configState.collect { config ->
    config?.let {
        // React to config changes
    }
}
```

### Event Tracking

```kotlin
// Track an event
haumeaClient.addEvent(
    eventName = "button_click",
    params = mapOf(
        "button_name" to "submit",
        "screen" to "home"
    ),
    onSuccess = { response ->
        // Event sent successfully
    },
    onError = { error ->
        // Handle error
    }
)
```

### Logging

```kotlin
// Send a log with severity
haumeaClient.addLog(
    severity = "info", // "debug", "info", "warn", or "error"
    message = "User completed onboarding",
    onSuccess = { response ->
        // Log sent successfully
    },
    onError = { error ->
        // Handle error
    }
)
```

### Cleanup

```kotlin
// Don't forget to close the client when done
haumeaClient.close()
```

## Error Handling

All network operations are non-blocking and include error callbacks. Common error cases include:

- Network connectivity issues
- Invalid API key or app ID
- Server errors

## Sample App

The repository includes a sample Compose Multiplatform app demonstrating the library's features. To run it:

1. Open the project in Android Studio
2. For Android: Run the `composeApp` configuration
3. For iOS: Open `sample/iosApp/iosApp.xcodeproj` in Xcode and run

## Requirements

- Kotlin 1.9.0 or higher
- Android API 21+
- iOS 13.0+
