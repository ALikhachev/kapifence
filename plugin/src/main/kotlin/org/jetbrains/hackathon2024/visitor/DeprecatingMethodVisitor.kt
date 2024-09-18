package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import proguard.MemberSpecification
import proguard.util.ClassNameParser
import proguard.util.NameParser
import proguard.util.StringMatcher

// TODO(Dmitrii Krasnov): add deprecation logic
class DeprecatingMethodVisitor(
    classWriter: ClassWriter,
    private val deprecationMessage: String,
//    private val matchers: List<Pair<StringMatcher, StringMatcher>>,
    private val specifications: List<MemberSpecification>,
) : BaseVisitor(classWriter) {

    private val shouldApplyChangesMap = mutableMapOf<String, Boolean>()

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
//        if ((Opcodes.ACC_PUBLIC and access) > 0)
//        shouldApplyChangesMap[name] = matchers.any { (nameMatcher, descriptorMatcher) ->
//            nameMatcher.matches(name) && descriptorMatcher.matches(descriptor)
//        }
        shouldApplyChangesMap[name] = specifications.any { spec ->
            NameParser().parse(spec.name).matches(name) &&
                    ClassNameParser().parse(spec.descriptor).matches(descriptor) &&
                    spec.requiredSetAccessFlags and access == spec.requiredSetAccessFlags &&
                    spec.requiredUnsetAccessFlags and access == 0
//            nameMatcher.matches(name) && descriptorMatcher.matches(descriptor)
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
}