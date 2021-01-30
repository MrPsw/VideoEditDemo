//
// Created by Admin on 2020/12/10.
//
#include "com_oaks_golf_FFmpegUtils.h"
#include "ffmpeg_cmd/ffmpeg_thread.h"
#include "ffmpeg_cmd/cJSON.h"
#include <cstdio>


extern "C" {
#include "ffmpeg_cmd/ffmpeg.h"
}


static JavaVM *jvm = NULL;
//java虚拟机
static jclass m_clazz = NULL;//当前类(面向java)

/**
 * 回调执行Java方法
 * 参看 Jni反射+Java反射
 */
void callJavaMethod(JNIEnv *env, jclass clazz, int ret) {
    if (clazz == NULL) {
        LOGE("---------------clazz isNULL---------------");
        return;
    }
    //获取方法ID (I)V指的是方法签名 通过javap -s -public FFmpegCmd 命令生成
    jmethodID methodID = (*env).GetStaticMethodID(clazz, "onExecuted", "(I)V");
    if (methodID == NULL) {
        LOGE("---------------methodID isNULL---------------");
        return;
    }
    //调用该java方法
    (*env).CallStaticVoidMethod(clazz, methodID, ret);
}

void callJavaMethodProgress(JNIEnv *env, jclass clazz, float ret) {
    if (clazz == NULL) {
        LOGE("---------------clazz isNULL---------------");
        return;
    }
    //获取方法ID (I)V指的是方法签名 通过javap -s -public FFmpegCmd 命令生成
    jmethodID methodID = (*env).GetStaticMethodID(clazz, "onProgress", "(F)V");
    if (methodID == NULL) {
        LOGE("---------------methodID isNULL---------------");
        return;
    }
    //调用该java方法
    (*env).CallStaticVoidMethod(clazz, methodID, ret);
}

/**
 * c语言-线程回调
 */
static void ffmpeg_callback(int ret) {
    JNIEnv *env;
    //附加到当前线程从JVM中取出JNIEnv, C/C++从子线程中直接回到Java里的方法时  必须经过这个步骤
    (*jvm).AttachCurrentThread(reinterpret_cast<JNIEnv **>((void **) &env), NULL);
    callJavaMethod(env, m_clazz, ret);

    //完毕-脱离当前线程
    (*jvm).DetachCurrentThread();
}

void ffmpeg_progress(float progress) {
    JNIEnv *env;
    (*jvm).AttachCurrentThread(reinterpret_cast<JNIEnv **>((void **) &env), NULL);
    callJavaMethodProgress(env, m_clazz, progress);
    (*jvm).DetachCurrentThread();
}


//将const char类型转换成jstring类型
jstring char2Jstring(JNIEnv *env, const char *pat) {
    //定义java String类 strClass
    jclass strClass = (env)->FindClass("java/lang/String");
    //获取java String类方法String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    //建立byte数组
    jbyteArray bytes = (env)->NewByteArray((jsize) strlen(pat));
    //将char* 转换为byte数组
    (env)->SetByteArrayRegion(bytes, 0, (jsize) strlen(pat), (jbyte *) pat);
    //设置String, 保存语言类型,用于byte数组转换至String时的参数
    jstring encoding = (env)->NewStringUTF("GB2312");
    //将byte数组转换为java String,并输出
    return (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_FFmpegCmd_exeCmd(JNIEnv *env, jclass clazz, jint cmdnum, jobjectArray cmdline) {

    (*env).GetJavaVM(&jvm);

    m_clazz = static_cast<jclass>((*env).NewGlobalRef(clazz));
    //---------------------------------C语言 反射Java 相关----------------------------------------
    //---------------------------------java 数组转C语言数组----------------------------------------
    int i = 0;//满足NDK所需的C99标准
    char **argv = NULL;//命令集 二维指针
    jstring *strr = NULL;

    if (cmdline != NULL) {
        argv = (char **) malloc(sizeof(char *) * cmdnum);
        strr = (jstring *) malloc(sizeof(jstring) * cmdnum);

        for (i = 0; i < cmdnum; ++i) {//转换
            strr[i] = (jstring) (*env).GetObjectArrayElement(cmdline, i);
            argv[i] = (char *) (*env).GetStringUTFChars(strr[i], 0);
        }

    }
    //---------------------------------java 数组转C语言数组----------------------------------------
    //---------------------------------执行FFmpeg命令相关----------------------------------------
    //新建线程 执行ffmpeg 命令
    ffmpeg_thread_run_cmd(cmdnum, argv);
    //注册ffmpeg命令执行完毕时的回调
    ffmpeg_thread_callback(ffmpeg_callback);

    free(strr);

}





extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_FFmpegCmd_exitCmd(JNIEnv *env, jclass type) {
    (*env).GetJavaVM(&jvm);
    m_clazz = static_cast<jclass>((*env).NewGlobalRef(type));
    ffmpeg_thread_cancel();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_oaks_ffmpeg_FFmpegUtils_getVideoInfo(JNIEnv *env, jclass clazz, jstring video_url) {

    char *path;
    path = (char *) env->GetStringUTFChars(video_url, 0);

    AVFormatContext *ic = avformat_alloc_context();

    if (avformat_open_input(&ic, path, NULL, NULL) < 0) {
        LOGE("could not open source %s", video_url);
        //return  -1;
    }

    if (avformat_find_stream_info(ic, NULL) < 0) {
        LOGE("could not find stream information");
        //return  -1;
    }

    LOGD("---------- dumping stream info ----------");

    cJSON *jsonroot = 0;
    char *jsonout = 0;
    //创建根节点对象
    jsonroot = cJSON_CreateObject();

    LOGD("input format: %s", ic->iformat->name);
    cJSON_AddStringToObject(jsonroot, "input_format", ic->iformat->name);
    LOGD("nb_streams: %d", ic->nb_streams);
    cJSON_AddNumberToObject(jsonroot, "nb_streams", ic->nb_streams);

    int64_t start_time = ic->start_time / AV_TIME_BASE;
    LOGD("start_time: %lld", start_time);
    cJSON_AddNumberToObject(jsonroot, "start_time", start_time);

    int64_t duration = ic->duration / AV_TIME_BASE;
    LOGD("duration: %lld s", duration);
    cJSON_AddNumberToObject(jsonroot, "duration", duration);

    int video_stream_idx = av_find_best_stream(ic, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    if (video_stream_idx >= 0) {
        AVStream *video_stream = ic->streams[video_stream_idx];
        LOGD("video nb_frames: %lld", video_stream->nb_frames);
        cJSON_AddNumberToObject(jsonroot, "video_nb_frames", video_stream->nb_frames);
        LOGD("video codec_id: %d", video_stream->codec->codec_id);
        cJSON_AddNumberToObject(jsonroot, "video_codec_id", video_stream->codec->codec_id);
        LOGD("video codec_name: %s", avcodec_get_name(video_stream->codec->codec_id));
        cJSON_AddStringToObject(jsonroot, "video_codec_name",
                                avcodec_get_name(video_stream->codec->codec_id));
        LOGD("video width x height: %d x %d", video_stream->codec->width,
             video_stream->codec->height);
        cJSON_AddNumberToObject(jsonroot, "video_width", video_stream->codec->width);
        cJSON_AddNumberToObject(jsonroot, "video_height", video_stream->codec->height);
        LOGD("video pix_fmt: %d", video_stream->codec->pix_fmt);
        cJSON_AddNumberToObject(jsonroot, "video_pix_fmt", video_stream->codec->pix_fmt);
        LOGD("video bitrate %lld kb/s", (int64_t) video_stream->codec->bit_rate / 1000);
        cJSON_AddNumberToObject(jsonroot, "video_bitrate",
                                (int64_t) video_stream->codec->bit_rate / 1000);
        LOGD("video avg_frame_rate: %d fps",
             video_stream->avg_frame_rate.num / video_stream->avg_frame_rate.den);
        cJSON_AddNumberToObject(jsonroot, "video_frame_rate", video_stream->avg_frame_rate.num /
                                                              video_stream->avg_frame_rate.den);
    }

    int audio_stream_idx = av_find_best_stream(ic, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    if (audio_stream_idx >= 0) {
        AVStream *audio_stream = ic->streams[audio_stream_idx];
        LOGD("audio codec_id: %d", audio_stream->codec->codec_id);
        cJSON_AddNumberToObject(jsonroot, "audio_codec_id", audio_stream->codec->codec_id);
        LOGD("audio codec_name: %s", avcodec_get_name(audio_stream->codec->codec_id));
        cJSON_AddStringToObject(jsonroot, "audio_codec_name",
                                avcodec_get_name(audio_stream->codec->codec_id));
        LOGD("audio sample_rate: %d", audio_stream->codec->sample_rate);
        cJSON_AddNumberToObject(jsonroot, "audio_sample_rate", audio_stream->codec->sample_rate);
        LOGD("audio channels: %d", audio_stream->codec->channels);
        cJSON_AddNumberToObject(jsonroot, "audio_channels", audio_stream->codec->channels);
        LOGD("audio sample_fmt: %d", audio_stream->codec->sample_fmt);
        cJSON_AddNumberToObject(jsonroot, "audio_sample_fmt", audio_stream->codec->sample_fmt);
        LOGD("audio frame_size: %d", audio_stream->codec->frame_size);
        cJSON_AddNumberToObject(jsonroot, "audio_frame_size", audio_stream->codec->frame_size);
        LOGD("audio nb_frames: %lld", audio_stream->nb_frames);
        cJSON_AddNumberToObject(jsonroot, "audio_nb_frames", audio_stream->nb_frames);
        LOGD("audio bitrate %lld kb/s", (int64_t) audio_stream->codec->bit_rate / 1000);
        cJSON_AddNumberToObject(jsonroot, "audio_bitrate",
                                (int64_t) audio_stream->codec->bit_rate / 1000);
    }
    LOGD("---------- dumping stream info ----------");
    avformat_close_input(&ic);
    return char2Jstring(env, cJSON_Print(jsonroot));

}



