package com.example.cast.client.utils;

import java.util.concurrent.ArrayBlockingQueue;

public class QueueManager {

    private static ArrayBlockingQueue<byte[]> mVideoQueue = new ArrayBlockingQueue<>(1000);
    private static ArrayBlockingQueue<byte[]> mAudioQueue = new ArrayBlockingQueue<>(1000);


    public static void addVideoData(byte[] data) {
        if (mVideoQueue.size() > 900) {
            mVideoQueue.poll();
        }
        mVideoQueue.add(data);
    }

    public static byte[] takeVideoData() {
        return mVideoQueue.poll();
    }

    public static void addAudioData(byte[] data) {
        if (mAudioQueue.size() > 900) {
            mAudioQueue.poll();
        }
        mAudioQueue.add(data);
    }

    public static byte[] takeAudioData() {
        return mAudioQueue.poll();
    }

}
