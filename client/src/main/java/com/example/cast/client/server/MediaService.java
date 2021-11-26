package com.example.cast.client.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.cast.client.MainActivity;
import com.example.cast.client.R;
import com.example.cast.client.utils.Clog;
import com.example.cast.client.utils.QueueManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class MediaService extends Service {

    private final static String TAG = "EncodeService";
    private int mResultCode = 1;
    private Intent mResultData;
    private int mRequestCode = 100;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int mScreenDensity = 0;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;
    private MediaCodec mMediaCodec;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private OutputStream fos;

    private boolean isStart = true;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 高版本录屏服务需要前台服务通知
        createNotificationChannel();

        mResultCode = intent.getIntExtra("resultCode", 1);
        mResultData = intent.getParcelableExtra("data");
        mRequestCode = intent.getIntExtra("requestCode", 100);
        getScreenBaseInfo();
        mMediaProjection = createProjection();

        if (mRequestCode == 100) {  //  录屏mp4文件
            mMediaRecorder = createRecorder();
            mVirtualDisplay = createVirtualDisplay();
            mMediaRecorder.start();
        } else if (mRequestCode == 200) { // 录屏h264编码
            mHandlerThread = new HandlerThread("Codec_Record");
            mHandlerThread.start();
            startVideoRecord();
            startAudioRecord();
        }
        return Service.START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("正在录屏") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        isStart = false;
    }


    private MediaRecorder createRecorder() {
        MediaRecorder mediaRecorder = new MediaRecorder();

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        File file = new File(getFilesDir(), "kun_recorder.mp4");
        mediaRecorder.setOutputFile(file);

        mediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);
        mediaRecorder.setVideoFrameRate(15);
        mediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mediaRecorder;
    }

    private void startAudioRecord() {
        AudioRecorder recorder = new AudioRecorder(this);
        AudioPlaybackCaptureConfiguration configuration = new AudioPlaybackCaptureConfiguration.Builder(mMediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        recorder.setCaptureConfig(configuration);
        recorder.recordStart();
    }

    private void getScreenBaseInfo() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        mScreenDensity = dm.densityDpi;
    }

    private MediaProjection createProjection() {
        return ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(mResultCode, mResultData);
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MediaRecorder", mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    private VirtualDisplay createVirtualDisplay(Surface surface) {
        return mMediaProjection.createVirtualDisplay("MediaRecorder", mScreenWidth, mScreenHeight, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
    }

    private void startVideoRecord() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat format = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    mScreenWidth,
                    mScreenHeight
            );
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mScreenWidth * mScreenHeight);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            Surface surface = mMediaCodec.createInputSurface();
            mHandler = new Handler(mHandlerThread.getLooper());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMediaCodec.start();
                    createVirtualDisplay(surface);

//                    writeFile();
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    int i = 0;
                    while (isStart) {
                        int outIndex = mMediaCodec.dequeueOutputBuffer(info, 10000);
                        if (outIndex >= 0) {
                            ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(outIndex);
                            byte[] outData = new byte[info.size];

                            byteBuffer.get(outData);
                            Clog.d(TAG, "i=" + i++ + " sendData- " + outData.length);
//                            if (mSocket != null) {
//                                mSocket.sendData(outData);
//                            }
                            QueueManager.addVideoData(outData);
                            // 写入手机本地文件
                            check(outData);
                            mMediaCodec.releaseOutputBuffer(outIndex, false);
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeFile() {
        try {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            fos = new FileOutputStream(getFilesDir() + "/codec.h264");
            int i = 0;
            while (isStart) {
                int outIndex = mMediaCodec.dequeueOutputBuffer(info, 10000);
                if (outIndex >= 0) {
                    ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(outIndex);
                    byte[] outData = new byte[info.size];

                    byteBuffer.get(outData);
                    Clog.d(TAG, "i=" + i++ + " sendData- " + outData.length);
                    // 写入手机本地文件
                    check(outData);
                    fos.write(outData);
                    mMediaCodec.releaseOutputBuffer(outIndex, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            realseStream();
        }
    }

    private void realseStream() {
        try {
            isStart = false;
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Clog.e(TAG, e.toString());
        }
    }

    private void check(byte[] data) {
        int index = 4;
        if (data[2] == (byte) 0x1) {
            index = 3;
        }

        // 0001 111 与
        int naluType = (data[index] & (byte) 0x1F);

        if (naluType == 7) {
            Clog.d(TAG, "SPS");
        } else if (naluType == 8) {
            Clog.d(TAG, "PPS");
        } else if (naluType == 5) {
            Clog.d(TAG, "IDR");
        } else {
            Clog.d(TAG, "非IDR=" + naluType);
        }
    }
}
