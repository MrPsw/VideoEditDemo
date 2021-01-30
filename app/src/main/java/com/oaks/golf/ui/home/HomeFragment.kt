package com.oaks.golf.ui.home

import android.content.Intent
import android.media.MediaFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.oaks.ffmpeg.FFmpegCmd
import com.oaks.ffmpeg.FFmpegCmd.OnCmdExecListener
import com.oaks.ffmpeg.FFmpegUtils
import com.oaks.golf.FFmpegPlayerActivity
import com.oaks.golf.PlayerActivity2
import com.oaks.golf.R
import com.oaks.golf.ui.decoder.DecoderUtils
import com.oaks.golf.ui.opengl.BitmapUtils
import com.oaks.golf.ui.widget.LoadDialog
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment() {

    private var mSurface: Surface? = null
    private lateinit var homeViewModel: HomeViewModel

    private var decoder1 = DecoderUtils()
    private var decoder2 = DecoderUtils()

    val videoPath1 = "/storage/emulated/0/test1/11222.mp4"
    val videoPath2 = "/storage/emulated/0/test1/11223.mp4"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        return inflater.inflate(R.layout.fragment_home, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        initViews()

        play.setOnClickListener {
            startVideoDecode()
        }


        var isTrue = false
        btn_turn.setOnClickListener {
            isTrue = !isTrue
            glView.setReverse(isTrue)
            glView2.setReverse(isTrue)
        }

        val inputMp4 = "/storage/emulated/0/test1/11222.mp4"
        val outMp4 = "/storage/emulated/0/test1/11223.mp4"
        val inputJPG = "/storage/emulated/0/test1/timg1.png"

        val inputMp42 = "/storage/emulated/0/test1/112222.mp4"
        val outMp42 = "/storage/emulated/0/test1/112223.mp4"
        val inputJPG2 = "/storage/emulated/0/test1/timg2.png"

        val inputMp43 = "/storage/emulated/0/test1/11222.mp4"
        val audioFile = "/storage/emulated/0/test1/森林狂想曲.mp3"
        val outMp43 = "/storage/emulated/0/test1/112223音频替换.mp4"

        video_info.text = FFmpegUtils.getVideoInfo(inputMp4)
        println(FFmpegUtils.getVideoInfo(inputMp4))
        watermark.setOnClickListener {
            addWaterMark(inputMp4, inputJPG, outMp4)
            //addWaterMark(inputMp42, inputJPG2, outMp42)
        }
        toH264.setOnClickListener {
            toH264(outMp4)
        }

        replace_audio.setOnClickListener {
            replaceAudio(inputMp43, audioFile, outMp43)
        }

        ffmpeg_player.setOnClickListener {
            startActivity(Intent(activity, FFmpegPlayerActivity::class.java))
        }
        ffmpeg_player2.setOnClickListener {
            startActivity(Intent(activity, PlayerActivity2::class.java))
        }


    }

    private fun addWaterMark(inputMp4: String, inputJPG: String, outMp4: String) {
        val bgn = System.currentTimeMillis()
        showLoadDialog()
        println("开始时间：$bgn")
        FFmpegUtils.addWaterMark(inputMp4, inputJPG, outMp4, object : OnCmdExecListener {
            override fun onSuccess() {
                println("结束时间：${System.currentTimeMillis()} 耗时：${(System.currentTimeMillis() - bgn) / 1000}秒")
                println("ffmpeg 操作成功")
                println("ffmpeg" + FFmpegUtils.getVideoInfo(outMp4))
                hideLoadDialog()
            }

            override fun onFailure() {
                println("ffmpeg 操作失败")
            }

            override fun onProgress(progress: Float) {
                println("ffmpeg 更新进度")
            }
        })

    }


    private fun replaceAudio(inputMp4: String, pcm: String, outMp4: String) {
        val bgn = System.currentTimeMillis()
        showLoadDialog()
        println("开始时间：$bgn")
        FFmpegUtils.replaceAudio(inputMp4, pcm, outMp4, object : OnCmdExecListener {
            override fun onSuccess() {
                println("结束时间：${System.currentTimeMillis()} 耗时：${(System.currentTimeMillis() - bgn) / 1000}秒")
                println("ffmpeg 操作成功")
                println("ffmpeg" + FFmpegUtils.getVideoInfo(outMp4))
                hideLoadDialog()
            }

            override fun onFailure() {
                println("ffmpeg 操作失败")
            }

            override fun onProgress(progress: Float) {
                println("ffmpeg 更新进度")
            }
        })

    }


    private var loadDialog: LoadDialog? = null

    fun showLoadDialog() {
        loadDialog = LoadDialog()
        loadDialog?.show(childFragmentManager, "")
    }

    fun hideLoadDialog() {
        loadDialog?.dismiss()
    }


    fun toH264(input: String) {
        var cmd2 = "ffmpeg -i %s -vcodec libx264 /storage/emulated/0/test1/test.mp4"
        cmd2 = String.format(cmd2, input)
        FFmpegCmd.exeCmd(
            cmd2.replace("  ", " ").split(" ".toRegex()).toTypedArray(),
            0,
            null
        )
    }

    private fun initViews() {
        glView.addSurfaceCreatedListener {
            decoder1.bindSurfaceTexture(it)
        }

        glView2.addSurfaceCreatedListener {
            decoder2.bindSurfaceTexture(it)
        }
    }


    fun showToast(string: String) {
        Toast.makeText(activity, string, Toast.LENGTH_SHORT).show()
    }


    private fun startVideoDecode() {

        decoder1.start(videoPath1, object : DecoderUtils.OnDecodingProgressListener {
            override fun onFrame(format: MediaFormat, width: Int, height: Int, time: Long) {
                glView.setVideoSize(width, height)
                seek_bar.post {
                    seek_bar.max = (decoder1.getDuration() / 1000000).toInt()
                    seek_bar.setProgress((time / 1000).toInt())
                    tv_time2.text = "${time / 1000}秒"
                }

            }

            override fun onFinish() {
            }

        })

        decoder2.start(videoPath2, object : DecoderUtils.OnDecodingProgressListener {
            override fun onFrame(format: MediaFormat, width: Int, height: Int, time: Long) {
                glView2.setVideoSize(width, height)
                tv_time.post {
                    tv_time.text = "${time / 1000}秒"
                }
            }

            override fun onFinish() {

            }

        })

        src_bitmap.setImageBitmap(BitmapUtils.createBitmap(788, 788))

        glView.setCanvasListener {
            src_bitmap.setImageBitmap(it)
        }

        seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    decoder1.seekTo(progress * 1000L * 1000L)
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }


}