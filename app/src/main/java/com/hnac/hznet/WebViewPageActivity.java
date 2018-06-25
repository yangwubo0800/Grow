package com.hnac.hznet;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.hnac.utils.HzNetUtil;

public class WebViewPageActivity extends AppCompatActivity {

    private String  TAG = this.getClass().getName();
    private WebView mWebviewPage;
    private String hzNetMobile = "http://192.168.65.100:8000/mobile/";
    private String localFile = "file:///android_asset/main.html";
    private String hzNet = "http://175.6.40.67:18894/hznet/index";
    private String hzInfo = "http://175.6.40.68/index";
    private ProgressBar mProgressBar;
    private LinearLayout mLoadFailTip;
    private boolean mIsLoadSuccess = true;
    private String baiduUrl = "https://www.baidu.com/";
    private String mRecordUrl;
    private long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"=====onCreate");
        setContentView(R.layout.activity_web_view_page);

        mWebviewPage = findViewById(R.id.webview_page);
        mProgressBar = findViewById(R.id.progressBar);
        mLoadFailTip = findViewById(R.id.load_fail_tip);

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
                // TODO: 对于非正常的url，不用webview打开
                try{
                    if(!url.startsWith("http://") && !url.startsWith("https://") ){
                        Log.d(TAG,"===== intent start activity");
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    return false;
                }

                //record the  final back url
                mRecordUrl = url;

                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String title = view.getTitle();
                Log.d(TAG,"=====onPageFinished title=" + title);
                // TODO: 是否会有反复加载的情况， 目前只会load一次，返回后相当于重新创建加载
                if (mIsLoadSuccess) {
                    mLoadFailTip.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(TAG,"=====onReceivedError errorCode=" + error.getErrorCode()
                    +" Description=" + error.getDescription());
                }
                //errorCode=-2 Description=net::ERR_INTERNET_DISCONNECTED 网络没连接
                //errorCode=-6 Description=net::ERR_CONNECTION_REFUSED    服务器文件不存在
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (null != error) {
                        int errCode = error.getErrorCode();
                        if ((errCode == -2) || (errCode == -6)) {
                            mIsLoadSuccess = false;
                            Toast.makeText(WebViewPageActivity.this, "加载失败", Toast.LENGTH_LONG).show();
                            view.setVisibility(View.GONE);
                            mLoadFailTip.setVisibility(View.VISIBLE);
                        }
                    }
                }
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
        Log.d(TAG,"onKeyDown keyCode=" + keyCode);
        //返回键处理
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebviewPage.canGoBack()) {
            mWebviewPage.goBack();
            if (HzNetApp.HzNet_DEBUG) {
                Log.d(TAG,"onKeyDown mWebviewPage can go back");
            }
            // TODO: 需要判断最后的网页到底是什么,会出现无法返回退出的情况。
            if (!TextUtils.isEmpty(mRecordUrl) && mRecordUrl.equals("http://175.6.40.68/login")) {
                // 提示再次按返回键退出程序
                if(System.currentTimeMillis() - exitTime > 2000) {
                    Toast.makeText(WebViewPageActivity.this, "再按一次退出程序", Toast.LENGTH_LONG).show();
                    exitTime = System.currentTimeMillis();
                } else {
                    finish();
                }
            }
            return true;
        } else {
            Log.d(TAG,"onKeyDown mWebviewPage can not go back, maybe not back key");
        }

        return super.onKeyDown(keyCode, event);
    }
}
