package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.DeckCard
import com.example.overlay.ElixirOverlayService
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
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusReady

@Composable
fun DeckCardTile(
    card: DeckCard,
    modifier: Modifier = Modifier,
    displayCost: Int = card.elixirCost,
    isInHand: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val borderColor = if (isInHand) GraphiteHairlineStrong else GraphiteHairline

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .aspectRatio(0.78f)
            .then(
                if (onClick != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (card.activeIconUrl.isNotBlank()) {
                AsyncImage(
                    model = card.activeIconUrl,
                    contentDescription = stringResource(R.string.cd_card_icon, card.name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp)
                        .background(GraphiteRaised)
                ) {
                    Text(
                        text = card.name.take(3),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = InkSecondary
                    )
                }
            }

            // Bottom Card Name Strip (>=11sp per unslop Pass 6)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(GraphiteBg.copy(alpha = 0.92f))
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = card.name,
                    fontSize = 11.sp,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Elixir Cost Badge (20x20, 6dp nested radius)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElixirAccent)
            ) {
                Text(
                    text = displayCost.toString(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            // Evolution indicator dot (quiet status dot instead of decorative icon)
            if (card.evolutionLevel > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ElixirAccent)
                )
            }
        }
    }
}

@Composable
fun MissingTokenBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GraphiteSurface,
        border = BorderStroke(1.dp, GraphiteHairlineStrong),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = InkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Не указан токен Clash Royale API",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Добавьте ключ с IP 45.79.218.79 для загрузки колод",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionLight,
                    contentColor = ActionLightInk
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(36.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Text("Настроить", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun OverlayControlAction(
    isOverlayActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canDrawOverlays = Settings.canDrawOverlays(context)

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (Settings.canDrawOverlays(context)) {
            ElixirOverlayService.start(context)
        }
    }

    val launchOverlay: () -> Unit = {
        if (isOverlayActive) {
            ElixirOverlayService.stop(context)
        } else if (!Settings.canDrawOverlays(context)) {
            openOverlayPermissionSettings(context)
        } else {
            val needsNotificationPerm =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED

            if (needsNotificationPerm) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ElixirOverlayService.start(context)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isOverlayActive) GraphiteRaised else GraphiteSurface,
        border = BorderStroke(
            1.dp,
            if (isOverlayActive) GraphiteHairlineStrong else GraphiteHairline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                if (isOverlayActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusReady)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOverlayActive) "Оверлей запущен поверх игры" else "Плавающий оверлей поверх игры",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        isOverlayActive -> "Перетащите окно в угол арены"
                        !canDrawOverlays -> "Нужно разрешение «Поверх других приложений»"
                        else -> "Таймер эликсира, цикл 8 карт и авто-скан"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isOverlayActive) {
                OutlinedButton(
                    onClick = launchOverlay,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GraphiteHairlineStrong),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Скрыть", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                }
            } else {
                Button(
                    onClick = launchOverlay,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionLight,
                        contentColor = ActionLightInk
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = if (canDrawOverlays) "Запуск" else "Разрешить",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

fun openOverlayPermissionSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun ensureAndStartOverlay(context: Context): Boolean {
    return if (Settings.canDrawOverlays(context)) {
        ElixirOverlayService.start(context)
        true
    } else {
        openOverlayPermissionSettings(context)
        false
    }
}
