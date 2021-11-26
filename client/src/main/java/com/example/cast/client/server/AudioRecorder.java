package com.example.cast.client.server;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.cast.client.utils.Clog;
import com.example.cast.client.utils.QueueManager;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class AudioRecorder implements IAudioRecorder {

    private final static String TAG = "AudioRecorder";

    private Context context;
    private AudioRecord audioRecord;
    // 缓存池大小
    private int bufferSize;
    // 单声道
    private int RECORD_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private int TRACK_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private int AUDIO_MODE = AudioTrack.MODE_STREAM;

    private AudioThread mThread;
    private InputStream mInputStream;
    private OutputStream mOutStream;
    private AudioTrack track;
    private Thread playThread;
    private AudioPlaybackCaptureConfiguration configuration;
    private byte[] buffer;
    private final static int DEFAULT_AUDIO_SIZE = 1920;

    private boolean isRecording = false;

    public AudioRecorder(Context context) {
        this.context = context;
        buffer = new byte[DEFAULT_AUDIO_SIZE];
//        initTrack();
    }

    private void initSocket() {
//        socket = ClientUdpSocket.getInstance();
//        int port = ISocket.DEFAULT_PORT;
//        String ip = ISocket.DEFAULT_IP;
//        socket.connect(ip, port, new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                super.handleMessage(msg);
//            }
//        });
    }

    @Override
    public void initRecorder() {
        if (null != audioRecord) {
            audioRecord.release();
        }
        bufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLING_RATE, RECORD_CHANNEL, AUDIO_FORMAT);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(false);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0,
                AudioManager.STREAM_VOICE_CALL);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
//        audioRecord = new AudioRecord(AUDIO_SOURCE, AUDIO_SAMPLING_RATE, RECORD_CHANNEL, AUDIO_FORMAT, bufferSize);
        createAudioRecord();

    }

    public void setCaptureConfig(AudioPlaybackCaptureConfiguration config) {
        configuration = config;
        initRecorder();
    }

    private void createAudioRecord() {
        audioRecord = new AudioRecord.Builder().setAudioFormat(
                new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(AUDIO_SAMPLING_RATE)
                        .setChannelMask(RECORD_CHANNEL)
                        .build())
                .setAudioPlaybackCaptureConfig(configuration)
                .build();
    }

    @Override
    public void recordStart() {
        if (mThread == null) {
            isRecording = true;
            audioRecord.startRecording();
            mThread = new AudioThread();
            mThread.start();
        }

    }

    @Override
    public void recordStop() {
        if (audioRecord != null) {
            isRecording = false;
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
            recordRelease();
        }
    }

    private void recordRelease() {
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            try {
                audioRecord.release();
                audioRecord = null;
                if (mOutStream != null) {
                    mOutStream.close();
                    mOutStream = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class AudioThread extends Thread {

        @Override
        public void run() {
            super.run();

            while (isRecording && audioRecord != null) {
                int audioSampleSize = audioRecord.read(buffer, 0, buffer.length);
                if (audioSampleSize > 0) {
                    Clog.w(TAG, "audioBuffer.size:" + audioSampleSize);
                    QueueManager.addAudioData(buffer);
                }
            }
        }

        public void writeFile() {
            try {
                mOutStream = new FileOutputStream(context.getFilesDir() + "/client_audio.pcm");
                byte[] audioBuffer = new byte[1920];
                while (isRecording && audioRecord != null) {
                    int audioSampleSize = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    if (audioSampleSize > 0) {
                        Clog.w(TAG, "audioBuffer:" + Arrays.toString(audioBuffer));
                        if (mOutStream != null) {
                            mOutStream.write(audioBuffer);
                        }
//                        ClientUdpSocket.getInstance().sendData(buffer);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                recordRelease();
                Clog.d(TAG, "exception: " + e);
            }
        }
    }


    /*************************************************************************/

    private void initTrack() {
        track = new AudioTrack(AudioManager.STREAM_MUSIC, AUDIO_SAMPLING_RATE, TRACK_CHANNEL, AUDIO_FORMAT, bufferSize, AUDIO_MODE);
    }

    public void playRecord() {
        if (playThread == null) {
            playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] music = new byte[1920];
                    try {
                        mInputStream = new FileInputStream(context.getFilesDir() + "/client_audio.pcm");
                        BufferedInputStream bis = new BufferedInputStream(mInputStream);
                        DataInputStream dis = new DataInputStream(bis);
                        int read;
                        while (dis.available() > 0) {
                            read = dis.read(music);
                            Clog.d(TAG, "read: " + read);
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
                        Clog.d(TAG, "播放失败：" + e);
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
