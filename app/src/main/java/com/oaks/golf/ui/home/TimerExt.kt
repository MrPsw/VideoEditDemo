package com.oaks.golf.ui.home

import java.util.*


/**
 * @date      2020/11/6
 * @author    Pengshuwen
 * @describe
 */

fun Timer.schedule(TimerTaskRun: () -> Unit, period: Long): Timer {
    this.schedule(object : TimerTask() {
        override fun run() {
            TimerTaskRun.invoke()
        }
    }, period)
    return this
}

