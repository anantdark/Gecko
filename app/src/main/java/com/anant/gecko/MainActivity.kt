package com.anant.gecko

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anant.gecko.ui.home.HomeScreen
import com.anant.gecko.ui.pack.GeckoButton
import com.anant.gecko.ui.pack.GeckoButtonTone
import com.anant.gecko.ui.pack.GeckoText
import com.anant.gecko.ui.settings.SettingsScreen
import com.anant.gecko.ui.theme.GeckoTheme
import com.anant.gecko.ui.theme.LocalGeckoTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: GeckoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeId by viewModel.themeId.collectAsStateWithLifecycle()
            GeckoTheme(themeId = themeId) {
                GeckoApp(
                    viewModel = viewModel,
                    onOpenFile = ::openFile,
                    onShareFile = ::shareFile,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasMicPermission()) {
            viewModel.bind()
        }
    }

    override fun onStop() {
        // Do NOT morph the launcher icon here — disabling the alias the user just left
        // makes the home shortcut report "app isn't installed" until the launcher refreshes.
        viewModel.unbind()
        super.onStop()
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/wav")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }
}

private enum class Screen { Home, Settings }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun GeckoApp(
    viewModel: GeckoViewModel,
    onOpenFile: (File) -> Unit,
    onShareFile: (File) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeId by viewModel.themeId.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.Home) }

    val permissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.bind()
        }
    }

    if (!permissionState.allPermissionsGranted) {
        PermissionGate(onRequest = { permissionState.launchMultiplePermissionRequest() })
        return
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            state = state,
            onToggleListen = viewModel::toggleListen,
            onSave = viewModel::save,
            onSaveAndContinue = viewModel::saveAndContinue,
            onStopRecording = viewModel::stopRecording,
            onOpenSettings = { screen = Screen.Settings },
            onOpenFile = onOpenFile,
            onShareFile = onShareFile,
            onDismissSaved = viewModel::dismissSaved,
        )
        Screen.Settings -> {
            val iconMorph by viewModel.iconMorph.collectAsStateWithLifecycle()
            SettingsScreen(
                memoryBytes = state.memoryBytes,
                sampleRate = state.sampleRate,
                totalMemorySeconds = state.totalMemorySeconds,
                captureMode = state.mode,
                themeId = themeId,
                iconMorph = iconMorph,
                onBack = { screen = Screen.Home },
                onMemorySelected = viewModel::setMemory,
                onQualitySelected = viewModel::setSampleRate,
                onThemeSelected = viewModel::setTheme,
                onIconMorphSelected = viewModel::setIconMorph,
            )
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    val pack = LocalGeckoTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        GeckoText("Gecko", pack.type.brand, pack.colors.ink)
        GeckoText(
            "Needs the microphone to keep a rolling memory of sound — nothing leaves your device.",
            pack.type.body,
            pack.colors.inkMuted,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        GeckoButton(
            label = "Allow microphone",
            onClick = onRequest,
            tone = GeckoButtonTone.Accent,
            leadingIcon = R.drawable.ic_ph_microphone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
