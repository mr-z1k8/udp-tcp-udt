package com.example.cast.client;

import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.cast.client.bean.DeviceInfo;
import com.example.cast.client.network.TcpSendThread;
import com.example.cast.client.network.UdpSendThread;
import com.example.cast.client.network.UdtSendThread;
import com.example.cast.client.server.MediaService;
import com.example.cast.client.utils.Clog;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class MainActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "MainActivity";
    private static final int H264ENCODE_REQUEST_CODE = 200;

    private Button btnQr;
    private Button btnCon;
    private Button btnStartMirror;
    private Button btnStopMirror;
    private TextView tvStatus;
    private EditText etDevIP;
    private EditText etDevPort;

    private DeviceInfo info;
    private TcpSendThread tcpSendThread;
    private UdpSendThread udpSendThread;
    private UdtSendThread udtSendThread;
    private MediaProjectionManager mediaProjectionManager;

    private IConnectListener mListener = new IConnectListener() {
        @Override
        public void onStatus(boolean isConnect) {
            if (isConnect) {
//                tvStatus.setText("已连接");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initThread() {
        tcpSendThread = new TcpSendThread(info);
        tcpSendThread.setConnectListener(mListener);
        tcpSendThread.start();

        udpSendThread = new UdpSendThread(info);
        udpSendThread.start();
    }

    private void initThreadToUdt() {
        udtSendThread = new UdtSendThread(info);
        udtSendThread.start();
    }


    private void initView() {
        btnQr = findViewById(R.id.btnQr);
        btnCon = findViewById(R.id.btnCon);
        btnStartMirror = findViewById(R.id.btnStartMirror);
        btnStopMirror = findViewById(R.id.btnStopMirror);
        tvStatus = findViewById(R.id.tvStatus);
        etDevIP = findViewById(R.id.etDevIP);
        etDevPort = findViewById(R.id.etPort);

        btnQr.setOnClickListener(this);
        btnCon.setOnClickListener(this);
        btnStartMirror.setOnClickListener(this);
        btnStopMirror.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnQr:
                Intent intent = new Intent();
                intent.setClass(this, ClientCaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            case R.id.btnCon:
                connectDevice();
                break;
            case R.id.btnStartMirror:
                if (tcpSendThread != null && udpSendThread != null) {
                    if (!tcpSendThread.isConnect() && !udpSendThread.isConnect()) {
                        Toast.makeText(this, "地址无法解析", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                startRecorder(H264ENCODE_REQUEST_CODE);
                break;
            case R.id.btnStopMirror:
                stopRecorder();
                break;
        }
    }

    private void initData() {
        checkPermission();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    private void checkPermission() {
        int checkPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                + ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                + ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ActivityCompat.checkSelfPermission(this, Manifest.permission.CAPTURE_AUDIO_OUTPUT);
        if (checkPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAPTURE_AUDIO_OUTPUT
            }, 123);
        }
    }

    private void connectDevice() {
        String ipStr = etDevIP.getText().toString();
        String portStr = etDevPort.getText().toString();
        info = new DeviceInfo();
        if (portStr != null && portStr.length() > 0) {
            info.port = Integer.parseInt(portStr);
        }
        if (ipStr != null && ipStr.length() > 0) {
            info.ip = ipStr;
        }
        parseRealInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            info = DeviceInfo.fromJson(result.getContents());
            Clog.d(TAG, "onActivityResult: " + info.toString());
            parseRealInfo();
        }
        if (resultCode == RESULT_OK && requestCode == H264ENCODE_REQUEST_CODE) {
            Intent service = new Intent(this, MediaService.class);
            service.putExtra("resultCode", resultCode);
            service.putExtra("data", data);
            service.putExtra("requestCode", requestCode);
            startService(service);
        }
    }

    private void parseRealInfo() {
        if (TextUtils.isEmpty(info.ip)) {
            Toast.makeText(this, "无法正确解析设备ip地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (info.port == 0) {
            Toast.makeText(this, "解析端口port错误", Toast.LENGTH_SHORT).show();
            return;
        }
//        initThread();
        initThreadToUdt();
    }

    private void stopRecorder() {
        Intent service = new Intent(this, MediaService.class);
        stopService(service);
    }

    private void startRecorder(int code) {
        createScreenCapture(code);
    }

    private void createScreenCapture(int code) {
        // 录制
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, code);
    }

    @Override
    protected void onDestroy() {
        if (tcpSendThread != null) {
            tcpSendThread.close();
            tcpSendThread = null;
        }
        if (udpSendThread != null) {
            udpSendThread.close();
            udpSendThread = null;
        }
        if (udtSendThread != null) {
            udtSendThread.close();
            udtSendThread = null;
        }
        super.onDestroy();
    }
}
