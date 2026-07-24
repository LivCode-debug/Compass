package com.example.api

data class ModelVariant(
    val name: String,
    val source: String,
    val downloadUrl: String,
    val expectedSize: String
) {
    companion object {
        val PRESETS = listOf(
            ModelVariant(
                name = "Gemma 4 E2B IT (CPU)",
                source = "HuggingFace (micmacfree - UNGATED)",
                downloadUrl = "https://huggingface.co/micmacfree/gemma-4-e2b-it-litertlm/resolve/main/gemma-4-e2b-it.litertlm",
                expectedSize = "2.5 GB"
            ),
            ModelVariant(
                name = "Gemma 4 E4B IT QAT (CPU)",
                source = "HuggingFace (DarrenJiaImbue - UNGATED)",
                downloadUrl = "https://huggingface.co/DarrenJiaImbue/gemma-4-E4B-it-qat-litertlm/resolve/main/gemma-4-E4B-it-qat.litertlm",
                expectedSize = "3.2 GB"
            )
        )
    }
}
