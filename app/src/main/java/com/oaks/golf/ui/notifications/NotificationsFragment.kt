package com.oaks.golf.ui.notifications

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.oaks.golf.R
import com.oaks.golf.decode
import com.oaks.golf.ui.widget.graffiti.DrawingBoardView
import com.oaks.golf.utils.RecordAudioUtil
import kotlinx.android.synthetic.main.fragment_notifications.*
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File


class NotificationsFragment : Fragment() {

    private lateinit var notificationsViewModel: NotificationsViewModel

    private var isReverse = false

    private var mediaRecorder: MediaRecorder? = null

    private var audioRecorder = RecordAudioUtil()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_notifications, container, false)
        val textView: TextView = root.findViewById(R.id.text_notifications)
        notificationsViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        return root
    }

    @SuppressLint("WrongConstant")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val inputMp4 = "/storage/emulated/0/test1/11122.mp4"

        val player = initMediaPlayer(inputMp4)


        initIjkMediaPlayer(inputMp4)

        btn_turn.setOnClickListener {
            isReverse = !isReverse
            glView.setReverse(isReverse)
        }

        view_crop_seekbar.setVideoUri(inputMp4)



        view_crop_seekbar.onSeekChange = {
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                println("sdsdsd $it")
                player.seekTo(it, 3)
//                mediaPlayer.seekTo(it)
//                mediaPlayer.pause()
            }
            player.pause()

        }


//        player.setOnInfoListener { mp, what, extra ->
//            view_crop_seekbar.g
//        }
//        glView.addSurfaceCreatedListener {
//            val surface = Surface(it)
//            player.setSurface(surface)
//            player.prepare()
//            player.seekTo(0)
//            player.start()
//
//            val isSuccess = audioRecorder.startRecord(requireActivity(), "/storage/emulated/0/test1/音频.pcm", null)
//            if (isSuccess) {
//                println("开启录制")
//            }
//            //  startRecorder(surface)
//        }

        player.setOnCompletionListener {
//            mediaRecorder?.stop()
//            mediaRecorder?.reset()
            audioRecorder.stopRecord()
        }




        decode(inputMp4) {
            it.forEach {
                image_h?.post {
                    image_h.addView(ImageView(requireActivity()).apply {
                        setImageBitmap(it)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    })
                }
            }
        }





        initDrawingBoard()

    }

    private fun initMediaPlayer(inputMp4: String): MediaPlayer {
        val player = MediaPlayer()
        player.setDataSource(inputMp4)

        player.setOnVideoSizeChangedListener { mp, width, height ->
            glView.setVideoSize(width, height)
            println("player w ${width} h ${height} ")
        }


        glView.holder.addCallback(object :SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                player.setDisplay(holder)
                player.prepare()
                player.start()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })


        return player
    }

    private fun initIjkMediaPlayer(inputMp4: String) {
        //初始化
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        val mediaPlayer = IjkMediaPlayer()
        mediaPlayer.setOption(1, "analyzemaxduration", 100L);
        mediaPlayer.setOption(1, "probesize", 10240L);
        mediaPlayer.setOption(1, "flush_packets", 1L);
        mediaPlayer.setOption(4, "packet-buffering", 0L);
        mediaPlayer.setOption(4, "framedrop", 1L);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48)
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1)
        mediaPlayer.dataSource = inputMp4
        mediaPlayer.setDisplay(glView.holder)
        mediaPlayer.setOnVideoSizeChangedListener { iMediaPlayer, i, i2, i3, i4 ->
            glView.setVideoSize(i, i2)
        }

        glView.addSurfaceCreatedListener {
            mediaPlayer.setSurface(Surface(it))
        }
        btn_play.setOnClickListener {
            mediaPlayer.prepareAsync()
            mediaPlayer.start()
        }
    }

    private fun startRecorder(surface: Surface) {

        mediaRecorder = MediaRecorder()
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)


        // 设置音频采集方式
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        //设置文件的输出格式
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        //设置audio的编码格式
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        //设置video的编码格式
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        //设置录制的视频编码比特率
        mediaRecorder?.setVideoEncodingBitRate(1024 * 1024)
        //设置录制的视频帧率,注意文档的说明:
        mediaRecorder?.setVideoFrameRate(30)
        //设置要捕获的视频的宽度和高度
        mediaRecorder?.setVideoSize(1080, 1920) //最高只能设置640x480

        //设置记录会话的最大持续时间（毫秒）
        //设置记录会话的最大持续时间（毫秒）
        mediaRecorder?.setMaxDuration(60 * 1000)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mediaRecorder?.setInputSurface(surface)


        }

        val path = File("/storage/emulated/0/test1/11223录制.mp4")
        if (!path.exists()) {
            path.createNewFile()
        }
        //设置输出文件的路径
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaRecorder?.setOutputFile(path)
        }
        //准备录制
        mediaRecorder?.prepare()
        //开始录制
        mediaRecorder?.start()
    }

    private fun initDrawingBoard() {
        btn_curve.setOnClickListener {
            drawing_board.setStyle(DrawingBoardView.STYLE_CURVE)
        }
        btn_line.setOnClickListener {
            drawing_board.setStyle(DrawingBoardView.STYLE_BEELINE)
        }
        btn_round.setOnClickListener {
            drawing_board.setStyle(DrawingBoardView.STYLE_OVAL)
        }
        btn_angle.setOnClickListener {
            drawing_board.setStyle(DrawingBoardView.STYLE_ANGLE)
        }
        btn_square.setOnClickListener {
            drawing_board.setStyle(DrawingBoardView.STYLE_SQUARE)
        }
        btn_retreat.setOnClickListener {
            drawing_board.retreat()
        }

        btn_square

    }


}