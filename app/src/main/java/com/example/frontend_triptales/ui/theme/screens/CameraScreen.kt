package com.example.frontend_triptales.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberImagePainter
import java.io.File
import java.io.IOException

@Composable
fun CameraScreen() {
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) {
            Toast.makeText(context, "Foto non scattata.", Toast.LENGTH_SHORT).show()
        }
    }

    val imageFile = createImageFile(context)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        photoUri?.let {
            Image(
                painter = rememberImagePainter(it),
                contentDescription = "Anteprima foto",
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                imageFile?.let { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    photoUri = uri
                    launcher.launch(uri)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scatta Foto", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (photoUri != null) {
                    Toast.makeText(context, "Foto condivisa con il gruppo!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nessuna foto da condividere.", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Condividi con il gruppo", fontSize = 18.sp)
        }
    }
}

fun createImageFile(context: Context): File? {
    val imageFileName = "JPEG_${System.currentTimeMillis()}_.jpg"
    val storageDir = context.cacheDir
    return try {
        File.createTempFile(imageFileName, ".jpg", storageDir)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun CameraPreview() {
    CameraScreen()
}
