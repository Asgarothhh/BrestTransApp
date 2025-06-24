package com.example.bresttransapp

import android.content.Context
import android.widget.Toast
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object DriveUploader {

    suspend fun uploadCsvToDrive(
        context: Context,
        records: List<TransportRecord>,
        folderId: String,
        credential: GoogleAccountCredential
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (records.isEmpty()) return@withContext false

            // Создание временного CSV-файла
            val tempFile = java.io.File(context.cacheDir, "records_${System.currentTimeMillis()}.csv")
            val writer = OutputStreamWriter(FileOutputStream(tempFile), Charsets.UTF_8)

            writer.write("Время,Регистрационный номер,Маршрут,Тип,Текущая,Следующая,Заполненность остановки,Заполненность транспорта,Вошло,Вышло,Широта,Долгота,Погода\n")
            for (record in records) {
                writer.write("${record.time},${record.vehicleNumber},${record.routeNumber},${record.type}," +
                        "${record.currentStop},${record.nextStop},${record.peopleAtStop}," +
                        "${record.peopleInTransport},${record.entered},${record.exited}," +
                        "${record.latitude},${record.longitude},${record.weather}\n")
            }
            writer.close()

            // Создание Drive API клиента
            val driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("BrestTransApp").build()

            // Создание метаданных файла
            val gDriveFile = File().apply {
                name = "История_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                parents = listOf(folderId)
            }

            val fileContent = FileContent("text/csv", tempFile)

            // Загрузка на Google Диск
            driveService.files().create(gDriveFile, fileContent)
                .setFields("id")
                .execute()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка при загрузке: ${e.message}", Toast.LENGTH_LONG).show()
            }
            false
        }
    }
}
