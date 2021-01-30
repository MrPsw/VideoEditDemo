package com.oaks.golf

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.oaks.golf.ui.decoder.DecodeExt
import com.oaks.golf.ui.decoder.selectVideoTrack


/**
 * @date      2021/1/22
 * @author    Pengshuwen
 * @describe
 */
fun decode(path: String, call: (bitmaps: MutableList<Bitmap>) -> Unit) {

    val mVideoExtractor = MediaExtractor()
    mVideoExtractor.setDataSource(path)
    Thread {
        // 查看是否含有视频轨
        val trackIndex = mVideoExtractor.selectVideoTrack()
        if (trackIndex < 0) {
            throw RuntimeException("this data source not video")
        }
        //选择轨道
        mVideoExtractor.selectTrack(trackIndex)
        //获取视频的格式信息
        val trackFormat = mVideoExtractor.getTrackFormat(trackIndex)

        var duration = trackFormat.getLong(MediaFormat.KEY_DURATION);//总时间

        println("时长  ${duration}")

        //设置解码支持的格式
        trackFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatRGBFlexible
        )
        //获取视频宽高
        val width = trackFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = trackFormat.getInteger(MediaFormat.KEY_HEIGHT)

        //创建解码器
        val type = trackFormat.getString(MediaFormat.KEY_MIME)!!
        val decoder = MediaCodec.createDecoderByType(type)

        if (trackFormat == null) {
            Log.d("Decode", "doDecode: format is null")
        }
        decoder.configure(trackFormat, null, null, 0)
        decoder.start()
        //readVideoBuffer(decoder, trackFormat, width, height)

        val bitmaps = mutableListOf<Bitmap>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            for (time in 0 until duration / 1000000) {
                DecodeExt.getBitmapBySec(mVideoExtractor, trackFormat, decoder, time * 100000)?.let {
                    bitmaps.add(it)
                }
            }
        }
        call?.invoke(bitmaps)
    }.start()

}


