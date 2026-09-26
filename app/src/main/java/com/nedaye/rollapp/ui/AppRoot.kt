package com.nedaye.rollapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nedaye.rollapp.AppViewModel
import com.nedaye.rollapp.Screen
import com.nedaye.rollapp.ui.screens.AssignScreen
import com.nedaye.rollapp.ui.screens.ImportScreen
import com.nedaye.rollapp.ui.screens.MappingScreen
import com.nedaye.rollapp.ui.screens.SummaryScreen
import com.nedaye.rollapp.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun AppRoot(vm: AppViewModel) {
    val colors = LocalAppColors.current
    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(14.dp))
            Text("নেদায়ে ইসলাম মহিলা ফাদ্বিল মাদ্রাসা", style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text("শিক্ষার্থীদের নতুন রোল দিন", style = MaterialTheme.typography.bodySmall, color = colors.inkSoft)
            Spacer(Modifier.height(10.dp))
            StepPills(vm)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f)) {
                when (vm.screen) {
                    Screen.Import -> ImportScreen(vm)
                    Screen.Mapping -> MappingScreen(vm)
                    Screen.Summary -> SummaryScreen(vm)
                    Screen.Assign -> AssignScreen(vm)
                }
            }
        }
        vm.toastMsg?.let { msg ->
            LaunchedEffect(msg) { delay(1700); vm.clearToast() }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                Surface(color = colors.ink, shape = MaterialTheme.shapes.extraLarge) {
                    Text(
                        msg, color = colors.bg, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepPills(vm: AppViewModel) {
    val colors = LocalAppColors.current
    val steps = listOf(Screen.Import to "১. আপলোড", Screen.Summary to "২. সারাংশ", Screen.Assign to "৩. রোল দিন")
    val cur = if (vm.screen == Screen.Mapping) Screen.Import else vm.screen
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEach { (s, label) ->
            val on = s == cur
            Surface(
                color = if (on) colors.accent else colors.surface,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    color = if (on) colors.surface else colors.inkSoft,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}
