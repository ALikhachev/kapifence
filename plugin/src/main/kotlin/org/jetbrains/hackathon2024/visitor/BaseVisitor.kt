package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import kotlin.metadata.jvm.KotlinClassMetadata

abstract class BaseVisitor(
    classWriter: ClassWriter,
) : ClassVisitor(Opcodes.ASM9, classWriter) {

    protected lateinit var kotlinMetadata: KotlinClassMetadata

    /**
     * Reads the existing [Metadata] and overrides it.
     * Does not define the super annotation visitor and does not contain calls to `super`,
     * because we don't need to write the existing annotation as is.
     *
     * The kotlin.Metadata of the supported versions won't change and has only primitive and array values,
     * thus we override only [visit] and [visitArray].
     */
    protected inner class MetadataAnnotationVisitor : AnnotationVisitor(Opcodes.ASM9) {
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
            kotlinMetadata = KotlinClassMetadata.Companion.readStrict(header)
        }
    }

    protected fun writeAnnotation(metadata: Metadata) {
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


    companion object {
        internal const val DEPRECATED_ANNOTATION_DESC = "Lkotlin/Deprecated;"
        internal const val METADATA_ANNOTATION_DESC = "Lkotlin/Metadata;"
    }
}