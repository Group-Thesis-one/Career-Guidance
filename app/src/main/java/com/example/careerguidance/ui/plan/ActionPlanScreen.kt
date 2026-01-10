package com.example.careerguidance.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPlanScreen(
    onBack: () -> Unit
) {
    val vm: ActionPlanViewModel = viewModel()
    val state by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.load()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Action Plan") },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val scroll = rememberScrollState()
        val visibleItems = state.items.filter { !it.done }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(20.dp)
        ) {

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "Goal: ${state.goalRole.ifBlank { "(not set)" }}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))

            Text(
                "Readiness score: ${state.readinessScore}/100",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Progress: ${state.completedCount}/${state.totalCount}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(14.dp))
            Text(
                "Tick a skill when you completed it using this Action Plan feature.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(16.dp))

            if (state.items.isEmpty()) {
                Text("No plan items yet.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            if (visibleItems.isEmpty()) {
                Text(
                    "All action plan items are completed.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open 'How we helped improve your skills' to see your completed skills.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            visibleItems.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${index + 1}. ${item.skill}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (item.isRequired) "Type: required" else "Type: optional",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Priority score: ${item.score}", style = MaterialTheme.typography.bodySmall)
                            }

                            Checkbox(
                                checked = item.done,
                                onCheckedChange = { checked ->
                                    vm.setDone(item.skill, checked)
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Why this matters", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(item.why, style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(12.dp))
                        Text("Steps", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.steps.forEachIndexed { i, step ->
                                Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
