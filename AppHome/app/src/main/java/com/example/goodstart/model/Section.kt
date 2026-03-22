package com.example.goodstart.model

data class Section(
    val id: String? = null,
    val isHeader: Boolean = false,
    val isAliyahHeader: Boolean = false,
    val isChapterHeader: Boolean = false,
    val he: String? = null,
    val en: String? = null,
    val ordinal: String? = null,
    val rashi: List<RashiItem>? = null,
    val verseNum: Int? = null,
    val chapterNum: Int? = null
) {
    data class RashiItem(val he: String? = null)
}
