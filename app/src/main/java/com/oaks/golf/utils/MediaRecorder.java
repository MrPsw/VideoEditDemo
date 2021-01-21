package com.oaks.golf.utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * @author liyuqing
 * @date 2018/11/6.
 * @description 写自己的代码，让别人说去吧
 * <p>
 * <p>
 * 录制类
 */
public class MediaRecorder {

    private final Context mContext;
    private final String mPath;
    private final int mWidth;
    private final int mHeight;
    private final EGLContext mEglContext;
    private MediaCodec mMediaCodec;
    private Surface mInputSurface;
    private MediaMuxer mMediaMuxer;
    private Handler mHandler;
    private EGLBase mEglBase;
    private boolean isStart;
    private int index;

    private float mSpeed;

    /**
     * @param mContext    上下文
     * @param mPath       存储录制视频的地址
     * @param mWidth      宽
     * @param mHeight     高
     * @param mEglContext eglContext
     */
    public MediaRecorder(Context mContext, String mPath, int mWidth, int mHeight, EGLContext mEglContext) {
        this.mContext = mContext.getApplicationContext();
        this.mPath = mPath;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mEglContext = mEglContext;

    }


    /**
     * 开始录制
     *
     * @param speed 快慢速
     */
    public void start(float speed) throws IOException {
        mSpeed = speed;

        /**
         *     配置MediaCodec编码器
         */

        //类型
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        //参数配置
        //1500kbs 码率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1500_00);
        //帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
        //关键帧间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 20);
        //颜色格式
        //从Surface当中获取的
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //编码器
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        //将参数配置给编码器
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);


        //交给虚拟屏幕 通过OpenGL 将预览的纹理 会知道这一个虚拟屏幕中
        //这样MediaCodec就会自动编码mInputSurface当中的图像了
        mInputSurface = mMediaCodec.createInputSurface();

        /**
         * H264
         * 封装成MP4文件写出去
         */
        mMediaMuxer = new MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);


        /**
         * 配置EGL环境
         * 启动一个子线程 去配置EGL环境
         *
         */
        HandlerThread handlerThread = new HandlerThread("VideoCodec");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();

        //用于其他线程 通知子线程
        mHandler = new Handler(looper);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //创建egl环境（虚拟设备)
                mEglBase = new EGLBase(mContext, mWidth, mHeight, mInputSurface, mEglContext);
                //启动编码器
                mMediaCodec.start();
                isStart = true;

            }
        });


    }

    /**
     * 传递纹理进来
     * 相当于调用一次就有一个新的图像需要编码
     *
     * @param textureId
     * @param timestamp
     */
    public void enCodeFrame(final int textureId, final long timestamp) {
        if (!isStart) {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //把图像画到虚拟屏幕上
                mEglBase.draw(textureId,timestamp);
                //从编码器的输出缓冲区获取编码后的数据
                getCodec(false);
            }
        });
    }


    /**
     * 获取编码后的数据
     *
     * @param endOfStream 标记是否结束录制
     */
    public void getCodec(boolean endOfStream) {

        //不录了
        if (endOfStream){
            mMediaCodec.signalEndOfInputStream();
        }

        //输出缓冲区
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //希望将已经编码完成后的数据都获取到然后写出到MP4文件中

        while (true){

            int status = mMediaCodec.dequeueOutputBuffer(bufferInfo,10_000);

            if (status == MediaCodec.INFO_TRY_AGAIN_LATER){
                if (!endOfStream){
                    break;
                }
            }else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                //
                MediaFormat outputFormat = mMediaCodec.getOutputFormat();
                //配置封装器
                index = mMediaMuxer.addTrack(outputFormat);
                mMediaMuxer.start();
            }else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                //忽略
            }else {
                //成功 取出一个有效的输出
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(status);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0){
                    bufferInfo.presentationTimeUs =(long) (bufferInfo.presentationTimeUs / mSpeed);
                    //写到mp4中
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    mMediaMuxer.writeSampleData(index,outputBuffer,bufferInfo);
                }

                mMediaCodec.releaseOutputBuffer(status,false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) !=0){
                    break;
                }
            }

        }
    }

    public void stop() {
        isStart = false;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                getCodec(true);
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
                mEglBase.release();
                mEglBase = null;
                mInputSurface = null;
                mHandler.getLooper().quitSafely();
                mHandler = null;
            }
        });
    }


}
