package com.example.cast.client.network;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Message;

import com.example.cast.client.IConnectListener;
import com.example.cast.client.bean.DeviceInfo;
import com.example.cast.client.utils.Clog;
import com.example.cast.client.utils.QueueManager;
import com.example.cast.client.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TcpSendThread extends Thread {

    private final static String TAG = "TcpSendThread";

    private final static String HOST = "192.168.4.32";
    private final static int PORT = 8003;
    private Socket mSocket;

    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private InetAddress inetAddress;
    private boolean isRunning = false;
    private DeviceInfo info;
    private IConnectListener mListener;

    public TcpSendThread() {
        this(null, new DeviceInfo(HOST, PORT));
    }

    public TcpSendThread(Context context) {
        this(context, new DeviceInfo(HOST, PORT));
    }

    public TcpSendThread(DeviceInfo info) {
        this(null, info);
    }

    public TcpSendThread(Context context, DeviceInfo info) {
        this.info = info;
    }

    @Override
    public void run() {
        super.run();
        try {
            inetAddress = InetAddress.getByName(info.ip);
            mSocket = new Socket(inetAddress, info.port);
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();
            isRunning = true;
        } catch (Exception e) {
            Clog.e(TAG, e.toString());
            isRunning = false;
        }
        mListener.onStatus(isRunning);

        while (isRunning && mOutputStream != null) {
            try {
                byte[] data = QueueManager.takeVideoData();
                if (data != null) {
                    Clog.d(TAG, "fram.length: " + data.length);
                    mOutputStream.write(Utils.splitBufferHead(data));
                    mOutputStream.flush();
                }
            } catch (Exception e) {
                Clog.e(TAG, e.toString());
                isRunning = false;
            }
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
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e) {
            Clog.e(TAG, e.toString());
        }
    }

    public void setConnectListener(IConnectListener listener) {
        this.mListener = listener;
    }
}
