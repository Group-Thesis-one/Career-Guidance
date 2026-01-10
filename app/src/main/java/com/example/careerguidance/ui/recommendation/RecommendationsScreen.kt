package com.example.careerguidance.ui.recommendation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
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
fun RecommendationsScreen(
    onBack: () -> Unit
) {
    val vm: GoalPlanViewModel = viewModel()
    val state by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.load()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Career Plan") },
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
            Text("Experience years: ${state.experienceYears}", style = MaterialTheme.typography.bodyMedium)

            val plan = state.plan
            if (plan != null) {
                Spacer(Modifier.height(10.dp))
                Text("Readiness score: ${plan.readinessScore}/100", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Required matched: ${plan.requiredMatched}/${plan.requiredTotal}   Optional matched: ${plan.optionalMatched}/${plan.optionalTotal}",
                    style = MaterialTheme.typography.bodyMedium
                )
                state.scoreDelta?.let { d ->
                    Spacer(Modifier.height(4.dp))
                    Text("Change since last check: ${if (d >= 0) "+$d" else "$d"}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Your skills (normalized)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (state.skills.isEmpty()) {
                Text("(no skills found yet)", style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.skills.forEach { s ->
                        AssistChip(onClick = { }, label = { Text(s) })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (plan != null) {
                Text("What to work on next", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                val topMissing = plan.missingSkills.take(10)

                if (topMissing.isEmpty()) {
                    Text("You already match all listed skills for this goal.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    topMissing.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("${index + 1}. ${item.skill}", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (item.isRequired) "Type: required" else "Type: optional",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Why: ${item.reason}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("Score: ${item.score}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                Text("Set a goal to get a personalized plan.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
