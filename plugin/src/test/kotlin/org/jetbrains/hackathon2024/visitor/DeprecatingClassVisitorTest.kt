package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import proguard.util.ClassNameParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertTrue

class DeprecatingClassVisitorTest {
    @Test
    fun testClassVisitor() {
        val classFilePath = "build/classes/kotlin/test/org/jetbrains/hackathon2024/test/ClassToBeDeprecated.class"
        val someClass = File(classFilePath)
        someClass.inputStream().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            val classVisitor = DeprecatingClassVisitor(
                classWriter,
                "Deprecated message",
                ClassNameParser().parse("org/jetbrains/hackathon2024/**")
            )
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            someClass.writeBytes(classWriter.toByteArray())
        }
        val parsedClassFile = parseClassFile(classFilePath)
        assert(
            parsedClassFile.contains(
                """
                Deprecated: true
                """.trimIndent()
            )
        ) {
            "ClassVisitor should add deprecated bytecode flag to the class file, but it didn't.\n$parsedClassFile"
        }
        assert(
            parsedClassFile.contains(
                """    
                |    kotlin.Deprecated(
                |      message="Deprecated message"
                |    )
                """.trimMargin()
            )
        ) {
            "ClassVisitor should add kotlin.Deprecated annotation, but it didn't.\n$parsedClassFile"
        }
    }

    private fun parseClassFile(pathToClassFile: String): String {
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
}