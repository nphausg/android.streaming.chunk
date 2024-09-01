package com.nphausg.app.streaming.chunked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nphausg.app.streaming.chunked.exo.PlayerSurface
import com.nphausg.app.streaming.chunked.exo.SURFACE_TYPE_TEXTURE_VIEW
import com.nphausg.app.streaming.chunked.service.StreamingService
import com.nphausg.app.streaming.chunked.ui.theme.ComposeTheme

val videos =
    listOf(
        "https://raw.githubusercontent.com/nphausg/android.streaming.chunked/main/docs/bye_bye_bye_nsync.mp3",
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/gear0/fileSequence0.aac",
        "https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-vp9-360.webm",
    )

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeTheme {
                val snackBarHostState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackBarHostState) },
                    floatingActionButton = {
                        Icon(Icons.Filled.PlayArrow, "")
                    },
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .background(Color.Red)
                            .height(200.dp)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        StreamingService.stream(
            resources.openRawResource(R.raw.bye_bye_bye_nsync).readBytes()
        )
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column {
        val context = LocalContext.current
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videos[0]))
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }
        // Manage lifecycle events
        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .then(modifier),
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(name = "Landscape", device = "spec:shape=Normal,width=640,height=360,unit=dp,dpi=480")
fun MainScreenPreview() {
    ComposeTheme {
        MainScreen()
    }
}