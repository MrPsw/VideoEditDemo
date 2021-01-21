package com.oaks.golf.ui.recorder

import android.opengl.*
import android.util.Log
import android.view.Surface


class EglCore(surface: Surface) {
    //后台获取数据
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglConfig: EGLConfig? = null
    private var eglSurface: EGLSurface? = null
    private var shareContext: EGLContext? = null

    init {
        eglDisplay = EGL14.eglGetCurrentDisplay()
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        var versionArray = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, versionArray, 0, versionArray, 0)) {
            throw RuntimeException("unable to init EGL14")
        }
        shareContext = EGL14.eglGetCurrentContext()
        Log.e("EglCore", "--->init ${EGL14.EGL_NO_CONTEXT}   eglContext=$eglContext")
        if (eglContext == null) {
            val renderableType: Int = EGL14.EGL_OPENGL_ES2_BIT
            var config = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            var configNum = IntArray(1)
            if (!EGL14.eglChooseConfig(
                    eglDisplay,
                    config,
                    0,
                    configs,
                    0,
                    configs.size,
                    configNum,
                    0
                )
            ) {
                throw RuntimeException("--->unable to find config")
            }
            eglConfig = configs[0]
            var attrib3list = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL14.eglGetCurrentContext(),
                attrib3list,
                0
            )
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                throw RuntimeException("unable to get eglSurface")
            }
            val surfaceConfig = intArrayOf(EGL14.EGL_NONE)
            eglSurface =
                EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceConfig, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                throw RuntimeException("unable to get eglSurface")
            }
            Log.e("EglCore", "---->init EGL success")
        }
        // Confirm with query.

        // Confirm with query.
        val values = IntArray(1)
        EGL14.eglQueryContext(
            eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
            values, 0
        )
    }

    public fun release() {
        if (eglDisplay != null) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglSurface = null
        eglDisplay = null
        eglContext = null
    }

    fun eglMakeCurrent() {
        eglContext?.let {
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, it)) {
                throw RuntimeException("unable to makeCurrent")
            }
        }
    }

    fun swapBuffer() {
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun setPresentTime(time: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, time)
    }
}