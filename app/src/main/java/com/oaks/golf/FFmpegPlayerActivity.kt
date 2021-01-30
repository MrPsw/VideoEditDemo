package com.oaks.golf

import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.oaks.ffmpeg.MediaPlayer
import kotlinx.android.synthetic.main.activity_ffmpeg_player.*

class FFmpegPlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg_player)

        val inputMp4 = "/storage/emulated/0/test1/11222.mp4"
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(inputMp4)
        mediaPlayer.prepare()
        mediaPlayer.setOnProgressListener {
            seek_bar.progress = it
            seek_bar.max = mediaPlayer.duration.toInt()
            println("进度:$it")
        }

        btn_pause.setOnClickListener {
            mediaPlayer.pause()
        }

        btn_play.setOnClickListener {
            mediaPlayer.start()
        }

        seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.setPosition(progress.toLong(), true)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer.setPosition(seekBar!!.progress.toLong(), false)
            }

        })

        mSurfaceView2.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.setSurface(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })

    }
}