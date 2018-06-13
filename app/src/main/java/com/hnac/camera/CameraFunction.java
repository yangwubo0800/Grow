package com.hnac.camera;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraFunction {

    private static final String TAG = CameraFunction.class.getClass().getName();
    private Context mContext;
    private static String fileFullName;

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
                String targetDir = sdDir + "/" + "webview_photo";
                File file = new File(targetDir);
                if (!file.exists()) {
                    file.mkdirs();
                }

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "PIC_" + timeStamp;
                String suffix = ".jpeg";
                fileFullName = targetDir + "/" + imageFileName + suffix;
            } else if (type.equals("video")) {
                String targetDir = sdDir + "/" + "webview_video";
                File file = new File(targetDir);
                if (!file.exists()) {
                    file.mkdirs();
                }

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String FileName = "VID_" + timeStamp;
                String suffix = ".mp4";
                fileFullName = targetDir + "/" + FileName + suffix;
            } else {
                fileFullName = null;
            }

            //create media file
            if (!TextUtils.isEmpty(fileFullName)) {
                mediaFile = new File(fileFullName);
            }

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
    public static  void takePhoto(Context context) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, "TakePhoto");
        //初始化并调用摄像头
        intent.putExtra(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(createMediaFile("photo")));
        context.startActivity(intent);
    }


    /**
     * 提供录制视频功能，配置限制录制时间为10s
     * @param context
     */
    public static void recordVideo(Context context) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        //设置视频录制的最长时间
        intent.putExtra (MediaStore.EXTRA_DURATION_LIMIT,10);
        //设置视频录制的画质
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0.5);
        // set the file save director
        try {
            Uri fileUri = Uri.fromFile(createMediaFile("video"));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        context.startActivity(intent);
    }
}
