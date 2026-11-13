package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AppSettings
import com.example.domain.ApiEndpointMode
import com.example.domain.UiResult
import com.example.ui.components.openOverlayPermissionSettings
import com.example.ui.theme.ActionLight
import com.example.ui.theme.ActionLightInk
import com.example.ui.theme.ElixirAccent
import com.example.ui.theme.GraphiteActive
import com.example.ui.theme.GraphiteBg
import com.example.ui.theme.GraphiteHairline
import com.example.ui.theme.GraphiteHairlineStrong
import com.example.ui.theme.GraphiteRaised
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerSurface
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusReadySurface
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    apiTestResult: UiResult<String>,
    onSaveApiConfig: (token: String, mode: ApiEndpointMode, customUrl: String, myTag: String) -> Unit,
    onTestApiAndSyncCards: (token: String, mode: ApiEndpointMode, customUrl: String) -> Unit,
    onSaveOverlayPrefs: (opacity: Float, scale: Float, startingElixir: Float, autoPhaseSwitch: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var tokenInput by remember(settings.apiToken) { mutableStateOf(settings.apiToken) }
    var selectedMode by remember(settings.endpointMode) { mutableStateOf(settings.endpointMode) }
    var customUrlInput by remember(settings.customBaseUrl) { mutableStateOf(settings.customBaseUrl) }
    var myTagInput by remember(settings.myPlayerTag) { mutableStateOf(settings.myPlayerTag) }
    var showToken by remember { mutableStateOf(false) }

    var opacity by remember(settings.overlayOpacity) { mutableFloatStateOf(settings.overlayOpacity) }
    var scale by remember(settings.overlayScale) { mutableFloatStateOf(settings.overlayScale) }
    var startingElixir by remember(settings.startingElixir) { mutableFloatStateOf(settings.startingElixir) }
    var autoPhase by remember(settings.autoPhaseSwitch) { mutableStateOf(settings.autoPhaseSwitch) }
    var copiedIpNotice by remember { mutableStateOf(false) }

    val canDrawOverlays = Settings.canDrawOverlays(context)
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // RoyaleAPI Proxy Guide Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GraphiteSurface,
                    border = BorderStroke(1.dp, GraphiteHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Подключение через прокси RoyaleAPI",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "На портале developer.clashroyale.com создайте ключ с IP прокси RoyaleAPI, чтобы токен работал при смене мобильной сети и Wi-Fi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GraphiteBg,
                            border = BorderStroke(1.dp, GraphiteHairlineStrong),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable {
                                    clipboard.setText(AnnotatedString("45.79.218.79"))
                                    copiedIpNotice = true
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "45.79.218.79",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = InkPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Скопировать IP",
                                    tint = InkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (copiedIpNotice) "Скопировано" else "Копировать IP",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                        }
                    }
                }
            }

            // API Endpoint & Token Config Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GraphiteSurface,
                    border = BorderStroke(1.dp, GraphiteHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Параметры Clash Royale API",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ApiEndpointMode.entries.forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMode = mode }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ActionLight,
                                        unselectedColor = InkMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = InkPrimary
                                    )
                                    if (mode.baseUrl.isNotBlank()) {
                                        Text(
                                            text = mode.baseUrl,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = InkMuted
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedMode == ApiEndpointMode.CUSTOM_PROXY) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = { customUrlInput = it },
                                label = { Text("URL прокси с /v1/") },
                                placeholder = { Text("https://my-proxy.example.com/v1/") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GraphiteHairlineStrong,
                                    unfocusedBorderColor = GraphiteHairline,
                                    focusedTextColor = InkPrimary,
                                    unfocusedTextColor = InkPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("Bearer токен") },
                            placeholder = { Text("JWT-токен из портала разработчика") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = InkSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        imageVector = if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Показать или скрыть токен",
                                        tint = InkSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GraphiteHairlineStrong,
                                unfocusedBorderColor = GraphiteHairline,
                                focusedTextColor = InkPrimary,
                                unfocusedTextColor = InkPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_token_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = myTagInput,
                            onValueChange = { myTagInput = it },
                            label = { Text("Ваш тег для журнала боёв") },
                            placeholder = { Text("#2PP") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GraphiteHairlineStrong,
                                unfocusedBorderColor = GraphiteHairline,
                                focusedTextColor = InkPrimary,
                                unfocusedTextColor = InkPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_my_tag_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    onSaveApiConfig(tokenInput, selectedMode, customUrlInput, myTagInput)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GraphiteHairlineStrong),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("settings_save_button")
                            ) {
                                Text("Сохранить", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                            }
                            Button(
                                onClick = {
                                    onSaveApiConfig(tokenInput, selectedMode, customUrlInput, myTagInput)
                                    onTestApiAndSyncCards(tokenInput, selectedMode, customUrlInput)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ActionLight,
                                    contentColor = ActionLightInk
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("settings_test_api_button")
                            ) {
                                if (apiTestResult is UiResult.Loading) {
                                    CircularProgressIndicator(
                                        color = ActionLightInk,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Text("Проверить API", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        when (apiTestResult) {
                            is UiResult.Success -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusReadySurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = apiTestResult.data,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusReady,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            is UiResult.Error -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StatusDangerSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = apiTestResult.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusDanger,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }

            // Overlay HUD Customization
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GraphiteSurface,
                    border = BorderStroke(1.dp, GraphiteHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Параметры оверлея и таймера",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Непрозрачность окна",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(opacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkPrimary
                            )
                        }
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.25f..0.95f,
                            onValueChangeFinished = {
                                onSaveOverlayPrefs(opacity, scale, startingElixir, autoPhase)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = ActionLight,
                                activeTrackColor = ActionLight,
                                inactiveTrackColor = GraphiteRaised
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Масштаб окна",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(scale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkPrimary
                            )
                        }
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.75f..1.15f,
                            onValueChangeFinished = {
                                onSaveOverlayPrefs(opacity, scale, startingElixir, autoPhase)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = ActionLight,
                                activeTrackColor = ActionLight,
                                inactiveTrackColor = GraphiteRaised
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Стартовый эликсир при сбросе",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0f to "0.0", 5f to "5.0 · стандарт", 7f to "7.0").forEach { (value, label) ->
                                val active = startingElixir == value
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) GraphiteActive else GraphiteRaised,
                                    border = BorderStroke(
                                        1.dp,
                                        if (active) GraphiteHairlineStrong else GraphiteHairline
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clickable {
                                            startingElixir = value
                                            onSaveOverlayPrefs(opacity, scale, startingElixir, autoPhase)
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) InkPrimary else InkSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Автоматическая смена фаз x1 · x2 · x3",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = InkPrimary
                                )
                                Text(
                                    text = "Переход на x2 в 2:00 и на x3 в 4:00",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                            Switch(
                                checked = autoPhase,
                                onCheckedChange = {
                                    autoPhase = it
                                    onSaveOverlayPrefs(opacity, scale, startingElixir, autoPhase)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ActionLightInk,
                                    checkedTrackColor = ActionLight,
                                    uncheckedThumbColor = InkSecondary,
                                    uncheckedTrackColor = GraphiteRaised
                                )
                            )
                        }
                    }
                }
            }

            // Permissions Checklist
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GraphiteSurface,
                    border = BorderStroke(1.dp, GraphiteHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Разрешения Android",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PermissionStatusRow(
                            title = "Поверх других приложений",
                            granted = canDrawOverlays,
                            onRequest = { openOverlayPermissionSettings(context) }
                        )
                        PermissionStatusRow(
                            title = "Уведомление фонового сервиса",
                            granted = hasNotificationPermission,
                            onRequest = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                }
            }

            // GitHub Actions APK Download Card
            item {
                var githubRepoInput by remember { mutableStateOf("ArticProject/Clashgll") }
                val cleanRepo = githubRepoInput
                    .trim()
                    .removePrefix("https://github.com/")
                    .removePrefix("http://github.com/")
                    .trim('/')

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GraphiteSurface,
                    border = BorderStroke(1.dp, GraphiteHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Скачивание APK через GitHub",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "В проект добавлен workflow .github/workflows/build-apk.yml. После отправки кода на GitHub готовый файл RoyalePulse.apk автоматически публикуется в разделе Releases и Actions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = githubRepoInput,
                            onValueChange = { githubRepoInput = it },
                            label = { Text("Ваш репозиторий GitHub") },
                            placeholder = { Text("username/royalepulse") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GraphiteHairlineStrong,
                                unfocusedBorderColor = GraphiteHairline,
                                focusedTextColor = InkPrimary,
                                unfocusedTextColor = InkPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_github_repo_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (cleanRepo.contains("/")) {
                                        val apkUrl = "https://github.com/$cleanRepo/releases/latest/download/RoyalePulse.apk"
                                        runCatching {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(apkUrl)
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                enabled = cleanRepo.contains("/"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ActionLight,
                                    contentColor = ActionLightInk
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("settings_download_github_apk")
                            ) {
                                Text("Скачать APK", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (cleanRepo.contains("/")) {
                                        val actionsUrl = "https://github.com/$cleanRepo/actions"
                                        runCatching {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(actionsUrl)
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                enabled = cleanRepo.contains("/"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GraphiteHairlineStrong),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Открыть Actions", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GraphiteRaised,
        border = BorderStroke(1.dp, GraphiteHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (granted) StatusReady else InkMuted)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = InkPrimary,
                modifier = Modifier.weight(1f)
            )
            if (!granted) {
                OutlinedButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GraphiteHairlineStrong),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Выдать", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                }
            } else {
                Text(
                    text = "Выдано",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
            }
        }
    }
}
