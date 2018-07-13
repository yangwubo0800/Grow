package com.grow;

import android.app.Application;
import android.util.Log;

public class GrowApp extends Application{
    public static boolean Grow_DEBUG = true;
    private String TAG = "GrowApp";

    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"GrowApp onCreate");
        Grow_DEBUG = true;
    }
}
