package com.example.cast.client.network;

import com.example.cast.client.bean.DeviceInfo;
import com.example.cast.client.utils.Clog;
import com.example.cast.client.utils.QueueManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import udt.UDTClient;

public class UdtSendThread extends Thread {

    private final static String TAG = "UdtSendThread";

    private final static String HOST = "192.168.4.32";
    private final static int PORT = 8003;
    private UDTClient client;
    private InetAddress mAddress;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private DeviceInfo info;

    private boolean isRunning = false;


    public UdtSendThread() {
        this(new DeviceInfo(HOST, PORT));
    }

    public UdtSendThread(DeviceInfo info) {
        this.info = info;
        try {
            mAddress = InetAddress.getByName(info.ip);
            Clog.d(TAG,"init");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        super.run();
        try {
            client = new UDTClient(mAddress);
            client.connect(info.ip, info.port);
            isRunning = true;
            Clog.d(TAG,"run");
        } catch (Exception e) {
            Clog.e(TAG, e.toString());
            isRunning = false;
            e.printStackTrace();
        }
        Clog.d(TAG,"isRunning");
        while (isRunning) {
            try {
                byte[] buffer = QueueManager.takeVideoData();
                if (buffer != null) {
                    Clog.d(TAG,"buffer.length: " + buffer.length);
                    client.sendBlocking(buffer);
                }
            } catch (Exception e) {
                Clog.e(TAG, e.toString());
                isRunning = false;
            }
        }
    }

    public void close() {
        isRunning = false;
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (client != null) {
                client = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
