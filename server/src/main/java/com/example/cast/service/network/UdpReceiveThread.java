package com.example.cast.service.network;

import android.content.Context;

import com.example.cast.service.utils.Slog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class UdpReceiveThread extends Thread {

    private final static String TAG = "UdpReceiveThread";
    private final static int CONNECT_OUT_TIME = 120 * 1000;
    private final static int PORT = 8003;

    private DatagramSocket socket;
    private byte[] audioBuffer;
    private OutputStream fos;
    private boolean isRunning = false;
    private IReceviceListener mListener;

    public UdpReceiveThread() {
        this(null);
    }

    public UdpReceiveThread(Context context) {
        audioBuffer = new byte[1920];
        try {
            if (context != null) {
                fos = new FileOutputStream(context.getFilesDir() + "/server_audio.pcm");
            }
            socket = new DatagramSocket(PORT);
            socket.setReuseAddress(true);
            socket.setSoTimeout(CONNECT_OUT_TIME);
            isRunning = true;
        } catch (Exception e) {
            Slog.d(TAG, e.toString());
        }
    }

    public void setReceiveListener(IReceviceListener listener) {
        this.mListener = listener;
    }

    @Override
    public void run() {
        super.run();
        DatagramPacket packet = new DatagramPacket(audioBuffer, audioBuffer.length);
        try {
            while (isRunning) {
                socket.receive(packet);
                if (packet.getData() == null) return;
                int size = packet.getLength();
                byte[] data = Arrays.copyOf(audioBuffer, size);
                mListener.onResult(data);
            }
        } catch (IOException e) {
            isRunning = false;
            Slog.d(TAG, "接收数据失败 e: " + e);
        }
    }

    private void receiveWriteFile(Context context) {
        DatagramPacket packet = new DatagramPacket(audioBuffer, audioBuffer.length);
        try {
            int i = 0;
            fos = new FileOutputStream(context.getFilesDir() + "/service_audio.pcm");
            while (true) {
                Slog.d(TAG, "开始准备接收...");
                socket.receive(packet);
                if (packet.getData() == null) {
                    return;
                }
                Slog.d(TAG, "连接成功,正在接收数据...");
                Slog.d(TAG, "i=" + i++ + " acceptData- " + packet.getLength());
                // 写入文件
                byte[] data = Arrays.copyOf(audioBuffer, packet.getLength());
                fos.write(data);
            }
        } catch (IOException e) {
            Slog.d(TAG, "接收数据失败 e: " + e);
            e.printStackTrace();
        } finally {
            realseStream();
        }
    }

    private void realseStream() {
        try {
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Slog.d(TAG, e.toString());
        }
    }

    public void close() {
        isRunning = false;
        try {
            if (fos != null) {
                fos.close();
                fos = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
