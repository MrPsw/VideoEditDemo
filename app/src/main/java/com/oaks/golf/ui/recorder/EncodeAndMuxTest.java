package com.oaks.golf.ui.recorder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Environment;

import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

// wiki:
// http://bigflake.com/mediacodec/EncodeAndMuxTest.java.txt

public class EncodeAndMuxTest  {
    private static final String TAG = EncodeAndMuxTest.class.getSimpleName();

    // 输出文件路径
    private static final File MY_OUTPUT_DIR = Environment.getExternalStorageDirectory();
    // H.264编码
    private static final String MY_MIME_TYPE = "video/avc";
    // 视频文件的宽高
    private static final int MY_VIDEO_WIDTH = 480;
    private static final int MY_VIDEO_HEIGHT = 480;
    // 视频码率
    private static final int MY_BIT_RATE = 800000;
    // 每秒钟15帧
    private static final int MY_FPS = 15;


    // 总共30帧,每一秒15帧,所以30帧为2秒钟
    private static final int NUM_FRAMES = 30;


    // RGB color values for generated frames
    private static final int TEST_R0 = 0;
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    //
    private static final int TEST_R1 = 236;
    private static final int TEST_G1 = 50;
    private static final int TEST_B1 = 186;


    // encoder / muxer state
    private MediaCodec mEncoder;
    // H.264 转 mp4
    private MediaMuxer mMuxer;


    private CodecInputSurface mInputSurface;

    private int mTrackIndex;
    private boolean mMuxerStarted;

    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo;


    /**
     * opengl绘制一个buffer，转成MP4,程序入口
     */
    public void testEncodeVideoToMp4() {

        try {
            // 初始化Encoder
            initVideoEncoder();
            // 设置 EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx
            mInputSurface.makeCurrent();

            // 共30帧
            for (int i = 0; i < NUM_FRAMES; i++) {
                // mEncoder从缓冲区取数据，然后交给mMuxer编码
                drainEncoder(false);
                // opengl绘制一帧
                generateSurfaceFrame(i);
                // 设置图像，发送给EGL的显示时间
                mInputSurface.setPresentationTime(computePresentationTimeNsec(i));
                // Submit it to the encoder
                mInputSurface.swapBuffers();
            }
            // send end-of-stream to encoder, and drain remaining output
            drainEncoder(true);
        } finally {
            // release encoder, muxer, and input Surface
            releaseEncoder();
        }
    }

    /**
     * 初始化视频编码器
     */
    private void initVideoEncoder() {
        // 创建一个buffer
        mBufferInfo = new MediaCodec.BufferInfo();

        //-----------------MediaFormat-----------------------
        // mediaCodeC采用的是H.264编码
        MediaFormat format = MediaFormat.createVideoFormat(MY_MIME_TYPE, MY_VIDEO_WIDTH, MY_VIDEO_HEIGHT);
        // 数据来源自surface
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 视频码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, MY_BIT_RATE);
        // fps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, MY_FPS);
        //设置关键帧的时间
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        //-----------------Encoder-----------------------
        try {
            mEncoder = MediaCodec.createEncoderByType(MY_MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 创建一个surface
            Surface surface = mEncoder.createInputSurface();
            // 创建一个CodecInputSurface,其中包含GL相关
            mInputSurface = new CodecInputSurface(surface);
            //
            mEncoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //-----------------输出文件路径-----------------------
        // 输出文件路径
        String outputPath = new File(MY_OUTPUT_DIR,
                "test." + MY_VIDEO_WIDTH + "x" + MY_VIDEO_HEIGHT + ".mp4").toString();

        //-----------------MediaMuxer-----------------------
        try {
            // 输出为MP4
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     * 释放资源
     */
    private void releaseEncoder() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }


    /**
     * mEncoder从缓冲区取数据，然后交给mMuxer编码
     *
     * @param endOfStream 是否停止录制
     */
    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        // 停止录制
        if (endOfStream) {
            mEncoder.signalEndOfInputStream();
        }
        //拿到输出缓冲区,用于取到编码后的数据
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            //拿到输出缓冲区的索引
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {

                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                //拿到输出缓冲区,用于取到编码后的数据
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                //
                MediaFormat newFormat = mEncoder.getOutputFormat();
                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                //
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
            } else {
                //获取解码后的数据
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }
                //
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                //
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    // 编码
                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                }
                //释放资源
                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {

                    }
                    break;      // out of while
                }
            }
        }
    }

    /**
     * Generates a frame of data using GL commands.  We have an 8-frame animation
     * sequence that wraps around.  It looks like this:
     * <pre>
     *   0 1 2 3
     *   7 6 5 4
     * </pre>
     * We draw one of the eight rectangles and leave the rest set to the clear color.
     */
    private void generateSurfaceFrame(int frameIndex) {
        frameIndex %= 8;

        int startX, startY;
        if (frameIndex < 4) {
            // (0,0) is bottom-left in GL
            startX = frameIndex * (MY_VIDEO_WIDTH / 4);
            startY = MY_VIDEO_HEIGHT / 2;
        } else {
            startX = (7 - frameIndex) * (MY_VIDEO_WIDTH / 4);
            startY = 0;
        }

        GLES20.glClearColor(TEST_R0 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(startX, startY, MY_VIDEO_WIDTH / 4, MY_VIDEO_HEIGHT / 2);
        GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    /**
     * Generates the presentation time for frame N, in nanoseconds.
     * 好像是生成当前帧的时间,具体怎么计算的,不懂呀??????????????????????????
     */
    private static long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / MY_FPS;
    }


    /**
     * Holds state associated with a Surface used for MediaCodec encoder input.
     * <p>
     * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
     * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
     * to the video encoder.
     * <p>
     * This object owns the Surface -- releasing this will release the Surface too.
     */
    private static class CodecInputSurface {
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

        private Surface mSurface;

        /**
         * Creates a CodecInputSurface from a Surface.
         */
        public CodecInputSurface(Surface surface) {
            if (surface == null) {
                throw new NullPointerException();
            }
            mSurface = surface;

            initEGL();
        }

        /**
         * 初始化EGL
         */
        private void initEGL() {

            //--------------------mEGLDisplay-----------------------
            // 获取EGL Display
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            // 错误检查
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("unable to get EGL14 display");
            }
            // 初始化
            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                throw new RuntimeException("unable to initialize EGL14");
            }

            // Configure EGL for recording and OpenGL ES 2.0.
            int[] attribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    //
                    EGL14.EGL_RENDERABLE_TYPE,
                    EGL14.EGL_OPENGL_ES2_BIT,
                    // 录制android
                    EGL_RECORDABLE_ANDROID,
                    1,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            // eglCreateContext RGB888+recordable ES2
            EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0);

            // Configure context for OpenGL ES 2.0.
            int[] attrib_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            //--------------------mEGLContext-----------------------
            //  eglCreateContext
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                    attrib_list, 0);
            checkEglError("eglCreateContext");

            //--------------------mEGLSurface-----------------------
            // 创建一个WindowSurface并与surface进行绑定,这里的surface来自mEncoder.createInputSurface();
            // Create a window surface, and attach it to the Surface we received.
            int[] surfaceAttribs = {
                    EGL14.EGL_NONE
            };
            // eglCreateWindowSurface
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                    surfaceAttribs, 0);
            checkEglError("eglCreateWindowSurface");
        }

        /**
         * Discards all resources held by this class, notably the EGL context.  Also releases the
         * Surface that was passed to our constructor.
         * 释放资源
         */
        public void release() {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }

            mSurface.release();

            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;

            mSurface = null;
        }

        /**
         * Makes our EGL context and surface current.
         * 设置 EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx
         */
        public void makeCurrent() {
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
            checkEglError("eglMakeCurrent");
        }

        /**
         * Calls eglSwapBuffers.  Use this to "publish" the current frame.
         * 用该方法，发送当前Frame
         */
        public boolean swapBuffers() {
            boolean result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
            checkEglError("eglSwapBuffers");
            return result;
        }

        /**
         * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
         * 设置图像，发送给EGL的时间间隔
         */
        public void setPresentationTime(long nsecs) {
            // 设置发动给EGL的时间间隔
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
            checkEglError("eglPresentationTimeANDROID");
        }

        /**
         * Checks for EGL errors.  Throws an exception if one is found.
         * 检查错误,代码可以忽略
         */
        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
        }
    }
}
