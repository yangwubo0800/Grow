package com.hnac.hznet;

import android.app.Application;
import android.util.Log;

public class HzNetApp extends Application {
    public static boolean HzNet_DEBUG = true;
    private String TAG = "HzNetApp";

    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"HzNetApp onCreate");
        HzNet_DEBUG = true;
    }
}
