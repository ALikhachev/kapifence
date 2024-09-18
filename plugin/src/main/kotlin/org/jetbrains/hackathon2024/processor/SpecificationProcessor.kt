package org.jetbrains.hackathon2024.processor

import org.jetbrains.hackathon2024.visitor.DeprecatingClassFields
import org.jetbrains.hackathon2024.visitor.DeprecatingClassVisitor
import org.jetbrains.hackathon2024.visitor.DeprecatingClassMethodVisitor
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
                        ClassNameParser().parse(methodSpecification.name) to NameParser().parse(methodSpecification.descriptor)
                    }
                    classReader.accept(
                        DeprecatingClassMethodVisitor(cw, deprecationMessage, specification.methodSpecifications),
                        ClassReader.EXPAND_FRAMES
                    )
                }

                specification.fieldSpecifications?.isNotEmpty() ?: false -> {
                    classReader.accept(
                        DeprecatingClassFields(cw, deprecationMessage, specification.fieldSpecifications),
                        ClassReader.EXPAND_FRAMES
                    )
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