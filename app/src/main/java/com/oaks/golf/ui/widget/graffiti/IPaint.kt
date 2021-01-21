package com.oaks.golf.ui.widget.graffiti

import android.graphics.Canvas
import android.view.MotionEvent


/**
 * @date      2021/1/12
 * @author    Pengshuwen
 * @describe
 */
open abstract class IPaint {

    abstract fun onTouchEvent(event: MotionEvent?):Boolean

    abstract fun onDraw(canvas: Canvas?)

    abstract fun isEdit():Boolean

}