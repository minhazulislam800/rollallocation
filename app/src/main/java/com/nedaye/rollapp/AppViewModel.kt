package com.nedaye.rollapp

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nedaye.rollapp.data.ExcelIO
import com.nedaye.rollapp.data.SessionStore
import com.nedaye.rollapp.data.Settings
import com.nedaye.rollapp.model.FieldDef
import com.nedaye.rollapp.model.Student
import com.nedaye.rollapp.model.deepCopy
import com.nedaye.rollapp.model.isBlank
import com.nedaye.rollapp.model.isDup
import com.nedaye.rollapp.model.isInvalid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Import, Mapping, Summary, Assign }

private val NAME_KEYS = listOf("name", "student name", "নাম", "শিক্ষার্থীর নাম")
private val FATHER_KEYS = listOf("father", "father name", "fathers name", "father's name", "পিতার নাম", "পিতা")
private val MOTHER_KEYS = listOf("mother", "mother name", "mothers name", "mother's name", "মাতার নাম", "মাতা")

class AppViewModel(app: Application) : AndroidViewModel(app) {

    var screen by mutableStateOf(Screen.Import)
        private set
    private val backStack = mutableListOf<Screen>()

    var headers by mutableStateOf(listOf<String>())
    val students = mutableStateListOf<Student>()
    var colMap by mutableStateOf(Triple<String?, String?, String?>(null, null, null))
    var fileName by mutableStateOf("")
    var fingerprint by mutableStateOf("")
    private var pendingRows: List<List<String>> = emptyList()

    var search by mutableStateOf("")
    var filter by mutableStateOf("all")
    var saveStatus by mutableStateOf("সংরক্ষিত")
    var canUndo by mutableStateOf(false)
    private val undoStack = mutableListOf<List<Student>>()
    var toastMsg by mutableStateOf<String?>(null)
        private set

    // Set right after a successful export so the UI can offer a share sheet
    // (WhatsApp etc.) — consumed once via clearExportedUri().
    var lastExportedUri by mutableStateOf<Uri?>(null)
        private set
    fun clearExportedUri() { lastExportedUri = null }

    // Hidden field list — default is just Roll. Changed only via the "+" in the
    // assign-screen topbar (see FieldManagerDialog). Persisted app-wide.
    var fieldDefs by mutableStateOf(listOf<FieldDef>())
        private set

    init {
        fieldDefs = Settings.loadFields(getApplication())
        SessionStore.loadCurrent(getApplication())?.let { (fname, h, s) ->
            if (s.isNotEmpty()) {
                fileName = fname
                headers = h
                students.addAll(s)
                fingerprint = SessionStore.fingerprint(h, s)
                backStack.add(Screen.Import)
                backStack.add(Screen.Summary)
                screen = Screen.Assign
            }
        }
    }

    fun goto(s: Screen) { backStack.add(screen); screen = s }
    fun goBack(): Boolean {
        if (backStack.isEmpty()) return false
        screen = backStack.removeAt(backStack.size - 1)
        return true
    }

    fun showToast(msg: String) { toastMsg = msg }
    fun clearToast() { toastMsg = null }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            showToast("ফাইল পড়া হচ্ছে...")
            val parsed = withContext(Dispatchers.IO) {
                ExcelIO.readFirstSheet(getApplication<Application>().contentResolver, uri)
            }
            if (parsed == null || parsed.headers.isEmpty()) {
                showToast("ফাইলটি পড়া যায়নি")
                return@launch
            }
            headers = parsed.headers
            pendingRows = parsed.rows
            val map = detectColumns(parsed.headers)
            colMap = map
            fileName = queryFileName(uri) ?: "students.xlsx"
            if (map.first == null || map.second == null || map.third == null) {
                goto(Screen.Mapping)
            } else {
                buildStudentsAndProceed(map)
            }
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        } catch (e: Exception) { null }
    }

    fun confirmMapping(name: String?, father: String?, mother: String?) {
        if (name == null || father == null || mother == null) {
            showToast("তিনটি কলামই বেছে দিন")
            return
        }
        colMap = Triple(name, father, mother)
        buildStudentsAndProceed(colMap)
    }

    private fun buildStudentsAndProceed(map: Triple<String?, String?, String?>) {
        val ni = headers.indexOf(map.first)
        val fi = headers.indexOf(map.second)
        val mi = headers.indexOf(map.third)
        val built = pendingRows.mapIndexed { idx, row ->
            Student(
                id = idx,
                name = row.getOrNull(ni) ?: "",
                father = row.getOrNull(fi) ?: "",
                mother = row.getOrNull(mi) ?: "",
                originalRow = row
            )
        }.toMutableList()

        val fp = SessionStore.fingerprint(headers, built)
        val existing = SessionStore.load(getApplication(), fp)
        if (existing != null && existing.third.any { it.roll.isNotBlank() }) {
            val rollByKey = existing.third.associateBy { "${it.name}|${it.father}|${it.mother}" }
            for (i in built.indices) {
                rollByKey["${built[i].name}|${built[i].father}|${built[i].mother}"]?.let {
                    built[i] = built[i].copy(roll = it.roll)
                }
            }
        }

        fingerprint = fp
        students.clear(); students.addAll(built)
        persist()
        goto(Screen.Summary)
    }

    private fun persist() {
        SessionStore.save(getApplication(), fingerprint, fileName, headers, students)
    }

    fun startAssigning() { goto(Screen.Assign) }

    fun startNewImport() {
        SessionStore.clearCurrent(getApplication())
        headers = listOf(); students.clear(); fileName = ""; fingerprint = ""
        undoStack.clear(); canUndo = false
        screen = Screen.Import
        backStack.clear()
    }

    private fun detectColumns(h: List<String>): Triple<String?, String?, String?> {
        fun norm(s: String) = s.trim().lowercase()
        var n: String? = null; var f: String? = null; var m: String? = null
        for (col in h) {
            val nc = norm(col)
            if (n == null && NAME_KEYS.contains(nc)) n = col
            else if (f == null && FATHER_KEYS.contains(nc)) f = col
            else if (m == null && MOTHER_KEYS.contains(nc)) m = col
        }
        return Triple(n, f, m)
    }

    fun isBlank(s: Student) = s.isBlank()
    fun isInvalid(s: Student) = s.isInvalid()
    fun isDup(s: Student) = s.isDup(students)

    data class Stats(val total: Int, val assigned: Int, val blank: Int, val err: Int)
    fun stats(): Stats {
        var assigned = 0; var blank = 0; var err = 0
        students.forEach {
            when {
                isBlank(it) -> blank++
                isInvalid(it) || isDup(it) -> err++
                else -> assigned++
            }
        }
        return Stats(students.size, assigned, blank, err)
    }

    fun filteredStudents(): List<Student> {
        val q = search.trim().lowercase()
        var list = students.toList()
        if (q.isNotEmpty()) list = list.filter {
            it.name.lowercase().contains(q) || it.father.lowercase().contains(q) || it.mother.lowercase().contains(q)
        }
        return when (filter) {
            "todo" -> list.filter { isBlank(it) }
            "err" -> list.filter { isDup(it) || isInvalid(it) }
            else -> list
        }
    }

    fun rollFieldActive(): Boolean = fieldDefs.any { it.isRoll }

    fun addField(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        val id = Settings.slugify(trimmed, fieldDefs.map { it.id })
        fieldDefs = fieldDefs + FieldDef(id, trimmed)
        Settings.saveFields(getApplication(), fieldDefs)
    }

    fun removeField(id: String) {
        fieldDefs = fieldDefs.filterNot { it.id == id }
        Settings.saveFields(getApplication(), fieldDefs)
    }

    private fun snapshot() {
        undoStack.add(students.map { it.deepCopy() })
        if (undoStack.size > 20) undoStack.removeAt(0)
        canUndo = true
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeAt(undoStack.size - 1)
        students.clear(); students.addAll(prev)
        canUndo = undoStack.isNotEmpty()
        persist()
        showToast("আনডু হয়েছে")
    }

    fun setRoll(id: Int, value: String) = setFieldValue(id, "roll", value)

    fun setFieldValue(studentId: Int, fieldId: String, value: String) {
        val idx = students.indexOfFirst { it.id == studentId }
        if (idx < 0) return
        val cur = students[idx]
        if (fieldId == "roll" && cur.roll.isBlank() && value.isNotBlank()) {
            snapshot()
        }
        students[idx] = if (fieldId == "roll") cur.copy(roll = value)
        else cur.copy(extra = (cur.extra + (fieldId to value)).toMutableMap())
        saveStatus = "সংরক্ষণ হচ্ছে..."
        viewModelScope.launch {
            persist()
            saveStatus = "সংরক্ষিত"
        }
    }


    fun exportNow(uri: Uri) {
        viewModelScope.launch {
            val st = stats()
            showToast(
                if (st.blank > 0 || st.err > 0)
                    "${st.assigned} জনের রোল দেওয়া আছে, বাকিরা ছাড়াই এক্সপোর্ট হচ্ছে..."
                else "সবার রোল দেওয়া আছে, এক্সপোর্ট হচ্ছে..."
            )
            val headerRow = fieldDefs.map { it.label } + headers
            val dataRows = students.map { s ->
                fieldDefs.map { f -> if (f.isRoll) s.roll else s.extra[f.id] ?: "" } +
                    headers.indices.map { i -> s.originalRow.getOrNull(i) ?: "" }
            }
            withContext(Dispatchers.IO) {
                ExcelIO.writeXlsx(getApplication<Application>().contentResolver, uri, headerRow, dataRows)
            }
            showToast("ফাইল তৈরি হয়েছে")
            lastExportedUri = uri
        }
    }
}
