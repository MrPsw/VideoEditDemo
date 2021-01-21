package com.oaks.golf.ui.widget.graffiti

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent


/**
 * @date      2021/1/12
 * @author    Pengshuwen
 * @describe
 */
public class AnglePaint(var x: Float, var y: Float, var x1: Float, var y1: Float, var x2: Float, var y2: Float) : IPaint() {


    private var vertex_x = x
    private var vertex_y = y

    private var array: Array<Array<Float>> = arrayOf(arrayOf(x1, y1), arrayOf(x2, y2))


    private var paint = Paint().apply {
        color = Color.parseColor("#FF290F")
        isAntiAlias = true
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return true
    }


    override fun onDraw(canvas: Canvas?) {

        if (vertex_x != 0f || vertex_y != 0f) {
            canvas?.drawCircle(vertex_x, vertex_y, 2f, paint)
        }
        if (array[0][0] != 0f || array[0][1] != 0f) {
            canvas?.drawLine(vertex_x, vertex_y, array[0][0], array[0][1], paint)
        }
        if (array[1][0] != 0f || array[1][1] != 0f) {
            canvas?.drawLine(vertex_x, vertex_y, array[1][0], array[1][1], paint)
        }

    }

    override fun isEdit(): Boolean {
        return true
    }


}