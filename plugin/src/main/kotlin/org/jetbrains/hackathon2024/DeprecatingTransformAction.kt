package org.jetbrains.hackathon2024

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.hackathon2024.parser.ProguardParser
import org.jetbrains.hackathon2024.processor.SpecificationProcessor
import org.jetbrains.hackathon2024.visitor.DeprecatingClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import proguard.KeepClassSpecification
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

internal abstract class DeprecatingTransformAction :
    TransformAction<DeprecatingTransformAction.Parameters> {
    internal interface Parameters : TransformParameters {
        @get:Optional
        @get:Input
        val deprecationMessage: Property<String>

        @get:Input
        val proguardConfig: ListProperty<String>
    }

    private val logger = Logging.getLogger(DeprecatingTransformAction::class.java)

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:InputArtifact
    abstract val inputProvider: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputProvider.get().asFile
        val outputFile = outputs.file(inputFile.name)

        ZipOutputStream(outputFile.outputStream()).use { resultZip ->
            archiveOperations.zipTree(inputFile).visit { details ->
                val file = details.file
                if (file.path.endsWith(".class")) {
                    logger.info("Transforming ${file.name}")
                    // not optimal :(
                    var inputStream: InputStream = file.inputStream()
                    var lastBytes: ByteArray? = null
                    for (proguardConfig in parameters.proguardConfig.get()) {
                        val classReader = ClassReader(inputStream)
                        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
                        logger.info("Handling $proguardConfig")
                        SpecificationProcessor().process(
                            classReader, classWriter, ProguardParser().parse(proguardConfig), parameters.deprecationMessage.orNull
                                ?: "The class is deprecated within the project by KapiFence plugin"
                        )
                        lastBytes = classWriter.toByteArray()
                        inputStream = classWriter.toByteArray().inputStream()
                    }
                    val entry = ZipEntry(details.relativePath.pathString)
                    resultZip.putNextEntry(entry)
                    resultZip.write(lastBytes!!)
                    resultZip.closeEntry()
                } else {
                    val archivePath = if (details.isDirectory) "${details.path}/" else details.path
                    val entry = ZipEntry(archivePath)
                    resultZip.putNextEntry(entry)
                    if (!details.isDirectory) {
                        details.file.inputStream().use { inputStream ->
                            inputStream.copyTo(resultZip)
                        }
                    }
                    resultZip.closeEntry()
                }
            }
        }
    }
}