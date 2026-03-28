package com.example.goodstart.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.goodstart.model.MamaarSection
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.nio.charset.Charset

object MamaarExtractor {

    data class Result(
        val title: String,
        val sections: List<MamaarSection>
    )

    fun getFileName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val col = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && col >= 0) it.getString(col) else null
        } ?: "מאמר"
    }

    fun extract(context: Context, uri: Uri): Result {
        PDFBoxResourceLoader.init(context)

        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open PDF stream")

        val doc: PDDocument = PDDocument.load(stream)
        return try {
            val stripper = PDFTextStripper().apply {
                addMoreFormatting = false
                sortByPosition   = true
            }

            val pageTexts = (1..doc.numberOfPages).map { i ->
                stripper.startPage = i
                stripper.endPage   = i
                val raw = stripper.getText(doc)
                fixEncoding(raw)          // ← fix Hebrew encoding before anything else
            }

            val runningHeaders = detectRunningHeaders(pageTexts)
            val fullText = pageTexts.joinToString("\n") { cleanPage(it, runningHeaders) }

            val title    = detectTitle(pageTexts.firstOrNull() ?: "")
            val sections = splitIntoSections(fullText)

            Result(title, sections)
        } finally {
            doc.close()
            stream.close()
        }
    }

    // ─── Encoding fix ────────────────────────────────────────────────────────

    /**
     * PDFBox reads old Hebrew PDFs (CP1255 / visual order) as Latin-1/CP1252,
     * producing garbled characters.
     *
     * Strategy:
     *  1. If the text already contains Hebrew Unicode → do nothing.
     *  2. Otherwise re-encode the garbled chars back to raw bytes via CP1252,
     *     then decode those bytes as CP1255 (Windows Hebrew).
     *  3. Reverse each line, because the text was stored in visual RTL order.
     */
    private fun fixEncoding(text: String): String {
        if (text.containsHebrew()) return text          // already correct Unicode Hebrew

        return try {
            val cp1252  = Charset.forName("windows-1252")
            val cp1255  = Charset.forName("windows-1255")

            // Encode garbled string back to bytes through CP1252
            val bytes = text.toByteArray(cp1252)

            // Decode those bytes as CP1255 (Hebrew)
            val decoded = String(bytes, cp1255)

            if (!decoded.containsHebrew()) return text  // conversion produced no Hebrew → give up

            // Reverse visual order → logical order per line
            decoded.lines().joinToString("\n") { line ->
                if (line.containsHebrew()) line.reversed() else line
            }
        } catch (e: Exception) {
            text   // fall back to original if anything goes wrong
        }
    }

    private fun String.containsHebrew() =
        any { it.code in 0x05D0..0x05EA }

    // ─── Running-header detection ─────────────────────────────────────────────

    private fun detectRunningHeaders(pages: List<String>): Set<String> {
        if (pages.size < 2) return emptySet()
        val headLines = pages.map { page ->
            page.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.isPageNumber() }
                .take(3)
        }
        val freq      = headLines.flatten().groupingBy { it }.eachCount()
        // הורדת הרף ל-25% במקום 50%, כדי לתפוס כותרות מתחלפות (זוגי/אי-זוגי) או פרקים בלי כותרת
        val threshold = (pages.size / 4).coerceAtLeast(2)
        return freq.filter { it.value >= threshold }.keys
    }

    // ─── Page cleaning ───────────────────────────────────────────────────────

    private fun cleanPage(raw: String, runningHeaders: Set<String>): String {
        val result  = mutableListOf<String>()
        var inNotes = false

        for (line in raw.lines()) {
            val t = line.trim()

            if (t.isEmpty()) {
                if (result.isNotEmpty()) result.add("")
                continue
            }
            if (t.isPageNumber())         continue
            if (t in runningHeaders)      continue

            if (!inNotes && t.isFootnoteBlockStart()) { inNotes = true; continue }
            if (inNotes) continue

            // Remove inline footnote-reference digits glued to a Hebrew letter
            val cleaned = t.replace(
                Regex("(?<=[\\u0590-\\u05FF\"])(\\d{1,2})(?=\\s|$|[\\u0590-\\u05FF\",(])"), ""
            )
            result.add(cleaned)
        }

        return result.joinToString("\n")
    }

    private fun String.isPageNumber(): Boolean {
        val t = trim()
        // מספרים בלבד או עם תווים עוטפים כמו: - 12 - | [12] | (12)
        if (t.matches(Regex("^[-_\\[(]*\\d{1,4}[-_\\])]*$"))) return true
        // אותיות עבריות מוקפות בתווים (ספררויות עבריות): - יב -
        if (t.matches(Regex("^[-_\\[(]+[א-ת]{1,3}[-_\\])]+$"))) return true
        // מתחיל במילה עמוד
        if (t.startsWith("עמוד ")) return true
        return false
    }

    private fun String.isFootnoteBlockStart(): Boolean {
        val t = trim()
        // 1. זיהוי קו מפריד של הערות שוליים (לפחות 3 קווים תחתונים/מקפים)
        if (t.matches(Regex("^[_\\-\u2014=]{3,}.*"))) return true
        
        // 2. הערות שוליים שמתחילות במספרים: 1. | 1) | (1) | [1]
        if (t.matches(Regex("^[(\\[]?\\d{1,3}[).\\]]?\\s+.*"))) return true
        
        // 3. הערות שוליים באותיות עבריות מוקפות: (א) | [א] | *א*
        // (החרגנו "א)" כדי לא לדרוס את חלוקת הפסקאות שרצה למעלה)
        if (t.matches(Regex("^[(\\[\\*][א-ת][)\\]\\*]\\s+.*"))) return true
        
        // 4. מתחיל בכוכבית
        if (t.matches(Regex("^\\*+.*"))) return true

        return false
    }

    // ─── Title detection ─────────────────────────────────────────────────────

    private fun detectTitle(firstPage: String): String {
        val candidates = firstPage.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.isPageNumber() }
        return candidates.firstOrNull()?.take(80) ?: "מאמר"
    }

    // ─── Section splitting ───────────────────────────────────────────────────

    private fun splitIntoSections(text: String): List<MamaarSection> {
        // Hebrew letter + ) at the start of a line: ב) ג) ד) …
        val markerRe = Regex("(?m)^([א-ת]\\)\\s)")
        val matches  = markerRe.findAll(text).toList()

        if (matches.isEmpty()) return splitByParagraphGroups(text)

        val sections = mutableListOf<MamaarSection>()

        val intro = text.substring(0, matches.first().range.first).trim()
        if (intro.isNotEmpty()) sections.add(MamaarSection(null, intro))

        for (i in matches.indices) {
            val start = matches[i].range.first
            val end   = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val chunk = text.substring(start, end).trim()
            val lines   = chunk.lines()
            val heading = lines.firstOrNull()?.trim()
            val body    = lines.drop(1).joinToString("\n").trim()
            sections.add(MamaarSection(heading, body))
        }

        return sections.ifEmpty { listOf(MamaarSection(null, text.trim())) }
    }

    private fun splitByParagraphGroups(text: String): List<MamaarSection> {
        val paragraphs = text.split(Regex("\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) return listOf(MamaarSection(null, text.trim()))

        val sections = mutableListOf<MamaarSection>()
        sections.add(MamaarSection(null, paragraphs.first()))
        paragraphs.drop(1).chunked(4).forEach { chunk ->
            sections.add(MamaarSection(null, chunk.joinToString("\n\n")))
        }
        return sections
    }
}
