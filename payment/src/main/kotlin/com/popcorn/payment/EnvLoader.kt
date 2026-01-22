package com.popcorn.payment

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object EnvLoader {
    fun load(paths: List<String>) {
        paths
            .map { Paths.get(it) }
            .firstOrNull { Files.isRegularFile(it) }
            ?.let { loadFile(it) }
    }

    private fun loadFile(path: Path) {
        val lines = Files.readAllLines(path)
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                continue
            }
            val idx = line.indexOf('=')
            if (idx <= 0) {
                continue
            }
            val key = line.substring(0, idx).trim()
            var value = line.substring(idx + 1).trim()
            if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
                value = value.substring(1, value.length - 1)
            }
            if (key.isEmpty()) {
                continue
            }
            val alreadySet = System.getProperty(key) != null || System.getenv(key) != null
            if (!alreadySet) {
                System.setProperty(key, value)
            }
        }
    }
}
