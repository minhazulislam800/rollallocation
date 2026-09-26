package com.nedaye.rollapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nedaye.rollapp.AppViewModel
import com.nedaye.rollapp.ui.theme.LocalAppColors

@Composable
fun SummaryScreen(vm: AppViewModel) {
    val colors = LocalAppColors.current
    Column {
        Text("✅", style = MaterialTheme.typography.headlineSmall)
        Text("ফাইল প্রস্তুত হয়েছে", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Spacer(Modifier.height(6.dp))
        Surface(color = colors.accentSoft, shape = MaterialTheme.shapes.extraLarge) {
            Text(
                "${vm.students.size} জন শিক্ষার্থী পাওয়া গেছে",
                color = colors.accent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        val map = vm.colMap
        listOf(
            "Student Name" to map.first,
            "Father's Name" to map.second,
            "Mother's Name" to map.third
        ).forEach { (label, value) ->
            Surface(
                color = colors.bg, shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("✓ $label → ${value ?: ""}", modifier = Modifier.padding(10.dp), color = colors.ink)
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { vm.startAssigning() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) { Text("রোল দেওয়া শুরু করুন") }
        TextButton(onClick = { vm.startNewImport() }, modifier = Modifier.fillMaxWidth()) {
            Text("ভিন্ন ফাইল বেছে নিন")
        }
    }
}
