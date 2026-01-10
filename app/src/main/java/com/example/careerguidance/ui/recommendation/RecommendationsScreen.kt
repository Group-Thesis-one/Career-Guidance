package com.example.careerguidance.ui.recommendation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    onBack: () -> Unit
) {
    val vm: RecommendationsViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Career Recommendations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error ?: "Error")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.loadRecommendations() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Top 10 roles based on your skills",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    items(state.recommendations) { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val percent = (rec.score * 100).roundToInt()
                                Text(rec.title, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(4.dp))
                                Text("Match score: $percent%")

                                if (rec.matchedSkills.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text("Matched skills:", style = MaterialTheme.typography.titleMedium)
                                    Text(rec.matchedSkills.joinToString(", "))
                                }

                                if (rec.missingRequired.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text("Missing required skills:", style = MaterialTheme.typography.titleMedium)
                                    Text(rec.missingRequired.joinToString(", "))
                                }

                                if (rec.missingOptional.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text("Missing optional skills:", style = MaterialTheme.typography.titleMedium)
                                    Text(rec.missingOptional.joinToString(", "))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
