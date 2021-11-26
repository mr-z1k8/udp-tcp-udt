package com.example.cast.service.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioUtil {

    private final static String TAG = "AudioUtil";

    private static AudioUtil instance;
    // 缓存池大小
    private int bufferSize = 1920;
    // 单声道
    private int TRACK_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int AUDIO_MODE = AudioTrack.MODE_STREAM;
    int AUDIO_SAMPLING_RATE = 44100;

    private AudioTrack track;
    private Thread playThread;
    private InputStream mInputStream;

    private boolean isRunning = true;

    private AudioUtil() {
        initTrack();
    }

    public static AudioUtil getInstance(){
        if (instance == null){
            synchronized (AudioUtil.class){
                if (instance == null){
                    instance = new AudioUtil();
                }
            }
        }
        return instance;
    }

    private void initTrack() {
        Slog.d(TAG, "初始化track");
        track = new AudioTrack(AudioManager.STREAM_MUSIC, AUDIO_SAMPLING_RATE, TRACK_CHANNEL, AUDIO_FORMAT, bufferSize, AUDIO_MODE);
        track.setVolume(32f);
    }

    /**
     * 裸流数据播放
     * @param data
     */
    public void revicesRecord(byte[] data) {
        track.play();
        track.write(data, 0, data.length);
    }

    public void stopRecord() {
        isRunning = false;
        if (track != null) {
            track = null;
        }
    }

    /**
     * 媒体文件播放
     * @param context
     */
    public void playRecord(Context context) {
        if (playThread == null) {
            playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] music = new byte[1920];
                    try {
                        mInputStream = new FileInputStream(context.getFilesDir() + "/service_audio.pcm");
                        BufferedInputStream bis = new BufferedInputStream(mInputStream);
                        DataInputStream dis = new DataInputStream(bis);
                        int read;
                        while (dis.available() > 0) {
                            read = dis.read(music);
                            Slog.d(TAG, "read: " + read);
                            if (read == AudioTrack.ERROR_BAD_VALUE || read == AudioTrack.ERROR_INVALID_OPERATION) {
                                continue;
                            }
                            if (read != 0 && read != -1) {
                                track.play();
                                track.write(music, 0, read);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Slog.d(TAG, "播放失败：" + e);
                    } finally {
                        if (mInputStream != null) {
                            try {
                                mInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            playThread.start();
        }
    }
}
