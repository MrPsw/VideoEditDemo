package com.oaks.golf.ui.widget.graffiti

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat


/**
 * @date      2021/1/12
 * @author    Pengshuwen
 * @describe
 */
class DrawingBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val items = mutableListOf<IPaint>()

    private var currentStyle = STYLE_CURVE

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        items.forEach {
            it?.onDraw(canvas)
        }
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
    }


    private var mCurveView: CurvePaint? = null

    private var mLineView: LinePaint? = null

    private var mAngleView: AnglePaint? = null

    private var mRoundView: RoundPaint? = null

    private var mSquareView: SquarePaint? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (currentStyle) {
            STYLE_CURVE -> {
                if (mCurveView == null || mCurveView?.isEdit() == false) {
                    mCurveView = CurvePaint()
                }
                mCurveView?.onTouchEvent(event)
                if (!items.contains(mCurveView)) {
                    items.add(mCurveView!!)
                }
            }
            STYLE_BEELINE -> {
                if (mLineView == null || mLineView?.isEdit() == false) {
                    mLineView = LinePaint()
                }
                mLineView?.onTouchEvent(event)
                if (!items.contains(mLineView)) {
                    items.add(mLineView!!)
                }
            }
            STYLE_ROUND -> {

            }
            STYLE_ANGLE -> {
                if (mAngleView == null || mAngleView?.isEdit() == false) {
                    //mAngleView = AnglePaint()
                }
                mAngleView?.onTouchEvent(event)
                if (!items.contains(mAngleView)) {
                    items.add(mAngleView!!)
                }
            }
            STYLE_OVAL -> {
                if (mRoundView == null || mRoundView?.isEdit() == false) {
                    mRoundView = RoundPaint()
                }
                mRoundView?.onTouchEvent(event)
                if (!items.contains(mRoundView)) {
                    items.add(mRoundView!!)
                }
            }
            STYLE_SQUARE -> {
                if (mSquareView == null || mSquareView?.isEdit() == false) {
                    mSquareView = SquarePaint()
                }
                mSquareView?.onTouchEvent(event)
                if (!items.contains(mSquareView)) {
                    items.add(mSquareView!!)
                }
            }
        }
        invalidate()
        return true
    }


    companion object {
        const val STYLE_CURVE = 1
        const val STYLE_BEELINE = 2
        const val STYLE_ROUND = 3
        const val STYLE_ANGLE = 4
        const val STYLE_OVAL = 5
        const val STYLE_SQUARE = 6
    }


    fun setStyle(style: Int) {
        this.currentStyle = style
    }

    fun retreat() {
        if (items.isNotEmpty()) {
            items.removeAt(items.size - 1)
        }
        invalidate()
    }


}