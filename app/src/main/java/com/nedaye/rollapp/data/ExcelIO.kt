package com.nedaye.rollapp.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Minimal, dependency-free .xlsx reader/writer (no Apache POI — POI has known
 * Android-compatibility issues). Reads the first worksheet only. Writes plain
 * inline-string cells, which Excel/Google Sheets/WPS all open fine.
 */
object ExcelIO {

    data class ParsedSheet(val headers: List<String>, val rows: List<List<String>>)

    fun readFirstSheet(resolver: ContentResolver, uri: Uri): ParsedSheet? {
        val entries = readZipEntries(resolver, uri) ?: return null
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetPath = entries.keys.firstOrNull { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }
            ?: return null
        val rows = parseSheetRows(entries[sheetPath]!!, sharedStrings)
        if (rows.isEmpty()) return ParsedSheet(emptyList(), emptyList())
        return ParsedSheet(rows[0], rows.drop(1))
    }

    private fun readZipEntries(resolver: ContentResolver, uri: Uri): Map<String, ByteArray>? {
        val input = resolver.openInputStream(uri) ?: return null
        val map = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val buf = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var n = zis.read(chunk)
                    while (n >= 0) { buf.write(chunk, 0, n); n = zis.read(chunk) }
                    map[entry.name] = buf.toByteArray()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return map
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var event = parser.eventType
        val sb = StringBuilder()
        var inSi = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "si") { inSi = true; sb.setLength(0) }
                XmlPullParser.TEXT -> if (inSi) sb.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "si") { list.add(sb.toString()); inSi = false }
            }
            event = parser.next()
        }
        return list
    }

    private fun colRowFromRef(ref: String): Pair<Int, Int> {
        val col = ref.takeWhile { it.isLetter() }
        val row = ref.dropWhile { it.isLetter() }.toIntOrNull() ?: 1
        var n = 0
        for (c in col) n = n * 26 + (c.uppercaseChar() - 'A' + 1)
        return Pair(n - 1, row)
    }

    private fun parseSheetRows(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rowsMap = sortedMapOf<Int, MutableMap<Int, String>>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var event = parser.eventType
        var curType = ""
        var curCol = 0
        var curRow = 0
        val valSb = StringBuilder()
        var inValue = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            curType = parser.getAttributeValue(null, "t") ?: ""
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            val (c, r) = if (ref.isNotEmpty()) colRowFromRef(ref) else Pair(curCol, curRow)
                            curCol = c; curRow = r
                        }
                        "v", "t" -> { inValue = true; valSb.setLength(0) }
                    }
                }
                XmlPullParser.TEXT -> if (inValue) valSb.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (parser.name == "v" || parser.name == "t") {
                        inValue = false
                        val raw = valSb.toString()
                        val value = if (curType == "s") raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) ?: "" } ?: "" else raw
                        rowsMap.getOrPut(curRow) { mutableMapOf() }[curCol] = value
                    }
                }
            }
            event = parser.next()
        }
        val maxCol = rowsMap.values.maxOfOrNull { m -> m.keys.maxOrNull() ?: 0 } ?: 0
        return rowsMap.keys.sorted().map { r ->
            val rowMap = rowsMap[r]!!
            (0..maxCol).map { c -> rowMap[c] ?: "" }
        }
    }

    fun writeXlsx(resolver: ContentResolver, uri: Uri, headers: List<String>, rows: List<List<String>>) {
        val out = resolver.openOutputStream(uri) ?: return
        ZipOutputStream(out).use { zos ->
            fun entry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            entry(
                "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "</Types>"
            )
            entry(
                "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>"
            )
            entry(
                "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<sheets><sheet name=\"Roll Assigned\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
            )
            entry(
                "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "</Relationships>"
            )

            fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            fun colLetter(i: Int): String {
                var n = i + 1
                val sb = StringBuilder()
                while (n > 0) { val m = (n - 1) % 26; sb.insert(0, ('A' + m)); n = (n - 1) / 26 }
                return sb.toString()
            }
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            fun writeRow(rowIdx: Int, cells: List<String>) {
                sb.append("<row r=\"").append(rowIdx).append("\">")
                cells.forEachIndexed { ci, v ->
                    val ref = colLetter(ci) + rowIdx
                    sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(esc(v)).append("</t></is></c>")
                }
                sb.append("</row>")
            }
            writeRow(1, headers)
            rows.forEachIndexed { i, r -> writeRow(i + 2, r) }
            sb.append("</sheetData></worksheet>")
            entry("xl/worksheets/sheet1.xml", sb.toString())
        }
    }
}
