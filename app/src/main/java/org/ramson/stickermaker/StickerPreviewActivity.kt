package org.ramson.stickermaker

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import org.ramson.stickermaker.ui.theme.StickerMakerTheme
import java.io.File

class StickerPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = (intent.getStringExtra("file")!!)
        if (!File(filePath).exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            finish()
        }
        val bitmap = BitmapFactory.decodeFile(filePath)

        if (bitmap == null) {
            Toast.makeText(this, "bitmap not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        setContent {
            val iBitmap = bitmap.asImageBitmap()
            StickerMakerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Image(bitmap = iBitmap, contentDescription = "")
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(activity: Activity, file: File): Intent {
            return Intent(activity, StickerPreviewActivity::class.java).apply {
                putExtra("file", file.absolutePath)
            }
        }
    }
}
