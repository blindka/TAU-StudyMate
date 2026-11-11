package com.tau.studymate.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Decodes an image file at most [maxSide] pixels on its long edge, so a 12MP photo doesn't take ~50MB. */
fun decodeScaled(file: File, maxSide: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()
