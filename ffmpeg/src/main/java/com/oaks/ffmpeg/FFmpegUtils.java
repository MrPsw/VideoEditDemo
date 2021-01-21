package com.oaks.ffmpeg;

import java.io.File;

/**
 * @author Pengshuwen
 * @date 2020/12/9
 * @describe
 */
public class FFmpegUtils {


    static {
        System.loadLibrary("learn_ffmpeg");
    }


    /**
     * 使用ffmpeg命令行给视频添加水印
     *
     * @param srcFile    源文件
     * @param waterMark  水印文件路径
     * @param targetFile 添加水印后的文件
     * @return
     */
    public static void addWaterMark(String srcFile, String waterMark, String targetFile, FFmpegCmd.OnCmdExecListener listener) {


        File inputFile = new File(srcFile);
        if (!inputFile.exists()) {
            return;
        }
//        String videoInfo = FFmpegUtils.getVideoInfo(srcFile);

        String waterMarkCmd = "ffmpeg -i %s -i %s -filter_complex overlay=x=10:y=main_h-overlay_h-10 -c:v libx264 -c:a copy %s";
        waterMarkCmd = String.format(waterMarkCmd, srcFile, waterMark, targetFile);

        String[] s = waterMarkCmd.replace("  ", " ").split(" ");
        System.out.println(waterMarkCmd);
        FFmpegCmd.exeCmd(waterMarkCmd.replace("  ", " ").split(" "), 0, listener);

    }


    /**
     * 替换原有音频
     *
     * @param srcFile    视频文件
     * @param pcm        音频文件
     * @param targetFile 添加水印后的文件
     * @return
     */
    public static void replaceAudio(String srcFile, String pcm, String targetFile, FFmpegCmd.OnCmdExecListener listener) {


        File inputFile = new File(srcFile);
        if (!inputFile.exists()) {
            return;
        }

        String cmd = "ffmpeg -i %s -i %s -ss 00:01:28 -t 13 -c:v copy -map 0:v -map 1:a %s";
        cmd = String.format(cmd, srcFile, pcm, targetFile);
        cmd.replace("  ", " ");
        System.out.println(cmd);
        FFmpegCmd.exeCmd(cmd.replace("  ", " ").split(" "), 0, listener);
    }


    public native static String getVideoInfo(String videoUrl);




}


