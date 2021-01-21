package com.oaks.golf.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Pengshuwen
 * @date 2021/1/7
 * @describe
 */
public class Utils {


    public Bitmap toBitmap(byte[] srcData, int format, int width, int height) {
        YuvImage yuv = new YuvImage(srcData, format, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
        Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(),
                0, stream.size());
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
