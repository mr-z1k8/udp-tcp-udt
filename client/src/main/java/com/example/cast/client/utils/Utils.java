package com.example.cast.client.utils;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class Utils {

    public static int getUid(Context context) {
        try {
            String packageName = context.getPackageName(); // 指定包名
            PackageManager pm = context.getPackageManager();
            @SuppressLint("WrongConstant") ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
            Clog.d("Utils", "uid: " + ai.uid);
            return ai.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static byte[] splitBufferHead(byte[] buffer) {
        byte[] head = new byte[4];
        int len = buffer.length + head.length;
        byte[] data = new byte[len];
        head[0] = (byte) ((len >> 3 * 8) & 0xff);
        head[1] = (byte) ((len >> 2 * 8) & 0xff);
        head[2] = (byte) ((len >> 1 * 8) & 0xff);
        head[3] = (byte) ((len >> 0 * 8) & 0xff);
        System.arraycopy(head, 0, data, 0, head.length);
        System.arraycopy(buffer, 0, data, head.length, buffer.length);
        return data;
    }
}
