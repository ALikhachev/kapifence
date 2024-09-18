package org.jetbrains.hackathon2024.visitor

import org.jetbrains.hackathon2024.parser.ProguardParser
import org.jetbrains.hackathon2024.utils.parseClassFile
import org.junit.jupiter.api.Test

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File

class DeprecatingMethodVisitorTest {

    @Test
    fun testMethodVisitor() {
        val classFilePath =
            "build/classes/kotlin/test/org/jetbrains/hackathon2024/test/ClassWithMethodToBeDeprecated.class"
        val someClass = File(classFilePath)
        val outputFile = kotlin.io.path.createTempFile(suffix = ".class").toFile()
        val specification =
            ProguardParser().parse("class org.jetbrains.hackathon2024.test.ClassWithMethodToBeDeprecated { public void *(); }")
                .first()
        someClass.inputStream().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            val methodVisitor = DeprecatingClassMethodVisitor(
                classWriter,
                "Deprecated method message",
                specification.methodSpecifications,
            )
            classReader.accept(methodVisitor, ClassReader.EXPAND_FRAMES)
            outputFile.writeBytes(classWriter.toByteArray())
        }
        val parsedClassFile = parseClassFile(outputFile.absolutePath)

        val numberOfDeprecatedMethodsInClassFile = parsedClassFile.split(
            """    
                |    Deprecated: true
                |    RuntimeVisibleAnnotations:
                |      0: #34(#35=s#36)
                |        kotlin.Deprecated(
                |          message="Deprecated method message"
                |        )
                """.trimMargin()
        ).size - 1
        assert(numberOfDeprecatedMethodsInClassFile == 3) {
            "ClassVisitor should add kotlin.Deprecated annotation, but it didn't.\n$parsedClassFile"
        }
    }
}