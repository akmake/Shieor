package com.example.goodstart.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * פורט מ-AdminPage.jsx — אותה לוגיקה בדיוק:
 *  1. מיפוי encoding עברי שבור (hebrewMap)
 *  2. היפוך סוגריים (RTL)
 *  3. סינון הערות שוליים (fontSize < 11)
 *  4. חיתוך header (10% עליון, חוץ מעמוד 1)
 *  5. מיון Y↓ X← (קריאה מימין לשמאל)
 *  6. זיהוי רווחים בין מילים (gap > 4)
 *  7. סינון מספרי עמוד + כותרות ריצה
 *  8. שחזור פסקאות לפי נקודה / נקודתיים / פסיק-נקודה
 */
class ChabadPdfExtractor : PDFTextStripper() {

    private data class Item(
        val x: Float, val y: Float, val w: Float,
        val fontSize: Float, val text: String
    )

    private val pageItems  = mutableMapOf<Int, MutableList<Item>>()
    private val pageHeights = mutableMapOf<Int, Float>()
    private var currentPage = 0

    init { sortByPosition = true }

    // ── Hebrew encoding fix (AdminPage.jsx → hebrewMap) ──────────────────────
    companion object {
        private val hebrewMap = mapOf(
            0xe0 to 'א', 0x2021 to 'א', 0xe1 to 'ב', 0x00B7 to 'ב',
            0xe2 to 'ג', 0x201A to 'ג', 0xe3 to 'ד', 0x201E to 'ד',
            0xe4 to 'ה', 0x2030 to 'ה', 0xe5 to 'ו', 0x00C2 to 'ו',
            0xe6 to 'ז', 0x00CA to 'ז', 0xe7 to 'ח', 0x00C1 to 'ח',
            0xe8 to 'ט', 0x00CB to 'ט', 0xe9 to 'י', 0x00C8 to 'י',
            0xC8 to 'י', 0xea to 'ך', 0x00CD to 'ך', 0xCD to 'ך',
            0xeb to 'כ', 0x00CE to 'כ', 0xec to 'ל', 0x00CF to 'ל',
            0xCF to 'ל', 0xed to 'ם', 0x00CC to 'ם',
            0xee to 'מ', 0x00D3 to 'מ', 0xD3 to 'מ',
            0xef to 'ן', 0x00D4 to 'ן',
            0xf0 to 'נ', 0xF8FF to 'נ',
            0xf1 to 'ס', 0x00D2 to 'ס', 0xf2 to 'ע', 0x00DA to 'ע',
            0xf3 to 'ף', 0x00DB to 'ף', 0xf4 to 'פ', 0x00D9 to 'פ', 0xD9 to 'פ',
            0xf5 to 'ץ', 0x0131 to 'ץ', 0xf6 to 'צ', 0x02C6 to 'צ',
            0xf7 to 'ק', 0x02DC to 'ק', 0xf8 to 'ר', 0x00AF to 'ר',
            0xf9 to 'ש', 0x02D8 to 'ש', 0xfa to 'ת', 0x02D9 to 'ת'
        )

        private val RUNNING_HEADERS = setOf(
            "ספר המאמרים", "תשכ\"ד", "ש\"פ צו",
            "שבת הגדול", "ניסן ה'תשכ\"ד"
        )

        private fun fixHebrew(text: String): String = buildString {
            for (c in text) append(hebrewMap[c.code] ?: c)
        }

        private fun flipBrackets(c: Char) = when (c) {
            '(' -> ')';  ')' -> '('
            '[' -> ']';  ']' -> '['
            '{' -> '}';  '}' -> '{'
            '<' -> '>';  '>' -> '<'
            else -> c
        }
    }

    // ── PDFBox hooks ──────────────────────────────────────────────────────────

    override fun startPage(page: PDPage) {
        currentPage++
        pageItems[currentPage]  = mutableListOf()
        pageHeights[currentPage] = page.mediaBox.height
        super.startPage(page)
    }

    override fun processTextPosition(text: TextPosition) {
        val raw = text.unicode ?: return
        if (raw.isBlank()) return

        // fixHebrew + bracket flip + reverse (כמו ב-JS)
        val fixed = fixHebrew(raw)
            .map { flipBrackets(it) }
            .reversed()
            .joinToString("")

        if (fixed.isBlank()) return

        pageItems[currentPage]?.add(
            Item(x = text.x, y = text.y, w = text.width, fontSize = text.fontSize, text = fixed)
        )
    }

    // suppress default text output — אנחנו בונים את הטקסט בעצמנו
    override fun writeString(text: String, textPositions: List<TextPosition>) { }

    // ── Public API ────────────────────────────────────────────────────────────

    fun extract(document: PDDocument): Pair<String, Int> {
        pageItems.clear()
        pageHeights.clear()
        currentPage = 0
        getText(document)                       // מפעיל startPage + processTextPosition
        return buildOutput(document.numberOfPages)
    }

    // ── Processing (AdminPage.jsx → extractOCR) ───────────────────────────────

    private fun buildOutput(numPages: Int): Pair<String, Int> {
        val allLines = mutableListOf<String>()

        for (pageNum in 1..numPages) {
            val items  = pageItems[pageNum]  ?: continue
            val pageH  = pageHeights[pageNum] ?: continue
            processPageItems(items, pageH, pageNum, allLines)
        }

        // שחזור פסקאות (כמו ב-JS)
        val paragraphs  = mutableListOf<String>()
        val currentPara = mutableListOf<String>()

        for (line in allLines) {
            currentPara.add(line)
            if (line.endsWith('.') || line.endsWith(':') || line.endsWith(';')) {
                paragraphs.add(currentPara.joinToString(" "))
                currentPara.clear()
            }
        }
        if (currentPara.isNotEmpty()) paragraphs.add(currentPara.joinToString(" "))

        return Pair(paragraphs.joinToString("\n\n"), numPages)
    }

    private fun processPageItems(
        items: List<Item>, pageHeight: Float, pageNum: Int, output: MutableList<String>
    ) {
        // topCropY = viewport.height * 0.90 ב-JS (y מלמטה)
        // בPDFBox: y מלמעלה → אנחנו מסירים y < 10% (חלק עליון)
        val topCropThreshold = pageHeight * 0.10f

        // סינון (כמו ב-JS)
        val filtered = items.filter { item ->
            if (item.fontSize < 11f)                            return@filter false
            if (item.text.trim().isEmpty())                     return@filter false
            if (pageNum != 1 && item.y < topCropThreshold)     return@filter false
            true
        }

        // קיבוץ לשורות לפי קירבת Y (tolerance 5), ואז מיון X יורד (RTL)
        // זה מקביל ל-items.sort((a,b) => abs(b.y-a.y)>5 ? b.y-a.y : b.x-a.x) ב-JS
        val sortedByY = filtered.sortedBy { it.y }
        val lineGroups = mutableListOf<MutableList<Item>>()

        for (item in sortedByY) {
            val last = lineGroups.lastOrNull()
            if (last == null || abs(item.y - last.first().y) > 5f) {
                lineGroups.add(mutableListOf(item))
            } else {
                last.add(item)
            }
        }

        // מיון כל שורה ימין → שמאל
        val lines = mutableListOf<String>()
        for (group in lineGroups) {
            val sorted = group.sortedByDescending { it.x }

            val sb       = StringBuilder()
            var lastLeft = Float.MIN_VALUE

            for (item in sorted) {
                if (lastLeft == Float.MIN_VALUE) {
                    sb.append(item.text)
                } else {
                    // gap = lastLeft - (item.x + item.w)  — כמו ב-JS
                    val gap = lastLeft - (item.x + item.w)
                    if (gap > 4f) sb.append(' ')
                    sb.append(item.text)
                }
                lastLeft = item.x
            }

            if (sb.isNotEmpty()) lines.add(sb.toString())
        }

        // סינון שורות (כמו ב-JS)
        for (line in lines) {
            var l = line.trim()
            if (l.isEmpty()) continue
            if (l.matches(Regex("^\\d{1,4}$")))           continue   // מספר עמוד
            if (RUNNING_HEADERS.any { l.contains(it) })   continue   // כותרת ריצה
            l = l.replace(Regex("\\s{2,}"), " ").trim()
            if (l.contains("__") || l.contains("--"))     continue
            if (l.length < 2)                             continue
            output.add(l)
        }
    }
}
