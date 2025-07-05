# Haumea KMP Library

A Kotlin Multiplatform library for interacting with the Haumea Labs Remote Config API.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
// For multiplatform projects
sourceSets {
    commonMain {
        dependencies {
            implementation("com.haumealabs:haumea:1.0.0")
        }
    }
}

// For Android projects
implementation("com.haumealabs:haumea:1.0.0")

// For iOS projects
pod("Haumea", "com.haumealabs:haumea:1.0.0")
```

## Usage

### Basic Usage

```kotlin
import com.haumealabs.haumea.HaumeaClient
import com.haumealabs.haumea.models.RemoteConfig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

// Initialize the client
val haumeaClient = HaumeaClient(
    apiKey = "your-api-key",
    appId = "your-app-id",
    platform = "android" // or "ios", "web", etc.
)

// Fetch config synchronously
runBlocking {
    val config = haumeaClient.fetchConfig()
    println("Fetched config: $config")
}

// Or observe config changes
runBlocking {
    haumeaClient.configState.collect { config ->
        config?.let {
            println("Config updated: $it")
        }
    }
}

// Don't forget to close the client when done
haumeaClient.close()
```

### Error Handling

```kotlin
try {
    val config = haumeaClient.fetchConfig()
    // Handle successful response
} catch (e: Exception) {
    // Handle error (network, parsing, etc.)
    println("Error fetching config: ${e.message}")
}
```

## Sample Apps

- **Android**: Open the project in Android Studio and run the sample app
- **iOS**: Open `sample/iosApp/iosApp.xcodeproj` in Xcode and run the sample app

### Publish to MavenLocal

1) Run `./gradlew :rating:publishToMavenLocal`
2) Open `~/.m2/repository/com/haumealabs/"}`

### Publish to MavenCentral

1) Create a account and a namespace on Sonatype:
   https://central.sonatype.org/register/central-portal/#create-an-account
2) Add developer id, name, email and the project url to
   `/convention-plugins/src/main/kotlin/convention.publication.gradle.kts`
3) Generate a GPG key:
   https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/#generating-a-gpg-key-pair
   ```
   gpg --full-gen-key
   gpg --keyserver keyserver.ubuntu.com --send-keys XXXXXXXX
   gpg --export-secret-key XXXXXXXX > XXXXXXXX.gpg
   ```
4) Add the secrets to `local.properties`:
   ```
   signing.keyId=XXXXXXXX
   signing.password=[key password]
   signing.secretKeyRingFile=../XXXXXXXX.gpg
   ossrhUsername=[Sonatype token_user]
   ossrhPassword=[Sonatype token_password]
   ```
5) Run `./gradlew :haumeakmp:publishAllPublicationsToSonatypeRepository`
