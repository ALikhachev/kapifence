package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import proguard.MemberSpecification
import proguard.util.ClassNameParser
import proguard.util.NameParser
import kotlin.metadata.Visibility
import kotlin.metadata.hasAnnotations
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.moduleName
import kotlin.metadata.jvm.signature
import kotlin.metadata.visibility

class DeprecatingClassMethodVisitor(
    classWriter: ClassWriter,
    private val deprecationMessage: String,
    private val specifications: List<MemberSpecification>,
) : BaseVisitor(classWriter) {

    private val shouldApplyChangesMap = mutableMapOf<Pair<String, String>, Boolean>()

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val shouldBeDeprecated = specifications.any { spec ->
            NameParser().parse(spec.name).matches(name) &&
                    ClassNameParser().parse(spec.descriptor).matches(descriptor) &&
                    spec.requiredSetAccessFlags and access == spec.requiredSetAccessFlags &&
                    spec.requiredUnsetAccessFlags and access == 0
        }
        shouldApplyChangesMap[name to descriptor] = shouldBeDeprecated

        val resultAccess = if (shouldBeDeprecated) access or Opcodes.ACC_DEPRECATED else access

        val originalVisitor = super.visitMethod(resultAccess, name, descriptor, signature, exceptions)

        if (shouldBeDeprecated) {
            return DeprecatingMethodVisitor(originalVisitor)
        }

        return originalVisitor
    }

    override fun visitEnd() {
        val metadata = kotlinMetadata
        if (metadata is KotlinClassMetadata.Class) {
            // TODO: file facades?
            val kClass = metadata.kmClass

            kClass.constructors.forEach{ constructor ->
                if (shouldApplyChangesMap[constructor.signature?.name to constructor.signature?.descriptor] == true) {
                    constructor.hasAnnotations = true
                }
            }

            kClass.functions.forEach { function ->
                val bytecodeName = if (function.visibility == Visibility.INTERNAL) {
                    "${function.name}$${kClass.moduleName}"
                } else {
                    function.name
                }
                if (shouldApplyChangesMap[bytecodeName to function.signature?.descriptor] == true) {
                    function.hasAnnotations = true
                }
            }
        }

        writeAnnotation(metadata.write())
    }

    private inner class DeprecatingMethodVisitor(originalVisitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, originalVisitor) {
        override fun visitEnd() {
            val deprecatedAnnotation = super.visitAnnotation(DEPRECATED_ANNOTATION_DESC, true)
            deprecatedAnnotation.visit("message", deprecationMessage)
            deprecatedAnnotation.visitEnd()
        }
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        return when {
            desc == METADATA_ANNOTATION_DESC -> return MetadataAnnotationVisitor()
            else -> super.visitAnnotation(desc, visible)
        }
    }
}