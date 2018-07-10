package com.hnac.hznet;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
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

public class WebViewActivity extends AppCompatActivity {

    private String  TAG = "WebViewActivity";
    private WebView mWebviewPage;
    private View mErrorView;
    private Context mContext;
    private ProgressBar mProgressBar;
    private final int TAKE_PHOTO_REQUEST = 2;
    private final int RECORD_VIDEO_REQUEST = 3;
    private final int SCAN_QRCODE_REQUEST = 4;
    private final int PERMISSION_REQUEST_CAMERA_FOR_PHOTO = 5;
    private final int PERMISSION_REQUEST_CAMERA_FOR_VIDEO = 6;
    private final int PERMISSION_REQUEST_CAMERA_FOR_SCAN = 7;

    //本地调试H5
    private String localFile = "file:///android_asset/main.html";
    //http, 萤石云开发测试地址，直播流
    private String liveUrl = "http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.hd.m3u8";
    //https，录制视频播放
    private String videoUrl = "http://175.6.40.67:18894/hznet/app/VID20170430200439.mp4";
    private String debug = "http://www.baidu.com";
    private String netVideoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        setContentView(R.layout.activity_web_view);

        mWebviewPage = findViewById(R.id.webview_function_page);
        mProgressBar = findViewById(R.id.progressBar);

        WebSettings webSettings = mWebviewPage.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //设置缓存模式，无网络时依然可以打开已经打开过的网页
        if (HzNetUtil.isNetworkAvailable(this)) {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);

        mWebviewPage.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (HzNetApp.HzNet_DEBUG) {
                    Log.d(TAG," shouldOverrideUrlLoading url=" + url);
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
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

        mWebviewPage.loadUrl(localFile);

        //注册JS调用的natvie接口
        mWebviewPage.addJavascriptInterface(new Object() {

            /**
             * 功能：供JS使用，提供拍照功能，生成照片路径无法直接返回，需要等相机界面返回
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void nativeTakePhoto() {
                if (HzNetApp.HzNet_DEBUG) {
                    Log.d(TAG,"nativeTakePhoto");
                }
                if (HzNetUtil.selfPermissionGranted(mContext, android.Manifest.permission.CAMERA)) {
                    Log.d(TAG,"nativeTakePhoto already has camera permission");
                    Intent it = CameraFunction.takePhoto(mContext);
                    startActivityForResult(it, TAKE_PHOTO_REQUEST);
                } else {
                    //动态申请权限
                    Log.d(TAG,"nativeTakePhoto request CAMERA Permissions");
                    ActivityCompat.requestPermissions(WebViewActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA_FOR_PHOTO);
                }
            }

            /**
             * 功能：供JS使用，提供录像功能，生成录像路径无法直接返回，需要等相机界面返回
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void nativeRecordVideo() {
                if (HzNetApp.HzNet_DEBUG) {
                    Log.d(TAG,"nativeRecordVideo");
                }
                if (HzNetUtil.selfPermissionGranted(mContext, android.Manifest.permission.CAMERA)) {
                    Log.d(TAG,"nativeRecordVideo already has camera permission");
                    Intent it = CameraFunction.recordVideo(mContext);
                    startActivityForResult(it, RECORD_VIDEO_REQUEST);
                } else {
                    //动态申请权限
                    Log.d(TAG,"nativeRecordVideo request CAMERA Permissions");
                    ActivityCompat.requestPermissions(WebViewActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA_FOR_VIDEO);
                }
            }

            /**
             * 功能：供JS使用，提供扫码功能，生成扫码信息无法直接返回，需要等相机界面返回
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void scanQRCode() {
                if (HzNetApp.HzNet_DEBUG) {
                    Log.d(TAG,"scanQRCode");
                }
                if (HzNetUtil.selfPermissionGranted(mContext, android.Manifest.permission.CAMERA)) {
                    Log.d(TAG,"scanQRCode already has camera permission");
                    Intent it = new Intent();
                    it.setClass(mContext, com.hnac.zxing.CaptureActivity.class);
                    startActivityForResult(it, SCAN_QRCODE_REQUEST);
                } else {
                    //动态申请权限
                    Log.d(TAG,"scanQRCode request CAMERA Permissions");
                    ActivityCompat.requestPermissions(WebViewActivity.this,
                            new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA_FOR_SCAN);
                }
            }

            @JavascriptInterface
            public void livePlay() {
                //LivePlayActivity.intentTo(mContext, liveUrl, "VideoTitle");
                com.hnac.ijkplayer.ui.LivePlayActivity.intentTo(mContext, liveUrl, "VideoTitle");
            }

            /**
             * 由JS传入直播地址和标题
             * @param liveUrl
             * @param liveTitle
             */
            @JavascriptInterface
            public void livePlay(String liveUrl, String liveTitle) {
                com.hnac.ijkplayer.ui.LivePlayActivity.intentTo(mContext, liveUrl, liveTitle);
            }


            /**
             * 功能：拨号
             * @param number
             */
            @JavascriptInterface
            public void CallNumber(String number) {
                HzNetUtil.callNumber(WebViewActivity.this, number);
            }


            @JavascriptInterface
            public void IjkPlayVideo() {
                com.hnac.ijkplayer.ui.IjkVideoPlayActivity.intentTo(mContext, videoUrl, "VideoTitle");
            }

            /**
             * 由JS 传入视频地址和标题
             * @param videoUrl
             * @param videoTitle
             */
            @JavascriptInterface
            public void IjkPlayVideo(String videoUrl, String videoTitle) {
                com.hnac.ijkplayer.ui.IjkVideoPlayActivity.intentTo(mContext, videoUrl, videoTitle);
            }


            @JavascriptInterface
            public void IjkPlayLocalVideo() {
                com.hnac.ijkplayer.ui.IjkVideoPlayActivity.intentTo(mContext, netVideoUrl, "LocalVideoTitle");
            }

            /**
             * 功能：提供清理缓存功能
             * 参数：无
             * 返回值：无
             */
            @JavascriptInterface
            public void CleanWebCache() {
                HzNetUtil.clearCacheFile(mContext);
            }

            @JavascriptInterface
            public void SendNotification() {
                NotificationUtils.getInstance(mContext).setNotificationTitle("test_title");
                NotificationUtils.getInstance(mContext).setNotificationContent("test_content");
                NotificationUtils.getInstance(mContext).sendNotification(null, 1001);
                //自定义通知
                //NotificationUtils.getInstance(WebViewTagFunctionActivity.this).sendCustomNotification();
            }

            /**
             * 功能：发送通知给状态栏
             * @param title
             * @param content
             */
            @JavascriptInterface
            public void SendNotification(String title, String content) {
                NotificationUtils.getInstance(mContext).setNotificationTitle(title);
                NotificationUtils.getInstance(mContext).setNotificationContent(content);
                NotificationUtils.getInstance(mContext).sendNotification(null, 1001);
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
        Log.d(TAG,"onActivityResult requestCode="+requestCode
                +" resultCode="+resultCode + " intent="+intent);
        String scanResult = null;

        if (null != intent) {
            //扫码结果
            scanResult = intent.getStringExtra(CaptureActivity.KEY_DATA);
        }
        // 处理各种返回值
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SCAN_QRCODE_REQUEST:
                    //返回给JS
                    mWebviewPage.loadUrl("javascript:funFromjs('" + scanResult + "')");
                    ToastUtil.makeText(mContext, "扫码结果：" + scanResult);
                    break;
                case TAKE_PHOTO_REQUEST:
                    Log.d(TAG,"onActivityResult TAKE_PHOTO_REQUEST ");
                    if (TextUtils.isEmpty(CameraFunction.fileFullName)) {
                        ToastUtil.makeText(mContext, "拍照失败了");
                    } else {
                        ToastUtil.makeText(mContext, "照片生成路径：" + CameraFunction.fileFullName);
                    }
                    break;
                case RECORD_VIDEO_REQUEST:
                    Log.d(TAG,"onActivityResult RECORD_VIDEO_REQUEST");
                    if (TextUtils.isEmpty(CameraFunction.fileFullName)) {
                        ToastUtil.makeText(mContext, "录像失败了");
                    } else {
                        ToastUtil.makeText(mContext, "录像生成路径：" + CameraFunction.fileFullName);
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
        Log.d(TAG,"onRequestPermissionsResult requestCode="+requestCode);
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA_FOR_PHOTO:
                if (null != grantResults && grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        Intent it = CameraFunction.takePhoto(mContext);
                        startActivityForResult(it, TAKE_PHOTO_REQUEST);
                        Log.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_PHOTO permission get succeed");
                    } else {
                        ToastUtil.makeText(mContext, "请打开相机权限");
                    }
                } else {
                    Log.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_PHOTO no grant result");
                    ToastUtil.makeText(mContext, "请打开相机权限");
                }
                break;

            case PERMISSION_REQUEST_CAMERA_FOR_VIDEO:
                if (null != grantResults && grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        Intent it = CameraFunction.recordVideo(mContext);
                        startActivityForResult(it, RECORD_VIDEO_REQUEST);
                        Log.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_VIDEO permission get succeed");
                    } else {
                        ToastUtil.makeText(mContext, "请打开相机权限");
                    }
                } else {
                    Log.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_VIDEO no grant result");
                    ToastUtil.makeText(mContext, "请打开相机权限");
                }
                break;
            case PERMISSION_REQUEST_CAMERA_FOR_SCAN:
                if (null != grantResults && grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        Intent it = new Intent();
                        it.setClass(mContext, com.hnac.zxing.CaptureActivity.class);
                        startActivityForResult(it, SCAN_QRCODE_REQUEST);
                        Log.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_SCAN permission get succeed");
                    } else {
                        ToastUtil.makeText(mContext, "请打开相机权限");
                    }
                } else {
                    Log.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_SCAN no grant result");
                    ToastUtil.makeText(mContext, "请打开相机权限");
                }
                break;
            default:
                break;
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
