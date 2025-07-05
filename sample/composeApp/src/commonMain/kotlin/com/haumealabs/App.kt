package com.haumealabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haumealabs.haumea.HaumeaClient
import kotlinx.coroutines.launch

@Composable
fun App() {
    val haumeaClient = remember {
        HaumeaClient(
            apiKey = "hml_4k9x2p8n7q6w5e3r1t0y9u8i7o6p5a4s3d2f1g0h",
            appId = "com.haumealabs.haumeasample"
        )
    }

    LaunchedEffect(Unit) {
        haumeaClient.userId = "user123"
        haumeaClient.addLog("debug", "App started")
    }
    
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var flags by remember { mutableStateOf<Map<String, String>?>(null) }
    var rawResponse by remember { mutableStateOf<String?>(null) }
    
    fun fetchConfig() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            rawResponse = null
            
            haumeaClient.fetchConfig()
                .onSuccess { config ->
                    haumeaClient.addLog("debug", "Successfully fetched config: $config")
                    flags = config
                    errorMessage = null
                }
                .onFailure { error ->
                    haumeaClient.addLog("error", "Failed to fetch config: ${error.message}")
                    errorMessage = error.message
                    flags = null
                    rawResponse = error.cause?.message
                        ?.substringAfter("Raw response: ")
                        ?.substringBefore("\n")
                }
                .also {
                    isLoading = false
                }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            haumeaClient.close()
        }
    }
    
    LaunchedEffect(Unit) {
        fetchConfig()
    }
    
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Haumea Remote Config",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        haumeaClient.addEvent(
                            eventName = "button_click",
                            params = mapOf("button_name" to "submit", "screen" to "home")
                        )
                        fetchConfig()
                              },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Refresh Config")
                    }
                }
                
                if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Error message
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage ?: "An unknown error occurred",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                rawResponse?.let { response ->
                                    if (response.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Response:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = response,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                    }
                } else {
                    flags?.let { flagsMap ->
                        if (flagsMap.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Remote Configuration",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${flagsMap.size} flag${if (flagsMap.size != 1) "s" else ""} loaded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    LazyColumn(
                                        modifier = Modifier.padding(top = 16.dp)
                                    ) {
                                        items(flagsMap.entries.toList().sortedBy { it.key }) { (key, value) ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = value,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (key != flagsMap.keys.last()) {
                                                    Divider(
                                                        modifier = Modifier.padding(vertical = 8.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No configuration flags found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } ?: run {
                        if (!isLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Tap the button to load configuration",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                // Keep the original Fibonacci example as a fallback
                if (flags == null && errorMessage == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Text("Sample App", style = MaterialTheme.typography.headlineSmall)
                        Text("Tap the button to load configuration", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
