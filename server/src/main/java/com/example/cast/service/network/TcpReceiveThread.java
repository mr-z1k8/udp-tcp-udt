package com.example.cast.service.network;

import android.content.Context;

import com.example.cast.service.utils.Slog;
import com.example.cast.service.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class TcpReceiveThread extends Thread {

    private final static String TAG = "TcpReceiveThread";

    private ServerSocket mServerSocket;
    private Socket mSocket;

    private OutputStream mOutStream;
    private InputStream mInputStream;
    private byte[] buffer;

    private boolean isRunning = true;
    private int readLength = 0;
    private int msgLength = 0;
    private int status = -1;
    private final static int error = -1;
    public final static int stop = 0;
    public final static int ready = 1;
    public final static int start = 2;
    private final static int DEFAULT_DATA_SIZE = 1024;
    private final static int PORT = 8003;

    private IReceviceListener mListener;

    public TcpReceiveThread() {
        this(null, null);

    }

    public TcpReceiveThread(ServerSocket socket) {
        this(null, socket);
    }

    public TcpReceiveThread(Context context, ServerSocket serverSocket) {
        mServerSocket = serverSocket;
        buffer = new byte[DEFAULT_DATA_SIZE];
        try {
            if (mServerSocket == null) {
                mServerSocket = new ServerSocket(PORT);
            }
            status = ready;
            if (context != null) {
                mOutStream = new FileOutputStream(context.getFilesDir() + "/server_code.h264");
            }
        } catch (Exception e) {
            status = error;
            Slog.d(TAG, e.toString());
        }
    }

    public boolean isConnected() {
        boolean isConnect = false;
        if (status >= ready) {
            isConnect = true;
        }
        return isConnect;
    }

    @Override
    public void run() {
        super.run();
        try {
            if (mServerSocket == null) {
                mServerSocket = new ServerSocket(PORT);
                status = ready;
            }
            if (status == ready) {
                isRunning = true;
                mListener.onStatus(ready);
            }
            while (true) {
                mSocket = mServerSocket.accept();
                mInputStream = mSocket.getInputStream();
                if (mInputStream == null) return;
                status = start;
                int ret = splitPerFramData();
                if (ret == -1) {
                    status = stop;
                }
            }
        } catch (Exception e) {
            status = error;
            Slog.e(TAG, e.toString());
        }

    }

    public void setReceiveListener(IReceviceListener listener) {
        this.mListener = listener;
    }

    public void close() {
        isRunning = false;
        try {
            if (mOutStream != null) {
                mOutStream.close();
                mOutStream = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            if (mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (Exception e) {
            Slog.d(TAG, e.toString());
        }
    }

    private int splitPerFramData() throws IOException {
        while (isRunning) {
            int packageLength = mInputStream.read(buffer, readLength, buffer.length - readLength);
            readLength += packageLength;
            if (packageLength == -1) {
                isRunning = false;
                continue;
            }
            while (true) {
                if (msgLength <= 0 && readLength >= 4) {
                    msgLength = (int) Utils.decodeIntBigEndian(buffer, 0, 4);
//                    if (msgLength > 40 * DEFAULT_DATA_SIZE) {
//                        readLength = 0;
//                        msgLength = 0;
//                        receiver = new byte[DEFAULT_DATA_SIZE];
//                        break;
//                    }
                    if (msgLength < 0) {
                        isRunning = false;
                        break;
                    }
                    if (msgLength > buffer.length) {
                        byte[] tmp = new byte[buffer.length];
                        System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                        buffer = new byte[msgLength];
                        System.arraycopy(tmp, 0, buffer, 0, tmp.length);
                    }
                }
                if (msgLength <= 0) {
                    break;
                }
                if (readLength < msgLength) {
                    break;
                }
                mListener.onResult(Arrays.copyOf(buffer, msgLength));
                readLength -= msgLength;
                if (readLength > 0) {
                    byte[] tmp = new byte[readLength];
                    System.arraycopy(buffer, msgLength, tmp, 0, tmp.length);
                    buffer = new byte[Math.max(DEFAULT_DATA_SIZE, tmp.length)];
                    System.arraycopy(tmp, 0, buffer, 0, tmp.length);
                    msgLength = 0;
                    continue;
                } else {
                    msgLength = 0;
                    buffer = new byte[DEFAULT_DATA_SIZE];
                    break;
                }
            }
            try {
                // 防止cpu空转
                Thread.sleep(2);
            } catch (Exception e) {
                Slog.w(TAG, e.toString());
            }
        }
        return -1;
    }

}
