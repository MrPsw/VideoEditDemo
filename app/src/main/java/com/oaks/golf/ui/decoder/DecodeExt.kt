package com.oaks.golf.ui.decoder

import android.media.MediaCodec
import android.media.MediaExtractor
import android.util.Log
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
                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
                decoder.queueInputBuffer(inIndex, 0, size, mVideoMediaExtractor.sampleTime, 0)
                mVideoMediaExtractor.advance()
            }
        }
        return isComplete
    }

    fun redOutBuffer(decoder: MediaCodec, bufferInfo: MediaCodec.BufferInfo, call: (outIndex:Int) -> Unit) {
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


}