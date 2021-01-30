//
// Created by Administrator on 2019/11/21.
//

#ifndef ENJOYPLAYER_VIDEOCHANNEL_H
#define ENJOYPLAYER_VIDEOCHANNEL_H


#include <android/native_window.h>
#include "BaseChannel.h"
#include "AudioChannel.h"

extern "C" {
#include "libavcodec/avcodec.h"

};

class VideoChannel : public BaseChannel {
    friend void *videoPlay_t(void *args);

public:
    VideoChannel(int channelId, JavaCallHelper *helper, AVCodecContext *avCodecContext,
                 const AVRational &base, double fps);

    virtual  ~VideoChannel();

    void setWindow(ANativeWindow *window);

public:
    virtual void play();

    virtual void stop();

    virtual void decode();

    virtual void pause();

private:
    void _play();

    void _onDraw(uint8_t *data[4], int linesize[4], int width, int height);

private:
    double fps;
    pthread_mutex_t surfaceMutex;
    pthread_t videoDecodeTask, videoPlayTask;
    ANativeWindow *window = 0;
public:
    AudioChannel *audioChannel = 0;


};


#endif //ENJOYPLAYER_VIDEOCHANNEL_H
