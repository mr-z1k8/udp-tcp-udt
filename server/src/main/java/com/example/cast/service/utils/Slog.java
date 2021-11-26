package com.example.cast.service.utils;

import android.util.Log;

public class Slog {

    private final static String TAG = "mr_zengkun";

    private final static int VERBOSE = 1;
    private final static int DEBUG = 2;
    private final static int INFO = 3;
    private final static int WARN = 4;
    private final static int ERROR = 5;
    private final static int ASSERT = 6;
    private final static int ALL = 7;

    private static int Level = ALL;

    static {

    }

    public static void v(String subTag, String value) {
        if (Level >= VERBOSE) {
            Log.d(TAG, "[" + subTag + "]: " + value);
        }
    }

    public static void d(String subTag, String value) {
        if (Level >= DEBUG) {
            Log.d(TAG, "[" + subTag + "]: " + value);
        }
    }

    public static void i(String subTag, String value) {
        if (Level >= INFO) {
            Log.d(TAG, "[" + subTag + "]: " + value);
        }
    }

    public static void w(String subTag, String value) {
        if (Level >= WARN) {
            Log.d(TAG, "[" + subTag + "]: " + value);
        }
    }

    public static void e(String subTag, String value) {
        if (Level >= ERROR) {
            Log.d(TAG, "[" + subTag + "]: " + value);
        }
    }

    public static void a(String subTag, String value) {
        if (Level >= ASSERT) {
            Log.d(TAG, "[" + subTag + "]: " + value);
        }
    }


}
