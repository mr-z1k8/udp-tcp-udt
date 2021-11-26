package com.example.cast.client.server;

public interface IAudioRecorder {

    int AUDIO_SAMPLING_RATE = 44100;
    int AUDIO_ENCODING_BIT_RATE = 96000;

    void initRecorder();

    void recordStart();

    void recordStop();

}
