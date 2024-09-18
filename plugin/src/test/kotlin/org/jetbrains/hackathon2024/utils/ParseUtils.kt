package org.jetbrains.hackathon2024.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

internal fun parseClassFile(pathToClassFile: String): String {
    val processBuilder = ProcessBuilder(
        "javap",
        "-v",
        pathToClassFile,
    )
    processBuilder.redirectErrorStream()
    val process = processBuilder.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    process.waitFor()

    return reader.lines().collect(Collectors.joining("\n"))
}