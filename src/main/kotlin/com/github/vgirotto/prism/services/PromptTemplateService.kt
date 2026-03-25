package com.github.vgirotto.prism.services

import com.github.vgirotto.prism.model.PromptTemplate
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Manages prompt templates stored in ~/.claude/prompt-templates.json.
 * Templates support variables: {selection}, {file}, {language}.
 */
@Service(Service.Level.APP)
class PromptTemplateService {

    private val log = Logger.getInstance(PromptTemplateService::class.java)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val templateFile: File
        get() {
            val claudeDir = File(System.getProperty("user.home"), ".claude")
            claudeDir.mkdirs()
            return File(claudeDir, "prompt-templates.json")
        }

    fun getTemplates(): List<PromptTemplate> {
        return try {
            val file = templateFile
            if (!file.exists()) return defaultTemplates()
            val json = file.readText()
            if (json.isBlank()) return defaultTemplates()
            val type = object : TypeToken<List<PromptTemplate>>() {}.type
            gson.fromJson(json, type) ?: defaultTemplates()
        } catch (e: Exception) {
            log.warn("Failed to read templates", e)
            defaultTemplates()
        }
    }

    fun saveTemplates(templates: List<PromptTemplate>) {
        try {
            templateFile.writeText(gson.toJson(templates))
        } catch (e: Exception) {
            log.warn("Failed to save templates", e)
        }
    }

    fun addTemplate(template: PromptTemplate) {
        val list = getTemplates().toMutableList()
        list.add(template)
        saveTemplates(list)
    }

    fun removeTemplate(name: String) {
        val list = getTemplates().filter { it.name != name }
        saveTemplates(list)
    }

    /**
     * Resolves template variables and returns the final prompt string.
     */
    fun resolveTemplate(
        template: PromptTemplate,
        selection: String? = null,
        filePath: String? = null,
        language: String? = null,
    ): String {
        var result = template.prompt
        if (template.includeSelection && selection != null) {
            result = result.replace("{selection}", selection)
            // If prompt doesn't use {selection} but includeSelection is true, append it
            if (!template.prompt.contains("{selection}") && selection.isNotBlank()) {
                result = "$result\n\n$selection"
            }
        } else {
            result = result.replace("{selection}", "")
        }

        if (template.includeFileRef && filePath != null) {
            result = result.replace("{file}", "@$filePath")
            if (!template.prompt.contains("{file}") && filePath.isNotBlank()) {
                result = "$result in @$filePath"
            }
        } else {
            result = result.replace("{file}", "")
        }

        result = result.replace("{language}", language ?: "")

        return result.trim()
    }

    private fun defaultTemplates(): List<PromptTemplate> {
        val defaults = listOf(
            PromptTemplate(
                name = "Review for bugs",
                prompt = "Review this code for bugs and issues: {selection}",
            ),
            PromptTemplate(
                name = "Explain this code",
                prompt = "Explain what this code does step by step: {selection}",
            ),
            PromptTemplate(
                name = "Write unit tests",
                prompt = "Write comprehensive unit tests for this code: {selection}",
            ),
        )
        // Save defaults on first use
        try { saveTemplates(defaults) } catch (_: Exception) {}
        return defaults
    }

    companion object {
        fun getInstance(): PromptTemplateService =
            com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(PromptTemplateService::class.java)
    }
}
