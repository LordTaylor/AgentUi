package com.agentcore

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.agentcore.App
import com.agentcore.di.appModule
import com.agentcore.shared.ConnectionMode
import com.agentcore.shared.CoreLauncher
import org.koin.core.context.startKoin
import java.awt.Toolkit

fun main() {
    // Suppress known Compose Desktop bug: "ActiveParent with no focused child"
    // Triggered by clickable modifier in mouse input mode when composable is removed mid-gesture.
    // Safe to ignore — does not corrupt state.
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        if (throwable is IllegalArgumentException && throwable.message?.contains("ActiveParent") == true) {
            // intentionally suppressed
        } else {
            defaultHandler?.uncaughtException(thread, throwable)
                ?: throwable.printStackTrace()
        }
    }

    application {
        // Detect if agent-core is already running
        val autoMode: ConnectionMode = CoreLauncher.detectRunningMode() ?: ConnectionMode.STDIO

        startKoin {
            modules(appModule)
        }

        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val windowState = rememberWindowState(
            width = (screenSize.width * 0.5).dp,
            height = (screenSize.height * 0.8).dp,
            position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Agent Core UI",
            state = windowState
        ) {
            window.minimumSize = java.awt.Dimension(640, 480)
            App(autoMode = autoMode)
        }
    }
}
