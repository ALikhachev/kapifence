package org.jetbrains.hackathon2024.bytecode

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.metadata.hasAnnotations
import kotlin.metadata.jvm.KotlinClassMetadata

class DeprecatingClassTransformer(
    cv: ClassWriter,
    private val deprecationMessage: String,
    private val processedClassCallback: (className: String) -> Unit = {},
) : ClassVisitor(Opcodes.ASM9, cv) {
    private var isAlreadyDeprecated = false

    override fun visit(
        version: Int, access: Int, name: String, signature: String?, superName: String?,
        interfaces: Array<out String>?,
    ) {
        processedClassCallback(name.replace('/', '.'))
        val deprecatedAccess = access or Opcodes.ACC_DEPRECATED
        super.visit(version, deprecatedAccess, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String?>?
    ): MethodVisitor? {

        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        when (desc) {
            DEPRECATED_ANNOTATION_DESC -> isAlreadyDeprecated = true
            METADATA_ANNOTATION_DESC -> return MetadataAnnotationVisitor()
        }
        return super.visitAnnotation(desc, visible)
    }

    override fun visitEnd() {
        super.visitEnd()
        if (!isAlreadyDeprecated) {
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

        private fun writeAnnotation(metadata: Metadata) {
            cv.visitAnnotation(METADATA_ANNOTATION_DESC, true).apply {
                visit("k", metadata.kind)
                visitArray("mv").apply {
                    metadata.metadataVersion.forEach { visit(null, it) }
                    visitEnd()
                }
                visitArray("d1").apply {
                    metadata.data1.forEach { visit(null, it) }
                    visitEnd()
                }
                visitArray("d2").apply {
                    metadata.data2.forEach { visit(null, it) }
                    visitEnd()
                }
                if (metadata.extraString.isNotEmpty()) {
                    visit("xs", metadata.extraString)
                }
                if (metadata.packageName.isNotEmpty()) {
                    visit("pn", metadata.packageName)
                }
                visit("xi", metadata.extraInt)
                visitEnd()
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

    companion object {
        private const val DEPRECATED_ANNOTATION_DESC = "Lkotlin/Deprecated;"
        private const val METADATA_ANNOTATION_DESC = "Lkotlin/Metadata;"
    }
}