package com.oaks.golf.ui.opengl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


/**
 * @date      2020/12/1
 * @author    Pengshuwen
 * @describe
 */
class TriangleDrawer(var textureId: Int = -1) : IDrawer {




    //顶点坐标
    val mVertexCoors = floatArrayOf(
        0f, 1f,
        -1f, -1f,
        1f, -1f
    )


    //纹理坐标
    val mTextureCoors = floatArrayOf(
        0.5f, 0f,
        0f, 1f,
        1f, 1f
    )

    //纹理id
    private var mTextureId = -1

    //opengl id
    private var mProgram = -1


    private lateinit var vertexPos: FloatBuffer

    private lateinit var texturePos: FloatBuffer

    private var mVertexPosHandler: Int = -1

    private var mTexturePosHandler: Int = -1



    init {
        initPos()
    }

    override fun draw() {
        if (mTextureId != -1) {
            createGLPrg()
            doDraw()
        }
    }

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

            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition")
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate")
        }
        //使用OpenGL程序
        GLES20.glUseProgram(mProgram)
    }

    private fun doDraw() {
        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler)
        GLES20.glEnableVertexAttribArray(mTexturePosHandler)
        //设置着色器参数
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, vertexPos)
        GLES20.glVertexAttribPointer(
            mTexturePosHandler,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texturePos
        )
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }


    private fun getVertexShader(): String {
        return "attribute vec4 aPosition;" +
                "void main() {" +
                "  gl_Position = aPosition;" +
                "}"
    }

    private fun getFragmentShader(): String {
        return "precision mediump float;" +
                "void main() {" +
                "  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);" +
                "}"
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        //根据type创建顶点着色器或者片元着色器
        val shader = GLES20.glCreateShader(type)
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun initPos() {
        val b = ByteBuffer.allocateDirect(mVertexCoors.size * 4)
        b.order(ByteOrder.nativeOrder())

        vertexPos = b.asFloatBuffer()
        vertexPos.put(mVertexCoors)
        vertexPos.position(0)


        val a = ByteBuffer.allocateDirect(mTextureCoors.size * 4)
        a.order(ByteOrder.nativeOrder())

        texturePos = a.asFloatBuffer()
        texturePos.put(mTextureCoors)
        texturePos.position(0)

    }

    override fun setTextureID(id: Int) {
        mTextureId = id

    }

    override fun release() {

        GLES20.glDisableVertexAttribArray(mVertexPosHandler)
        GLES20.glDisableVertexAttribArray(mTexturePosHandler)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(mTextureId), 0)
        GLES20.glDeleteProgram(mProgram)
    }
}