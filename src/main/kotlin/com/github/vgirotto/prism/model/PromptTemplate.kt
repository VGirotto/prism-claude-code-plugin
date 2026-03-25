package com.github.vgirotto.prism.model

data class PromptTemplate(
    val name: String,
    val prompt: String,
    val includeSelection: Boolean = true,
    val includeFileRef: Boolean = true,
)
