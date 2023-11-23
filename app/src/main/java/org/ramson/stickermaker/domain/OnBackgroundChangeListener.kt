package org.ramson.stickermaker.domain

import android.graphics.Bitmap

interface OnBackgroundChangeListener {

    fun onSuccessWithMask(bitmap: Bitmap, mask: Bitmap){}
    fun onSuccess(bitmap: Bitmap)
    fun onFailed(exception: Exception)

}