package com.oaks.golf.ui.opengl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint


/**
 * @date      2020/12/3
 * @author    Pengshuwen
 * @describe
 */
object BitmapUtils {

    fun createBitmap(width: Int, height: Int): Bitmap? {
        if (width <= 0 && height <= 0) {
            return null
        }
        val paint = Paint()
        paint.textSize = 28f
        paint.color = Color.parseColor("#ffffff")
        paint.isAntiAlias = true
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        //canvas.drawColor(Color.parseColor("#E22018"))
        canvas.drawText("这是绘制的文字", 25f, 100f, paint)
        canvas.drawText("oaks", 200f, 200f, paint)

        println("bitmap: width=$width hegiht=$height")
        return bitmap
    }


    fun createTransparentBitmap(width: Int, height: Int): Bitmap? {
        if (width <= 0 && height <= 0) {
            return null
        }
        val paint = Paint()
        paint.textSize = 28f
        paint.color = Color.parseColor("#ffffff")
        paint.isAntiAlias = true
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        //canvas.drawColor(Color.parseColor("#E22018"))
        canvas.drawText("这是绘制的文字", 25f, 100f, paint)
        canvas.drawText("oaks", 200f, 200f, paint)

        println("bitmap: width=$width hegiht=$height")
        return bitmap
    }


}