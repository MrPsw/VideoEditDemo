package com.oaks.golf.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * CreateTime 2017/10/13 22:25
 * Author LiuShiHua
 * Description：录音
 * <p>
 * 1.判断没有存储卡——不录音
 * 2.判断存储卡容量小于50M——不录音
 * 3.默认小于1秒不保存
 * <p>
 * 自动保存
 */

public class RecordAudioUtil {

    private boolean highQuality = true; // 高质量为true, 低质量为false

    private MediaRecorder mMediaRecorder;
    private boolean isRunning = false;
    private File saveFile;
    private long endTimeLong;
    private long startTimeLong;
    private FileInputStream fis;
    private FileOutputStream fos;
    private Timer timer;
    private Handler handler;
    private final int HAND_VOICE_HIGH = 0x10;

    /**
     * 开始录音
     *
     * @return 开始成功/失败
     */
    public boolean startRecord(Context context, String savePath, Handler handler) {
        this.handler = handler;
        if (isRunning) {
            return false;
        }
        if (!hasSdcard()) {
            Toast.makeText(context, "请先插入SD卡(存储卡)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isSDCanUseSize50M()) {
            Toast.makeText(context, "内存已经不足50M了，请先清理手机空间", Toast.LENGTH_SHORT).show();
        }
        saveFile = new File(savePath);
        if (!saveFile.exists()) {
            saveFile.getParentFile().mkdirs();
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "文件创建失败", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 在setOutputFormat之前
        mMediaRecorder.setOutputFile(saveFile.getAbsolutePath());//设置输出路径【会直接保存】

        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioSamplingRate(highQuality ? 44100 : 22050);
        mMediaRecorder.setAudioEncodingBitRate(16000);
        mMediaRecorder.setAudioChannels(1);
        mMediaRecorder.setAudioSamplingRate(8000); // 96 kHz is a very high sample rate, and is not guaranteed to be supported. I suggest that you try a common sample rate <= 48 kHz (e.g. 48000, 44100, 22050, 16000, 8000).
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            startTimeLong = System.currentTimeMillis();
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            startVoiceTimer();
        } catch (Exception e) {//未授权也会在这里抛出异常
            e.printStackTrace();
            Log.d("------------>录音", "mMediaRecorder报错:" + e.getMessage());
            isRunning = false;
            return false;
        }
        isRunning = true;
        return true;
    }

    //获取声音分贝的timer
    private void startVoiceTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (handler != null) {
                    if (isRunning) {
                        Message message = new Message();
                        message.what = HAND_VOICE_HIGH;
                        message.arg1 = mMediaRecorder.getMaxAmplitude();
                        handler.sendMessage(message);
                    } else {
                        this.cancel();
                    }
                }
            }
        }, 100, 100);
    }

    //暂停录音
    public void pause() {

    }

    /**
     * 停止录音
     * 【不对外提供是否保存的功能——始终保存】
     * isNeedSave 是否保存录音文件, true表示保存, false表示不保存
     */
    public void stopRecord() {
        boolean isNeedSave = true;
        if (!isRunning) {
            return;
        }
        if (mMediaRecorder == null) return;
        if(timer!=null){
            timer.cancel();
        }
        endTimeLong = System.currentTimeMillis();

        /** 开始时间与结束时间小于一秒则不保存 */
        if ((endTimeLong - startTimeLong) < 1000) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRunning = false;
            if (saveFile.exists()) {
                saveFile.delete();
            }
            return;
        }

        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        isRunning = false;
        if (isNeedSave) { // 保存文件【文件已经保存】
//            try {
//                fis = new FileInputStream(saveFile);
//                byte[] oldByte = new byte[(int) saveFile.length()];
//                fis.read(oldByte); // 读取
//                // 加密
//                fos = new FileOutputStream(saveFile);
//                fos.write(oldByte);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    fis.close();
//                    fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        } else { // 不保存文件
            if (saveFile.exists()) {
                saveFile.delete();
            }
        }
    }

    /**
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            // 有存储的SDCard
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获得sd卡剩余容量是否有50M，即可用大小
     *
     * @return
     */
    public static boolean isSDCanUseSize50M() {
        if (!hasSdcard()) {
            return false;
        }
        File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path.getPath());
        long size = sf.getBlockSize();//SD卡的单位大小
        long total = sf.getBlockCount();//总数量
        long available = sf.getAvailableBlocks();//可使用的数量
        DecimalFormat df = new DecimalFormat();
        df.setGroupingSize(3);//每3位分为一组
        if (size * available / 1024 / 1024 < 50) {
            return false;
        }
        return true;
    }
}

