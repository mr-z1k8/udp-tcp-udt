package com.example.cast.service;

import android.app.Application;
import android.content.Context;


public class ServiceApplication extends Application {

    private static Context applicationContext;

    public static Context getApplication() {
        return applicationContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
    }
}
