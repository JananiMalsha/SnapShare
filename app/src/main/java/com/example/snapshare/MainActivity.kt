package com.example.snapshare

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import android.content.Intent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF00796B),     // button colour (teal)
                    onPrimary = Color.White,         // button text colour
                    background = Color(0xFFE0F2F1),  // screen background
                    error = Color(0xFFC62828)
                )
            ) {
                CameraScreen()
            }
        }
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = photoUri
            if (uri != null) {
                try {
                    val bitmap = context.contentResolver
                        .openInputStream(uri)
                        ?.use { stream -> BitmapFactory.decodeStream(stream) }

                    if (bitmap != null) {
                        photoBitmap = bitmap
                        errorMessage = null
                    } else {
                        errorMessage = "Could not load the captured photo."
                    }
                } catch (e: Exception) {
                    errorMessage = "Could not open the photo: ${e.message}"
                }
            } else {
                errorMessage = "Photo location was not available."
            }
        } else {
            errorMessage = "Photo capture was cancelled or failed."
        }
    }

    fun createPhotoUri(): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val photoFile = File.createTempFile(
            "camera_photo_",
            ".jpg",
            imagesDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Simple Camera App",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF004D40),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    try {
                        val uri = createPhotoUri()
                        photoUri = uri
                        photoBitmap = null
                        errorMessage = null
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        errorMessage = "Could not open the camera: ${e.message}"
                    }
                }
            ) {
                Text("Open Camera")
            }

            Spacer(modifier = Modifier.height(24.dp))

            photoBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )
            }
            if (photoUri != null && photoBitmap != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val uri = photoUri ?: return@Button

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = android.content.ClipData.newUri(
                                context.contentResolver,
                                "Captured photo",
                                uri
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share your photo")
                        )
                    }
                ) {
                    Text("Share")
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}