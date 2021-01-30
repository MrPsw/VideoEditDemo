package com.oaks.golf.ui.decoder

import android.graphics.SurfaceTexture
import android.media.*
import android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import com.oaks.golf.ui.audio.AudioPlayer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors


/**
 * @date      2020/12/1
 * @author    Pengshuwen
 * @describe
 */
class DecoderUtils {


    private var startMs: Long = 0
    private val TAG = "Decoder"
    private var isSeek = false


    private var mVideoMediaExtractor: MediaExtractor

    private var mAudioMediaExtractor: MediaExtractor

    private var audioDecoder: MediaCodec? = null

    private var videoDecoder: MediaCodec? = null

    private var isPlay = false

    private var path: String? = null

    //用于解码后数据的输出，交给OpenGL渲染
    private var mSurface: Surface? = null

    //用于播放音频
    private var mAudioPlayer: AudioPlayer? = null

    private var mCall: OnDecodingProgressListener? = null


    constructor() {
        mVideoMediaExtractor = MediaExtractor()
        mAudioMediaExtractor = MediaExtractor()
    }

    fun bindSurfaceTexture(surfaceTexture: SurfaceTexture?) {
        surfaceTexture?.let {
            this.mSurface = Surface(surfaceTexture)
        }
    }

    fun seekTo(long: Long) {

        isSeek = true
        stopDecode()

        mVideoMediaExtractor?.seekTo(long, SEEK_TO_CLOSEST_SYNC)
        mAudioMediaExtractor?.seekTo(long, SEEK_TO_CLOSEST_SYNC)

        startMs = System.currentTimeMillis() - (long / 1000)
        println("重置解码开始时间${startMs} + ${long} =  ${System.currentTimeMillis()}  ")

        startDecode()

    }


    fun startDecode() {
        audioDecoder?.start()
        videoDecoder?.start()
        isPlay = true
    }

    fun stopDecode() {
        audioDecoder?.stop()
        videoDecoder?.stop()
        isPlay = false
    }


    fun start(videoPath: String, call: OnDecodingProgressListener) {

        if (mSurface == null) return
        if (path != videoPath) {
            mAudioMediaExtractor = MediaExtractor()
            mVideoMediaExtractor = MediaExtractor()
        }
        this.path = videoPath
        this.mCall = call

        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "video path is empty")
            return
        }
        val file = File(path)
        if (file == null || !file.exists()) {
            Log.e(TAG, "video path is not exists")
            return
        }


        mVideoMediaExtractor.setDataSource(path!!)
        mAudioMediaExtractor.setDataSource(path!!)

        //开启线程解码
        var mExecutorService = Executors.newFixedThreadPool(2);
        mExecutorService.execute(Thread {
            startVideoDecode()
        })
        mExecutorService.execute(Thread {
            startAudioDecode()
        })
    }


    private fun startVideoDecode() {
        if (path == null || mSurface == null) return

        var decodeSucceed = false  //解码完成
        var exceptionOccur = false //解码异常

        try {
            videoDecoder = this.doVideoDecode(mSurface!!)
        } catch (e: IOException) {
            Log.e(TAG, "doDecode exception: $e")
            exceptionOccur = true
        } finally {
            if (mVideoMediaExtractor != null) {
                mVideoMediaExtractor.release()
            }
            videoDecoder?.let {
                decodeSucceed = true
                it.stop()
                //it.release()
            }

        }

        if (decodeSucceed) {
            Log.d(TAG, "解码完成")
        }

        if (exceptionOccur) {
            Log.d(TAG, "解码失败")
        }
    }


    public fun startAudioDecode() {
        if (path == null || mSurface == null) return

        var decodeSucceed = false
        var exceptionOccur = false
        try {
            audioDecoder = doAudioDecode()
        } catch (e: IOException) {
            Log.e(TAG, "doDecode exception: $e")
            exceptionOccur = true
        } finally {
            if (mAudioMediaExtractor != null) {
                mAudioMediaExtractor.release()
            }
            audioDecoder?.let {
                decodeSucceed = true
                it.stop()
                // it.release()
            }

        }

        if (decodeSucceed) {
            Log.d(TAG, "解码成功")
        }

        if (exceptionOccur) {
            Log.d(TAG, "解码失败")
        }
    }


    private fun doAudioDecode(): MediaCodec? {
        // 查看是否含有音频轨
        val trackIndex = mAudioMediaExtractor.selectAudioTrack()
        if (trackIndex < 0) {
            throw RuntimeException("this data source not audio")
        }
        mAudioMediaExtractor.selectTrack(trackIndex)
        //获取音频侦
        val trackFormat = mAudioMediaExtractor.getTrackFormat(trackIndex)
        //创建解码器
        val type = trackFormat.getString(MediaFormat.KEY_MIME)!!

        val decoder = MediaCodec.createDecoderByType(type)
        decoder.configure(trackFormat, null, null, 0)

        mAudioPlayer = AudioPlayer(
            trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        decoder.start()

        readAudioBuffer(decoder)

        return decoder;
    }

    private fun readAudioBuffer(decoder: MediaCodec) {


        var isAudioFinish = false
        var inputEof = false
        val bufferInfo = MediaCodec.BufferInfo()
        val inputBuffers: Array<ByteBuffer> = decoder.inputBuffers
        var outputBuffers: Array<ByteBuffer> = decoder.outputBuffers


        // 开始的时间
        startMs = System.currentTimeMillis()

        while (!isAudioFinish) {
            if (!inputEof) {
                val inIndex: Int = decoder.dequeueInputBuffer(10 * 1000)
                if (inIndex >= 0) {
                    val inputBuffer = inputBuffers[inIndex]
                    val size = mAudioMediaExtractor.readSampleData(inputBuffer, 0)
                    if (size < 0) {
                        inputEof = true
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, size, mAudioMediaExtractor.sampleTime, 0)
                        mAudioMediaExtractor.advance()
                    }
                }
            }


            // 返回一个被成功解码的buffer的 index 或者是一个信息  同时更新 videoBufferInfo 的数据
            val outIndex: Int = decoder.dequeueOutputBuffer(bufferInfo, 10 * 1000)
            when (outIndex) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED")
                    outputBuffers = decoder.getOutputBuffers()
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d(TAG, "New format " + decoder.getOutputFormat())
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d(TAG, "dequeueOutputBuffer timed out!")
                else -> {
                    val buffer = outputBuffers[outIndex]
                    Log.v(
                        TAG,
                        "We can't use this buffer but render it due to the API limit, $buffer"
                    )

                    sleepRender(bufferInfo)

                    //用来保存解码后的数据
                    val outData = ByteArray(bufferInfo.size)
                    buffer[outData]
                    //清空缓存
                    buffer.clear()
                    //播放解码后的数据
                    mAudioPlayer?.play(outData, 0, bufferInfo.size)

                    decoder.releaseOutputBuffer(outIndex, false)
                }
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM !== 0) {
                Log.v(TAG, "buffer stream end")
                mCall?.onFinish()
                break
            }


        }

    }


    private var duration: Long = 0L

    fun getDuration(): Long {
        return duration
    }


    private fun doVideoDecode(surface: Surface): MediaCodec? {

        // 查看是否含有视频轨
        val trackIndex = mVideoMediaExtractor.selectVideoTrack()
        if (trackIndex < 0) {
            throw RuntimeException("this data source not video")
        }
        //选择轨道
        mVideoMediaExtractor.selectTrack(trackIndex)
        //获取视频的格式信息
        val trackFormat = mVideoMediaExtractor.getTrackFormat(trackIndex)

        duration = trackFormat.getLong(MediaFormat.KEY_DURATION);//总时间

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
            Log.d(TAG, "doDecode: format is null")
        }
        decoder.configure(trackFormat, surface, null, 0)
        decoder.start()
        isPlay = true
        readVideoBuffer(decoder, trackFormat, width, height)

        return decoder
    }

    private fun readVideoBuffer(
        decoder: MediaCodec,
        trackFormat: MediaFormat,
        width: Int,
        height: Int
    ) {
        val inputBuffers: Array<ByteBuffer> = decoder.inputBuffers
        var outBuffers: Array<ByteBuffer> = decoder.outputBuffers

        val bufferInfo = MediaCodec.BufferInfo()

        var inputEof = false
        var outputEof = false


        // 开始的时间
        startMs = System.currentTimeMillis()

        var frameIndex = 0
        while (!outputEof) {
            //解码数据
            if (!inputEof) {
                inputEof = DecodeExt.redInputBuffer(mVideoMediaExtractor, decoder, inputBuffers)
            }
            //读取数据显示
            DecodeExt.redOutBuffer(decoder, bufferInfo) { outIndex ->
                //如果缓冲区里的可展示时间>当前视频播放的总时间，就休眠一下 展示当前的帧，
                if (!isPlay) return@redOutBuffer
                sleepRender(bufferInfo)
                mCall?.onFrame(trackFormat, width, height, bufferInfo.presentationTimeUs / 1000)
                //渲染为true就会渲染到surface   configure() 设置的surface
                decoder.releaseOutputBuffer(outIndex, true)
                frameIndex++
                Log.v(TAG, "frameIndex   $frameIndex")
            }
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM !== 0) {
                Log.v(TAG, "buffer stream end")
                mCall?.onFinish()
                break
            }
        }
    }

    private fun sleepRender(bufferInfo: MediaCodec.BufferInfo) {
        // 这里的时间是 毫秒  presentationTimeUs 的时间是累加的 以微秒进行一帧一帧的累加
        // audioBufferInfo 是改变的

        if (isSeek) {
            println("bufferInfo.presentationTimeUs ${bufferInfo.presentationTimeUs / 1000}  >  ${System.currentTimeMillis() - startMs} currentTime= ${System.currentTimeMillis()}  startMs= ${startMs}")
        }

        println("presentationTimeUs:" + bufferInfo.presentationTimeUs)

        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
            try {
                // 10 毫秒
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                break
            }
        }
    }



    interface OnDecodingProgressListener {
        fun onFrame(format: MediaFormat, width: Int, height: Int, time: Long)
        fun onFinish()
    }

}