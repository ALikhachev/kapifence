package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import proguard.util.StringMatcher

// TODO(Dmitrii Krasnov): add deprecation logic
class DeprecatingMethodVisitor(
    classWriter: ClassWriter,
    private val deprecationMessage: String,
    private val methodMatchers: List<StringMatcher>,
    ) : BaseVisitor(classWriter) {

    private var shouldApplyChanges = false

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        shouldApplyChanges = methodMatchers.any { it.matches(descriptor) }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
}