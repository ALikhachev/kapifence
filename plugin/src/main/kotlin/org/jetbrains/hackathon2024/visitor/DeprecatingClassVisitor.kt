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
        super.visitEnd()
        if (!isAlreadyDeprecated && shouldApplyChanges) {
            // don't deprecate and don't override the message if the class is already deprecated
            val deprecatedAnnotation = super.visitAnnotation(DEPRECATED_ANNOTATION_DESC, true)
            deprecatedAnnotation.visit("message", deprecationMessage)
            deprecatedAnnotation.visitEnd()
        }
    }

    /**
     * Reads the existing [Metadata] and overrides it.
     * Does not define the super annotation visitor and does not contain calls to `super`,
     * because we don't need to write the existing annotation as is.
     *
     * The kotlin.Metadata of the supported versions won't change and has only primitive and array values,
     * thus we override only [visit] and [visitArray].
     */
    private inner class MetadataAnnotationVisitor : AnnotationVisitor(Opcodes.ASM9) {
        private val values = mutableMapOf<String, Any>()

        override fun visit(name: String, value: Any?) {
            if (value != null) {
                values[name] = value
            }
        }

        override fun visitArray(name: String): AnnotationVisitor {
            return object : AnnotationVisitor(Opcodes.ASM9) {
                private val list = mutableListOf<String>()

                override fun visit(name: String?, value: Any?) {
                    if (value != null) {
                        list.add(value as String)
                    }
                }

                override fun visitEnd() {
                    values[name] = list.toTypedArray()
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun visitEnd() {
            val header = Metadata(
                kind = values["k"] as Int? ?: 1,
                metadataVersion = values["mv"] as IntArray? ?: intArrayOf(),
                data1 = values["d1"] as Array<String>? ?: emptyArray(),
                data2 = values["d2"] as Array<String>? ?: emptyArray(),
                extraString = values["xs"] as String? ?: "",
                packageName = values["pn"] as String? ?: "",
                extraInt = values["xi"] as Int? ?: 0
            )

            val metadata = KotlinClassMetadata.Companion.readStrict(header)
            if (metadata is KotlinClassMetadata.Class) {
                // TODO: file facades?
                val kClass = metadata.kmClass
                kClass.hasAnnotations = true
            }

            writeAnnotation(metadata.write())
        }
    }
}