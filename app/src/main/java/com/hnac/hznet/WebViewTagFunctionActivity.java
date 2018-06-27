package com.hnac.hznet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.hnac.camera.CameraFunction;
import com.hnac.camera.QRCodeScanActivity;
import com.hnac.utils.HzNetUtil;
import com.hnac.utils.NotificationUtils;

public class WebViewTagFunctionActivity extends AppCompatActivity {

    private String  TAG = this.getClass().getName();
    private WebView mWebviewPage;
    private String localFile = "file:///android_asset/main.html";
    private String hzNetDebug = "http://175.6.40.67:18894/hznet/app/main.html";
    private ProgressBar mProgressBar;
    private final int SCAN_QRCODE_REQUEST = 2;
    private final int TAKE_PHOTO_REQUEST = 3;
    private final int RECORD_VIDEO_REQUEST = 4;
    private final int PERMISSION_REQUEST_CAMERA = 5;
    private final int CAMERA_PERMISSIONS_REQUEST_CODE = 6;
    //手机本地视频
    private String localVideoUrl = "/sdcard/webview_video/VID_20180625_154855.mp4";
    private boolean mGotCameraPermission = false;
    //http, 萤石云开发测试地址，直播流
    private String liveUrl = "http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.hd.m3u8";
    //RTMP 格式安卓自带mediaPlayer 不支持，直播流
    private String liveUrl2 = "rtmp://rtmp.open.ys7.com/openlive/0a2cff841ba243809a9a8611e29edc9b.hd";
    //https，录制视频播放
    //private String videoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4";
    private String videoUrl = "http://175.6.40.67:18894/hznet/app/VID20170430200439.mp4";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPermission();
        setContentView(R.layout.activity_web_view_tag_function);

        mWebviewPage = findViewById(R.id.webview_function_page);
        mProgressBar = findViewById(R.id.progressBar);

        WebSettings webSettings = mWebviewPage.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //设置缓存模式，无网络时依然可以打开已经打开过的网页
        if (HzNetUtil.isNetworkAvailable(this)) {
            Log.d(TAG,"=====network available");
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            Log.d(TAG,"=====network not available");
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);

        mWebviewPage.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG,"=====shouldOverrideUrlLoading url=" + url);
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String title = view.getTitle();
                // Log.d(TAG,"=====onPageFinished title=" + title);
            }

        });

        //设置加载过程 进度条
        mWebviewPage.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setProgress(newProgress);
                if (newProgress ==  100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                super.onProgressChanged(view, newProgress);
            }
        });

        mWebviewPage.loadUrl(hzNetDebug);

        //注册JS调用的natvie接口
        mWebviewPage.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void nativeTakePhoto() {
                Log.d(TAG,"======nativeTakePhoto");
                Intent it = new Intent();
                if (ContextCompat.checkSelfPermission(WebViewTagFunctionActivity.this,
                        android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                    //动态申请权限
                    Log.d(TAG,"=====requestPermissions");
                    ActivityCompat.requestPermissions(WebViewTagFunctionActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    if (mGotCameraPermission) {
                        it = CameraFunction.takePhoto(WebViewTagFunctionActivity.this);
                    }
                } else {
                    it = CameraFunction.takePhoto(WebViewTagFunctionActivity.this);
                }

                startActivityForResult(it, TAKE_PHOTO_REQUEST);
            }

            @JavascriptInterface
            public void nativeRecordVideo() {
                Log.d(TAG,"======nativeRecordVideo");
                Intent it = new Intent();
                if (ContextCompat.checkSelfPermission(WebViewTagFunctionActivity.this,
                        android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                    //动态申请权限
                    Log.d(TAG,"=====requestPermissions");
                    ActivityCompat.requestPermissions(WebViewTagFunctionActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    if (mGotCameraPermission) {
                        it = CameraFunction.recordVideo(WebViewTagFunctionActivity.this);
                    }
                } else {
                    it = CameraFunction.recordVideo(WebViewTagFunctionActivity.this);
                }
                startActivityForResult(it, RECORD_VIDEO_REQUEST);
            }

            @JavascriptInterface
            public void scanQRCode() {
                if (ContextCompat.checkSelfPermission(WebViewTagFunctionActivity.this,
                        android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                    //动态申请权限
                    Log.d(TAG,"=====requestPermissions");
                    ActivityCompat.requestPermissions(WebViewTagFunctionActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    if (mGotCameraPermission) {
                        Intent it = new Intent();
                        it.setClass(WebViewTagFunctionActivity.this, QRCodeScanActivity.class);
                        startActivityForResult(it, SCAN_QRCODE_REQUEST);
                    }
                } else {
                    //已经获得权限，直接开启扫码
                    Log.d(TAG,"=====already has permission");
                    Intent it = new Intent();
                    it.setClass(WebViewTagFunctionActivity.this, QRCodeScanActivity.class);
                    startActivityForResult(it, SCAN_QRCODE_REQUEST);
                }
            }

            @JavascriptInterface
            public void livePlay() {
                LivePlayActivity.intentTo(WebViewTagFunctionActivity.this, liveUrl, "VideoTitle");
            }

            @JavascriptInterface
            public void MediaPlayVideo() {
                Intent it = new Intent();
                it.setClass(WebViewTagFunctionActivity.this, VideoPlayActivity.class);
                it.putExtra("videoUrl", videoUrl);
                startActivity(it);
            }

            @JavascriptInterface
            public void IjkPlayVideo() {
                IjkVideoPlayActivity.intentTo(WebViewTagFunctionActivity.this, videoUrl, "VideoTitle");
            }

            @JavascriptInterface
            public void IjkPlayLocalVideo() {
                IjkVideoPlayActivity.intentTo(WebViewTagFunctionActivity.this, localVideoUrl, "LocalVideoTitle");
            }

            @JavascriptInterface
            public void CleanWebCache() {
                HzNetUtil.clearCacheFile(WebViewTagFunctionActivity.this);
            }

            @JavascriptInterface
            public void SendNotification() {
                NotificationUtils.getInstance(WebViewTagFunctionActivity.this).setNotificationTitle("test_title");
                NotificationUtils.getInstance(WebViewTagFunctionActivity.this).setNotificationContent("test_content");
                NotificationUtils.getInstance(WebViewTagFunctionActivity.this).sendNotification(null, 1001);
                //自定义通知
                //NotificationUtils.getInstance(WebViewTagFunctionActivity.this).sendCustomNotification();
            }


        }, "functionTag");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"=====onDestroy");

        // TODO: 清理缓存, 删除相关缓存目录下的文件
        HzNetUtil.clearCacheFile(this);

        // TODO: 调用常规接口清理缓存
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        //webview 自身接口清理记录
        mWebviewPage.clearCache(true);
        mWebviewPage.clearFormData();
        mWebviewPage.clearHistory();

        //其实还是无法回收内存
        mWebviewPage.removeAllViews();
        mWebviewPage.destroy();

        //自杀退出
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    /**
     * 处理返回键
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //返回键处理
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebviewPage.canGoBack()) {
            mWebviewPage.goBack();
            if (HzNetApp.HzNet_DEBUG) {
                Log.d(TAG,"onKeyDown mWebviewPage can go back");
            }
            // TODO: 需要判断最后的网页到底是什么,会出现无法返回退出的情况。
            return true;
        } else {
            Log.d(TAG,"onKeyDown mWebviewPage can not go back, maybe not back key");
        }

        return super.onKeyDown(keyCode, event);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG,"=====onActivityResult requestCode="+requestCode
                +" resultCode="+resultCode + " intent="+intent);
        if (null != intent) {
            //扫码结果
            String scanResult = intent.getStringExtra("result");
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case SCAN_QRCODE_REQUEST:
                        //返回给JS
                        mWebviewPage.loadUrl("javascript:funFromjs('" + scanResult + "')");
                        break;
                    case TAKE_PHOTO_REQUEST:
                        Log.d(TAG,"=======onActivityResult TAKE_PHOTO_REQUEST intent="+intent.toString());
                        break;
                    case RECORD_VIDEO_REQUEST:
                        Log.d(TAG,"=======onActivityResult RECORD_VIDEO_REQUEST intent="+intent.toString());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults );
        Log.d(TAG,"=====onRequestPermissionsResult requestCode="+requestCode);
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (null != grantResults && grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                        Intent it = new Intent();
//                        it.setClass(WebViewTagFunctionActivity.this, QRCodeScanActivity.class);
//                        startActivityForResult(it, SCAN_QRCODE_REQUEST);
                        Log.d(TAG,"=====onRequestPermissionsResult permission get succeed");
                        mGotCameraPermission = true;
                    } else {
                        Toast.makeText(WebViewTagFunctionActivity.this, "请打开相机权限",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG,"=====onRequestPermissionsResult no grant result");
                    Toast.makeText(WebViewTagFunctionActivity.this, "请打开相机权限",
                            Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }


    //Android7.0获取照相机权限
    private void getPermission(){
        Log.d(TAG,"======getPermission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(WebViewTagFunctionActivity.this, "您已经拒绝过一次", Toast.LENGTH_LONG).show();
            }
            Log.d(TAG,"======getPermission requestPermissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSIONS_REQUEST_CODE);
        }
    }
}
