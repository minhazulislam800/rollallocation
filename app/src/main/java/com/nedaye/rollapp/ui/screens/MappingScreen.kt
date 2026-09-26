package com.nedaye.rollapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nedaye.rollapp.AppViewModel
import com.nedaye.rollapp.ui.theme.LocalAppColors

@Composable
fun MappingScreen(vm: AppViewModel) {
    val colors = LocalAppColors.current
    var nameSel by remember { mutableStateOf(vm.colMap.first) }
    var fatherSel by remember { mutableStateOf(vm.colMap.second) }
    var motherSel by remember { mutableStateOf(vm.colMap.third) }

    Column {
        Text("একটি কলাম নিজে বেছে দিন", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Text("বাকি সব ঠিকঠাক শনাক্ত হয়েছে", style = MaterialTheme.typography.bodySmall, color = colors.inkSoft)
        Spacer(Modifier.height(10.dp))
        ColumnPicker("Student Name", vm.headers, nameSel) { nameSel = it }
        ColumnPicker("Father's Name", vm.headers, fatherSel) { fatherSel = it }
        ColumnPicker("Mother's Name", vm.headers, motherSel) { motherSel = it }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { vm.confirmMapping(nameSel, fatherSel, motherSel) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) { Text("নিশ্চিত করুন ও চালিয়ে যান") }
    }
}

@Composable
private fun ColumnPicker(label: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.ink)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected ?: "— নির্বাচন করুন —")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
                }
            }
        }
    }
}
