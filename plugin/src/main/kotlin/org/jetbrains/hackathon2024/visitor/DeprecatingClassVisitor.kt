package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import proguard.util.StringMatcher
import kotlin.metadata.hasAnnotations
import kotlin.metadata.jvm.KotlinClassMetadata

class DeprecatingClassVisitor(
    classWriter: ClassWriter,
    private val deprecationMessage: String,
    private val matcher: StringMatcher
) : BaseVisitor(classWriter) {
    private var isAlreadyDeprecated = false
    private var shouldApplyChanges = false

    override fun visit(
        version: Int, access: Int, name: String, signature: String?, superName: String?,
        interfaces: Array<out String>?,
    ) {
        shouldApplyChanges = matcher.matches(name)
        val resultingAccessFlags =
            if (shouldApplyChanges) access or Opcodes.ACC_DEPRECATED else access
        super.visit(version, resultingAccessFlags, name, signature, superName, interfaces)
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        return when {
            shouldApplyChanges && desc == DEPRECATED_ANNOTATION_DESC -> {
                isAlreadyDeprecated = true
                super.visitAnnotation(desc, visible)
            }
            shouldApplyChanges && desc == METADATA_ANNOTATION_DESC -> return MetadataAnnotationVisitor()
            else -> super.visitAnnotation(desc, visible)
        }
    }

    override fun visitEnd() {
        val metadata = kotlinMetadata
        if (metadata is KotlinClassMetadata.Class) {
            // TODO: file facades?
            val kClass = metadata.kmClass
            kClass.hasAnnotations = true
        }

        kotlinMetadata?.let {
            writeAnnotation(it.write())
        }
        super.visitEnd()
        if (!isAlreadyDeprecated && shouldApplyChanges) {
            // don't deprecate and don't override the message if the class is already deprecated
            val deprecatedAnnotation = super.visitAnnotation(DEPRECATED_ANNOTATION_DESC, true)
            deprecatedAnnotation.visit("message", deprecationMessage)
            deprecatedAnnotation.visitEnd()
        }
    }
}