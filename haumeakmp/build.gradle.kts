plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.serialization)
    id("com.vanniktech.maven.publish") version "0.33.0"
    id("signing")
}

group = "com.haumealabs"
version = "1.0.1"

kotlin {
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            
            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.android)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }

}

android {
    namespace = "com.haumealabs"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
}

mavenPublishing {

    publishToMavenCentral(true)

    coordinates(group.toString(), "haumea-kmp", version.toString())

    pom {
        name.set("Haumea KMP")
        description.set("Kotlin Multiplatform Library for Haumea System")
        url.set("https://github.com/Haumea-Labs/HaumeaKMP")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("jsoriase")
                name.set("jsoriase")
                email.set("haumealabs@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/Haumea-Labs/HaumeaKMP.git")
            developerConnection.set("scm:git:ssh://git@github.com:haumealabs/HaumeaKMP.git")
            url.set("https://https://github.com/Haumea-Labs/HaumeaKMP")
        }
    }


    signAllPublications()
}
