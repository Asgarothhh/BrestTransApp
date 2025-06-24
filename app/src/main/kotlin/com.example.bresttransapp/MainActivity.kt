package com.example.bresttransapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.bresttransapp.ui.theme.BrestTransAppTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn

class MainActivity : ComponentActivity() {

    // Регистрируем обработчик результата входа в аккаунт Google
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data

            // Обрабатываем результат входа
            DriveAuthHelper.handleSignInResult(data) { account ->
                account?.let {
                    // Сохраняем имя аккаунта в SharedPreferences
                    val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("accountName", it.account?.name).apply()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем поддержку прозрачных системных баров (статусбар и навбар)
        enableEdgeToEdge()

        // Получаем SharedPreferences для хранения состояния регистрации и аккаунта
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Проверяем, был ли пользователь уже зарегистрирован ранее
        val isRegistered = prefs.getBoolean("is_registered", false)

        // Проверяем, уже ли выполнен вход в Google аккаунт
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            // Если нет — запускаем интент входа
            val signInIntent = DriveAuthHelper.getSignInIntent(this)
            signInLauncher.launch(signInIntent)
        } else {
            // Если аккаунт найден — сохраняем имя в prefs
            prefs.edit().putString("accountName", account.account?.name).apply()
        }

        // Устанавливаем Compose-контент
        setContent {
            // Применяем тему оформления
            BrestTransAppTheme {
                // Поверхностный контейнер с фоном
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Запускаем навигацию с учётом, зарегистрирован ли пользователь
                    MainNavigation(startFromRegistration = !isRegistered)
                }
            }
        }
    }
}
