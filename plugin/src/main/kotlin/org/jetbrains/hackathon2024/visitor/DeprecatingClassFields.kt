package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.*
import proguard.MemberSpecification
import proguard.util.ClassNameParser
import proguard.util.NameParser
import kotlin.metadata.hasAnnotations
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.syntheticMethodForAnnotations

class DeprecatingClassFields(
    classWriter: ClassWriter,
    private val deprecationMessage: String,
    private val specifications: List<MemberSpecification>,
) : BaseVisitor(classWriter) {

    private val shouldApplyChangesMap = mutableMapOf<String, Boolean>()

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {

        val shouldBeDeprecated = specifications.any { spec ->
            NameParser().parse(spec.name).matches(name) &&
                    ClassNameParser().parse(spec.descriptor).matches(descriptor) &&
                    spec.requiredSetAccessFlags and access == spec.requiredSetAccessFlags &&
                    spec.requiredUnsetAccessFlags and access == 0
        }
        shouldApplyChangesMap[name] = shouldBeDeprecated

        val resultAccess = if (shouldBeDeprecated) access or Opcodes.ACC_DEPRECATED else access

        val originalVisitor = super.visitField(resultAccess, name, descriptor, signature, value)

        return originalVisitor
    }

    override fun visitMethod(
        access: Int,
        methodName: String,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val kotlinMetadata = kotlinMetadata
        if (kotlinMetadata is KotlinClassMetadata.Class) {
            val kClass = kotlinMetadata.kmClass
            val property = kClass.properties.filter { property ->
                shouldApplyChangesMap[property.name] == true && property.getterSignature?.name == methodName
            }.firstOrNull()
            if (property != null) {
                property.getter.hasAnnotations = true
                property.hasAnnotations = true
                property.syntheticMethodForAnnotations =
                    property.getterSignature!!.copy(name = "${property.getterSignature!!.name}\$annotations")
                val syntheticFieldAccess = access or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_DEPRECATED
                val newVisitor = DeprecatingMethodVisitor(
                    this.visitMethod(
                        syntheticFieldAccess,
                        "${property.getterSignature!!.name}\$annotations",
                        descriptor,
                        signature,
                        exceptions
                    )
                )
                newVisitor.visitEnd()
                return super.visitMethod(
                    access or Opcodes.ACC_DEPRECATED,
                    methodName,
                    descriptor,
                    signature,
                    exceptions
                )
            }
        }

        return super.visitMethod(access, methodName, descriptor, signature, exceptions)
    }

    override fun visitEnd() {
        kotlinMetadata?.let {
            writeAnnotation(it.write())
        }
    }

    private inner class DeprecatingMethodVisitor(originalVisitor: MethodVisitor) :
        MethodVisitor(Opcodes.ASM9, originalVisitor) {
        override fun visitEnd() {
            val deprecatedAnnotation = super.visitAnnotation(DEPRECATED_ANNOTATION_DESC, true)
            deprecatedAnnotation.visit("message", deprecationMessage)
            deprecatedAnnotation.visitEnd()
        }
    }

    private inner class DeprecatingFieldVisitor(originalVisitor: FieldVisitor) :
        FieldVisitor(Opcodes.ASM9, originalVisitor) {
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