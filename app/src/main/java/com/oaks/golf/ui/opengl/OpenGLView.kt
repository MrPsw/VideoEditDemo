package com.oaks.golf.ui.opengl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * @date      2020/12/1
 * @author    Pengshuwen
 * @describe
 */
class OpenGLView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    GLSurfaceView(context, attrs) {


    private var renderer: GLRenderer? = null

    private var graffitiLayerBitmap: Bitmap? = null
    private var graffitiLayerCanvans: Canvas? = null
    private var paint: Paint? = null
    private var onCanvasListener: ((bitmap: Bitmap?) -> Unit)? = null


    init {


        //设置OpenGL 版本
        setEGLContextClientVersion(2)
        renderer = GLRenderer(getContext())
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY


//        graffitiLayerBitmap = Bitmap.createBitmap(788, 788, Bitmap.Config.ARGB_8888)
//        graffitiLayerCanvans = Canvas(graffitiLayerBitmap!!)
//
//        paint = Paint()
//        paint?.isAntiAlias = true
//        paint?.strokeWidth = 12f
//        paint?.color = Color.parseColor("#E22018")
//
//
//        val scale = 1f
//        var downX = 0f
//        var downY = 0f
//        var upX = 0f
//        var upY = 0f

//        setOnTouchListener { view, event ->
//
//
//            when (event?.action) {
//                MotionEvent.ACTION_CANCEL -> {
//                }
//                MotionEvent.ACTION_DOWN -> {
//                    downX = event.getX() * scale
//                    downY = event.getY() * scale
//                }
//                MotionEvent.ACTION_UP -> {
//                    upX = event.getX() * scale
//                    upY = event.getY() * scale
//                    graffitiLayerCanvans?.drawLine(downX, downY, upX, upY, paint!!)
//
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    upX = event.getX() * scale
//                    upY = event.getY() * scale
//                    graffitiLayerCanvans?.drawLine(downX, downY, upX, upY, paint!!)
//                    downX = upX
//                    downY = upY
//                }
//                else -> {
//                }
//            }
//
//            drawer2.setBitmap(graffitiLayerBitmap)
//            onCanvasListener?.invoke(graffitiLayerBitmap)
//
//            true
//        }
    }

    fun addSurfaceCreatedListener(mSurfaceCreated: (surfaceTexture: SurfaceTexture?) -> Unit) {
        renderer?.addSurfaceCreatedListener(mSurfaceCreated)
    }


    fun setVideoSize(width: Int, height: Int) {
        renderer?.setVideoSize(width, height)
    }

    fun setReverse(isReverse: Boolean) {
        renderer?.reversal(isReverse)
    }


    fun setCanvasListener(onCanvas: (bitmap: Bitmap?) -> Unit) {
        this.onCanvasListener = onCanvas
    }


}


class GLRenderer constructor(context: Context?) : GLSurfaceView.Renderer {


    private var mContext: Context? = context

    private var drawer: VideoDrawer? = null
    private var drawer2: BitmapDrawer? = null


    fun setVideoSize(width: Int, height: Int) {
        drawer?.setVideoSize(width, height)
        mContext?.let {
            drawer2?.init(it)
        }


    }

    fun reversal(isReverse: Boolean) {
        drawer?.reverse(isReverse)
    }

    init {
        drawer = VideoDrawer()
      //  drawer2 = BitmapDrawer()
    }

    var mSurfaceCreated: ((surfaceTexture: SurfaceTexture?) -> Unit)? = null

    fun addSurfaceCreatedListener(mSurfaceCreated: (surfaceTexture: SurfaceTexture?) -> Unit) {
        this.mSurfaceCreated = mSurfaceCreated
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
        drawer?.setTextureID(textureIds[0])
        mSurfaceCreated?.invoke(drawer?.getSurfaceTexture())
        //drawer2.setTextureID(textureIds[1])

    }


    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        drawer?.setGlSize(width, height)
        //drawer2.setGlSize(width, height)
        println("onSurfaceChanged:width=$width, height$height")

    }


    override fun onDrawFrame(p0: GL10?) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawer?.draw()
        // drawer2.draw()
        //println("onDrawFrame")
    }


}
