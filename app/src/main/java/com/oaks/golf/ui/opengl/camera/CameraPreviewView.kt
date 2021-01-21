package com.oaks.golf.ui.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.oaks.golf.ui.opengl.camera.CameraDrawer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * @date      2020/12/1
 * @author    Pengshuwen
 * @describe
 */
class CameraPreviewView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    GLSurfaceView(context, attrs) {


    private var renderer: CameraRenderer? = null


    init {
        setEGLContextClientVersion(2)
        renderer = CameraRenderer(getContext())
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun getRenderer(): CameraRenderer? {
        return renderer
    }

}


class CameraRenderer constructor(context: Context?) : GLSurfaceView.Renderer {


    var mCameraDrawer = CameraDrawer()


    var mSurfaceCreated: ((surfaceTexture: SurfaceTexture?) -> Unit)? = null


    fun addSurfaceCreated(call: (surfaceTexture: SurfaceTexture?) -> Unit) {
        this.mSurfaceCreated = call
    }

    private fun createTextureIds(count: Int): IntArray {
        val texture = IntArray(count)
        GLES20.glGenTextures(count, texture, 0) //生成纹理
        return texture
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {

        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND);
        //开启GL的混合模式，即图像叠加
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)


        val textureIds = createTextureIds(2)
        mCameraDrawer?.setTextureID(textureIds[0])
        mSurfaceCreated?.invoke(mCameraDrawer.getSurfaceTexture())
    }


    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        mCameraDrawer?.setGlSize(width, height)
        mCameraDrawer?.setVideoSize(width,height)


    }


    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        mCameraDrawer?.draw()

    }


}
