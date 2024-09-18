package org.jetbrains.hackathon2024.visitor

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

abstract class BaseVisitor(
    classWriter: ClassWriter,
) : ClassVisitor(Opcodes.ASM9, classWriter)