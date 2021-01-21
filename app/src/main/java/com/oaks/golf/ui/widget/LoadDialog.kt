package com.oaks.golf.ui.widget

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment


/**
 * @date      2021/1/13
 * @author    Pengshuwen
 * @describe
 */
class LoadDialog : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LinearLayout(requireActivity()).apply {

            addView(ProgressBar(requireActivity()))
            addView(TextView(requireActivity()).apply {
                text = "请稍后"
            })
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#ffffff"))


        }
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(300,300)
        }
    }

}