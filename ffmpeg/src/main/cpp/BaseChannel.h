//
// Created by Admin on 2021/1/26.
//

#ifndef GOLF_BASECHANNEL_H
#define GOLF_BASECHANNEL_H

extern "C" {
#include <libavcodec/avcodec.h>
};

#include "safe_queue.h"
#include "JavaCallHelper.h"
#include "Log.h"

//todo 待处理
#define PLAY_STATUS_PLAYING 3
#define PLAY_STATUS_PAUSE 4
#define PLAY_STATUS_END 5


class BaseChannel {
public:
    BaseChannel(int channelId, JavaCallHelper *helper, AVCodecContext *avCodecContext, AVRational
    base) : channelId(channelId),
            helper(helper),
            avCodecContext(avCodecContext),
            time_base(base) {
        pkt_queue.setReleaseHandle(releaseAvPacket);
        frame_queue.setReleaseHandle(releaseAvFrame);
    }


    virtual ~BaseChannel() {
        if (avCodecContext) {
            avcodec_close(avCodecContext);
            avcodec_free_context(&avCodecContext);
            avCodecContext = 0;
        }
        pkt_queue.clear();
        frame_queue.clear();
    };

    virtual void play() = 0;

    virtual void stop() = 0;

    virtual void decode() = 0;

    virtual void pause() = 0;

    void setEnable(bool enable) {
        pkt_queue.setEnable(enable);
        frame_queue.setEnable(enable);
    }

    static void releaseAvFrame(AVFrame *&frame) {
        if (frame) {
            av_frame_free(&frame);
            frame = 0;
        }
    }


    static void releaseAvPacket(AVPacket *&packet) {
        if (packet) {
            av_packet_free(&packet);
            packet = 0;
        }
    }


public:
    int channelId;
    JavaCallHelper *helper;
    AVCodecContext *avCodecContext;
    AVRational time_base;

    SafeQueue<AVPacket *> pkt_queue;
    SafeQueue<AVFrame *> frame_queue;

    bool isPlaying = false;
    bool isPause = false;

    long seekTime = -1;
    bool isSeek = false;

    double clock = 0;




};

#endif //GOLF_BASECHANNEL_H


