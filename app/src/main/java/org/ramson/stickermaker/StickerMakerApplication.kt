package org.ramson.stickermaker

import android.app.Application
import com.google.android.material.color.DynamicColors

class StickerMakerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

}