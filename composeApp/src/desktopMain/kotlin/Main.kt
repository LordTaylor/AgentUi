import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.agentcore.App
import com.agentcore.di.appModule
import org.koin.core.context.startKoin
import com.agentcore.shared.CoreLauncher

fun main() = application {
    // Startup Core
    CoreLauncher.ensureRunning()

    // Initialize Koin
    startKoin {
        modules(appModule)
    }

    Window(onCloseRequest = ::exitApplication, title = "Agent Core UI") {
        App()
    }
}
