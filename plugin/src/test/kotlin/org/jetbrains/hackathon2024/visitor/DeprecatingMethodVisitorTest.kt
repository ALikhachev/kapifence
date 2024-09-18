package org.jetbrains.hackathon2024.visitor

import org.jetbrains.hackathon2024.parser.ProguardParser
import org.jetbrains.hackathon2024.utils.parseClassFile
import org.junit.jupiter.api.Test

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import proguard.util.ClassNameParser
import proguard.util.NameParser
import java.io.File

class DeprecatingMethodVisitorTest {

    @Test
    fun testMethodVisitor() {
        val classFilePath = "build/classes/kotlin/test/org/jetbrains/hackathon2024/test/ClassWithMethodToBeDeprecated.class"
        val someClass = File(classFilePath)
        val outputFile = kotlin.io.path.createTempFile(suffix = ".class").toFile()
        val specification =
            ProguardParser().parse("class org.jetbrains.hackathon2024.test.ClassWithMethodToBeDeprecated { public void *(); }").first()
        someClass.inputStream().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            val methodVisitor = DeprecatingMethodVisitor(
                classWriter,
                "Deprecated 1 message 1",
//                specification.methodSpecifications.map { NameParser().parse(it.name) to ClassNameParser().parse(it.descriptor)},
                specification.methodSpecifications,
            )
            classReader.accept(methodVisitor, ClassReader.EXPAND_FRAMES)
            outputFile.writeBytes(classWriter.toByteArray())
        }
        val parsedClassFile = parseClassFile(outputFile.absolutePath)

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
}