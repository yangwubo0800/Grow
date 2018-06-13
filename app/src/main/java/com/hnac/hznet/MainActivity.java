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
        setContentView(R.layout.activity_main);

        Button webviewPage = findViewById(R.id.webview_page_load);
        webviewPage.setOnClickListener(myListener);

    }


    View.OnClickListener myListener =  new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent it = new Intent();
            switch (view.getId()) {
                case R.id.webview_page_load:
                    it.setClass(MainActivity.this, WebViewPageActivity.class);
                    break;
                default:
                    break;
            }
            startActivity(it);
        }
    };
}
