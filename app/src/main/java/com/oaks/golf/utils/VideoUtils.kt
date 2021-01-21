package com.oaks.golf.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File


/**
 * @date      2021/1/11
 * @author    Pengshuwen
 * @describe
 */
object VideoUtils {

    fun getVideoThumbnail(context: Context, path: String, call: (MutableList<Bitmap>) -> Unit) {
        Thread {
            val bitmaps = mutableListOf<Bitmap>()
            val file = File(path)
            if (file.exists() && file.isFile) {
                var bitmap: Bitmap? = null
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.fromFile(file))
                    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val seconds = Integer.valueOf(time) / 1000
                    val timeS = Integer.valueOf(time) / 10
                    for (i in 1..10) {
                        bitmap = retriever.getFrameAtTime(
                            (i * timeS * 1000).toLong(),
                            MediaMetadataRetriever.OPTION_CLOSEST
                        )
                        bitmap?.let {
                            bitmaps.add(it)
                        }
                        println("bitmaps:size ${bitmaps.size}")
                    }
                    call.invoke(bitmaps)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                } finally {
                    try {
                        retriever.release()
                    } catch (e: RuntimeException) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

}