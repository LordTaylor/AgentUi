package com.agentcore.ui

import androidx.compose.ui.input.key.*

fun shortenPath(path: String): String {
    val items = path.split("/")
    return if (items.size > 2) ".../${items.takeLast(2).joinToString("/")}" else path
}

fun shortenDisplayPath(path: String): String {
    val home = System.getProperty("user.home") ?: ""
    return if (path.startsWith(home)) "~${path.removePrefix(home)}" else path
}

/** Blocking folder picker dialog — call only from Dispatchers.IO. */
fun pickFolderDialog(currentPath: String): String? {
    return try {
        var result: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            val chooser = javax.swing.JFileChooser(currentPath.ifEmpty { System.getProperty("user.home") }).apply {
                fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Wybierz folder roboczy"
            }
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                result = chooser.selectedFile.absolutePath
            }
            latch.countDown()
        }
        latch.await(60, java.util.concurrent.TimeUnit.SECONDS)
        result
    } catch (_: Exception) { null }
}

/** Blocking image picker dialog — call only from Dispatchers.IO. */
fun pickImageDialog(): String? {
    return try {
        var result: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            val chooser = javax.swing.JFileChooser().apply {
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp", "gif")
                dialogTitle = "Wybierz obraz"
            }
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                result = chooser.selectedFile.absolutePath
            }
            latch.countDown()
        }
        latch.await(60, java.util.concurrent.TimeUnit.SECONDS)
        result
    } catch (_: Exception) { null }
}
