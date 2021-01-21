package com.oaks.golf.ui.decoder

import android.media.MediaExtractor
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.util.concurrent.Executors


/**
 * @date      2021/1/8
 * @author    Pengshuwen
 * @describe
 */
class VideoDecoder {

    private val TAG = "VideoDecoder"

    private var mVideoMediaExtractor: MediaExtractor = MediaExtractor()

    private var mAudioMediaExtractor: MediaExtractor = MediaExtractor()

    private var videoPath: String? = null


    constructor() {

    }

    /**
     * set video path
     */
    fun setVideoPath(path: String) {
        this.videoPath = path
    }


    fun start() {
        if (checkFile()) {

            mVideoMediaExtractor.setDataSource(videoPath!!)
            mAudioMediaExtractor.setDataSource(videoPath!!)

            //开启线程解码
            var mExecutorService = Executors.newFixedThreadPool(2);
            mExecutorService.execute(Thread {
                startVideoDecode()
            })
            mExecutorService.execute(Thread {
                startAudioDecode()
            })

        }

    }

    private fun startAudioDecode() {

    }

    private fun startVideoDecode() {

    }

    private fun checkFile(): Boolean {
        if (isEmpty(videoPath)) {
            Log.e(TAG, "video path is empty")
            return false
        }
        val file = File(videoPath)
        if (file == null || !file.exists()) {
            Log.e(TAG, "video file is not exists")
            return false
        }
        return true
    }


    private fun isEmpty(str: String?): Boolean {
        return TextUtils.isEmpty(str)
    }


    fun cancel() {

    }

}