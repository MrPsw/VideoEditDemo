//
// Created by Admin on 2021/1/25.
//

#include "MediaPlayer.h"
#include "Log.h"
#include <malloc.h>
#include <time.h>

extern "C" {
#include <libavformat/avformat.h>
}

void *prepare_t(void *args) {
    MediaPlayer *player = static_cast<MediaPlayer *>(args);
    player->_prepare();
    return 0;
}

MediaPlayer::MediaPlayer(JavaCallHelper *helper) : helper(helper) {
    avformat_network_init();
}

MediaPlayer::~MediaPlayer() {
    avformat_network_deinit();
    delete helper;
    helper = 0;
    if (path) {
        delete[] path;
        path = 0;
    }
}

void MediaPlayer::setDataSource(const char *path_) {

//    if (path != 0) {
//        delete[] path;
//    }
    path = new char[strlen(path_) + 1];
    strcpy(path, path_);
}

void MediaPlayer::prepare() {
    //解析  耗时！
    pthread_create(&prepareTask, 0, prepare_t, this);
}

void MediaPlayer::_prepare() {

    avFormatContext = avformat_alloc_context();
    if (avformat_open_input(&avFormatContext, path, 0, 0) != 0) {
        //打开视频失败
        helper->onError(FFMPEG_CAN_NOT_OPEN_URL, THREAD_CHILD);
        goto ERROR;
    }

    if (avformat_find_stream_info(avFormatContext, 0) < 0) {
        // 获取视频信息失败
        helper->onError(FFMPEG_CAN_NOT_FIND_STREAMS, THREAD_CHILD);
        goto ERROR;
    }

    // 得到视频时长，单位是秒
    duration = avFormatContext->duration / AV_TIME_BASE;
    // 这个媒体文件中有几个媒体流 (视频流、音频流)
    for (int i = 0; i < avFormatContext->nb_streams; ++i) {
        AVStream *avStream = avFormatContext->streams[i];
        // 解码信息
        AVCodecParameters *parameters = avStream->codecpar;
        //查找解码器
        AVCodec *dec = avcodec_find_decoder(parameters->codec_id);
        if (!dec) {
            helper->onError(FFMPEG_FIND_DECODER_FAIL, THREAD_CHILD);
            goto ERROR;
        }

        AVCodecContext *codecContext = avcodec_alloc_context3(dec);
        // 把解码信息赋值给了解码上下文中的各种成员
        if (avcodec_parameters_to_context(codecContext, parameters) < 0) {
            helper->onError(FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL, THREAD_CHILD);
            goto ERROR;
        }
        //多线程解码
        if (parameters->codec_type == AVMEDIA_TYPE_VIDEO) {
            codecContext->thread_count = 8;
        } else if (parameters->codec_type == AVMEDIA_TYPE_AUDIO) {
            codecContext->thread_count = 1;
        }

        // 打开解码器
        if (avcodec_open2(codecContext, dec, 0) != 0) {
            //打开失败
            helper->onError(FFMPEG_OPEN_DECODER_FAIL, THREAD_CHILD);
            goto ERROR;
        }
        if (parameters->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioChannel = new AudioChannel(i, helper, codecContext, avStream->time_base);
        } else if (parameters->codec_type == AVMEDIA_TYPE_VIDEO) {
            // 帧率
            double fps = av_q2d(avStream->avg_frame_rate);
            if (isnan(fps) || fps == 0) {
                fps = av_q2d(avStream->r_frame_rate);
            }
            if (isnan(fps) || fps == 0) {
                fps = av_q2d(av_guess_frame_rate(avFormatContext, avStream, 0));
            }

            videoChannel = new VideoChannel(i, helper, codecContext, avStream->time_base, fps);
            videoChannel->setWindow(window);
        }
    }

    // 如果媒体文件中没有音视频
    if (!videoChannel && !audioChannel) {
        helper->onError(FFMPEG_NOMEDIA, THREAD_CHILD);
        goto ERROR;
    }

    //告诉java准备好了，可以播放了
    helper->onParpare(THREAD_CHILD);
    return;
    ERROR:
    LOGE("失败释放");
    release();
}

void *start_t(void *args) {
    MediaPlayer *player = static_cast<MediaPlayer *>(args);
    player->_start();
    return 0;
}

void MediaPlayer::start() {
    //1、读取媒体源的数据
    //2、根据数据类型放入Audio/VideoChannel的队列中
    isPlaying = 1;

    //如果是暂停 则先处理恢复
    if (videoChannel && videoChannel->isPause && audioChannel && audioChannel->isPause) {
        videoChannel->isPause = false;
        audioChannel->isPause = false;
        return;
    }

    if (videoChannel) {
        videoChannel->audioChannel = audioChannel;
        videoChannel->play();
    }
    if (audioChannel) {
        audioChannel->play();
    }
    pthread_create(&startTask, 0, start_t, this);
}

void MediaPlayer::_start() {
    int ret;
    while (isPlaying) {

        if (videoChannel->isSeek) {
            LOGE("seek标识改变 读取帧");
            continue;
        }

        if (isPauseReading) {
            continue;
        }
        AVPacket *packet = av_packet_alloc();
        //获取一帧封装数据
        ret = av_read_frame(avFormatContext, packet);
        if (ret == 0) {
            if (videoChannel && packet->stream_index == videoChannel->channelId) {
                double nowTime = packet->pts * av_q2d(videoChannel->time_base);

                LOGE("seek 读取帧时间：%lf 是否是关键帧%d", nowTime, packet->flags & AV_PKT_FLAG_KEY);

                if (isMove && videoChannel->seekTime != -1) {
                    double targetTime = (videoChannel->seekTime / 1000.0);
                    LOGE("seek 当前时间：%f 目标时间：%f", nowTime, targetTime);
                    if (nowTime < targetTime || (nowTime > targetTime && nowTime - targetTime <= 0.4)) {
                        LOGE("找到帧 当前时间：%f 目标时间：%f", nowTime, targetTime);
                        videoChannel->pkt_queue.enQueue(packet);
                    } else {
                        isPauseReading = true;
                    }
                } else {
                    videoChannel->pkt_queue.enQueue(packet);
                }


            } else if (audioChannel && packet->stream_index == audioChannel->channelId) {
                audioChannel->pkt_queue.enQueue(packet);
            } else {
                av_packet_free(&packet);
            }

        } else {
            av_packet_free(&packet);
            if (ret == AVERROR_EOF) { //end of file
                //读取完毕，不一定播放完毕
                if (videoChannel->pkt_queue.empty() && videoChannel->frame_queue.empty()
                    && audioChannel->pkt_queue.empty() && audioChannel->frame_queue.empty()) {
                    //播放完毕
                    break;
                }
//            av_usleep(10000);
            } else {
                LOGE("读取数据包失败，返回:%d 错误描述:%s", ret, av_err2str(ret));
                break;
            }
        }
    }
    isPlaying = 0;
    audioChannel->stop();
    videoChannel->stop();
}


void MediaPlayer::pause() {

    if (audioChannel) {
        audioChannel->isPause = true;
    }
    if (videoChannel) {
        videoChannel->isPause = true;
    }
}


void MediaPlayer::seekTo(int64_t seek_time) {

    if (audioChannel == NULL)
        return;
    if (duration <= 0)
        return;
    if ((seek_time / 1000) <= duration) {

        audioChannel->isSeek = true;
        audioChannel->pkt_queue.clear();
        audioChannel->frame_queue.clear();

        videoChannel->isSeek = true;
        videoChannel->pkt_queue.clear();
        videoChannel->frame_queue.clear();


//        audioChannel->clock = 0;
//        videoChannel->clock = 0;

        audioChannel->seekTime = seek_time;
        videoChannel->seekTime = seek_time;

        int64_t k1 = (int64_t) (((double) seek_time / (double) 1000) / av_q2d(audioChannel->time_base));
        av_seek_frame(avFormatContext, audioChannel->channelId, k1, AVSEEK_FLAG_BACKWARD);

        long long k2 = (int64_t) ((seek_time / 1000) / av_q2d(videoChannel->time_base));
        av_seek_frame(avFormatContext, videoChannel->channelId, k2, AVSEEK_FLAG_BACKWARD);

        audioChannel->isSeek = false;
        videoChannel->isSeek = false;

    }
}

void MediaPlayer::setPosition(long time, bool move) {

    //原来不在拖动状态  或者现在不在拖动状态  都得清空数据重新seek
    if (!isMove || !move || videoChannel->seekTime == time) {
        //audioChannel->isSeek = true;
        audioChannel->pkt_queue.clear();
        audioChannel->frame_queue.clear();
        videoChannel->isSeek = true;
        videoChannel->pkt_queue.clear();
        videoChannel->frame_queue.clear();
    }

    isMove = move;

    isPauseReading = false;

    if (move && time != videoChannel->seekTime) {
        videoChannel->seekTime = time;
        int64_t k1 = (int64_t) ((time / 1000) / av_q2d(videoChannel->time_base));
        int ret = av_seek_frame(avFormatContext, videoChannel->channelId, k1, AVSEEK_FLAG_BACKWARD);
        if (ret >= 0) {
            //seek finish
        }

        LOGE("seek:拖动时间 %ld", time);

        videoChannel->isSeek = false;
        audioChannel->isPlaying = false;


        return;
    } else {
        audioChannel->isPlaying = true;
    }


//    int64_t k1 = (int64_t) ((time / 1000) / av_q2d(audioChannel->time_base));
//    av_seek_frame(avFormatContext, audioChannel->channelId, k1, AVSEEK_FLAG_BACKWARD);
//
//    int64_t k2 = (int64_t) ((time / 1000) / av_q2d(videoChannel->time_base));
//    av_seek_frame(avFormatContext, videoChannel->channelId, k2, AVSEEK_FLAG_BACKWARD);


}


void MediaPlayer::setWindow(ANativeWindow *window) {
    this->window = window;
    if (videoChannel) {
        videoChannel->setWindow(window);
    }
}


void MediaPlayer::stop() {
    isPlaying = 0;
    pthread_join(prepareTask, 0);
    pthread_join(startTask, 0);
    release();
}

jlong MediaPlayer::getDuration() {
    return duration;
}


void MediaPlayer::release() {
    if (audioChannel) {
        delete audioChannel;
        audioChannel = 0;
    }
    if (videoChannel) {
        delete videoChannel;
        videoChannel = 0;
    }
    if (avFormatContext) {
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = 0;
    }
}