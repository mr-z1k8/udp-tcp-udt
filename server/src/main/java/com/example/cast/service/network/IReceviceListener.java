package com.example.cast.service.network;

public interface IReceviceListener {

    void onStatus(int status);

    void onResult(byte[] data);
}
