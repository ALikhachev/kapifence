package org.jetbrains.hackathon2024.transformations

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.jetbrains.hackathon2024.bytecode.DeprecatingClassTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject

abstract class DependencyTransformAction : TransformAction<DependencyTransformAction.Params> {

    interface Params : TransformParameters

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:InputArtifact
    abstract val inputArtifactFiles: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val file = inputArtifactFiles.get().asFile
        val outputFile = outputs.file(file.name)

        ZipOutputStream(outputFile.outputStream()).use { resultZip ->
            archiveOperations.zipTree(file).visit { someClass ->
                val file = someClass.file
                if (file.endsWith("AbstractSet.class")) {
                    println("Deprecating $file")
                    file.inputStream().use { inputStream ->
                        val classReader = ClassReader(inputStream)
                        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
                        val classVisitor = DeprecatingClassTransformer(
                            classWriter,
                            "Deprecated message",
                        )
                        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                        val entry = ZipEntry(someClass.relativePath.pathString)
                        resultZip.putNextEntry(entry)
                        resultZip.write(classWriter.toByteArray())
                        resultZip.closeEntry()
                    }
                } else {

                    if (someClass.file.isDirectory) {
                        val entry = ZipEntry("${someClass.relativePath.pathString}/")
                        resultZip.putNextEntry(entry)
                        resultZip.closeEntry()
                    } else {
                        val entry = ZipEntry(someClass.relativePath.pathString)
                        resultZip.putNextEntry(entry)
                        resultZip.write(someClass.file.readBytes())
                        resultZip.closeEntry()
                    }
//                println("Not deprecating $file")
                }
            }
        }

//        file.copyTo(outputFile)
    }
}