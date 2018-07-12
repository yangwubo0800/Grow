package com.hnac.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraFunction {

    private static final String TAG = "CameraFunction";

    //生成文件路径，全局变量，供外面使用,相当于是工具类了，不以对象提供，而以静态类暴露
    public static String fileFullName;
    private static String FILE_PROVIDER_AUTHORTIES = "com.hznet.fileprovider";
    private static String PHOTO_PATH = "/hznet/photo/";
    private static String VIDEO_PATH = "/hznet/video/";

    /**
     * 根据时间生成图片或者视频名称
     * @param type
     * @return
     */
    private static File createMediaFile(String type) {

        if (TextUtils.isEmpty(type)) {
            return null;
        }

        //判断是否有SD卡
        String sdDir = null;
        boolean isSDcardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if(isSDcardExist) {
            sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            sdDir = Environment.getRootDirectory().getAbsolutePath();
            Log.d(TAG," NO SD CARD !!!!");
            return null;
        }

        File mediaFile = null;

        try {
            if (type.equals("photo")) {
                String targetDir = sdDir + PHOTO_PATH;
                File file = new File(targetDir);
                if (!file.exists()) {
                    file.mkdirs();
                }
                Log.d(TAG,"=====createMediaFile photo file.exists()="+file.exists());

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "PIC_" + timeStamp;
                String suffix = ".jpeg";
                fileFullName = targetDir  + imageFileName + suffix;
            } else if (type.equals("video")) {
                String targetDir = sdDir + VIDEO_PATH;
                File file = new File(targetDir);
                if (!file.exists()) {
                    file.mkdirs();
                }

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String FileName = "VID_" + timeStamp;
                String suffix = ".mp4";
                fileFullName = targetDir + FileName + suffix;
            } else {
                fileFullName = null;
            }

            //create media file
            if (!TextUtils.isEmpty(fileFullName)) {
                mediaFile = new File(fileFullName);
            }

            Log.d(TAG,"=====createMediaFile fileFullName="+fileFullName);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return mediaFile;
        }

    }

    /**
     * 提供拍照功能
     * @param context
     */
    public static  Intent takePhoto(Context context) {
        Log.d(TAG, "=====takePhoto");
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, "TakePhoto");

        if (takePhotoIntent.resolveActivity(context.getPackageManager()) != null) {
            File newFile = createMediaFile("photo");
            // TODO: Android7.0 upgrade
            Uri contentUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORTIES, newFile);
            Log.i(TAG, "contentUri = " + contentUri.toString());
            List<ResolveInfo> resInfoList= context.getPackageManager().queryIntentActivities(takePhotoIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.i(TAG, "packageName = " + packageName);
                context.grantUriPermission(packageName, contentUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

//            context.startActivity(takePhotoIntent);
        }

        return takePhotoIntent;
    }


    /**
     * 提供录制视频功能
     * @param context
     */
    public static Intent recordVideo(Context context) {
        Log.d(TAG, "=====recordVideo");
        Intent recordVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        recordVideoIntent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, "TakePhoto");

        if (recordVideoIntent.resolveActivity(context.getPackageManager()) != null) {
            File newFile = createMediaFile("video");
            // TODO: Android7.0 upgrade
            Uri contentUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORTIES, newFile);
            Log.i(TAG, "contentUri = " + contentUri.toString());
            List<ResolveInfo> resInfoList= context.getPackageManager().queryIntentActivities(recordVideoIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, contentUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            recordVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            //context.startActivity(recordVideoIntent);
        }

        return recordVideoIntent;
    }
}
