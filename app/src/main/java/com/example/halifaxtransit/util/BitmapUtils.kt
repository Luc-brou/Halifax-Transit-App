package com.example.halifaxtransit.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap

fun bitmapFromDrawable(
    context: Context,
    @DrawableRes id: Int
): Bitmap {
    val drawable = ContextCompat.getDrawable(context, id)!!

    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}