package com.example.bresttransapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    records: List<TransportRecord>,
    onDelete: (TransportRecord) -> Unit,
    onDeleteAll: () -> Unit,
    onExport: () -> Unit,
    onUpload: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("История записей", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(records) { record ->
                ElevatedCard(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${record.time} — ${record.vehicleNumber} (${record.routeNumber}, ${record.type})", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("${record.currentStop} → ${record.nextStop}", style = MaterialTheme.typography.bodyMedium)
                        Text("Заполненность остановки: ${record.peopleAtStop}, заполненность транспорта: ${record.peopleInTransport}")
                        Text("Вошло: ${record.entered}, вышло: ${record.exited}")
                        Text("Координаты: ${record.latitude}, ${record.longitude}")
                        Text("Погода: ${record.weather}")
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onDelete(record) },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Удалить запись")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDeleteAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить всё", maxLines = 1)
            }
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        if (records.isEmpty()) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val filename = "bresttrans_data_${sdf.format(Date())}.csv"
                        val file = File(context.getExternalFilesDir(null), filename)
                        val writer = OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)
                        writer.write("Время,Регистрационный номер,Маршрут,Тип,Текущая,Следующая,Заполненность остановки,Заполненность транспорта,Вошло,Вышло,Широта,Долгота,Погода\n")
                        records.forEach { record ->
                            writer.write("${record.time},${record.vehicleNumber},${record.routeNumber},${record.type},${record.currentStop},${record.nextStop},${record.peopleAtStop},${record.peopleInTransport},${record.entered},${record.exited},${record.latitude},${record.longitude},${record.weather}\n")
                        }
                        writer.close()
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Файл сохранён: ${file.name}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Сохранить CSV", maxLines = 1)
            }
            Button(
                onClick = onUpload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Отправить в Google Drive", maxLines = 1)
            }
        }
    }
}
