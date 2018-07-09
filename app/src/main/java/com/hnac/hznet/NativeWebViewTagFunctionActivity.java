package com.hnac.hznet;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hnac.camera.CameraFunction;
import com.hnac.utils.HzNetUtil;
import com.hnac.utils.NotificationUtils;
import com.hnac.utils.ToastUtil;
import com.hnac.zxing.CaptureActivity;

public class NativeWebViewTagFunctionActivity extends AppCompatActivity {

    private String  TAG = "NativeWebViewTagFunctionActivity";
    private WebView mWebviewPage;
    private View mErrorView;
    private String localFile = "file:///android_asset/main.html";
    private ProgressBar mProgressBar;
    private final int SCAN_QRCODE_REQUEST = 2;
    private final int TAKE_PHOTO_REQUEST = 3;
    private final int RECORD_VIDEO_REQUEST = 4;
    private final int PERMISSION_REQUEST_CAMERA = 5;
    private final int CAMERA_PERMISSIONS_REQUEST_CODE = 6;


    //http, 萤石云开发测试地址，直播流
    private String liveUrl = "http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.hd.m3u8";
    //RTMP 格式安卓自带mediaPlayer 不支持，直播流
    private String liveUrl2 = "rtmp://rtmp.open.ys7.com/openlive/0a2cff841ba243809a9a8611e29edc9b.hd";
    //https，录制视频播放
    //private String videoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4";
    private String videoUrl = "http://175.6.40.67:18894/hznet/app/VID20170430200439.mp4";

    //手机本地视频
    private String localVideoUrl = "/sdcard/webview_video/VID_20180625_154855.mp4";
    private boolean mGotCameraPermission = false;
    private String debug = "http://www.baidu.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPermission();

        setContentView(R.layout.activity_native_web_view_tag_function);

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

            // 旧版本，会在新版本中也可能被调用，所以加上一个判断，防止重复显示
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    return;
                }
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG," onReceivedError description=" + description);
                // 在这里显示自定义错误页
                showErrorPage();//显示错误页面
            }


            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (null != error) {
                    CharSequence errDes = error.getDescription();
                    Log.e(TAG," onReceivedError errDes=" + errDes);
                }
                showErrorPage();//显示错误页面
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.e(TAG," onReceivedHttpError ");
                showErrorPage();//显示错误页面
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

        mWebviewPage.loadUrl(debug);

        //注册JS调用的natvie接口
        mWebviewPage.addJavascriptInterface(new Object() {

            /**
             * 功能：供JS使用，提供拍照功能，生成照片路径无法直接返回，需要等相机界面返回
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void nativeTakePhoto() {
                Log.d(TAG,"======nativeTakePhoto");
                Intent it = new Intent();
                if (ContextCompat.checkSelfPermission(NativeWebViewTagFunctionActivity.this,
                        android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                    //动态申请权限
                    Log.d(TAG,"=====requestPermissions");
                    ActivityCompat.requestPermissions(NativeWebViewTagFunctionActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    if (mGotCameraPermission) {
                        it = CameraFunction.takePhoto(NativeWebViewTagFunctionActivity.this);
                    }
                } else {
                    it = CameraFunction.takePhoto(NativeWebViewTagFunctionActivity.this);
                }

                startActivityForResult(it, TAKE_PHOTO_REQUEST);
            }

            /**
             * 功能：供JS使用，提供录像功能，生成录像路径无法直接返回，需要等相机界面返回
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void nativeRecordVideo() {
                Log.d(TAG,"======nativeRecordVideo");
                Intent it = new Intent();
                if (ContextCompat.checkSelfPermission(NativeWebViewTagFunctionActivity.this,
                        android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                    //动态申请权限
                    Log.d(TAG,"=====requestPermissions");
                    ActivityCompat.requestPermissions(NativeWebViewTagFunctionActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    if (mGotCameraPermission) {
                        it = CameraFunction.recordVideo(NativeWebViewTagFunctionActivity.this);
                    }
                } else {
                    it = CameraFunction.recordVideo(NativeWebViewTagFunctionActivity.this);
                }
                startActivityForResult(it, RECORD_VIDEO_REQUEST);
            }

            /**
             * 功能：供JS使用，提供扫码功能，生成扫码信息无法直接返回，需要等相机界面返回
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void scanQRCode() {
                if (ContextCompat.checkSelfPermission(NativeWebViewTagFunctionActivity.this,
                        android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                    //动态申请权限
                    Log.d(TAG,"=====requestPermissions");
                    ActivityCompat.requestPermissions(NativeWebViewTagFunctionActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    if (mGotCameraPermission) {
                        Intent it = new Intent();
                        it.setClass(NativeWebViewTagFunctionActivity.this, com.hnac.zxing.CaptureActivity.class);
                        startActivityForResult(it, SCAN_QRCODE_REQUEST);
                    }
                } else {
                    //已经获得权限，直接开启扫码
                    Log.d(TAG,"=====already has permission");
                    Intent it = new Intent();
                    it.setClass(NativeWebViewTagFunctionActivity.this, com.hnac.zxing.CaptureActivity.class);
                    startActivityForResult(it, SCAN_QRCODE_REQUEST);
                }
            }

            @JavascriptInterface
            public void livePlay() {
                //LivePlayActivity.intentTo(NativeWebViewTagFunctionActivity.this, liveUrl, "VideoTitle");
                com.hnac.ijkplayer.ui.LivePlayActivity.intentTo(NativeWebViewTagFunctionActivity.this, liveUrl, "VideoTitle");
            }

            /**
             * 由JS传入直播地址和标题
             * @param liveUrl
             * @param liveTitle
             */
            @JavascriptInterface
            public void livePlay(String liveUrl, String liveTitle) {
                com.hnac.ijkplayer.ui.LivePlayActivity.intentTo(NativeWebViewTagFunctionActivity.this, liveUrl, liveTitle);
            }

            @JavascriptInterface
            public void MediaPlayVideo() {
//                Intent it = new Intent();
//                it.setClass(NativeWebViewTagFunctionActivity.this, VideoPlayActivity.class);
//                it.putExtra("videoUrl", videoUrl);
//                startActivity(it);
//                Toast.makeText(NativeWebViewTagFunctionActivity.this, "本地mediaPlayer弃用",
//                        Toast.LENGTH_SHORT).show();
                ToastUtil.makeText(NativeWebViewTagFunctionActivity.this, "本地mediaPlayer已经弃用");
            }

            @JavascriptInterface
            public void IjkPlayVideo() {
                com.hnac.ijkplayer.ui.IjkVideoPlayActivity.intentTo(NativeWebViewTagFunctionActivity.this, videoUrl, "VideoTitle");
            }

            /**
             * 由JS 传入视频地址和标题
             * @param videoUrl
             * @param videoTitle
             */
            @JavascriptInterface
            public void IjkPlayVideo(String videoUrl, String videoTitle) {
                com.hnac.ijkplayer.ui.IjkVideoPlayActivity.intentTo(NativeWebViewTagFunctionActivity.this, videoUrl, videoTitle);
            }


            @JavascriptInterface
            public void IjkPlayLocalVideo() {
                com.hnac.ijkplayer.ui.IjkVideoPlayActivity.intentTo(NativeWebViewTagFunctionActivity.this, localVideoUrl, "LocalVideoTitle");
            }

            /**
             * 功能：提供清理缓存功能
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void CleanWebCache() {
                HzNetUtil.clearCacheFile(NativeWebViewTagFunctionActivity.this);
            }

            @JavascriptInterface
            public void SendNotification() {
                NotificationUtils.getInstance(NativeWebViewTagFunctionActivity.this).setNotificationTitle("test_title");
                NotificationUtils.getInstance(NativeWebViewTagFunctionActivity.this).setNotificationContent("test_content");
                NotificationUtils.getInstance(NativeWebViewTagFunctionActivity.this).sendNotification(null, 1001);
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


    /**
     * 处理启动本地其他activity功能返回值处理
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG,"=====onActivityResult requestCode="+requestCode
        +" resultCode="+resultCode + " intent="+intent);
        String scanResult = null;
        if (null != intent) {
            //扫码结果
            scanResult = intent.getStringExtra(CaptureActivity.KEY_DATA);
        }
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SCAN_QRCODE_REQUEST:
                    //返回给JS
                    mWebviewPage.loadUrl("javascript:funFromjs('" + scanResult + "')");
                    ToastUtil.makeText(NativeWebViewTagFunctionActivity.this, "扫码结果：" + scanResult);
                    break;
                case TAKE_PHOTO_REQUEST:
                    Log.d(TAG,"=======onActivityResult TAKE_PHOTO_REQUEST ");
                    if (TextUtils.isEmpty(CameraFunction.fileFullName)) {
                        ToastUtil.makeText(NativeWebViewTagFunctionActivity.this, "拍照失败了");
                    } else {
                        ToastUtil.makeText(NativeWebViewTagFunctionActivity.this, "照片生成路径：" + CameraFunction.fileFullName);
                    }
                    break;
                case RECORD_VIDEO_REQUEST:
                    Log.d(TAG,"=======onActivityResult RECORD_VIDEO_REQUEST");
                    if (TextUtils.isEmpty(CameraFunction.fileFullName)) {
                        ToastUtil.makeText(NativeWebViewTagFunctionActivity.this, "录像失败了");
                    } else {
                        ToastUtil.makeText(NativeWebViewTagFunctionActivity.this, "录像生成路径：" + CameraFunction.fileFullName);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 权限请求结果处理
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults );
        Log.d(TAG,"=====onRequestPermissionsResult requestCode="+requestCode);
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (null != grantResults && grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                        Intent it = new Intent();
//                        it.setClass(NativeWebViewTagFunctionActivity.this, QRCodeScanActivity.class);
//                        startActivityForResult(it, SCAN_QRCODE_REQUEST);
                        Log.d(TAG,"=====onRequestPermissionsResult permission get succeed");
                        mGotCameraPermission = true;
                    } else {
                        Toast.makeText(NativeWebViewTagFunctionActivity.this, "请打开相机权限",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG,"=====onRequestPermissionsResult no grant result");
                    Toast.makeText(NativeWebViewTagFunctionActivity.this, "请打开相机权限",
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
                Toast.makeText(NativeWebViewTagFunctionActivity.this, "您已经拒绝过一次", Toast.LENGTH_LONG).show();
            }
            Log.d(TAG,"======getPermission requestPermissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSIONS_REQUEST_CODE);
        }
    }


    protected void showErrorPage() {
        LinearLayout webParentView = (LinearLayout)mWebviewPage.getParent();
        initErrorPage();//初始化自定义页面
        while (webParentView.getChildCount() > 1) {
            webParentView.removeViewAt(0);
        }
        @SuppressWarnings("deprecation")
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewPager.LayoutParams.FILL_PARENT, ViewPager.LayoutParams.FILL_PARENT);
        webParentView.addView(mErrorView, 0, lp);
    }
    /****
     * 把系统自身请求失败时的网页隐藏
     */
    protected void hideErrorPage() {
        LinearLayout webParentView = (LinearLayout)mWebviewPage.getParent();
        while (webParentView.getChildCount() > 1) {
            webParentView.removeViewAt(0);
        }
    }
    /***
     * 显示加载失败时自定义的网页
     */
    protected void initErrorPage() {
        if (mErrorView == null) {
            mErrorView = View.inflate(this, R.layout.activity_error, null);
            RelativeLayout layout = (RelativeLayout)mErrorView.findViewById(R.id.online_error_btn_retry);
            // TODO: 众多页面在一个webview里面跑，需要管理 url 才知道加载哪个，所以不做重新加载。
//            layout.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
////                    hideErrorPage();
////                    mWebviewPage.loadUrl("about:blank");
//                    hideErrorPage();
//                    mWebviewPage.loadUrl(localFile);
//
//                }
//            });
            mErrorView.setOnClickListener(null);
        }
    }

}
