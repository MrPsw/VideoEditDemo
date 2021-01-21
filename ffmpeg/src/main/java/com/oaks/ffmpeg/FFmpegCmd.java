package com.oaks.ffmpeg;

/**
 * @author Pengshuwen
 * @date 2020/12/22
 * @describe
 */
public class FFmpegCmd {


    public native static void exeCmd(int cmdnum, String[] argv);


    public native static void exitCmd();

    public static void exeCmd(String[] cmds, long duration, OnCmdExecListener listener) {
        sOnCmdExecListener = listener;
        sDuration = duration;

        for (int i = 0; i < cmds.length; i++) {
            System.out.println("cmd" + i + ":" + cmds[i]);
        }
        exeCmd(cmds.length, cmds);


    }


    private static OnCmdExecListener sOnCmdExecListener;
    private static long sDuration;

    /**
     * FFmpeg执行结束回调，由C代码中调用
     */
    public static void onExecuted(int ret) {
        if (sOnCmdExecListener != null) {
            if (ret == 0) {
                sOnCmdExecListener.onProgress(sDuration);
                sOnCmdExecListener.onSuccess();
            } else {
                sOnCmdExecListener.onFailure();
            }
        }
    }

    /**
     * FFmpeg执行进度回调，由C代码调用
     */
    public static void onProgress(float progress) {
        if (sOnCmdExecListener != null) {
            if (sDuration != 0) {
                sOnCmdExecListener.onProgress(progress / (sDuration / 1000) * 0.95f);
            }
        }
    }

    public interface OnCmdExecListener {
        void onSuccess();

        void onFailure();

        void onProgress(float progress);
    }


}
