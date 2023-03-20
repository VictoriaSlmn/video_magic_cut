package com.example.video_magic_cut

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.video_magic_cut.ui.GallerySelectMode.MP4
import com.example.video_magic_cut.ui.GallerySelectMode.NONE
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {

    private lateinit var findInterestingVideoParts: FindInterestingVideoParts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findInterestingVideoParts = FindInterestingVideoParts(applicationContext)
        setContent {
            Content()
        }
    }

    @Composable
    fun Content() {
        var magic by remember { mutableStateOf(false) }
        var outputVideoUri by remember { mutableStateOf(Uri.EMPTY) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(32.dp)
            ) {
                when {
                    magic -> ProgressIndicator()
                    outputVideoUri == Uri.EMPTY -> SelectionMenu(
                        onMagicStarted = { magic = true },
                        onMagicFinished = { outputUri ->
                            magic = false
                            outputVideoUri = outputUri
                        }
                    )
                    else -> MagicResult(outputVideoUri) { outputVideoUri = Uri.EMPTY }
                }
            }
        }
    }

    @Composable
    private fun MagicResult(outputVideoUri: Uri, onStartNewMagic: () -> Unit) {
        Text(
            stringResource(R.string.output_video_label) + outputVideoUri.path,
            modifier = Modifier.padding(16.dp)
        )

        CreateButton(stringResource(R.string.repeat_magic_button)) {
            onStartNewMagic()
        }
    }

    @Composable
    private fun ProgressIndicator() =
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

    @Composable
    private fun SelectionMenu(
        onMagicStarted: () -> Unit,
        onMagicFinished: (Uri) -> Unit,
    ) {
        var videoUri by remember { mutableStateOf(Uri.EMPTY) }
        var gallerySelectMode by remember { mutableStateOf(NONE) }

        when (gallerySelectMode) {
            MP4 -> GallerySelect(
                mode = gallerySelectMode,
                onUri = { uri ->
                    gallerySelectMode = NONE
                    videoUri = uri
                }
            )
            NONE -> {
                if (videoUri == Uri.EMPTY) {
                    CreateButton(stringResource(R.string.select_video_button)) {
                        gallerySelectMode = MP4
                    }
                } else {
                    Text(
                        stringResource(R.string.video_label) + videoUri.path,
                        modifier = selectionMenuPadding
                    )
                }

                if (videoUri != Uri.EMPTY) {
                    CreateButton(stringResource(R.string.magic_button)) {
                        onMagicStarted()
                        CoroutineScope(Dispatchers.Main).launch {
                            val output =
                                withContext(Dispatchers.Default) {
                                    findInterestingVideoParts.generateOptimisedVideo(videoUri)
                                }
                            onMagicFinished(output)
                        }
                    }
                }
            }
            else -> throw RuntimeException("Not supported media type $gallerySelectMode")
        }
    }

    @Composable
    private fun CreateButton(name: String, onClick: () -> Unit) =
        Button(
            modifier = selectionMenuPadding,
            onClick = {
                onClick()
            }
        ) {
            Text(name)
        }

    private val selectionMenuPadding = Modifier.padding(4.dp)
}
