package com.example.cast.client.network;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.example.cast.client.bean.DeviceInfo;
import com.example.cast.client.utils.Clog;
import com.example.cast.client.utils.QueueManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UdpSendThread extends Thread {

    private final static String TAG = "UdpSendThread";

    private final static String HOST = "192.168.4.32";
    private final static int PORT = 8003;
    private DatagramSocket mSocket;
    private InetSocketAddress mAddress;
    private OutputStream mOutputStream;
    private DeviceInfo info;

    public UdpSendThread() {
        this(null, new DeviceInfo(HOST, PORT));
    }

    public UdpSendThread(Context context) {
        this(context, new DeviceInfo(HOST, PORT));
    }

    public UdpSendThread(DeviceInfo info) {
        this(null, info);
    }

    public UdpSendThread(Context context, DeviceInfo info) {
        this.info = info;
        try {
            if (context != null) {
                mOutputStream = new FileOutputStream(context.getFilesDir() + "/client_audio.pcm");
            }
        } catch (Exception e) {
            Clog.e(TAG, e.toString());
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            mSocket = new DatagramSocket(info.port);
            mSocket.setReuseAddress(true);
            mAddress = new InetSocketAddress(info.ip, info.port);
            mSocket.connect(mAddress);
            while (true){
                DatagramPacket packet = null;
                byte[] data = QueueManager.takeAudioData();
                if (data != null) {
                    packet = new DatagramPacket(data, data.length, mAddress);
                    Clog.d(TAG, "packet.length: " + packet.getLength());
                    mSocket.send(packet);
                }
            }
        } catch (Exception e) {
            Clog.d(TAG, e.toString());
        }
    }

    public boolean isConnect() {
        boolean flag = false;
        if (mSocket != null) {
            flag = mSocket.isConnected();
            Clog.d(TAG, "flag: " + flag);
        }
        return flag;
    }

    public void close() {
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
            if (!mSocket.isClosed()) {
                mSocket.close();
                mSocket = null;
            }
        } catch (Exception e) {
            Clog.e(TAG, e.toString());
        }
    }

}
