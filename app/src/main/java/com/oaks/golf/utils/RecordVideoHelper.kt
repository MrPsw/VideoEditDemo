package com.oaks.golf.utils

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Build
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import java.io.File


/**
 * @date      2021/1/14
 * @author    Pengshuwen
 * @describe
 */
class RecordVideoHelper {

    private var mRecorder: MediaRecorder? = null
    private var camera: Camera? = null
    private var mSurfaceHolder: SurfaceHolder? = null

    fun previewCamera(mSurfaceTexture: SurfaceTexture?) {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)?.apply {
            if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            }
            setPreviewTexture(mSurfaceTexture)
            setDisplayOrientation(90)
            startPreview()
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startRecord(savePath: String) {
        if (mRecorder == null) {
            mRecorder = MediaRecorder()
        }


        if (camera != null) {
            camera?.setDisplayOrientation(90);
            camera?.unlock();
            mRecorder?.setCamera(camera);
        }
        try {
            // 这两项需要放在setOutputFormat之前
            mRecorder?.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            mRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)

            // Set output file format
            mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            // 这两项需要放在setOutputFormat之后
            mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP)
            mRecorder?.setVideoSize(640, 480)
            mRecorder?.setVideoFrameRate(30)
            mRecorder?.setVideoEncodingBitRate(3 * 1024 * 1024)
            mRecorder?.setOrientationHint(90)
            //设置记录会话的最大持续时间（毫秒）
            mRecorder?.setMaxDuration(30 * 1000)
            mRecorder?.setPreviewDisplay(mSurfaceHolder?.getSurface())

            val saveFile = File(savePath)
//            if (!saveFile.exists()) {
//                saveFile.createNewFile()
//            }
            mRecorder?.setOutputFile(saveFile)
            mRecorder?.prepare()
            mRecorder?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun stopRecord() {
        mRecorder?.stop();
        mRecorder?.reset();
        mRecorder?.release();
        mRecorder == null
    }



}