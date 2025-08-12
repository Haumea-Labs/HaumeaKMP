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
            implementation("com.haumealabs:haumea-kmp:1.1.0")
        }
    }
}

// For Android-only projects
implementation("com.haumealabs:haumea-kmp:1.1.0")
```

Note: CocoaPods integration is not required; consume the KMP artifact directly in your shared module.

## Usage

### Initialization

```kotlin
import com.haumealabs.haumea.HaumeaClient

// Initialize the client with required parameters
val haumeaClient = HaumeaClient(
    apiKey = "your-api-key",
    appId = "your-app-id" // required by constructor; not used by the current backend
)

// Optionally set a custom user ID
haumeaClient.userId = "custom-user-id"
```

### Remote Configuration

```kotlin
// Fetch configuration (suspending)
val result = haumeaClient.fetchConfig()
result.onSuccess { config ->
    val featureEnabled = config["feature_flag"]?.toBoolean() ?: false
}.onFailure { error ->
    // Handle error
}

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
    onSuccess = { /* sent */ },
    onError = { /* handle */ }
)
```

### Logging

```kotlin
// Send a log with severity
haumeaClient.addLog(
    severity = "info", // recommended: "debug", "info", "warn", "error"
    message = "User completed onboarding",
    onSuccess = { /* sent */ },
    onError = { /* handle */ }
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
- Invalid API key
- Server errors

## Backend

- Default base URL: `https://tgkcqvkyxpephsbhrkqm.functions.supabase.co`
- Endpoints used by the client:
  - `GET /sdk-flags` (reads remote flags; sends `x-api-key` header)
  - `POST /sdk-events` (batches events; body `{ apiKey, events: [...] }`)
  - `POST /sdk-logs` (batches logs; body `{ apiKey, logs: [...] }`)

## Sample App

The repository includes a sample Compose Multiplatform app demonstrating the library's features. To run it:

1. Open the project in Android Studio
2. For Android: Run the `composeApp` configuration
3. For iOS: Open `sample/iosApp/iosApp.xcodeproj` in Xcode and run

## Requirements

- Kotlin 2.1.0 or higher
- Android API 21+
- iOS 13.0+
