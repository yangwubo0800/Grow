package com.hnac.hznet;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hnac.ijkplayer.IjkVideoView;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkVideoPlayActivity extends AppCompatActivity {

    private static final String TAG = "IjkVideoPlayActivity";
    private String mVideoPath;
    private Uri mVideoUri;
    private IjkVideoView mVideoView;
    //视频加载完成之前的等待进度提示
    private LinearLayout mLoadProcess;
    //点击显示视频标题或者返回等信息
    private SurfaceView mVideoInfo;
    private boolean mIsVideoInfoVisble = true;
    private LinearLayout mTitleInfo;
    //控制面板
    private LinearLayout mControlPanel;
    private ImageView mPlayPauseButton;
    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private TextView mTotalTime;

    public static Intent newIntent(Context context, String videoPath, String videoTitle) {
        Intent intent = new Intent(context, IjkVideoPlayActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoTitle", videoTitle);
        return intent;
    }

    public static void intentTo(Context context, String videoPath, String videoTitle) {
        context.startActivity(newIntent(context, videoPath, videoTitle));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ijk_video_play);
        Log.d(TAG,"=====onCreate");

        LinearLayout backIcon = findViewById(R.id.video_play_back);
        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"=====backIcon finish");
                finish();
            }
        });
        String videoTitle =  getIntent().getStringExtra("videoTitle");
        TextView title = findViewById(R.id.video_title);
        title.setText(videoTitle);

        mTitleInfo = findViewById(R.id.ll_title);
        //控制面板,需要等播放之后才显示出来
        mControlPanel = findViewById(R.id.control_panel);
        //播放按钮
        mPlayPauseButton = findViewById(R.id.play_pause_button);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerProcess();
            }
        });
        //播放进度条
        mSeekBar = findViewById(R.id.video_seek_bar);
        mCurrentTime = findViewById(R.id.tv_currentTime);
        mTotalTime = findViewById(R.id.tv_allTime);

        //设置各个控件的动作
        mVideoInfo = findViewById(R.id.video_info_surface);
        mVideoInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mIsVideoInfoVisble) {
                    mTitleInfo.setVisibility(View.GONE);
                    mControlPanel.setVisibility(View.VISIBLE);
                    mIsVideoInfoVisble = false;
                } else {
                    mTitleInfo.setVisibility(View.VISIBLE);
                    mControlPanel.setVisibility(View.VISIBLE);
                    mIsVideoInfoVisble = true;
                }
                return false;
            }
        });

        mLoadProcess = findViewById(R.id.video_play_progress_layout);

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
        //将所有控件都找出来初始化之后，再来进行统一配置，放置空指针出现
        playVideoConfig();
    }


    private void playVideoConfig() {
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener(){
            @Override
            public void onPrepared(IMediaPlayer mp) {
                Log.d(TAG,"=====playVideo onPrepared");
                //隐藏加载进度
                mLoadProcess.setVisibility(View.GONE);
                //显示播放控制面板
                mControlPanel.setVisibility(View.VISIBLE);
                //设置播放进度
                mSeekBar.setMax(mVideoView.getDuration());
                mSeekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
                mSeekBar.setEnabled(true);
                mSeekBar.setVisibility(View.VISIBLE);
                mHandler.post(updateThread);
            }
        });

        mVideoView.setOnInfoListener(new IMediaPlayer.OnInfoListener(){
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    mLoadProcess.setVisibility(View.VISIBLE);
                } else if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    // 此接口每次回调完START就回调END,若不加上判断就会出现缓冲图标一闪一闪的卡顿现象
                    if (mVideoView.isPlaying()) {
                        mLoadProcess.setVisibility(View.GONE);
                    }
                }
                return true;
            }
        });

        mVideoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer iMediaPlayer) {
                mHandler.removeCallbacks(updateThread);
                mSeekBar.setProgress(0);
                mSeekBar.setSecondaryProgress(0);
                mSeekBar.setEnabled(false);
                //点击可以重新播放
                mPlayPauseButton.setImageResource(R.drawable.stop);
                mHandler.post(updateThread);
            }
        });
    }

    // 播放处理，只支持播放和暂停
    private void playerProcess() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            mPlayPauseButton.setImageResource(R.drawable.stop);
        } else {
            mVideoView.start();
            mPlayPauseButton.setImageResource(R.drawable.play);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVideoView.start();
        Log.d(TAG,"=====onStart start play video");
    }

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

    // 进度条控制
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            // TODO Auto-generated method stub
            if (progress >= 0) {
                // 如果是用户手动拖动控件，则设置视频跳转。
                if (fromUser) {
                    mVideoView.seekTo(progress);
                }

                int allCount = mVideoView.getDuration();
                int currentCount = mVideoView.getCurrentPosition();

                mCurrentTime.setText(String.format("%02d:%02d",
                        (int) currentCount / 60000,
                        (int) (currentCount / 1000) % 60));
                mTotalTime.setText(String.format("%02d:%02d", allCount / 60000,
                        (allCount / 1000) % 60));

            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }
    }

    // 播放进度条更新进程
    Handler mHandler = new Handler();
    Runnable updateThread = new Runnable() {
        public void run() {
            if (mVideoView != null) {
                mSeekBar.setProgress(mVideoView.getCurrentPosition());
                mHandler.postDelayed(updateThread, 100);
            }
        }
    };

}
