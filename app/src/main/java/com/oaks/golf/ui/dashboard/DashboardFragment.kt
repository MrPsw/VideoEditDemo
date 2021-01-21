package com.oaks.golf.ui.dashboard

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.oaks.golf.R
import com.oaks.golf.ui.recorder.CodecUtil
import com.oaks.golf.utils.MediaRecorder
import com.oaks.golf.utils.RecordVideoHelper
import kotlinx.android.synthetic.main.fragment_dashboard.*

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel

    private var mRecordHelper = RecordVideoHelper()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSurfaceView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mSurfaceView.keepScreenOn = true

        var codecUtil: CodecUtil? = null
        codecUtil = CodecUtil(requireActivity(), 300, 300) {

        }
        mSurfaceView.getRenderer()?.let { cameraRenderer ->
            cameraRenderer.addSurfaceCreated {
                mRecordHelper.previewCamera(it)
            }
        }


        start_record?.setOnClickListener {
            mRecordHelper?.startRecord("/storage/emulated/0/test1/录制的视频.mp4")
            codecUtil.start()
        }
        stop_record?.setOnClickListener {
            mRecordHelper?.stopRecord()
            codecUtil.stop()
        }
    }

    private fun showToast(str: String) {
        Toast.makeText(activity, str, Toast.LENGTH_SHORT).show()
    }
}