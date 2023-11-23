package org.ramson.stickermaker

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors
import org.ramson.stickermaker.databinding.ActivityMainBinding
import org.ramson.stickermaker.ui.theme.StickerMakerTheme

class MainActivity : AppCompatActivity() {

    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        val permissions = when{

            Build.VERSION.SDK_INT  == Build.VERSION_CODES.TIRAMISU -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.CAMERA
            )
            Build.VERSION.SDK_INT  > Build.VERSION_CODES.TIRAMISU -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                //android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.CAMERA
            )
            Build.VERSION.SDK_INT in 29 until 33 -> arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            )
            else -> arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            )

        }

        val permLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ){
            it.forEach { (permission, granted) ->
                if (!granted) {
                    Toast.makeText(
                        this,
                        "Permission $permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            startPickerActivity()
        }

        binding.createNewStickers.setOnClickListener { permLauncher.launch(permissions) }
        binding.saved.setOnClickListener {
            startActivity(
                SavedStickersActivity.newIntent(this)
            )
        }

        binding.cView.setContent {
            StickerMakerTheme{}
        }

    }

    private fun startPickerActivity() {
        startActivity(
            PickerActivity.newIntent(this)
        )
    }

}