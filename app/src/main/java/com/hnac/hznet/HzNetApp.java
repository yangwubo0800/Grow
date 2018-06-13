package com.hnac.hznet;

import android.app.Application;

public class HzNetApp extends Application {
    public static boolean HzNet_DEBUG = false;

    public void onCreate() {
        super.onCreate();
        HzNet_DEBUG = true;
    }
}
