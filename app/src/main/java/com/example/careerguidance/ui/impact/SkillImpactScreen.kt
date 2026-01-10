package com.example.careerguidance.ui.impact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SkillImpactScreen(
    onBack: () -> Unit
) {
    val vm: SkillImpactViewModel = viewModel()
    val state by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.load()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("How we helped improve your skills") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.padding(8.dp))
            }

            Text("Goal: ${state.goalRole.ifBlank { "(not set)" }}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(6.dp))

            val base = state.baselineScore
            val latest = state.latestScore
            val change = state.scoreChange

            if (base != null && latest != null && change != null) {
                Text("Readiness score: $latest/100", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(4.dp))
                Text("Improvement since first check: ${if (change >= 0) "+$change" else "$change"}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Readiness score history not available yet.", style = MaterialTheme.typography.bodyMedium)
                Text("Open Recommendations once to create the first score snapshot.", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.padding(14.dp))

            Text("Completed using Action Plan feature", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(8.dp))

            if (state.completedSkills.isEmpty()) {
                Text("(no completed skills yet)", style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.completedSkills.forEach { s ->
                        AssistChip(onClick = { }, label = { Text(s) })
                    }
                }
            }
        }
    }
}
