package com.oaks.golf

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import io.microshow.rxffmpeg.player.MeasureHelper
import io.microshow.rxffmpeg.player.RxFFmpegPlayerControllerImpl
import io.microshow.rxffmpeg.player.RxFFmpegPlayerView
import kotlinx.android.synthetic.main.activity_player2.*

class PlayerActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player2)

        val inputMp4 = "/storage/emulated/0/test1/11222.mp4"

        btn_play.setOnClickListener {
            mSurfaceView.play(inputMp4, true)
        }

        //设置播放器内核
        mSurfaceView.switchPlayerCore(RxFFmpegPlayerView.PlayerCoreType.PCT_RXFFMPEG_PLAYER);

        //设置控制层容器 和 视频尺寸适配模式
        mSurfaceView.setController(RxFFmpegPlayerControllerImpl(this), MeasureHelper.FitModel.FM_DEFAULT);


        mSurfaceView?.mPlayer?.setOnTimeUpdateListener { mediaPlayer, currentTime, totalTime ->
            mSeekBar.max = totalTime
            mSeekBar.progress = currentTime
        }

        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mSurfaceView.mPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

    }
}