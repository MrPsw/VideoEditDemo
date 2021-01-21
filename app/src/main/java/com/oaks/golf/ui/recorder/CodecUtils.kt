package com.oaks.golf.ui.recorder

import android.content.Context
import android.media.*
import android.os.*
import android.util.Log
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer

class CodecUtil(context: Context, width: Int, height: Int, call: (surface: Surface) -> Unit) {
    //H.264
    private var mediaCodec: MediaCodec? = null
    private var audioCodec: MediaCodec? = null

    //mp4
    private var mediaMuxer: MediaMuxer? = null
    private var audioRecord: AudioRecord? = null

    //获取openGl渲染的数据
    private var eglCore: EglCore? = null

    private var handlerThread: HandlerThread? = null
    private var handler: EncodeHandler? = null
    private val bufferInfo = MediaCodec.BufferInfo()
    private var videoTrack: Int = -1
    private var audioTrack: Int = -1
    private var baseTimeStamp: Long = -1L
    val START_RECORD: Int = 0
    val STOP_RECORD: Int = 1
    var isRecording = false
    private var mAudioRunnable: AudioCodecRunnable? = null
    private var mAudioThread: Thread? = null
    private var bufferSize: Int = 0
    private val lock: Object = Object()
    private var hasStartMuxer: Boolean = false

    init {
        handlerThread = HandlerThread("videoEncode")
        handlerThread?.let {
            it.start()
            handler = EncodeHandler(it.looper)
        }
        val path = context.getExternalFilesDir(null)
        val file = File("$path/test1")
        if (!file.exists()) {
            file.mkdir()
        }
        mediaMuxer =
            MediaMuxer("/$path/test1/test2.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val mediaFormat: MediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3500000)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mediaCodec?.let {
            it.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = it.createInputSurface()
            call?.invoke(surface)
            eglCore = EglCore(surface)
            it.start()
        }
        val audioMediaFormat: MediaFormat = MediaFormat.createAudioFormat(
            "audio/mp4a-latm" //音频编码的Mime
            , 48000, 2
        )
        audioMediaFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        audioMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
        audioCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        audioCodec?.configure(audioMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioCodec?.start()
        bufferSize = AudioRecord.getMinBufferSize(
            48000,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            48000,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        mAudioRunnable = AudioCodecRunnable()
        mAudioThread = Thread(mAudioRunnable)
        mAudioThread?.start()
    }



    fun drainEncode(endOfStream: Boolean) {
        var outPutBuffer = mediaCodec?.outputBuffers;
        mediaCodec?.let {
            while (isRecording) {
                val codeStatus: Int = it.dequeueOutputBuffer(bufferInfo, 10000)
                if (codeStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) {
                        break
                    }
                } else if (codeStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outPutBuffer = mediaCodec?.outputBuffers;
                } else if (codeStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized(lock) {
                        mediaCodec?.let {
                            var mediaFormat = it.outputFormat;
                            mediaMuxer?.let {
                                videoTrack = it.addTrack(mediaFormat)
                                if (videoTrack >= 0 && audioTrack >= 0) {
                                    it.start()
                                    hasStartMuxer = true
                                }
                            }
                        }
                    }
                } else if (codeStatus < 0) {
                    Log.e("CodecUtils", "->unexpected result from encoder.dequeueOutputBuffer code=$codeStatus")
                } else {
                    var encodecData = outPutBuffer?.get(codeStatus)
                    encodecData?.let {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0 && hasStartMuxer) {
                            mediaMuxer?.writeSampleData(videoTrack, encodecData, bufferInfo)
                        }
                    }
                    mediaCodec?.releaseOutputBuffer(codeStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
        }
    }

    fun release() {
        Thread {
            try {
                stop()
                handlerThread?.quit()
                mAudioThread?.interrupt()
                audioRecord?.stop()
                audioRecord?.release()
                audioCodec?.stop()
                audioCodec?.release()
                mediaCodec?.signalEndOfInputStream()
                mediaCodec?.stop()
                mediaCodec?.release()
                eglCore?.release()
                audioRecord = null
                audioCodec = null
                handlerThread = null
                mediaCodec = null
                eglCore = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    inner class EncodeHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                START_RECORD -> {
                    drainEncode(false)
                    prepare()
                    draw()//如果不绘画将得到无画面的视频
                    if (baseTimeStamp == -1L) {
                        baseTimeStamp = System.nanoTime()
                    }
                    var time = System.nanoTime() - baseTimeStamp
                    eglCore?.setPresentTime(time)
                    eglCore?.swapBuffer()
                }
                STOP_RECORD -> {
                    drainEncode(true)
                }
            }
        }
    }

    fun start() {
        isRecording = true
        handler?.sendEmptyMessage(START_RECORD)
    }

    fun stop() {
        isRecording = false
        handler?.sendEmptyMessage(STOP_RECORD)
    }

    private var hasPrepare: Boolean = false
    fun prepare() {
        if (hasPrepare) {
            return
        }
        hasPrepare = true
        eglCore?.eglMakeCurrent()
        //showFilter.create()
    }

//    private var showFilter: NoFilter = NoFilter();

    fun setTextureId(textureId: Int) {
        //showFilter.setTextureId(textureId)
    }

    fun draw() {
        //showFilter.drawFrame()
    }

    private inner class AudioCodecRunnable : Runnable {
        private var baseTimeStamp: Long = -1
        var mInfo = MediaCodec.BufferInfo()

        init {
            baseTimeStamp = System.nanoTime()
            audioRecord?.startRecording()
        }

        override fun run() {
            while (isRecording) {
                if (audioCodec != null) {
                    audioCodec?.let {
                        val index: Int = it.dequeueInputBuffer(0)
                        if (index >= 0) {
                            val buffer: ByteBuffer? =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    it.getInputBuffer(index)
                                } else {
                                    it.inputBuffers[index]
                                }
                            buffer?.let {
                                it.clear()
                                val audioLength: Int = audioRecord?.read(it, bufferSize) ?: 0
                                if (audioLength > 0) {
                                    val curTimeStamp: Long = System.nanoTime()
                                    val time = (curTimeStamp - baseTimeStamp) / 1000
                                    var endFlag: Int = if (isRecording) {
                                        0
                                    } else {
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    }
                                    audioCodec?.queueInputBuffer(//将audioRecord的数据放置在MediaCodec中转码再塞到MediaMuxer合成Mp4
                                        index,
                                        0,
                                        audioLength,
                                        time,
                                        endFlag
                                    )
                                }
                            }
                        }
                        var outIndex: Int = MediaCodec.INFO_TRY_AGAIN_LATER
                        do {
                            if (audioCodec == null) {
                                outIndex = 1
                            } else {
                                outIndex = it.dequeueOutputBuffer(mInfo, 0)
                                if (outIndex >= 0) {
                                    val buffer: ByteBuffer? =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            it.getOutputBuffer(outIndex)
                                        } else {
                                            it.outputBuffers[outIndex]
                                        }
                                    buffer?.position(mInfo.offset)
                                    if (buffer != null && hasStartMuxer && mInfo.presentationTimeUs > 0) {
                                        try {
                                            mediaMuxer?.writeSampleData(audioTrack, buffer, mInfo)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    it.releaseOutputBuffer(outIndex, false)
                                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    synchronized(lock) {
                                        var mediaFormat = it.outputFormat;
                                        mediaMuxer?.let {
                                            audioTrack = it.addTrack(mediaFormat)
                                            if (videoTrack >= 0 && audioTrack >= 0) {
                                                it.start()
                                                hasStartMuxer = true
                                            }
                                        }
                                    }
                                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {

                                }
                            }
                        } while (outIndex >= 0)
                    }
                } else {
                    break
                }
            }
        }
    }
}