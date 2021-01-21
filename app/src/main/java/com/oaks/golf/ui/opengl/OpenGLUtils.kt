package com.oaks.golf.ui.opengl

import android.opengl.Matrix


/**
 * @date      2020/12/2
 * @author    Pengshuwen
 * @describe
 */
object OpenGLUtils {

    fun getMatrix(
        mVideoWidth: Int,
        mVideoHeight: Int,
        mWorldWidth: Int,
        mWorldHeight: Int
    ): FloatArray? {

        if (mVideoWidth != -1 && mVideoHeight != -1 && mWorldWidth != -1 && mWorldHeight != -1) {
            var prjMatrix = FloatArray(16)
            val originRatio = mVideoWidth / mVideoHeight.toFloat()
            val worldRatio = mWorldWidth / mWorldHeight.toFloat()
            if (mWorldWidth > mWorldHeight) {
                if (originRatio > worldRatio) {
                    val actualRatio = originRatio / worldRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -1f, 1f,
                        -actualRatio, actualRatio,
                        -1f, 3f
                    )
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    val actualRatio = worldRatio / originRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -actualRatio, actualRatio,
                        -1f, 1f,
                        -1f, 3f
                    )
                }
            } else {
                if (originRatio > worldRatio) {
                    val actualRatio = originRatio / worldRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -1f, 1f,
                        -actualRatio, actualRatio,
                        -1f, 3f
                    )
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    val actualRatio = worldRatio / originRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -actualRatio, actualRatio,
                        -1f, 1f,
                        -1f, 3f
                    )
                }
            }
            return prjMatrix
        }
        return null
    }





}