package com.grow.ijkplayer.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.grow.ijkplayer.IjkVideoView;
import com.grow.ijkplayer.R;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class LivePlayActivity extends AppCompatActivity {

    private static final String TAG = "LivePlayActivity";
    private String mVideoPath;
    private Uri mVideoUri;
    private IjkVideoView mVideoView;
    private LinearLayout mLivePlayLayouProgress;
    private SurfaceView mVideoInfo;
    private boolean mIsVideoInfoVisble = true;
    private LinearLayout mTitleInfo;


    /**
     * 提供给外部接口调用启动直播界面，传入直播源
     * @param context
     * @param videoPath
     * @param videoTitle
     */
    public static void intentTo(Context context, String videoPath, String videoTitle) {
        context.startActivity(newIntent(context, videoPath, videoTitle));
    }

    public static Intent newIntent(Context context, String videoPath, String videoTitle) {
        Intent intent = new Intent(context, LivePlayActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoTitle", videoTitle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"=====com.grow.ijkplayer.ui onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ijkplayer_hnac_activity_live_play);
        mLivePlayLayouProgress = findViewById(R.id.liveplay_layoutprogress);
        mVideoInfo = (SurfaceView)findViewById(R.id.videoinfo_surface);
        mTitleInfo = findViewById(R.id.ll_title);

        mVideoInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mIsVideoInfoVisble) {
                    mTitleInfo.setVisibility(View.GONE);
                    mIsVideoInfoVisble = false;
                } else {
                    mTitleInfo.setVisibility(View.VISIBLE);
                    mIsVideoInfoVisble = true;
                }
                return false;
            }
        });

        LinearLayout btClose = findViewById(R.id.liveplay_close);
        btClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        String videoTitle =  getIntent().getStringExtra("videoTitle");
        TextView title = findViewById(R.id.tv_title);
        title.setText(videoTitle);

        // handle arguments
        mVideoPath = getIntent().getStringExtra("videoPath");

        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (!TextUtils.isEmpty(intentAction)) {
            if (intentAction.equals(Intent.ACTION_VIEW)) {
                mVideoPath = intent.getDataString();
            } else if (intentAction.equals(Intent.ACTION_SEND)) {
                mVideoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    String scheme = mVideoUri.getScheme();
                    if (TextUtils.isEmpty(scheme)) {
                        Log.e(TAG, "Null unknown scheme\n");
                        finish();
                        return;
                    }
                    if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
                        mVideoPath = mVideoUri.getPath();
                    } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                        Log.e(TAG, "Can not resolve content below Android-ICS\n");
                        finish();
                        return;
                    } else {
                        Log.e(TAG, "Unknown scheme " + scheme + "\n");
                        finish();
                        return;
                    }
                }
            }
        }

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        // prefer mVideoPath
        if (mVideoPath != null)
            mVideoView.setVideoPath(mVideoPath);
        else if (mVideoUri != null)
            mVideoView.setVideoURI(mVideoUri);
        else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }
        playVideoConfig();

    }

    /**
     * 由于onstop会暂停，所以此处需要start player.
     */
    @Override
    protected void onStart() {
        super.onStart();
        mVideoView.start();
        Log.d(TAG,"=====onStart start play video");
    }

    /**
     * 当界面退出到后台后，此处会停止播放，如果没有这只后台播放；
     * 考虑减少用户流量损失
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "=====onStop isBackgroundPlayEnabled=" + mVideoView.isBackgroundPlayEnabled());
        //没有设置后台播放，直接停掉
        if (!mVideoView.isBackgroundPlayEnabled()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();
        } else {
            mVideoView.enterBackground();
        }
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=====onDestroy");
    }


    private void playVideoConfig() {
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener(){
            @Override
            public void onPrepared(IMediaPlayer mp) {
                Log.d(TAG,"=====playVideo onPrepared");
                mLivePlayLayouProgress.setVisibility(View.GONE);
            }
        });

        // TODO: 增加错误处理
    }

}
