package org.minekot.toolchain

import kotlinx.serialization.json.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Writes MineKot codestyle template files into a project.
 */
@DisableCachingByDefault(because = "Small template writer with project-local output.")
abstract class WriteMineKotCodestyleTask : DefaultTask() {
    /**
     * Directory that receives the codestyle template files.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Writes all bundled codestyle resources.
     */
    @TaskAction
    fun writeCodestyle() {
        mineKotCodestyleDescriptors.forEach { descriptor ->
            writeResource(descriptor.resourcePath, descriptor.outputPath)
        }
        writeIntellijWorkspaceSettings()
    }

    private fun writeResource(resourceName: String, relativePath: String) {
        val target = outputDirectory.file(relativePath).get().asFile
        target.writeMineKotText(javaClass.classLoader.readMineKotResourceText(resourceName))
    }

    private fun writeIntellijWorkspaceSettings() {
        val target = outputDirectory.file(".idea/workspace.xml").get().asFile
        target.ensureMineKotParentDirectory()

        val document = if (target.exists()) readWorkspaceDocument(target) else newWorkspaceDocument()

        document.documentElement.upsertOptionComponent(document, "FormatOnSaveOptions", "myRunOnSave", "true")
        document.documentElement.upsertOptionComponent(document, "OptimizeOnSaveOptions", "myRunOnSave", "true")
        document.documentElement.upsertPropertiesComponent(document)
        document.documentElement.upsertOptionComponent(
            document,
            "VcsManagerConfiguration",
            "CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT",
            "true",
        )
        document.documentElement.upsertOptionComponent(
            document,
            "VcsManagerConfiguration",
            "OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT",
            "true",
        )
        document.documentElement.upsertOptionComponent(
            document,
            "VcsManagerConfiguration",
            "REFORMAT_BEFORE_PROJECT_COMMIT",
            "true",
        )
        document.documentElement.upsertOptionComponent(
            document,
            "VcsManagerConfiguration",
            "REARRANGE_BEFORE_PROJECT_COMMIT",
            "true",
        )

        target.writeMineKotText(document.toXmlText())
    }

    private fun readWorkspaceDocument(target: File): Document =
        runCatching {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(target)
        }.getOrElse { failure ->
            throw failure.asGradleException("Failed to parse IntelliJ workspace settings at ${target.path}.")
        }

    private fun newWorkspaceDocument(): Document =
        runCatching {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().also { document ->
                val project = document.createElement("project")
                project.setAttribute("version", "4")
                document.appendChild(project)
            }
        }.getOrElse { failure ->
            throw failure.asGradleException("Failed to create new IntelliJ workspace settings document.")
        }

    private fun Element.childElements(): Sequence<Element> =
        (0 until childNodes.length)
            .asSequence()
            .map(childNodes::item)
            .filterIsInstance<Element>()

    private fun Element.upsertOptionComponent(
        document: Document,
        componentName: String,
        optionName: String,
        value: String,
    ) {
        val component = findComponent(componentName) ?: document.createComponent(componentName).also(::appendChild)
        component.upsertOption(document, optionName, value)
    }

    private fun Element.upsertPropertiesComponent(document: Document) {
        val component = findComponent("PropertiesComponent")
            ?: document.createComponent("PropertiesComponent").also(::appendChild)
        val source = component.textContent.trim()
        val existing = runCatching {
            if (source.isBlank()) {
                JsonObject(emptyMap())
            } else {
                Json.parseToJsonElement(source).jsonObject
            }
        }.getOrDefault(JsonObject(emptyMap()))
        val keyToString = runCatching {
            existing["keyToString"]?.jsonObject ?: JsonObject(emptyMap())
        }.getOrDefault(JsonObject(emptyMap()))
        val updated = buildJsonObject {
            existing.forEach { (name, value) ->
                put(name, value)
            }
            putJsonObject("keyToString") {
                keyToString.forEach { (name, value) ->
                    put(name, value)
                }
                put("code.cleanup.on.save", "true")
                put("rearrange.code.on.save", "true")
                put("settings.editor.selected.configurable", "actions.on.save")
            }
        }

        while (component.hasChildNodes()) {
            component.removeChild(component.firstChild)
        }
        component.appendChild(document.createCDATASection(Json.encodeToString(JsonObject.serializer(), updated)))
    }

    private fun Document.createComponent(name: String): Element =
        createElement("component").also { component ->
            component.setAttribute("name", name)
        }

    private fun Element.findComponent(name: String): Element? =
        childElements().firstOrNull { element ->
            element.tagName == "component" && element.getAttribute("name") == name
        }

    private fun Element.upsertOption(document: Document, name: String, value: String) {
        val option = findOption(name) ?: document.createElement("option").also(::appendChild)
        option.setAttribute("name", name)
        option.setAttribute("value", value)
    }

    private fun Element.findOption(name: String): Element? =
        childElements().firstOrNull { element ->
            element.tagName == "option" && element.getAttribute("name") == name
        }

    private fun Document.toXmlText(): String {
        val writer = StringWriter()
        val transformer = runCatching {
            TransformerFactory.newInstance().newTransformer()
        }.getOrElse { failure ->
            throw failure.asGradleException("Failed to initialize XML transformer.")
        }
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        runCatching {
            transformer.transform(DOMSource(this), StreamResult(writer))
        }.getOrElse { failure ->
            throw GradleException("Failed to transform document to XML.", failure)
        }
        return writer.toString().trimEnd() + "\n"
    }
}
