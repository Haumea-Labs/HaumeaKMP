import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import com.haumealabs.App

fun MainViewController(): UIViewController = ComposeUIViewController { App() }