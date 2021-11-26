package com.example.cast.service.utils;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Decoder {

    private SurfaceHolder holder;
    private int mWidth;
    private int mHeight;

    private final static int TIME_INTERNAL = 5;
    private int mCount = 0;

    private MediaCodec mCodec;

    public Decoder(SurfaceHolder holder, int mWidth, int mHeight) {
        this.holder = holder;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
    }

    public Decoder(SurfaceHolder holder) {
        this(holder, holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
    }

    public void startCodec() {
        initDecoder();
    }

    private void initDecoder() {
        try {
            mCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
            mCodec.configure(format, holder.getSurface(), null, 0);
            mCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean decode(byte[] data, int offset, int length) {
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int index = mCodec.dequeueInputBuffer(-1);
        if (index >= 0) {
            ByteBuffer inputBuffer = inputBuffers[index];
            inputBuffer.clear();
            inputBuffer.put(data, offset, length);
            mCodec.queueInputBuffer(index, 0, length, mCount * TIME_INTERNAL, 0);
            mCount++;
        } else {
            return false;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputIndex >= 0) {
            // true 解码数据显示到surface上
            mCodec.releaseOutputBuffer(outputIndex, true);
            outputIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return true;
    }


    public void close(){
        if (mCodec != null){
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
    }
}
