package com.soundsing.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.soundsing.app.service.AudioRouteService
import com.soundsing.app.ui.components.AppPermissions
import com.soundsing.app.ui.components.PermissionRequester
import com.soundsing.app.ui.navigation.SoundSingNavGraph
import com.soundsing.app.ui.theme.SoundSingTheme
import com.soundsing.app.viewmodel.SoundSingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SoundSingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            SoundSingTheme {
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()

                val projectionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        try {
                            val projection = mediaProjectionManager.getMediaProjection(
                                result.resultCode,
                                result.data!!
                            )
                            AudioRouteService.activeMediaProjection = projection

                            val serviceIntent = Intent(this, AudioRouteService::class.java).apply {
                                action = AudioRouteService.ACTION_START
                                putExtra(
                                    AudioRouteService.EXTRA_TARGET_DEVICE_ID,
                                    uiState.selectedOutputDevice?.id ?: -1
                                )
                            }
                            ContextCompat.startForegroundService(this, serviceIntent)
                            viewModel.bindService()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this,
                                "Failed to initialize capture session: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Audio capture consent was declined.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                PermissionRequester(
                    onAllGranted = {
                        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                        projectionLauncher.launch(captureIntent)
                    }
                ) { requestPermissionsAndStart ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        SoundSingNavGraph(
                            navController = navController,
                            uiState = uiState,
                            onStartRouting = {
                                if (AppPermissions.hasAllPermissions(this@MainActivity)) {
                                    val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                                    projectionLauncher.launch(captureIntent)
                                } else {
                                    requestPermissionsAndStart()
                                }
                            },
                            onStopRouting = {
                                viewModel.stopRouting()
                            },
                            onVolumeChange = { vol ->
                                viewModel.setVolume(vol)
                            },
                            onDelayChange = { delay ->
                                viewModel.setDelayMs(delay)
                            },
                            onSelectDevice = { dev ->
                                viewModel.selectDevice(dev)
                            },
                            onRefreshDevices = {
                                viewModel.refreshDevices()
                            },
                            onRefreshAudit = {
                                viewModel.loadCompatibilityReport()
                            },
                            onDismissError = {
                                viewModel.clearError()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService()
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService()
    }
}
