package com.oaks.golf.ui.opengl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


/**
 * @date      2020/12/1
 * @author    Pengshuwen
 * @describe
 */
class VideoDrawer : IDrawer {


    // 顶点坐标
    private val mVertexCoors = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    // 纹理坐标
    private val mTextureCoors = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    // 纹理坐标
    private val mTextureCoors2 = floatArrayOf(
        1f, 1f,
        0f, 1f,
        1f, 0f,
        0f, 0f
    )

    //纹理id
    private var mTextureId = -1

    //opengl 程序
    private var mProgram = -1

    private var isReverse = false

    private lateinit var vertexPos: FloatBuffer

    private lateinit var texturePos: FloatBuffer

    private var mVertexPosHandler: Int = -1

    private var mTexturePosHandler: Int = -1

    //矩阵变换接收者
    private var mVertexMatrixHandler: Int = -1

    //坐标变换矩阵
    private var mMatrix: FloatArray? = null


    private var mGl_Height: Int = -1
    private var mGl_Width: Int = -1

    private var mVideo_Height: Int = -1
    private var mVideo_Width: Int = -1

    private var mSurfaceTexture: SurfaceTexture? = null

    init {
        initPos()
    }


    fun setVideoSize(width: Int, height: Int) {
        this.mVideo_Width = width;
        this.mVideo_Height = height;
    }


    fun reverse(isReverse: Boolean) {
        this.isReverse = isReverse

        if (isReverse) {
            //初始化纹理坐标
            val a = ByteBuffer.allocateDirect(mTextureCoors2.size * 4)
            a.order(ByteOrder.nativeOrder())
            texturePos = a.asFloatBuffer()
            texturePos.put(mTextureCoors2)
            texturePos.position(0)
        } else {
            //初始化纹理坐标
            val a = ByteBuffer.allocateDirect(mTextureCoors.size * 4)
            a.order(ByteOrder.nativeOrder())
            texturePos = a.asFloatBuffer()
            texturePos.put(mTextureCoors)
            texturePos.position(0)
        }

    }


    fun setGlSize(width: Int, height: Int) {
        this.mGl_Width = width;
        this.mGl_Height = height;
    }


    override fun draw() {

        if (mTextureId != -1) {
            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg()
            //【步骤3: 激活并绑定纹理单元】
            activateTexture()
            //【步骤4: 绑定图片到纹理单元】
            updateTexture()

            doDraw()
        }
    }


    /**
     * 初始化OpenGl
     */
    private fun createGLPrg() {
        if (mProgram == -1) {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader())
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader())

            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES20.glCreateProgram()
            //将顶点着色器加入到程序
            GLES20.glAttachShader(mProgram, vertexShader)
            //将片元着色器加入到程序中
            GLES20.glAttachShader(mProgram, fragmentShader)
            //连接到着色器程序
            GLES20.glLinkProgram(mProgram)

            mVertexMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix")
            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition")
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate")

        }
        //使用OpenGL程序
        GLES20.glUseProgram(mProgram)
    }

    /**
     * 激活绑定Texture
     */
    private fun activateTexture() {
        //激活指定纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId)
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(mTexturePosHandler, 0)
        //配置边缘过渡参数
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

    }

    private fun updateTexture() {
        mSurfaceTexture?.updateTexImage()
    }

    private fun doDraw() {

        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler)
        GLES20.glEnableVertexAttribArray(mTexturePosHandler)

        //将变换矩阵传递给顶点着色器
        mMatrix = OpenGLUtils.getMatrix(mVideo_Width, mVideo_Height, mGl_Width, mGl_Height)
        if (mMatrix != null) {
            GLES20.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mMatrix, 0)
        }
        //设置着色器参数
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, vertexPos)
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, texturePos)
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    /**
     * 顶点着色器代码
     */
    private fun getVertexShader(): String {
        return "attribute vec4 aPosition;" +
                "uniform mat4 uMatrix;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "    gl_Position = aPosition*uMatrix;" +
                "    vCoordinate = aCoordinate;" +
                "}"
    }

    /**
     * 片元着色器代码
     */
    private fun getFragmentShader(): String {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 vCoordinate;" +
                "uniform samplerExternalOES uTexture;" +
                "void main() {" +
                "  gl_FragColor=texture2D(uTexture, vCoordinate);" +
                "}"
    }

    /**
     * 加载着色器
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        //根据type创建顶点着色器或者片元着色器
        val shader = GLES20.glCreateShader(type)
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }


    private fun initPos() {

        //初始化顶点坐标
        val b = ByteBuffer.allocateDirect(mVertexCoors.size * 4)
        b.order(ByteOrder.nativeOrder())
        vertexPos = b.asFloatBuffer()
        vertexPos.put(mVertexCoors)
        vertexPos.position(0)

        //初始化纹理坐标
        val a = ByteBuffer.allocateDirect(mTextureCoors.size * 4)
        a.order(ByteOrder.nativeOrder())
        texturePos = a.asFloatBuffer()
        texturePos.put(mTextureCoors)
        texturePos.position(0)

    }

    override fun setTextureID(id: Int) {
        mTextureId = id
        mSurfaceTexture = SurfaceTexture(id)
    }

    override fun release() {

        GLES20.glDisableVertexAttribArray(mVertexPosHandler)
        GLES20.glDisableVertexAttribArray(mTexturePosHandler)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(mTextureId), 0)
        GLES20.glDeleteProgram(mProgram)
    }

    fun getSurfaceTexture(): SurfaceTexture? {
        return mSurfaceTexture
    }

}