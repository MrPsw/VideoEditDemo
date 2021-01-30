package com.oaks.golf.ui.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.IOException
import java.nio.ByteBuffer


/**
 * @date      2021/1/11
 * @author    Pengshuwen
 * @describe
 */
object DecodeExt {

    fun redInputBuffer(mVideoMediaExtractor: MediaExtractor, decoder: MediaCodec, inputBuffers: Array<ByteBuffer>): Boolean {

        var isComplete = false
        val inIndex = decoder.dequeueInputBuffer(10 * 1000)
        if (inIndex >= 0) {
            val inputBuffer = inputBuffers[inIndex]  //获取一个空的inputBuffer
            inputBuffer.clear()
            //readSampleData 将数据写入inputBuffer
            val size = mVideoMediaExtractor.readSampleData(inputBuffer, 0)
            if (size < 0) {
                isComplete = true
                decoder.queueInputBuffer(inIndex, 0, 0, 0, BUFFER_FLAG_END_OF_STREAM)
            } else {
                decoder.queueInputBuffer(inIndex, 0, size, mVideoMediaExtractor.sampleTime, 0)
                mVideoMediaExtractor.advance()
            }
        }
        return isComplete
    }

    fun redOutBuffer(decoder: MediaCodec, bufferInfo: MediaCodec.BufferInfo, call: (outIndex: Int) -> Unit) {
        // 返回一个被成功解码的buffer的 index 或者是一个信息  同时更新 videoBufferInfo 的数据
        val outIndex = decoder.dequeueOutputBuffer(bufferInfo, 10 * 1000)
        when (outIndex) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Log.v("TAG", "format changed")
            MediaCodec.INFO_TRY_AGAIN_LATER -> Log.v("TAG", "超时")
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                Log.v("TAG", "output buffers changed")
                //outBuffers = decoder.outputBuffers
            }
            else -> {
                call?.invoke(outIndex)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Throws(IOException::class)
    fun getBitmapBySec(extractor: MediaExtractor, mediaFormat: MediaFormat, decoder: MediaCodec, sec: Long): Bitmap? {
        val info = MediaCodec.BufferInfo()
        var bitmap: Bitmap? = null
        var sawInputEOS = false
        var sawOutputEOS = false
        val stopDecode = false
        val width: Int = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height: Int = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
        var presentationTimeUs: Long = -1
        var outputBufferId: Int
        var image: Image? = null

        //视频定位到指定的时间的上一帧
        extractor.seekTo(sec, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        //因为extractor定位的帧不是准确的，所以我们要用一个循环不停读取下一帧来获取我们想要的时间画面。
        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                val inputBufferId = decoder.dequeueInputBuffer(-1)
                if (inputBufferId >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferId)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        //获取定位的帧的时间
                        presentationTimeUs = extractor.sampleTime
                        //把定位的帧压入队列
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                        //跳到下一帧
                        extractor.advance()
                    }
                }
            }
            outputBufferId = decoder.dequeueOutputBuffer(info, 100L)
            if (outputBufferId >= 0) {
                //能够有效输出
                if (info.flags and BUFFER_FLAG_END_OF_STREAM != 0 || presentationTimeUs.toInt() >= sec) {
                    //时间是指定时间或者已经是视频结束时间，停止循环
                    sawOutputEOS = true
                    val doRender = info.size != 0
                    if (doRender) {
                        //获取指定时间解码出来的Image对象。
                        image = decoder.getOutputImage(outputBufferId)
                        image?.let {
                            val rgbBuffer = YUV420_To_RGB(image)
                            bitmap = BitmapFactory.decodeByteArray(rgbBuffer, 0, rgbBuffer?.size ?: 0)
                        }
                        image?.close()
                    }
                }
                decoder.releaseOutputBuffer(outputBufferId, true)
            }
        }
        return bitmap
    }


}