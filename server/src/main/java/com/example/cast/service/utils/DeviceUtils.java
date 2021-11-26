package com.example.cast.service.utils;

import com.example.cast.service.network.TcpReceiveThread;

public class DeviceUtils {

    private static DeviceUtils instance;
    private TcpReceiveThread tcpReceiveThread;

    private DeviceUtils(){}

    public static DeviceUtils getInstance(){
        if (instance == null){
            synchronized (DeviceUtils.class){
                if (instance == null){
                    instance = new DeviceUtils();
                }
            }
        }
        return instance;
    }


    public TcpReceiveThread getTcpReceiveThread() {
        return tcpReceiveThread;
    }

    public void setTcpReceiveThread(TcpReceiveThread tcpReceiveThread) {
        this.tcpReceiveThread = tcpReceiveThread;
    }
}
