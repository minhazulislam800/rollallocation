package com.nedaye.rollapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nedaye.rollapp.AppViewModel
import com.nedaye.rollapp.Screen
import com.nedaye.rollapp.ui.components.TypewriterCredit
import com.nedaye.rollapp.ui.theme.LocalAppColors

@Composable
fun ImportScreen(vm: AppViewModel) {
    val colors = LocalAppColors.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importFile(it) }
    }
    Column(Modifier.fillMaxWidth()) {
    if (vm.students.isNotEmpty()) {
        val st = vm.stats()
        val progressLabel = if (vm.rollFieldActive())
            "${st.assigned}/${st.total} জনের রোল দেওয়া হয়েছে"
        else
            "${st.total} জন শিক্ষার্থী"
        Surface(
            color = colors.accentSoft,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, colors.accent)
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Text("চলমান কাজ আছে", style = MaterialTheme.typography.titleSmall, color = colors.accent)
                Spacer(Modifier.height(2.dp))
                Text(vm.fileName.ifBlank { "সংরক্ষিত ফাইল" }, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                Text(progressLabel, style = MaterialTheme.typography.bodySmall, color = colors.inkSoft)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.goto(Screen.Assign) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) { Text("কাজ চালিয়ে যান") }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("📥", style = MaterialTheme.typography.headlineSmall)
            Text("Excel ফাইল আপলোড করুন", style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text("মাদ্রাসার Excel ফাইলটি বেছে নিন", style = MaterialTheme.typography.bodySmall, color = colors.inkSoft)
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, colors.border, RoundedCornerShape(12.dp))
                    .background(colors.bg, RoundedCornerShape(12.dp))
                    .clickable {
                        launcher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/octet-stream"
                            )
                        )
                    }
                    .padding(vertical = 34.dp)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⬆️", style = MaterialTheme.typography.headlineSmall)
                    Text("ফাইল বেছে নিতে ট্যাপ করুন", style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text("শুধু .xlsx ফাইল চলবে", style = MaterialTheme.typography.bodySmall, color = colors.inkSoft)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "নাম, পিতার নাম, মাতার নাম — এই কলামগুলো থাকা যেকোনো Excel ফাইল চলবে",
                style = MaterialTheme.typography.bodySmall, color = colors.inkSoft,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.goto(Screen.Mapping) }, modifier = Modifier.fillMaxWidth()) {
                Text("কলাম নিজে থেকে না চিনলে যেমন দেখাবে (দেখুন)", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TypewriterCredit()
    }
    }
}
