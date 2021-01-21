package com.oaks.golf.ui.audio

import android.media.AudioManager
import android.media.AudioTrack


/**
 * @date      2020/12/2
 * @author    Pengshuwen
 * @describe
 */
class AudioPlayer {


    private var mAudioTrack: AudioTrack? = null

    constructor(frequency: Int, channel: Int, sampbit: Int) {

        mAudioTrack?.let {
            it.stop()
            it.release()
        }


        // 获得构建对象的最小缓冲区大小
        val minBufSize = AudioTrack.getMinBufferSize(frequency, channel, sampbit)
        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            frequency, channel, sampbit, minBufSize, AudioTrack.MODE_STREAM
        )
        mAudioTrack?.play()
    }


    /**
     * 将解码后的pcm数据写入audioTrack播放
     *
     * @param data   数据
     * @param offset 偏移
     * @param length 需要播放的长度
     */
    fun play(data: ByteArray?, offset: Int, length: Int) {
        if (data == null || data.isEmpty()) {
            return
        }
        try {
            mAudioTrack?.write(data, offset, length)
            println("播放音频")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}