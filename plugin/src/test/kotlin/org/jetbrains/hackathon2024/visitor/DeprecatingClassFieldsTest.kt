package org.jetbrains.hackathon2024.visitor

import org.jetbrains.hackathon2024.parser.ProguardParser
import org.jetbrains.hackathon2024.utils.parseClassFile
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File

class DeprecatingClassFieldsTest {

    @Test
    fun visitFieldVisitor() {
        val classFilePath = "build/classes/kotlin/test/org/jetbrains/hackathon2024/test/ClassWithFieldToBeDeprecated.class"
        val someClass = File(classFilePath)
//        val outputFile = kotlin.io.path.createTempFile(suffix = ".class").toFile()
        val outputFile = someClass
        val specification =
            ProguardParser().parse("class org.jetbrains.hackathon2024.test.ClassWithFieldToBeDeprecated { private java.lang.String propertyToBeDeprecated; }").first()
        someClass.inputStream().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            val methodVisitor = DeprecatingClassFields(
                classWriter,
                "Deprecated 2 message 2",
                specification.fieldSpecifications,
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
                |      message="Deprecated 2 message 2"
                |    )
                """.trimMargin()
            )
        ) {
            "ClassVisitor should add kotlin.Deprecated annotation, but it didn't.\n$parsedClassFile"
        }
    }
}