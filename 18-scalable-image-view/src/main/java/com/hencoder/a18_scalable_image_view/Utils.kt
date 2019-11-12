package com.hencoder.a18_scalable_image_view

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.TypedValue

fun Float.dp() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this,
        Resources.getSystem().displayMetrics
)

fun Int.dp() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        Resources.getSystem().displayMetrics
).toInt()

fun Resources.getAvatar(width: Int): Bitmap = BitmapFactory.Options()
        .let {
            it.inJustDecodeBounds = true
            BitmapFactory.decodeResource(this, R.drawable.avatar_rengwuxian, it)
            it.inJustDecodeBounds = false
            it.inDensity = it.outWidth
            it.inTargetDensity = width
            BitmapFactory.decodeResource(this, R.drawable.avatar_rengwuxian, it)
        }

fun Context.log(msg: String) = Log.d("DQ", msg)