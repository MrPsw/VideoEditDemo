package com.oaks.golf.ui.widget.graffiti

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent


/**
 * @date      2021/1/12
 * @author    Pengshuwen
 * @describe
 */
public class LinePaint : IPaint() {


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
                pointerId = event.getPointerId(0)
                start_x=event.x
                start_y=event.y
            }
            MotionEvent.ACTION_MOVE -> {
                end_x=event.x
                end_y=event.y
            }
            MotionEvent.ACTION_UP -> {
                if (pointerId == event.getPointerId(0)) {
                    isEdit = false
                    return true
                }
            }
        }
        return false
    }


    override fun onDraw(canvas: Canvas?) {
            canvas?.drawLine(start_x,start_y,end_x,end_y,paint)
    }


    override fun isEdit(): Boolean {
        return isEdit
    }


}