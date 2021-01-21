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
public class CurvePaint : IPaint() {



    private var newPath: Path? = null

    private var isEdit=true

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
                newPath = Path()
                newPath?.moveTo(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId == event.getPointerId(0)) {
                    newPath?.lineTo(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pointerId == event.getPointerId(0)) {
                    newPath?.lineTo(event.x, event.y)
                    isEdit=false
                    return true
                }
            }
        }
        return false
    }



    override fun onDraw(canvas: Canvas?) {
        newPath?.let {
            canvas?.drawPath(it, paint)
        }
    }


    override fun isEdit(): Boolean {
        return  isEdit
    }


}