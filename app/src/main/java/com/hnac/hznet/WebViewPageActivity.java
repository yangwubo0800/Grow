package com.hnac.hznet;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.hnac.utils.HzNetUtil;

public class WebViewPageActivity extends AppCompatActivity {

    private String  TAG = this.getClass().getName();
    private WebView mWebviewPage;
    private String hzNetMobile = "http://192.168.65.100:8000/mobile/";
    private String localFile = "file:///android_asset/main.html";
    private String hzNet = "http://175.6.40.67:18894/hznet/index";
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_page);

        mWebviewPage = findViewById(R.id.webview_page);
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

        mWebviewPage.loadUrl(hzNet);
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
}
