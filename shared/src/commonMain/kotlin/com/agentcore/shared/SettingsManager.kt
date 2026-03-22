package com.agentcore.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

class SettingsManager(private val filePath: String) {
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true 
    }

    fun <T> save(data: T, serializer: KSerializer<T>) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(serializer, data))
        } catch (e: Exception) {
            println("Error saving settings to $filePath: ${e.message}")
        }
    }

    fun <T> load(serializer: KSerializer<T>): T? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val content = file.readText()
                if (content.isBlank()) return null
                json.decodeFromString(serializer, content)
            } else null
        } catch (e: Exception) {
            println("Error loading settings from $filePath: ${e.message}")
            null
        }
    }
}
