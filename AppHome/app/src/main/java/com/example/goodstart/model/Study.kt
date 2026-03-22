package com.example.goodstart.model

data class Study(
    val key: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val accent: String? = null,
    val kind: String? = null,
    val label: String? = null,
    val ref: String? = null,
    val preview: String? = null,
    val available: Boolean = false,
    val sections: List<Section>? = null
)
