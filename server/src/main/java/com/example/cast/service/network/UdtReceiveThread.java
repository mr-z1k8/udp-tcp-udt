package com.example.cast.service.network;

import com.example.cast.service.utils.Slog;
import com.example.cast.service.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import udt.UDTServerSocket;
import udt.UDTSocket;

public class UdtReceiveThread extends Thread {
    private final static String TAG = "UdtReceiveThread";

    private final static int port = 8003;
    private UDTServerSocket mServerSocket;
    private UDTSocket mSocket;
    private byte[] buffer;
    private InputStream mInputStream;
    private final static int DEFAULT_DATA_SIZE = 102400;
    private IReceviceListener mListener;
    private boolean isRunning = false;

    public UdtReceiveThread() {
        buffer = new byte[DEFAULT_DATA_SIZE];
        Slog.d(TAG, "init");
    }

    public void setReceiveListener(IReceviceListener listener) {
        this.mListener = listener;
    }

    @Override
    public void run() {
        super.run();
        try {
            mServerSocket = new UDTServerSocket(port);
            mSocket = mServerSocket.accept();
            mInputStream = mSocket.getInputStream();
            Slog.d(TAG, "run");

            int len = 0;
            while (true) {
                Slog.d(TAG, "Running");
                isRunning = true;
                if (mInputStream != null && len != -1) {
                    len = mInputStream.read(buffer);
                    if (len == 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                            continue;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    byte[] tmp = new byte[len];
                    System.arraycopy(buffer, 0, tmp, 0, len);
                    mListener.onResult(tmp);
                    Slog.d(TAG, "tmp.length: " + tmp.length);
                }
            }
        } catch (Exception e) {
            isRunning = false;
            Slog.e(TAG, e.toString());
        }
    }

    public void close() {
        isRunning = false;
        try {
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            if (mServerSocket != null) {
                mServerSocket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
