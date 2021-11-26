package com.example.cast.service;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.cast.service.bean.DeviceInfo;
import com.example.cast.service.network.IReceviceListener;
import com.example.cast.service.network.TcpReceiveThread;
import com.example.cast.service.network.UdpReceiveThread;
import com.example.cast.service.network.UdtReceiveThread;
import com.example.cast.service.utils.AudioUtil;
import com.example.cast.service.utils.Decoder;
import com.example.cast.service.utils.Utils;

public class MainActivity extends Activity {

    private SurfaceView surfaceVideo;
    private ImageView qrImage;
    private TextView tvMsg;

    private SurfaceHolder holder;
    private Decoder decoder;
    private String content;

    private TcpReceiveThread tcpReceiveThread;
    private UdpReceiveThread udpReceiveThread;
    private UdtReceiveThread udtReceiveThread;

    private IReceviceListener mVideoListener = new IReceviceListener() {
        @Override
        public void onStatus(int status) {
            if (status == TcpReceiveThread.ready) {
                tvMsg.setText("server already started " + content);
            }
        }

        @Override
        public void onResult(byte[] data) {
            decoder.decode(data, 0, data.length);
        }
    };

    private IReceviceListener mAudioListener = new IReceviceListener() {
        @Override
        public void onStatus(int status) {

        }

        @Override
        public void onResult(byte[] data) {
            AudioUtil.getInstance().revicesRecord(data);
        }
    };

    private IReceviceListener mUdtVideoListener = new IReceviceListener() {
        @Override
        public void onStatus(int status) {

        }

        @Override
        public void onResult(byte[] data) {
            decoder.decode(data, 0, data.length);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
//        initData();
        initUdtData();
    }

    private void initData() {
        tcpReceiveThread = new TcpReceiveThread();
        tcpReceiveThread.setReceiveListener(mVideoListener);
        tcpReceiveThread.start();

        udpReceiveThread = new UdpReceiveThread();
        udpReceiveThread.setReceiveListener(mAudioListener);
        udpReceiveThread.start();
    }

    private void initUdtData(){
        udtReceiveThread = new UdtReceiveThread();
        udtReceiveThread.setReceiveListener(mUdtVideoListener);
        udtReceiveThread.start();
    }

    private void initView() {
        surfaceVideo = findViewById(R.id.surfceVideo);
        tvMsg = findViewById(R.id.tvMsg);
        qrImage = findViewById(R.id.ivQr);

        content = DeviceInfo.toJson(Utils.getIPAddress(), 8003);
        Bitmap bitmap = Utils.createQRCodeBitmap(content, 400, 400);
        qrImage.setImageBitmap(bitmap);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int height = metrics.heightPixels;
        int width = (height * 1080) / 2296;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = Gravity.CENTER;
        params.leftMargin = 0;
        params.topMargin = 0;
        surfaceVideo.setLayoutParams(params);

        holder = surfaceVideo.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                if (decoder == null) {
                    decoder = new Decoder(holder);
                    decoder.startCodec();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                if (decoder != null) {
                    decoder.close();
                    decoder = null;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (tcpReceiveThread != null) {
            tcpReceiveThread.close();
            tcpReceiveThread = null;
        }
        if (udpReceiveThread != null) {
            udpReceiveThread.close();
            udpReceiveThread = null;
        }
        if (udtReceiveThread != null){
            udtReceiveThread.close();
            udtReceiveThread = null;
        }
        super.onDestroy();
    }
}