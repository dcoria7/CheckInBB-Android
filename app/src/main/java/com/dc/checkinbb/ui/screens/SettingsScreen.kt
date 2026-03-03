package com.dc.checkinbb.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dc.checkinbb.BuildConfig
import com.dc.checkinbb.data.AppTheme
import com.dc.checkinbb.viewmodel.FeedingViewModel
import com.dc.checkinbb.viewmodel.ThemeViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    feedingViewModel: FeedingViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val baby by feedingViewModel.baby.collectAsState()
    val currentTheme by themeViewModel.currentTheme.collectAsState()

    // Local state — only applied on Save
    var babyName by remember(baby.name) { mutableStateOf(baby.name) }
    var selectedTheme by remember(currentTheme) { mutableStateOf(currentTheme) }
    var intervalHours by remember(baby.feedingIntervalHours) {
        mutableFloatStateOf(baby.feedingIntervalHours.toFloat())
    }
    var windowMin by remember(baby.feedingWindowMin) {
        mutableFloatStateOf(baby.feedingWindowMin.toFloat())
    }
    var windowMax by remember(baby.feedingWindowMax) {
        mutableFloatStateOf(baby.feedingWindowMax.toFloat())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configuración",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            themeViewModel.setTheme(selectedTheme)
                            feedingViewModel.updateBabySettings(
                                name = babyName,
                                intervalHours = roundToHalf(intervalHours).toDouble(),
                                windowMin = roundToHalf(windowMin).toDouble(),
                                windowMax = roundToHalf(windowMax).toDouble()
                            )
                            onDismiss()
                        }
                    ) {
                        Text(
                            "Guardar",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Sección Nombre del bebé ─────────────────────────────────────
            SettingsCard(header = "Mi bebé", footer = "Este nombre aparece en el título de la app") {
                OutlinedTextField(
                    value = babyName,
                    onValueChange = { babyName = it },
                    label = { Text("Nombre del bebé") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Sección Tema ────────────────────────────────────────────────
            SettingsCard(header = "Apariencia") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Tema de colores",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    val themeOptions = AppTheme.entries.toList()
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, theme ->
                            SegmentedButton(
                                selected = selectedTheme == theme,
                                onClick = { selectedTheme = theme },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = themeOptions.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = selectedTheme == theme) {
                                        Icon(
                                            Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    text = when (theme) {
                                        AppTheme.BOY -> "👦 Niño"
                                        AppTheme.GIRL -> "👧 Niña"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Sección Intervalo ───────────────────────────────────────────
            SettingsCard(header = "Tiempo entre tomas") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Intervalo recomendado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${"%.1f".format(roundToHalf(intervalHours))} horas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = intervalHours,
                        onValueChange = { intervalHours = it },
                        valueRange = 1f..6f,
                        steps = 9, // (6-1)/0.5 - 1 = 9 steps between stops
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1h", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("6h", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            // ── Sección Ventana ─────────────────────────────────────────────
            SettingsCard(
                header = "Ventana de alimentación",
                footer = "Se enviará notificación cuando entre en esta ventana"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Min window
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ventana mínima",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${"%.1f".format(roundToHalf(windowMin))} horas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Slider(
                            value = windowMin,
                            onValueChange = { windowMin = it },
                            valueRange = 1f..5f,
                            steps = 7, // (5-1)/0.5 - 1 = 7 steps
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50)
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Max window
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ventana máxima",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${"%.1f".format(roundToHalf(windowMax))} horas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF9800)
                            )
                        }
                        Slider(
                            value = windowMax,
                            onValueChange = { windowMax = it },
                            valueRange = 2f..8f,
                            steps = 11, // (8-2)/0.5 - 1 = 11 steps
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF9800),
                                activeTrackColor = Color(0xFFFF9800)
                            )
                        )
                    }
                }
            }

            // ── Info card ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Ajusta estos valores conforme tu bebé crece. Los recién nacidos suelen alimentarse cada 2–3 horas, mientras que bebés mayores pueden espaciar más las tomas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // ── Versión ─────────────────────────────────────────────────────
            SettingsCard(header = "Acerca de") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Versión",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Helper composable ────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    header: String,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = header.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Snap slider value to nearest 0.5 ────────────────────────────────────────
private fun roundToHalf(value: Float): Float = (value * 2).roundToInt() / 2f
