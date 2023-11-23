package org.ramson.stickermaker

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import org.ramson.stickermaker.ui.theme.StickerMakerTheme
import org.ramson.stickermaker.ui.theme.satoshiFont
import java.io.File

class SavedStickersActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val folder = File(filesDir, "Stickers")
        val files = folder.listFiles()
        setContent {
            StickerMakerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(Modifier.fillMaxSize()) {
                        if (files != null) {
                            if (files.isNotEmpty())
                                LazyVerticalGrid(GridCells.Adaptive(100.dp)) {
                                    items(files.size) {
                                        val file: File? = files[it]
                                        if (file != null)
                                            StickerItem(file = file) { clickedFile ->
                                                startActivity(
                                                    StickerPreviewActivity.newIntent(
                                                        this@SavedStickersActivity,
                                                        clickedFile
                                                    )
                                                )
                                            }
                                    }
                                }
                            else Text(
                                text = "No stickers saved",
                                modifier = Modifier.padding(10.dp),
                                fontFamily = satoshiFont
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(activity: Activity): Intent {
            return Intent(activity, SavedStickersActivity::class.java)
        }
    }
}

@Composable
fun StickerItem(
    file: File,
    onFileClicked: (File) -> Unit = {}
) {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val iBitmap = bitmap.asImageBitmap()
    Image(
        bitmap = iBitmap,
        contentDescription = "",
        modifier = Modifier
            .size(100.dp)
            .padding(5.dp)
            .clip(
                RoundedCornerShape(20)
            )
            .clickable {
                onFileClicked(file)
            }
    )
}
