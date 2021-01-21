package com.oaks.golf.ui.opengl

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import java.io.File
import java.nio.ByteBuffer


/**
 * @date      2020/12/3
 * @author    Pengshuwen
 * @describe
 */
class Mp4Utils {


    private var mMediaMuxer: MediaMuxer? = null

    //视频轨
    private var mVideoTrackIndex = -1

    //是否添加视频轨
    private var mIsVideoTrackAdd = true

    //音频轨
    private var mAudioTrackIndex = -1

    //是否添加音频轨
    private var mIsAudioTrackAdd = true

    private var mIsStart = false


    fun init() {
        val filePath = Environment.getExternalStorageDirectory().absolutePath.toString() + "/test${System.currentTimeMillis()}.mp4"
        val file = File(filePath)
        if (!file.exists()) {
            file.createNewFile()
        }
        mMediaMuxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }


    fun addVideoTrack(mediaFormat: MediaFormat) {

        mVideoTrackIndex //= try {
        mMediaMuxer?.addTrack(mediaFormat) ?: -1
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return
//        }
        mIsVideoTrackAdd = true


    }

    fun addAudioTrack(mediaFormat: MediaFormat) {
        if (mMediaMuxer != null) {
            mAudioTrackIndex = //try {
                mMediaMuxer?.addTrack(mediaFormat) ?: -1
//            } catch (e: Exception) {
//                e.printStackTrace()
//                return
//            }
            mIsAudioTrackAdd = true

        }
    }


    fun writeVideoData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (mIsStart) {
            mMediaMuxer?.writeSampleData(mVideoTrackIndex, byteBuffer, bufferInfo)
        }
    }

    fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (mIsStart) {
            mMediaMuxer?.writeSampleData(mAudioTrackIndex, byteBuffer, bufferInfo)
        }
    }


    fun startMuxer() {
        if (mIsAudioTrackAdd && mIsVideoTrackAdd) {
            mMediaMuxer?.start()
            mIsStart = false
            println("开始编码")
        }
    }

    fun release() {
        mIsAudioTrackAdd = false
        mIsVideoTrackAdd = false
        try {
            mMediaMuxer?.stop()
            mMediaMuxer?.release()
            mMediaMuxer = null

            println("编码完成")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}