package com.oaks.golf.entity


/**
 * @date      2020/12/23
 * @author    Pengshuwen
 * @describe
 */
data class VideoInfo(
    val audio_bitrate: Int,
    val audio_channels: Int,
    val audio_codec_id: Int,
    val audio_codec_name: String,
    val audio_frame_size: Int,
    val audio_nb_frames: Int,
    val audio_sample_fmt: Int,
    val audio_sample_rate: Int,
    val duration: Int,
    val input_format: String,
    val nb_streams: Int,
    val start_time: Int,
    val video_bitrate: Int,
    val video_codec_id: Int,
    val video_codec_name: String,
    val video_frame_rate: Int,
    val video_height: Int,
    val video_nb_frames: Int,
    val video_pix_fmt: Int,
    val video_width: Int
)