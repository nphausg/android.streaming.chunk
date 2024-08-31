package com.nphausg.app.streaming.chunked

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nphausg.app.streaming.chunked.server.BFFServer
import com.nphausg.app.streaming.chunked.ui.theme.ComposeTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.network.tls.CIOCipherSuites
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val getRunningServerInfo = { ticks: Int ->
    "The server is running on: ${Build.MODEL} at ${BFFServer.url} -> (${ticks}s ....)"
}

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Localhost for Android emulator
        val audioUrl = "${BFFServer.url}${BFFServer.STREAMING}"

        setContent {
            ComposeTheme {
                val scope = rememberCoroutineScope()
                val snackBarHostState = remember { SnackbarHostState() }
                var hasPlayed by remember { mutableStateOf(false) }
                LaunchedEffect(hasPlayed) {
                    scope.launch {
                        if (hasPlayed) {
                            snackBarHostState.showSnackbar("Play")
                        } else {
                            snackBarHostState.showSnackbar("Error")
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackBarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            stopAudio()
                            playAudio(
                                url = audioUrl,
                                onPrepareAsync = {
                                    scope.launch {
                                        snackBarHostState.showSnackbar(audioUrl)
                                    }
                                },
                                onSuccess = {
                                    hasPlayed = true
                                },
                                onFailure = {
                                    hasPlayed = false
                                }
                            )
                        }) {
                            if (hasPlayed) {
                                Icon(Icons.Filled.Done, "")
                            } else {
                                Icon(Icons.Filled.PlayArrow, "")
                            }
                        }
                    },
                ) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BFFServer.startStreaming()
    }

    private fun playAudio(
        url: String,
        onPrepareAsync: () -> Unit,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        try {
            mediaPlayer?.apply {
                // on below line we are setting audio stream type as
                // stream music on below line.
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                // on below line we are setting audio source
                // as audio url on below line.
                setDataSource(url)
                // on below line we are preparing
                // our media player.
                prepareAsync() // Use prepareAsync() for large files
                // on below line we are starting
                // our media player.
                onPrepareAsync()
                setOnPreparedListener {
                    // Start playback when the MediaPlayer is ready
                    start()
                    onSuccess()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onFailure()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.takeIf { it.isPlaying }?.let {
            // if media player is playing
            // we are stopping it on below line.
            it.stop()
            // on below line we are resetting
            // our media player.
            it.reset()
            // on below line we are calling release
            // to release our media player.
            it.release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // BFFServer.stop()
        stopAudio()
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    var ticks by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds)
            ticks++
        }
    }

    var hasStarted by remember { mutableStateOf(false) }
    val value by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0.9f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2500, easing = LinearEasing
            ), repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 20.dp, alignment = Alignment.CenterVertically
        )
    ) {
        val reusedModifier = Modifier.weight(1f)
        Spacer(modifier = reusedModifier)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dp(36f))
        ) {
            Button(enabled = !hasStarted, modifier = reusedModifier, onClick = {
                hasStarted = true
                BFFServer.start()
            }, content = { Text("Start") })
            Spacer(modifier = Modifier.weight(0.1f))
            Button(enabled = hasStarted, modifier = reusedModifier, onClick = {
                ticks = 0
                hasStarted = false
                BFFServer.stop()
            }, content = { Text("Stop") })
        }

        Column(modifier = Modifier.height(4.dp)) {
            if (hasStarted) {
                LinearProgressIndicator(
                    modifier = Modifier.width(64.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        Text(
            modifier = Modifier.graphicsLayer {
                if (hasStarted) {
                    scaleX = value
                    scaleY = value
                }
            },
            color = Color.Black,
            textAlign = TextAlign.Center,
            text = if (hasStarted) {
                getRunningServerInfo(ticks)
            } else {
                "Please click 'Start' to start the embedded server"
            },
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = reusedModifier)
    }

}

@Composable
@Preview(showBackground = true)
fun GreetingPreview() {
    ComposeTheme {
        MainScreen()
    }
}