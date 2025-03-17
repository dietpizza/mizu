package com.kepsake.mizu.ui.screens

import UserDataStore
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.viewmodels.MangaFileViewModel
import com.kepsake.mizu.ui.components.MangaCard
import com.kepsake.mizu.utils.extractCoverImage
import com.kepsake.mizu.utils.getFilePathFromUri
import com.kepsake.mizu.utils.getMangaFiles
import com.kepsake.mizu.utils.getZipPageCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

val TAG = "LibraryTab"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTab(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: MangaFileViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userDataStore = remember { UserDataStore(context) }
    val libraryPath by userDataStore.getString("lib.path").collectAsState(initial = "")
    val mangaFiles by viewModel.allMangaFiles.observeAsState(listOf())

    var isLoading by remember { mutableStateOf(false) }
    var currentDirectoryUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun syncLibrary(path: String?) {
        Log.e(TAG, "Sync Begin ${path}")
        // Use withContext instead of launch to make it wait
        withContext(Dispatchers.IO) {
            if (path != null || libraryPath.isNotEmpty()) {
                val target = path ?: libraryPath

                try {
                    val mangaUris = scanForManga(target)
                    val _mangaFiles = mangaUris.mapNotNull { processManga(context, it) }
                    viewModel.insertAll(_mangaFiles)

                } catch (e: Exception) {
                    errorMessage = "Error loading files: ${e.message}"
                }
            }
        }
        Log.e(TAG, "Sync End")
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            currentDirectoryUri = it

            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)

            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                val path = getFilePathFromUri(context, uri)

                if (path != null) {
                    UserDataStore(context).saveString("lib.path", path)
                    syncLibrary(path)
                }
                isLoading = false
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { },
        Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            directoryPickerLauncher.launch(null)
                        }) {
                            Text("Retry")
                        }
                    }
                }

                (mangaFiles.isEmpty() && !isLoading) -> {
                    val buttonText =
                        if (libraryPath.isNotEmpty()) "Change Folder" else "Select Folder"
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No comic files found",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { directoryPickerLauncher.launch(null) }) {
                                Text(buttonText)
                            }
                            if (!libraryPath.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { directoryPickerLauncher.launch(null) }) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding()
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(mangaFiles, key = { it.id }) { comicFile ->
                            MangaCard(comicFile)
                        }
                    }
                }
            }

        }
    }
}

suspend fun processManga(context: Context, path: String): MangaFile? =
    withContext(Dispatchers.IO) {
        try {
            val name = File(path).name

            // Generate unique ID for each comic file for caching
            val id = UUID.randomUUID().toString()
            val coverPath = extractCoverImage(context, id, path)
            val totalPages = getZipPageCount(path)

            if (coverPath != null) {
                return@withContext MangaFile(id, path, name, coverPath, 0, totalPages)
            }
            null

        } catch (e: Exception) {
            // Log error but continue processing other files
            Log.e("Library", "Error processing file ${path}", e)
            null
        }
    }

suspend fun scanForManga(path: String): List<String> =
    withContext(Dispatchers.IO) {
        getMangaFiles(path).map { it["path"] as String }.toList()
    }
