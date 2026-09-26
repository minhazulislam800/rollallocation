package com.nedaye.rollapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nedaye.rollapp.AppViewModel
import com.nedaye.rollapp.ui.theme.LocalAppColors

/**
 * Hidden power-user dialog: opened only via the small "+" in the assign-screen
 * topbar. Lets someone add extra entry fields (e.g. মোবাইল নম্বর) later, and
 * remove any field — Roll included — via the ✕. Not linked from anywhere else.
 */
@Composable
fun FieldManagerDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    var newLabel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("এন্ট্রি ফিল্ড ঠিক করুন") },
        text = {
            Column {
                Text(
                    "রোল এন্ট্রি স্ক্রিনে কোন কোন তথ্য নেওয়া হবে এখান থেকে ঠিক করুন। সাধারণ ব্যবহারে এখানে হাত দেওয়ার দরকার নেই।",
                    style = MaterialTheme.typography.bodySmall, color = colors.inkSoft
                )
                Spacer(Modifier.height(10.dp))
                vm.fieldDefs.forEach { f ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(f.label, modifier = Modifier.weight(1f), color = colors.ink)
                        TextButton(onClick = { vm.removeField(f.id) }) {
                            Text("✕", color = colors.error)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newLabel, onValueChange = { newLabel = it },
                        placeholder = { Text("যেমনঃ মোবাইল নম্বর") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { vm.addField(newLabel); newLabel = "" }) { Text("+ যোগ") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন") } }
    )
}
