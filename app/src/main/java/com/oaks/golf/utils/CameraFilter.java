package com.oaks.golf.utils;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.oaks.golf.R;


/**
 * @author liyuqing
 * @date 2018/11/2.
 * @description 写自己的代码，让别人说去吧
 * <p>
 * 不需要显示到屏幕上
 * 写入到帧缓存中
 * <p>
 * 什么是帧缓冲对象？
 */
public class CameraFilter extends AbstractFilter {

    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;
    private float[] matrix;

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vertex, R.raw.camera_frag);
    }

    @Override
    protected void initCoordinate() {
        mGLTextureBuffer.clear();

        //摄像头是颠倒的
//        float[] TEXTURE = {
//                0.0f, 0.0f,
//                0.0f, 1.0f,
//                1.0f, 0.0f,
//                1.0f, 1.0f,
//        };
        //调整好镜像
        float[] TEXTURE = {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
        };

        mGLTextureBuffer.put(TEXTURE);
    }

    public void destroyFrameBuffers() {
        //删除fbo纹理
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }

        //删除fbo
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    @Override
    public void release() {
        super.release();
        destroyFrameBuffers();
    }

    @Override
    public void onReady(int width, int height) {
        super.onReady(width, height);

        if (mFrameBuffers != null) {
            destroyFrameBuffers();
        }

        //1.创建fbo
        mFrameBuffers = new int[1];
        //离屏屏幕
        GLES20.glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);

        //2.创建fbo纹理
        mFrameBufferTextures = new int[1];//用来记录纹理id
        OpenGLUtils.glGenTextures(mFrameBufferTextures);//创建纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        //创建一个2d的图像
        //目标：2D纹理 + 等级 + 格式+ 宽、高 + 格式 + 数据类型 + 像素数据
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mOutputWidth, mOutputHeight,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //将帧缓冲对象设置为当前缓冲区
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

        //让fbo与纹理绑定起来
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);


        //解绑
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public int onDrawFrame(int textureId) {
        //设置显示窗口
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        //必须显示调用，否则就会默认显示到GLSurfaceView中了
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

        //使用着色器
        GLES20.glUseProgram(mGLProgramId);

        //传递坐标
        //传递坐标
        mGLVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, mGLVertexBuffer);
        GLES20.glEnableVertexAttribArray(vPosition);

        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(vCoord);

        //变换矩阵
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, matrix, 0);

        //激活
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        //因为这一层是摄像头后的第一层，即采集的纹理是SurfaceTexture的纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(vTexture, 0);

        //4个点
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


        //返回fbo的纹理id
        return mFrameBufferTextures[0];
    }


    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }
}
