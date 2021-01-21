package com.oaks.golf.utils;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;



/**
 * @author liyuqing
 * @date 2018/11/6.
 * @description 写自己的代码，让别人说去吧
 * <p>
 * <p>
 * EGL配置 与录制 OpenGL操作
 * 工具类
 */
public class EGLBase {

    private ScreenFilter mScreenFilter;
    private EGLSurface mEglSurface;
    private EGLDisplay mEglDisplay;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;

    public EGLBase(Context context, int width, int height, Surface surface, EGLContext eglContext) {
        //配置EGL环境
        createEGL(eglContext);

        //把Surface 贴到 mEglDisplay 产生联系
        int[] attrib_list = {
                EGL14.EGL_NONE
        };
        //绘制线程中的图像 就是往这个mEglSurface 上面去画
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay,mEglConfig,surface,attrib_list,0);
        //绑定当前线程的显示设备以及上下文，之后操作OpenGL 就是在这个虚拟显示上操作
        if (!EGL14.eglMakeCurrent(mEglDisplay,mEglSurface,mEglSurface,mEglContext)){
            throw new RuntimeException("eglMakeCurrent failed");
        }

        //向虚拟屏幕画画
        mScreenFilter = new ScreenFilter(context);
        mScreenFilter.onReady(width,height);

    }


    private void createEGL(EGLContext eglContext) {
        //创建虚拟显示器
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglDisplay failed");
        }

        //初始化显示器
        int[] version = new int[2];
        //major:主版本，minor 子版本
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }

        //egl 根据我们配置的属性 选择一个配置
        int[] arrtib_list = {
                EGL14.EGL_RED_SIZE, 8, // 缓冲区中 红分量 位数
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, //egl版本 2
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config=new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay,arrtib_list,0,configs,0,configs.length,num_config,0)){
            throw  new IllegalArgumentException("eglChooseConfig#2 failed");
        }

        mEglConfig = configs[0];
        int[] ctx_arrtib_list={
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, //egl版本 2
                EGL14.EGL_NONE
        };
        //创建EGL上下文
        //3.share_context
        mEglContext = EGL14.eglCreateContext(mEglDisplay,mEglConfig,eglContext,ctx_arrtib_list,0);
        if (mEglContext == EGL14.EGL_NO_CONTEXT){
            throw  new RuntimeException("EGL Context Error");
        }
    }

    /**
     * 画画
     * @param textureId 纹理id 代表一个图片
     * @param timestamp 时间戳
     */
    public void draw(int textureId, long timestamp) {
        //绑定当前线程的显示设备以及上下文
        if (!EGL14.eglMakeCurrent(mEglDisplay,mEglSurface,mEglSurface,mEglContext)){
            throw new RuntimeException("eglMakeCurrent failed");
        }

        //画画。画画到屏幕上
        mScreenFilter.onDrawFrame(textureId);
        //刷新eglSurface的时间戳
        EGLExt.eglPresentationTimeANDROID(mEglDisplay,mEglSurface,timestamp);


        //交换数据
        //双缓冲模式
        EGL14.eglSwapBuffers(mEglDisplay,mEglSurface);
    }

    public void release(){
        EGL14.eglDestroySurface(mEglDisplay,mEglSurface);
        EGL14.eglMakeCurrent(mEglDisplay,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroyContext(mEglDisplay,mEglContext);
        EGL14.eglReleaseThread();
        EGL14.eglTerminate(mEglDisplay);
    }
}
