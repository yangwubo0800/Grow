package com.hnac.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.PermissionChecker;
import android.util.Log;

import com.hnac.hznet.HzNetApp;

import java.io.File;

public class HzNetUtil {

    private static final String TAG = HzNetUtil.class.getClass().getName();

    /**
     * 检测是否联网
     * @param activity
     * @return
     */
    public static boolean isNetworkAvailable(Context activity) {
        //得到应用上下文
        Context context = activity.getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        } else {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0) {
                for (int i = 0; i < networkInfo.length; i++) {
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    //删除文件夹和文件夹里面的文件
    public static void deleteDir(final String pPath) {
        File dir = new File(pPath);
        deleteDirWithFile(dir);
    }

    public static void deleteDirWithFile(File dir) {

        try {
            if (dir == null || !dir.exists() || !dir.isDirectory()) {
                Log.d(TAG,"=====deleteDirWithFile return");
                return;
            }
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete(); // 删除所有文件
                }
                else if (file.isDirectory()) {
                    deleteDirWithFile(file); // 递规的方式删除文件夹
                }
            }
            dir.delete();// 删除目录本身
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 删除指定目录下的缓存文件
     * @param context
     */
    public static void clearCacheFile(Context context) {

        // TODO: 清理缓存, 删除相关缓存目录下的文件
        String cachePath = context.getCacheDir().getPath();
        deleteDir(cachePath);

        if (HzNetApp.HzNet_DEBUG) {
            Log.d(TAG,"clearCacheFile cachePath=" + cachePath);
        }

        String webviewCachePath = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            //高版本的目录是否如此需要测试，是否可以使用模拟器
            webviewCachePath = context.getDataDir().getPath() + "/app_webview";
        } else {
            webviewCachePath = "/data/data/" + context.getPackageName() + "/app_webview";
        }
        deleteDir(webviewCachePath);

        if (HzNetApp.HzNet_DEBUG) {
            Log.d(TAG,"clearCacheFile webviewCachePath=" + webviewCachePath);
        }

        //delete  code cache  dir
        String codeCachePath = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            codeCachePath = context.getCodeCacheDir().getPath();
        } else {
            //低版本的目录是否如此需要测试，是否可以使用模拟器
            codeCachePath = "/data/data/" + context.getPackageName() + "/code_cache";
        }
        deleteDir(codeCachePath);

        if (HzNetApp.HzNet_DEBUG) {
            Log.d(TAG,"clearCacheFile codeCachePath=" + codeCachePath);
        }
    }

    /**
     * 拨号功能，将号码送至拨号盘，准备拨打
     * @param activity
     * @param number
     */
    public static void callNumber(Activity activity, String number) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+number));
        activity.startActivity(intent);
    }

    /**
     * 检测权限是否授权, 如果已经授权，返回true
     * @param context
     * @param permission
     * @return
     */
    public static boolean selfPermissionGranted(Context context, String permission) {
        int targetSdkVersion = 0;
        boolean ret = false;

        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;
            Log.d(TAG,"selfPermissionGranted targetSdkVersion="+targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                ret = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            } else {
                ret = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
            }
        }else{
            return true;
        }
        Log.d(TAG,"selfPermissionGranted permission:" + permission +" grant:" + ret);
        return ret;
    }
}
