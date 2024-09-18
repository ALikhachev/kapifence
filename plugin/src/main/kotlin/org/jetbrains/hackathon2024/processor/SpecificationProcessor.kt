package org.jetbrains.hackathon2024.processor

import org.jetbrains.hackathon2024.visitor.DeprecatingClassVisitor
import org.jetbrains.hackathon2024.visitor.DeprecatingMethodVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import proguard.KeepClassSpecification
import proguard.util.ClassNameParser
import proguard.util.NameParser

class SpecificationProcessor {

    fun process(
        classReader: ClassReader,
        cw: ClassWriter,
        specifications: List<KeepClassSpecification>,
        // TODO(Dmitrii Krasnov): refactor this parameter
        deprecationMessage: String,
    ) {
        specifications.forEach { specification ->
            when {
                specification.methodSpecifications?.isNotEmpty() ?: false -> {
                    val matchers = specification.methodSpecifications.map { methodSpecification ->
                        NameParser().parse(methodSpecification.descriptor)
                    }
                    classReader.accept(
                        DeprecatingMethodVisitor(cw, deprecationMessage, matchers),
                        ClassReader.EXPAND_FRAMES
                    )
                }

                specification.fieldSpecifications?.isNotEmpty() ?: false -> {
                    // TODO(Dmitrii Krasnov): fields is not empty
                }

                else -> {
                    val classMatcher = ClassNameParser().parse(specification.className)
                    classReader.accept(
                        DeprecatingClassVisitor(cw, deprecationMessage, classMatcher),
                        ClassReader.EXPAND_FRAMES
                    )
                }
            }
        }
    }
}