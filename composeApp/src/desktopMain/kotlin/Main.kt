import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.agentcore.shared.CoreLauncher
import com.agentcore.api.AgentClient
import com.agentcore.App

fun main() = application {
    // Startup Core
    CoreLauncher.ensureRunning()

    Window(onCloseRequest = ::exitApplication, title = "Agent Core UI") {
        App()
    }
}
