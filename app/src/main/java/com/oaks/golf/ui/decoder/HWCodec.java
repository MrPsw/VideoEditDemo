package com.oaks.golf.ui.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HWCodec {

    private static final String TAG = "HWCodec";

    private static final long DEFAULT_TIMEOUT = 10 * 1000;

    private static final int MEDIA_TYPE_VIDEO = 1;
    private static final int MEDIA_TYPE_AUDIO = 2;
    private static final int MEDIA_TYPE_UNKNOWN = 0;

    private static final String MIME_TYPE_AVC = "video/avc";
    private static final String MIME_TYPE_AAC = "audio/mp4a-latm";

    public static boolean decode(String srcFilePath, String yuvDst, String pcmDst) {
        boolean vsucceed = decodeVideo(srcFilePath, yuvDst);
        boolean asucceed = decodeAudio(srcFilePath, pcmDst);
        return vsucceed && asucceed;
    }

    public static boolean decodeVideo(String srcFilePath, String dstFilePath) {
        return doDecode(srcFilePath, dstFilePath, MEDIA_TYPE_VIDEO);
    }

    public static boolean decodeAudio(String srcFilePath, String dstFilePath) {
        return doDecode(srcFilePath, dstFilePath, MEDIA_TYPE_AUDIO);
    }

    private static boolean doDecode(String src, String dst, int mediaType) {
        MediaExtractor extractor = null;
        MediaCodec decoder = null;
        FileOutputStream fos = null;
        boolean decodeSucceed = false;
        boolean exceptionOccur = false;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(src);
            fos = new FileOutputStream(dst);
            decoder = doDecode(extractor, fos, mediaType);
        } catch (IOException e) {
            Log.e(TAG, "doDecode exception: " + e);
            exceptionOccur = true;
        } finally {
            if (extractor != null) {
                extractor.release();
            }
            if (decoder != null) {
                decodeSucceed = true;
                decoder.stop();
                decoder.release();
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "doDecode close fos error: " + e.getLocalizedMessage());
                }
            }
        }
        return !exceptionOccur && decodeSucceed;
    }

    private static MediaCodec doDecode(MediaExtractor extractor, FileOutputStream fos,
                                       int mediaType) throws IOException {
        MediaFormat format = selectTrack(extractor, mediaType);
        if (format == null) {
            Log.e(TAG, "doDecode no " + mediaType + " track");
            return null;
        }
        Log.d(TAG, "docde format: " + format.toString());

        MediaCodec decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        decoder.configure(format, null, null, 0);
        decoder.start();

        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        boolean inputEof = false;
        boolean outputEof = false;
        while (!outputEof) {
            if (!inputEof) {
                int inIndex = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT);
                if (inIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inIndex];
                    inputBuffer.clear();
                    int size = extractor.readSampleData(inputBuffer, 0);
                    if (size < 0) {
                        inputEof = true;
                        decoder.queueInputBuffer(inIndex, 0, 0,
                                0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, size, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            int outIndex = decoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT);
            if (outIndex >= 0) {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    decoder.releaseOutputBuffer(outIndex, false);
                    continue;
                }

                if (bufferInfo.size != 0) {
                    ByteBuffer outputBuffer = outputBuffers[outIndex];
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    byte[] data = new byte[bufferInfo.size];
                    outputBuffer.get(data);
                    fos.write(data);
                }

                decoder.releaseOutputBuffer(outIndex, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputEof = true;
                }
            } else if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = decoder.getOutputBuffers();
                Log.d(TAG, "decoder output buffer have changed");
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat tmp = decoder.getOutputFormat();
                Log.d(TAG, "decoder output format change to " + tmp.toString());
            }

        }

        return decoder;
    }

    private static MediaFormat selectTrack(MediaExtractor extractor, int mediaType) {
        MediaFormat format = null;
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; ++i) {
            MediaFormat tmpFormat = extractor.getTrackFormat(i);
            if (getMediaType(tmpFormat) == mediaType) {
                format = tmpFormat;
                extractor.selectTrack(i);
                break;
            }
        }
        return format;
    }

    private static int getMediaType(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime.startsWith("video/")) {
            return MEDIA_TYPE_VIDEO;
        } else if (mime.startsWith("audio/")) {
            return MEDIA_TYPE_AUDIO;
        }
        return MEDIA_TYPE_UNKNOWN;
    }
}