package org.ramson.stickermaker

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import gun0912.tedimagepicker.builder.TedImagePicker
import kotlinx.coroutines.launch
import org.ramson.stickermaker.databinding.ActivityPickerBinding
import org.ramson.stickermaker.databinding.PickerBsLayoutBinding
import org.ramson.stickermaker.domain.BackgroundRemover
import org.ramson.stickermaker.domain.DrawView
import org.ramson.stickermaker.domain.OnBackgroundChangeListener
import org.ramson.stickermaker.domain.combineBitmapsWithPorterDuff
import org.ramson.stickermaker.ui.theme.StickerMakerTheme
import org.ramson.stickermaker.ui.theme.satoshiFont
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PickerActivity : AppCompatActivity() {

    private val binding: ActivityPickerBinding by lazy {
        ActivityPickerBinding.inflate(layoutInflater)
    }

    private var onEditModeChanged: ((Boolean) -> Unit) = { isAdding ->
        binding.drawView.isAdding = isAdding
    }
    private var maskedBitmap: Bitmap? = null

    private val cropImage = registerForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let {
                processUri(it)
            }
        } else {
            result.error?.printStackTrace()
        }
    }

    private fun processUri(it: Uri) {
        var mBitmap = it.getBitmap(this).copy(Bitmap.Config.ARGB_8888, true)
        val bitWidth = mBitmap.width
        if (bitWidth > screenWidth)
            mBitmap = mBitmap.scaleBitmapAndKeepRation(screenWidth, screenWidth)
        mBitmap =
            if (mBitmap.width >= 1080 || mBitmap.height >= 1080) ThumbnailUtils.extractThumbnail(
                mBitmap,
                1080,
                1080
            ) else mBitmap
        val bitmapPicked = mBitmap
        BackgroundRemover.bitmapForProcessing(
            bitmapPicked,
            false,
            object : OnBackgroundChangeListener {
                override fun onSuccess(bitmap: Bitmap) {

                }

                override fun onSuccessWithMask(bitmap: Bitmap, mask: Bitmap) {
                    super.onSuccessWithMask(bitmap, mask)
                    binding.drawView.setBitmap(mask)
                    maskedBitmap = bitmap
                    binding.drawView.maskAppliedListener =
                        object : DrawView.MaskAppliedListener {
                            override fun onMaskApplied(bitmap: Bitmap?) {
                                bitmap?.let { masked ->
                                    lifecycleScope.launch {
                                        val combinedImage = combineBitmapsWithPorterDuff(
                                            bitmapPicked,
                                            masked
                                        )
                                        maskedBitmap = combinedImage
                                        binding.ivResult.setImageBitmap(
                                            combinedImage
                                        )
                                    }
                                }
                            }
                        }
                    setUpCView()
                    binding.progressLayout.visibility = View.GONE
                }

                override fun onFailed(exception: Exception) {
                    exception.printStackTrace()
                }

            })

        val mutableBitmap: Bitmap = bitmapPicked.copy(Bitmap.Config.ARGB_8888, true)
        binding.ivBase.setImageBitmap(mutableBitmap)
        binding.ivBase.visibility = View.VISIBLE
        binding.drawView.setBitmap(mutableBitmap)

    }

    private val takePicture by lazy {
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it == true) {
                val cacheDirectory = cacheDir
                val tempFile = File(cacheDirectory, "temp_img.jpg")
                if (tempFile.exists()) {
                    val uri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        tempFile
                    )
                    if (uri != null)
                        cropImage.launch(
                            CropImageContractOptions(
                                uri = uri,
                                cropImageOptions = CropImageOptions(
                                    fixAspectRatio = true,
                                    aspectRatioX = 1,
                                    aspectRatioY = 1,
                                )
                            )
                        )
                }
            }
        }
    }

    private val pickPicture by lazy {
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            it?.let {
                cropImage.launch(
                    CropImageContractOptions(
                        uri = it,
                        cropImageOptions = CropImageOptions(
                            fixAspectRatio = true,
                            aspectRatioX = 1,
                            aspectRatioY = 1,
                        )
                    )
                )
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        cropImage
        takePicture
        pickPicture

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            showBottomSheet()
        else
            TedImagePicker.with(this)
                .cancelListener {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                }.start {
                    cropImage.launch(
                        CropImageContractOptions(
                            uri = it,
                            cropImageOptions = CropImageOptions(
                                fixAspectRatio = true,
                                aspectRatioX = 1,
                                aspectRatioY = 1,
                            )
                        )
                    )
                }

    }

    private fun showBottomSheet() {
        val bs = BottomSheetDialog(this)
        val pickerBsLayoutBinding = PickerBsLayoutBinding.inflate(layoutInflater)
        bs.setContentView(pickerBsLayoutBinding.root)
        pickerBsLayoutBinding.cam.setOnClickListener {
            val cacheDirectory = cacheDir
            val tempFile = File(cacheDirectory, "temp_img.jpg")
            if ((cacheDirectory.exists() || cacheDirectory.mkdirs()) && (!tempFile.exists() || tempFile.delete()))
                if (tempFile.createNewFile())
                    takePicture.launch(
                        FileProvider.getUriForFile(
                            this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            tempFile
                        )
                    )
            bs.dismiss()
        }
        pickerBsLayoutBinding.gallery.setOnClickListener {
            pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            bs.dismiss()
        }
        bs.show()

    }

    private fun setUpCView() {
        binding.cView.setContent {

            val brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xff00DDEB),
                    Color(0xff9BFF78)
                )
            )
            val brush2 = Brush.linearGradient(
                colors = listOf(
                    Color(0xff797BFF),
                    Color(0xffFF783F)
                )
            )
            val brushGrey = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xff5C5C5C),
                    Color(0xff5C5C5C),
                )
            )
            var isAdding by remember {
                mutableStateOf(true)
            }
            var sliderPosition by remember { mutableFloatStateOf(50f) }
            StickerMakerTheme(darkTheme = true, dynamicColor = false) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.brush),
                                contentDescription = "plus",
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isAdding) brush2 else brushGrey,
                                        RoundedCornerShape(20)
                                    )
                                    .size(75.dp)
                                    .padding(15.dp)
                                    .clickable {
                                        binding.ivResult.setBackgroundColor(
                                            android.graphics.Color.parseColor(
                                                "#8026FF00"
                                            )
                                        )
                                        isAdding = true
                                        onEditModeChanged(true)
                                    },
                                colorFilter = if (!isAdding) ColorFilter.tint(
                                    color = Color(
                                        0xff5C5C5C
                                    )
                                ) else null
                            )
                            Text(
                                text = "Draw",
                                modifier = Modifier.applyBrush(if (isAdding) brush2 else brushGrey)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.eraser),
                                contentDescription = "eraser",
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (!isAdding) brush2 else brushGrey,
                                        RoundedCornerShape(20)
                                    )
                                    .size(75.dp)
                                    .padding(15.dp)
                                    .clickable {
                                        binding.ivResult.setBackgroundColor(
                                            android.graphics.Color.parseColor(
                                                "#80FF0000"
                                            )
                                        )
                                        isAdding = false
                                        onEditModeChanged(false)
                                    },
                                colorFilter = if (isAdding) ColorFilter.tint(
                                    color = Color(
                                        0xff5C5C5C
                                    )
                                ) else null
                            )
                            Text(
                                text = "Eraser",
                                modifier = Modifier.applyBrush(if (!isAdding) brush2 else brushGrey)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.with_ai),
                                contentDescription = "with ai",
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        brushGrey,
                                        RoundedCornerShape(20)
                                    )
                                    .size(75.dp)
                                    .padding(15.dp)
                                    .clickable {
                                        binding.drawView.reset()
                                    },
                            )
                            Text(text = "AI Cut", modifier = Modifier.applyBrush(brush2))
                        }


                    }

                    Text(
                        text = "Brush Size", modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                            .fillMaxWidth()
                            .applyBrush(brush)
                    )
                    Slider(
                        modifier = Modifier
                            .semantics { contentDescription = "Brush Size" }
                            .padding(top = 5.dp, bottom = 15.dp, start = 10.dp, end = 10.dp),
                        value = sliderPosition,
                        onValueChange = {
                            binding.drawView.brushRadius = it
                            sliderPosition = it
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xff00DDEB),
                            thumbColor = Color(0xff9BFF78)
                        )

                    )

                    TextButton(
                        onClick = {
                            maskedBitmap?.let {
                                processBitmap(it)
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .background(brush2, shape = RoundedCornerShape(50))
                    ) {
                        Text(
                            text = "Create Sticker",
                            fontFamily = satoshiFont,
                            color = Color.White
                        )
                    }

                }
            }
        }
    }

    private fun processBitmap(it: Bitmap) {
        val folder = filesDir
        val file = File(folder, "Stickers/${System.currentTimeMillis()}.png")
        val parent = file.parentFile
        if (parent != null && (parent.exists() || parent.mkdirs())) {
            if (!file.exists() || file.delete()) {
                if (file.createNewFile()) {
                    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            it.saveToFile(file, Bitmap.CompressFormat.WEBP_LOSSLESS, 85)
                        } else {
                            it.saveToFile(file, Bitmap.CompressFormat.WEBP, 85)
                        }
                    ) {
                        Toast.makeText(this, "Sticker Created", Toast.LENGTH_SHORT).show()
                        startActivity(
                            StickerPreviewActivity.newIntent(this, file)
                        )
                        finish()
                    }
                }
            }
        }
    }


    companion object {
        fun newIntent(activity: AppCompatActivity): Intent {
            return Intent(activity, PickerActivity::class.java)
        }
    }

}


@Suppress("DEPRECATION")
fun Uri.getBitmap(context: Context): Bitmap {
    return if (Build.VERSION.SDK_INT < 28) {
        MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            this
        )
    } else {
        val source = ImageDecoder.createSource(context.contentResolver, this)
        ImageDecoder.decodeBitmap(source)
    }
}


fun Modifier.applyBrush(brush: Brush) = this
    .graphicsLayer(alpha = 0.99f)
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcAtop)
        }
    }


///Save a bitmap into file
fun Bitmap.saveToFile(
    file: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 85
): Boolean {
    return try {
        FileOutputStream(file).use { out ->
            this.compress(format, quality, out)
        }
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}


val screenWidth: Int = Resources.getSystem().displayMetrics.widthPixels
val screenHeight: Int = Resources.getSystem().displayMetrics.heightPixels


fun Bitmap.scaleBitmapAndKeepRation(
    reqHeight: Int = screenHeight,
    reqWidth: Int = screenWidth
): Bitmap {
    val matrix = Matrix()
    matrix.setRectToRect(
        RectF(
            0f, 0f, width.toFloat(),
            height.toFloat()
        ),
        RectF(0f, 0f, reqWidth.toFloat(), reqHeight.toFloat()),
        Matrix.ScaleToFit.CENTER
    )
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

