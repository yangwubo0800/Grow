package com.hnac.hznet;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //解决第一次安卓后，从系统安装中直接打开APP,进入应用后，按HOME键退出到后台，重新再次从桌面进入，应用重启问题
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        Button webviewPage = findViewById(R.id.webview_page_load);
        webviewPage.setOnClickListener(myListener);

        Button webviewFunctionPage = findViewById(R.id.webview_tag_function);
        webviewFunctionPage.setOnClickListener(myListener);

    }


    View.OnClickListener myListener =  new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent it = new Intent();
            switch (view.getId()) {
                case R.id.webview_page_load:
                    it.setClass(MainActivity.this, WebViewPageActivity.class);
                    break;
                case R.id.webview_tag_function:
                    it.setClass(MainActivity.this, WebViewTagFunctionActivity.class);
                    break;
                default:
                    break;
            }
            startActivity(it);
        }
    };
}
