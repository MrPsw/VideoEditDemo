//
// Created by Admin on 2021/1/25.
//

#ifndef GOLF_MEDIAPLAYER_H
#define GOLF_MEDIAPLAYER_H

#include <pthread.h>

#include <android/native_window.h>
#include "JavaCallHelper.h"
#include "VideoChannel.h"

extern "C" {
#include <libavformat/avformat.h>
}

class MediaPlayer {

    friend void *prepare_t(void *args);

    friend void *start_t(void *args);


public:
    MediaPlayer(JavaCallHelper *helper);

    ~MediaPlayer();

    void setDataSource(const char *path);

    void prepare();

    void setWindow(ANativeWindow *window);

    void start();

    void stop();

    void pause();

    void seekTo(jlong time);

    void setPosition( long time, bool move);

    jlong getDuration();


private:
    void _prepare();

    void _start();

    void release();

public:
    char *path;
    pthread_t prepareTask;
    JavaCallHelper *helper;
    int64_t duration;
    VideoChannel *videoChannel = 0;
    AudioChannel *audioChannel = 0;
    pthread_t startTask;
    bool isPlaying;
    AVFormatContext *avFormatContext = 0;
    ANativeWindow *window = 0;

    bool isMove;

    bool isPauseReading = false;


};


#endif //GOLF_MEDIAPLAYER_H
