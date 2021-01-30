package com.oaks.ffmpeg;

import android.view.Surface;

/**
 * @author Pengshuwen
 * @date 2021/1/25
 * @describe
 */
public class MediaPlayer {

    static {
        System.loadLibrary("learn_ffmpeg");
    }

    private final long nativeHandle;

    public MediaPlayer() {
        nativeHandle = nativeInit();
    }

    public void setDataSource(String path) {
        setDataSource(nativeHandle, path);
    }

    public void prepare() {
        prepare(nativeHandle);
    }

    public void setSurface(Surface surface) {
        setSurface(nativeHandle, surface);
    }

    public void start() {
        start(nativeHandle);
    }

    public void stop() {
        stop(nativeHandle);
    }

    public void pause() {
        pause(nativeHandle);
    }

    public void setPosition(long time,boolean move) {
        setPosition(nativeHandle, time,move);
    }

    public long getDuration() {
        return getDuration(nativeHandle);
    }


    private native long nativeInit();

    private native void setDataSource(long nativeHandle, String path);

    private native void prepare(long nativeHandle);

    private native void start(long nativeHandle);

    private native void setSurface(long nativeHandle, Surface surface);

    private native void pause(long nativeHandle);

    private native void seekTo(long nativeHandle, long time);

    private native void setPosition(long nativeHandle, long time, boolean move);

    private native void stop(long nativeHandle);

    private native long getDuration(long nativeHandle);

    private void onError(int errorCode) {
        if (null != onErrorListener) {
            onErrorListener.onError(errorCode);
        }
    }

    private void onPrepare() {
        if (null != onPrepareListener) {
            onPrepareListener.onPrepared();
        }
    }

    private void onProgress(int progress) {
        if (null != onProgressListener) {
            onProgressListener.onProgress(progress);
        }
    }

    private OnErrorListener onErrorListener;
    private OnProgressListener onProgressListener;
    private OnPrepareListener onPrepareListener;

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    public void setOnPrepareListener(OnPrepareListener onPrepareListener) {
        this.onPrepareListener = onPrepareListener;
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    public interface OnErrorListener {
        void onError(int err);
    }

    public interface OnPrepareListener {
        void onPrepared();
    }

    public interface OnProgressListener {
        void onProgress(int progress);
    }

}
