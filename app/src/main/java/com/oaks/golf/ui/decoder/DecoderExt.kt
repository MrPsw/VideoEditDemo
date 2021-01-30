package com.oaks.golf.ui.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


/**
 * @date      2021/1/22
 * @author    Pengshuwen
 * @describe
 */


fun MediaExtractor.selectVideoTrack(): Int {
    val numTracks = trackCount
    for (i in 0 until numTracks) {
        val format = getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime?.startsWith("video/") == true) {
            return i
        }
    }
    return -1
}


fun MediaExtractor.selectAudioTrack(): Int {
    val numTracks = trackCount
    for (i in 0 until numTracks) {
        val format = getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime?.startsWith("audio/") == true) {
            return i
        }
    }
    return -1
}


 fun bitmapToRGB(bitmap: Bitmap): ByteArray? {
    val bytes = bitmap.byteCount
    val buffer = ByteBuffer.allocate(bytes)
    bitmap.copyPixelsToBuffer(buffer)
    val rgba = buffer.array()
    val pixels = ByteArray(rgba.size / 4 * 3)
    val count = rgba.size / 4
    for (i in 0 until count) {
        pixels[i * 3] = rgba[i * 4]
        pixels[i * 3 + 1] = rgba[i * 4 + 1]
        pixels[i * 3 + 2] = rgba[i * 4 + 2]
    }
    return pixels
}

 fun YUV420_To_RGB(image: Image): ByteArray? {

    if (image.format != 35) {
        Log.d("ImeVideoPlayer", "Image Format " + image.format)
    }
    val width = image.width
    val height = image.height
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer[nv21, 0, ySize]
    vBuffer[nv21, ySize, vSize]
    uBuffer[nv21, ySize + vSize, uSize]
    val yuvImage = YuvImage(nv21, 17, width, height, null as IntArray?)
    val stream = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, stream)
    val bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
    return bitmapToRGB(bitmap)
}



