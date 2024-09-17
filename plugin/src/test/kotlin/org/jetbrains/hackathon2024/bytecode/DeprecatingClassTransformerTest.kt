package org.jetbrains.hackathon2024.bytecode

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import kotlin.test.Test

class DeprecatingClassTransformerTest {
    @Test
    fun test() {
        val someClass = File("build/classes/kotlin/main/org/jetbrains/hackathon2024/KapiFencePlugin.class")
        someClass.inputStream().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            val classVisitor = DeprecatingClassTransformer(
                classWriter,
                "Deprecated message",
            )
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        }
        // TODO assertions
    }
}