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
import kotlin.metadata.visibility

// TODO(Dmitrii Krasnov): add deprecation logic
class DeprecatingClassMethodVisitor(
    classWriter: ClassWriter,
    private val deprecationMessage: String,
    private val specifications: List<MemberSpecification>,
) : BaseVisitor(classWriter) {

    private val shouldApplyChangesMap = mutableMapOf<String, Boolean>()
    private lateinit var kotlinMetadata: Metadata

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
        shouldApplyChangesMap[name] = shouldBeDeprecated

        val resultAccess = if (shouldBeDeprecated) access or Opcodes.ACC_DEPRECATED else access

        val originalVisitor = super.visitMethod(resultAccess, name, descriptor, signature, exceptions)

        if (shouldBeDeprecated) {
            return DeprecatingMethodVisitor(originalVisitor)
        }

        return originalVisitor
    }

    override fun visitEnd() {
        val metadata = KotlinClassMetadata.Companion.readStrict(kotlinMetadata)
        if (metadata is KotlinClassMetadata.Class) {
            // TODO: file facades?
            val kClass = metadata.kmClass
            kClass.functions.forEach { function ->
                val bytecodeName = if (function.visibility == Visibility.INTERNAL) {
                    "${function.name}$${kClass.moduleName}"
                } else {
                    function.name
                }
                if (shouldApplyChangesMap[bytecodeName] == true) {
                    function.hasAnnotations = true
                }
            }
        }

        writeAnnotation(metadata.write())
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

    private inner class DeprecatingMethodVisitor(originalVisitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, originalVisitor) {
        override fun visitEnd() {
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
            kotlinMetadata = Metadata(
                kind = values["k"] as Int? ?: 1,
                metadataVersion = values["mv"] as IntArray? ?: intArrayOf(),
                data1 = values["d1"] as Array<String>? ?: emptyArray(),
                data2 = values["d2"] as Array<String>? ?: emptyArray(),
                extraString = values["xs"] as String? ?: "",
                packageName = values["pn"] as String? ?: "",
                extraInt = values["xi"] as Int? ?: 0
            )
        }
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        return when {
            desc == METADATA_ANNOTATION_DESC -> return MetadataAnnotationVisitor()
            else -> super.visitAnnotation(desc, visible)
        }
    }

    companion object {
        private const val DEPRECATED_ANNOTATION_DESC = "Lkotlin/Deprecated;"
        private const val METADATA_ANNOTATION_DESC = "Lkotlin/Metadata;"
    }
}