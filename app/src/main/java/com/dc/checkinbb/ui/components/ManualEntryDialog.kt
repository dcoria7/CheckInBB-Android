package com.dc.checkinbb.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // null = pickers not completed yet; non-null = confirmed timestamp
    var selectedMs by remember { mutableStateOf<Long?>(null) }
    // Guard against double-tap on Guardar
    var saved by remember { mutableStateOf(false) }

    // Show the date→time pickers when this composable first appears.
    // Using a stable key (true) so it only fires once per composition lifecycle.
    LaunchedEffect(true) {
        val cal = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, day ->
                // Update calendar with chosen date
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)

                // Create TimePicker INSIDE the DatePicker callback so it reads
                // the freshly-updated calendar values
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        // Clamp to current time so we can't register future entries
                        selectedMs = minOf(cal.timeInMillis, System.currentTimeMillis())
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    android.text.format.DateFormat.is24HourFormat(context)
                ).also { tp ->
                    tp.setOnCancelListener { onDismiss() }
                    tp.show()
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setOnCancelListener { onDismiss() }
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    // Confirmation dialog — only visible after pickers are done
    val confirmedMs = selectedMs
    if (confirmedMs != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirmar toma") },
            text = {
                Column {
                    Text(
                        text = "Fecha y hora seleccionada:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatter.format(Date(confirmedMs)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!saved) {
                            saved = true
                            onConfirm(confirmedMs)
                        }
                    },
                    enabled = !saved
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}
