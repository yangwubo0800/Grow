package com.hnac.hznet;

import android.content.Intent;
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

import com.hnac.camera.CameraFunction;
import com.hnac.camera.QRCodeScanActivity;
import com.hnac.utils.HzNetUtil;

public class WebViewTagFunctionActivity extends AppCompatActivity {

    private String  TAG = this.getClass().getName();
    private WebView mWebviewPage;
    private String localFile = "file:///android_asset/main.html";
    private String hzNetDebug = "http://175.6.40.67:18894/hznet/app/main.html";
    private ProgressBar mProgressBar;
    private final int SCAN_QRCODE_REQUEST = 2;
    //http, 萤石云开发测试地址，直播流
    private String liveUrl = "http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.hd.m3u8";
    //RTMP 格式安卓自带mediaPlayer 不支持，直播流
    private String liveUrl2 = "rtmp://rtmp.open.ys7.com/openlive/0a2cff841ba243809a9a8611e29edc9b.hd";
    //https，录制视频播放
    private String videoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                CameraFunction.takePhoto(WebViewTagFunctionActivity.this);
            }

            @JavascriptInterface
            public void nativeRecordVideo() {
                CameraFunction.recordVideo(WebViewTagFunctionActivity.this);
            }

            @JavascriptInterface
            public void scanQRCode() {
                Intent it = new Intent();
                it.setClass(WebViewTagFunctionActivity.this, QRCodeScanActivity.class);
                startActivityForResult(it, SCAN_QRCODE_REQUEST);

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
            public void CleanWebCache() {
                HzNetUtil.clearCacheFile(WebViewTagFunctionActivity.this);
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
        if (null != intent) {
            //扫码结果
            String scanResult = intent.getStringExtra("result");
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case SCAN_QRCODE_REQUEST:
                        //返回给JS
                        mWebviewPage.loadUrl("javascript:funFromjs('" + scanResult + "')");
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
