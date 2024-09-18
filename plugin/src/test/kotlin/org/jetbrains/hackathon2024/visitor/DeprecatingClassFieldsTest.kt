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
        val classFilePath =
            "build/classes/kotlin/test/org/jetbrains/hackathon2024/test/ClassWithFieldToBeDeprecated.class"
        val someClass = File(classFilePath)
        val outputFile = kotlin.io.path.createTempFile(suffix = ".class").toFile()
        val specification =
            ProguardParser().parse("class org.jetbrains.hackathon2024.test.ClassWithFieldToBeDeprecated { private java.lang.String *; }")
                .first()
        someClass.inputStream().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            val methodVisitor = DeprecatingClassFields(
                classWriter,
                "Deprecated field message",
                specification.fieldSpecifications,
            )
            classReader.accept(methodVisitor, ClassReader.EXPAND_FRAMES)
            outputFile.writeBytes(classWriter.toByteArray())
        }
        val parsedClassFile = parseClassFile(outputFile.absolutePath)

        val numberOfDeprecatedGettorsInClassFile = parsedClassFile.split(
            """    
            |    Deprecated: true
            |    RuntimeVisibleAnnotations:
            |      0: #53(#54=s#55)
            |        kotlin.Deprecated(
            |          message="Deprecated field message"
            |        )
            """.trimMargin()
        ).size - 1
        assert(numberOfDeprecatedGettorsInClassFile == 3) {
            "ClassVisitor should add kotlin.Deprecated annotation, but it didn't.\n$parsedClassFile"
        }
    }
}