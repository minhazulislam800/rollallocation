package com.nedaye.rollapp.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nedaye.rollapp.AppViewModel
import com.nedaye.rollapp.model.Student
import com.nedaye.rollapp.ui.components.FieldManagerDialog
import com.nedaye.rollapp.ui.theme.LocalAppColors

@Composable
fun AssignScreen(vm: AppViewModel) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var showFieldManager by remember { mutableStateOf(false) }

    LaunchedEffect(vm.lastExportedUri) {
        vm.lastExportedUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "এক্সেল ফাইল শেয়ার করুন"))
            vm.clearExportedUri()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> uri?.let { vm.exportNow(it) } }

    Column(Modifier.fillMaxSize()) {
        val st = vm.stats()
        val rollActive = vm.rollFieldActive()

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            if (rollActive) {
                LinearProgressIndicator(
                    progress = { if (st.total > 0) st.assigned / st.total.toFloat() else 0f },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = colors.accent, trackColor = colors.accentSoft
                )
                Spacer(Modifier.width(10.dp))
                Text("${st.assigned}/${st.total}", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            } else {
                Text("${vm.students.size} জন শিক্ষার্থী", style = MaterialTheme.typography.labelLarge, color = colors.ink, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.width(8.dp))
            // Small, unlabeled "+" — the only way into the hidden field manager.
            TextButton(onClick = { showFieldManager = true }) { Text("+") }
        }

        OutlinedTextField(
            value = vm.search, onValueChange = { vm.search = it },
            placeholder = { Text("নাম দিয়ে খুঁজুন...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true
        )

        if (rollActive) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                listOf("all" to "সব ${st.total}", "todo" to "বাকি ${st.blank}", "err" to "সমস্যা ${st.err}").forEach { (key, label) ->
                    FilterChip(
                        selected = vm.filter == key,
                        onClick = { vm.filter = key },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val list = vm.filteredStudents()

        if (vm.fieldDefs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("কোনো এন্ট্রি ফিল্ড নেই — + চেপে একটা যোগ করুন", color = colors.inkSoft)
            }
        } else if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("কোনো শিক্ষার্থী পাওয়া যায়নি", color = colors.inkSoft)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(list, key = { _, s -> s.id }) { i, s ->
                    StudentCard(vm, s, i)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(vm.saveStatus, style = MaterialTheme.typography.bodySmall, color = colors.inkSoft, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.undo() }, enabled = vm.canUndo) { Text("↺") }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = {
                    val base = vm.fileName.substringBeforeLast(".").ifBlank { "students" }
                    exportLauncher.launch("${base}_export.xlsx")
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) { Text("⭳ Export Excel") }
        }
    }

    if (showFieldManager) {
        FieldManagerDialog(vm) { showFieldManager = false }
    }
}

@Composable
private fun StudentCard(vm: AppViewModel, s: Student, index: Int) {
    val colors = LocalAppColors.current
    val focusManager = LocalFocusManager.current
    val rollField = vm.fieldDefs.firstOrNull { it.isRoll }
    val extraFields = vm.fieldDefs.filterNot { it.isRoll }
    val dup = rollField != null && vm.isDup(s)
    val inv = rollField != null && vm.isInvalid(s)

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (dup || inv) colors.error else colors.border)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", modifier = Modifier.width(22.dp), color = colors.inkSoft, style = MaterialTheme.typography.labelSmall)
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(s.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1)
                    Text("${s.father} · ${s.mother}", style = MaterialTheme.typography.bodySmall, color = colors.inkSoft, maxLines = 1)
                }
                if (rollField != null) {
                    var rollText by remember(s.id) { mutableStateOf(s.roll) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = rollText,
                            onValueChange = { v -> rollText = v; vm.setFieldValue(s.id, "roll", v) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.width(88.dp)
                        )
                        if (dup || inv) {
                            Text(if (dup) "ডুপ্লিকেট" else "সংখ্যা লিখুন", color = colors.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (extraFields.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                extraFields.forEach { f ->
                    var fieldText by remember(s.id, f.id) { mutableStateOf(s.extra[f.id] ?: "") }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(f.label, style = MaterialTheme.typography.labelSmall, color = colors.inkSoft, modifier = Modifier.width(96.dp))
                        OutlinedTextField(
                            value = fieldText,
                            onValueChange = { v -> fieldText = v; vm.setFieldValue(s.id, f.id, v) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
