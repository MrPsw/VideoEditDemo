//
// Created by Administrator on 2019/11/26.
//

#ifndef ENJOYPLAYER_AUDIOCHANNEL_H
#define ENJOYPLAYER_AUDIOCHANNEL_H


#include "BaseChannel.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

extern "C" {
#include <libswresample/swresample.h>
#include <libavcodec/avcodec.h>
};

class AudioChannel : public BaseChannel {
    friend void *audioPlay_t(void *args);

    friend void bqPlayerCallback(SLAndroidSimpleBufferQueueItf queue, void *pContext);

public:
    AudioChannel(int channelId, JavaCallHelper *helper, AVCodecContext *avCodecContext,
                 const AVRational &base);

    virtual ~AudioChannel();

public:
    virtual void play();

    virtual void stop();

    virtual void decode();

    virtual void pause();


private:
    void _play();

    int _getData();

    void _releaseOpenSL();

private:
    pthread_t audioDecodeTask, audioPlayTask;
    SwrContext *swrContext = 0;
    uint8_t *buffer;
    int bufferCount;
    int out_channls;
    int out_sampleSize;


    SLObjectItf engineObject = NULL;
    SLEngineItf engineInterface = NULL;
    SLObjectItf outputMixObject = NULL;
    SLObjectItf bqPlayerObject = NULL;
    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue = NULL;
    SLPlayItf bqPlayerInterface = NULL;
};


#endif //ENJOYPLAYER_AUDIOCHANNEL_H
