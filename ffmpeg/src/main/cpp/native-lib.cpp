

#include <jni.h>
#include <string.h>
#include "MediaPlayer.h"
#include "JavaCallHelper.h"
#include <android/native_window_jni.h>

JavaVM *javaVm = 0;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVm = vm;
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_nativeInit(JNIEnv *env, jobject thiz) {
    MediaPlayer *player = new MediaPlayer(new JavaCallHelper(javaVm, env, thiz));
    return (jlong) player;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_setDataSource(JNIEnv *env, jobject thiz, jlong native_handle, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->setDataSource(path);
    env->ReleaseStringUTFChars(path_, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_prepare(JNIEnv *env, jobject thiz, jlong native_handle) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->prepare();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_setSurface(JNIEnv *env, jobject thiz, jlong native_handle, jobject surface) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    player->setWindow(window);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_start(JNIEnv *env, jobject thiz, jlong native_handle) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->start();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_stop(JNIEnv *env, jobject thiz, jlong native_handle) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->stop();
    delete player;
}extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_pause(JNIEnv *env, jobject thiz, jlong native_handle) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->pause();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_seekTo(JNIEnv *env, jobject thiz, jlong native_handle, jlong time) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->seekTo(time);
}extern "C"
JNIEXPORT jlong JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_getDuration(JNIEnv *env,jobject thiz,jlong native_handle) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->getDuration();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_oaks_ffmpeg_MediaPlayer_setPosition(JNIEnv *env, jobject thiz, jlong native_handle, jlong time,jboolean is_move) {
    MediaPlayer *player = reinterpret_cast<MediaPlayer *>(native_handle);
    player->setPosition(time,is_move);
}