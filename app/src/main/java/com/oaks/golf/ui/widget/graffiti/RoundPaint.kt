package com.oaks.golf.ui.widget.graffiti

import android.graphics.*
import android.view.MotionEvent


/**
 * @date      2021/1/12
 * @author    Pengshuwen
 * @describe
 */
public class RoundPaint : IPaint() {


    private var newPath: Path? = null

    private var isEdit = true

    private var start_x = 0f

    private var start_y = 0f

    private var end_x = 0f

    private var end_y = 0f

    private var paint = Paint().apply {
        color = Color.parseColor("#FF290F")
        isAntiAlias = true
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }


    private var pointerId = 0

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                start_x = event.x
                start_y = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                end_x = event.x
                end_y = event.y
            }
            MotionEvent.ACTION_UP -> {
                isEdit = false
                return true
            }
        }
        return false
    }


    override fun onDraw(canvas: Canvas?) {
        if (start_x != 0f && start_y != 0f && end_x != 0f && end_y != 0f) {
            var rectf = RectF(start_x, start_y, end_x, end_y)
            canvas?.drawOval(rectf, paint)
        }

    }


    override fun isEdit(): Boolean {
        return isEdit
    }


}