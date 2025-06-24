package com.example.bresttransapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

// Данные остановок из JSON
data class StopEntry(val name: String, val moveto: String, val x: String, val y: String)

// Запись о транспорте
data class TransportRecord(
    val time: String,
    val vehicleNumber: String,
    val routeNumber: String,
    val type: String,
    val currentStop: String,
    val nextStop: String,
    val peopleAtStop: String,
    val peopleInTransport: String,
    val entered: String,
    val exited: String,
    val latitude: String,
    val longitude: String,
    val weather: String
)

// Ответ от OpenWeatherMap
data class WeatherResponse(val weather: List<Weather>, val main: Main)
data class Weather(val main: String, val description: String)
data class Main(val temp: Float)

interface WeatherApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("appid") apiKey: String,
        @Query("lang") lang: String = "ru",
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

suspend fun fetchWeather(lat: String, lon: String, context: Context): String {
    return try {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(WeatherApi::class.java)
        val response = api.getCurrentWeather(lat, lon, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
        val description = response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Неизвестно"
        val temperature = response.main.temp
        "$description, ${temperature}°C"
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка загрузки погоды", Toast.LENGTH_SHORT).show()
        "Ошибка"
    }
}

@Composable
fun DropdownMenuBox(selectedValue: String, onValueChange: (String) -> Unit, options: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Тип транспорта") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onValueChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TransportDataScreen(modifier: Modifier = Modifier, onSave: (TransportRecord) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val stops = remember { loadStops(context) }

    var vehicleNumber by remember { mutableStateOf("") }
    var routeNumber by remember { mutableStateOf("") }
    var transportType by remember { mutableStateOf("Автобус") }
    var currentStop by remember { mutableStateOf("") }
    var nextStop by remember { mutableStateOf("") }
    var peopleAtStop by remember { mutableStateOf("") }
    var peopleInTransport by remember { mutableStateOf("") }
    var entered by remember { mutableStateOf("") }
    var exited by remember { mutableStateOf("") }

    val nextStopOptions = stops.filter { it.name == currentStop }.map { it.moveto }.distinct()

    val allFieldsFilled = listOf(
        peopleAtStop,
        peopleInTransport,
        entered,
        exited
    ).all { it.isNotBlank() && it.matches(Regex("\\d+")) } &&
            listOf(vehicleNumber, routeNumber, currentStop, nextStop, transportType).all { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = vehicleNumber,
            onValueChange = { vehicleNumber = it },
            label = { Text("Регистрационный номер") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = routeNumber,
            onValueChange = { routeNumber = it },
            label = { Text("Номер маршрута") },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenuBox(
            selectedValue = transportType,
            onValueChange = { transportType = it },
            options = listOf("Автобус", "Троллейбус", "Маршрутка", "Сочленённый автобус", "Заказной автобус")
        )

        AutoCompleteTextField("Текущая остановка", currentStop, { currentStop = it }, stops.map { it.name }.distinct())
        AutoCompleteTextField("Следующая остановка", nextStop, { nextStop = it }, nextStopOptions)

        OutlinedTextField(
            value = peopleAtStop,
            onValueChange = { if (it.all(Char::isDigit)) peopleAtStop = it },
            label = { Text("Заполненность остановки") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = peopleInTransport,
            onValueChange = { if (it.all(Char::isDigit)) peopleInTransport = it },
            label = { Text("Заполненность транспорта") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = entered,
            onValueChange = { if (it.all(Char::isDigit)) entered = it },
            label = { Text("Вошло") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = exited,
            onValueChange = { if (it.all(Char::isDigit)) exited = it },
            label = { Text("Вышло") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (!allFieldsFilled) {
                    Toast.makeText(context, "Пожалуйста, заполните все поля корректно", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                coroutineScope.launch {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val time = sdf.format(Date())

                    val stopEntry = stops.find { it.name == currentStop && it.moveto == nextStop }
                        ?: stops.find { it.name == currentStop }

                    if (stopEntry == null) {
                        Toast.makeText(context, "Не найдены координаты для остановки", Toast.LENGTH_LONG).show()
                    }

                    val latitude = stopEntry?.y ?: "0.0"
                    val longitude = stopEntry?.x ?: "0.0"
                    val weather = fetchWeather(latitude, longitude, context)

                    onSave(
                        TransportRecord(
                            time,
                            vehicleNumber,
                            routeNumber,
                            transportType,
                            currentStop,
                            nextStop,
                            peopleAtStop,
                            peopleInTransport,
                            entered,
                            exited,
                            latitude,
                            longitude,
                            weather
                        )
                    )
                    Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .padding(bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = allFieldsFilled
        ) {
            Text("Сохранить", maxLines = 1)
        }
    }
}

@Composable
fun AutoCompleteTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var filteredOptions by remember { mutableStateOf(emptyList<String>()) }
    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                debounceJob?.cancel()
                debounceJob = coroutineScope.launch {
                    delay(500)
                    filteredOptions = options.filter { option ->
                        option.contains(it, ignoreCase = true) && it.isNotBlank()
                    }
                    expanded = filteredOptions.isNotEmpty()
                }
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded && filteredOptions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun loadStops(context: Context): List<StopEntry> {
    return try {
        val inputStream = context.assets.open("astops_with_next.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<List<StopEntry>>() {}.type
        Gson().fromJson(reader, type)
    } catch (e: Exception) {
        emptyList()
    }
}
