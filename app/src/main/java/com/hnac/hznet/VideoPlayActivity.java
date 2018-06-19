package com.hnac.hznet;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class VideoPlayActivity extends AppCompatActivity {

    private String TAG = "VideoPlayActivity";
    private MediaPlayer player;
    private SurfaceView surface;
    private SurfaceHolder surfaceHolder;
    private SeekBar seekBar;
    private ProgressBar layoutProgress = null;
    private RelativeLayout layoutVideo = null;
    private LinearLayout ll_title = null;
    private ImageView btnplayOrPause = null;
    private ImageView fullscreenImage = null;
    private TextView tv_loading = null; //加载状态
    private boolean isFullScreen = false;
    private boolean isVisble = true;
    private boolean isPlaying =true;
    TextView tv_currentTime = null;
    TextView tv_allTime = null;
    LinearLayout ll_back = null;
    LinearLayout ll_buttom = null;
    TextView tv_title=null;
//    //https
//    private String productInfoVideoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4";
//    //本地视频
//    private String localTest = "file:///sdcard/webview_video/VID_20180524_095820.mp4";
    private String mVideoUrl;
    int  flag = 0;
    private final int UPDATE_PLAY_CONTROL_BAR = 102;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        mVideoUrl = getIntent().getStringExtra("videoUrl");
        Log.d(TAG,"=====onCreate mVideoUrl=" + mVideoUrl);

        //视频上方返回+标题 横条
        ll_title=(LinearLayout)findViewById(R.id.ll_title);
        ll_title.bringToFront();
        ll_back=(LinearLayout)findViewById(R.id.ll_back);
        ll_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        tv_title=(TextView)findViewById(R.id.tv_title);

        //视频播放区域
        layoutVideo = (RelativeLayout) findViewById(R.id.videoinfo_video);
        //视频加载提示语
        tv_loading=(TextView)findViewById(R.id.tv_loading);
        //加载进度
        layoutProgress = (ProgressBar) findViewById(R.id.videoinfo_layout_progress);
        //视频播放view
        surface = (SurfaceView) findViewById(R.id.videoinfo_surface);
        //播放，暂停
        btnplayOrPause = (ImageView) findViewById(R.id.videoinfo_playbtn);
        btnplayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playerProcess();
            }
        });

        //底部进度控制条
        ll_buttom=(LinearLayout)findViewById(R.id.ll_buttom);
        seekBar = (SeekBar) findViewById(R.id.videoinfo_seekbar);
        tv_currentTime=(TextView)findViewById(R.id.tv_currentTime);
        tv_allTime=(TextView)findViewById(R.id.tv_allTime);

        //全屏显示
        fullscreenImage = (ImageView) findViewById(R.id.videoinfo_fullscreen);
        fullscreenImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                FullScreen();
            }
        });



        surfaceHolder = surface.getHolder();// SurfaceHolder是SurfaceView的控制接口
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
                playVideo();//播放
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                if(player!=null){
                    Log.d(TAG, "surfaceChanged");
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
                if (player != null)
                    player.release();
                if (holder != null) {
                    handler.removeCallbacks(updateThread);
                }
            }
        });
    }


    // 播放进度条更新进程
    Handler handler = new Handler();
    Runnable updateThread = new Runnable() {
        public void run() {
            if (player != null) {
                seekBar.setProgress(player.getCurrentPosition());
                handler.postDelayed(updateThread, 100);

            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"=====onDestroy");
    }

    Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
//                case 101:
////                    setVideoSize();
//                    break;
                case UPDATE_PLAY_CONTROL_BAR://隔3秒标题，进度条等消失
                    if(isPlaying&&isFullScreen){
                        ll_title.setVisibility(View.GONE);
                        ll_buttom.setVisibility(View.GONE);
                        btnplayOrPause.setVisibility(View.GONE);
                        isVisble=false;
                    }
                    break;
            }

            return false;
        }
    });


    // 播放处理，只支持播放和暂停
    private void playerProcess() {

        if (player.isPlaying()) {
            player.pause();
            isPlaying=false;
            btnplayOrPause.setImageResource(R.drawable.stop);
            if(!isFullScreen){
                btnplayOrPause.setVisibility(View.VISIBLE);
            }

        } else {
            player.start();
            isPlaying=true;
            btnplayOrPause.setImageResource(R.drawable.play);
            if(!isFullScreen){
                btnplayOrPause.setVisibility(View.GONE);
            }else{
                mHandler.sendEmptyMessageDelayed(UPDATE_PLAY_CONTROL_BAR, 3000);//隔3秒标题，进度条等消失
            }
        }
    }


    private void FullScreen() {
        Log.d(TAG,"=====FullScreen isFullScreen=" + isFullScreen);
        if (!isFullScreen) {
            //全屏播放
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isFullScreen = true;
        } else {
            //退出全屏模式
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isFullScreen = false;
        }

        //显示播放按钮，3秒中后进度条消失
        if(player.isPlaying()){
            btnplayOrPause.setVisibility(View.VISIBLE);
            mHandler.sendEmptyMessageDelayed(UPDATE_PLAY_CONTROL_BAR, 3000);//隔3秒标题，进度条等消失
        }
        //setVideoSize();
    }


    private void setVideoSize() {
        if(player==null){
            return;
        }
        try {

            int videoWidth = player.getVideoWidth();
            int videoHeight = player.getVideoHeight();
            Log.d(TAG,"videoWidth" + videoWidth +" videoHeight=" + videoHeight);
            if(videoHeight==videoWidth){
                if(flag%2==0){
                    videoWidth=videoWidth+1;
                }else{
                    videoHeight=videoHeight+1;
                }
                flag++;
            }
            float videoProportion = (float) videoWidth / (float) videoHeight;

            // Get the width of the screen
            int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
            int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
            Log.d(TAG,"screenWidth" + screenWidth +" screenHeight=" + screenHeight);

            float screenProportion = (float) screenWidth / (float) screenHeight;

            // Get the SurfaceView layout parameters
            android.view.ViewGroup.LayoutParams lp = surface.getLayoutParams();
            if (videoProportion > screenProportion) {
                lp.width = screenWidth;
                lp.height = (int) ((float) screenWidth / videoProportion);
            } else {
                lp.width = (int) (videoProportion * (float) screenHeight);
                lp.height = screenHeight;
            }
            // Commit the layout parameters
            surface.setLayoutParams(lp);
        }catch (Exception ex){
            Log.e(TAG,ex.toString());
        }

        // // Get the dimensions of the video
    }


    //播放视频
    public void playVideo(){
        player = new MediaPlayer();
        player.setLooping(true);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setDisplay(surfaceHolder);
        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                // TODO Auto-generated method stub

            }
        });
        player.setOnInfoListener(new MediaPlayer.OnInfoListener() {

            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    layoutProgress.setVisibility(View.VISIBLE);
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    // 此接口每次回调完START就回调END,若不加上判断就会出现缓冲图标一闪一闪的卡顿现象
                    if (mp.isPlaying()) {
                        layoutProgress.setVisibility(View.GONE);
                    }
                }
                return true;
            }
        });
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
//                mHandler.sendEmptyMessageDelayed(101, 0);
                //视频准备好了之后，隐藏加载进度提示
                tv_loading.setVisibility(View.GONE);
                layoutProgress.setVisibility(View.GONE);// 缓冲完成就隐藏
                //设置控制面板进度
                seekBar.setMax(mp.getDuration());
                seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
                handler.post(updateThread);
                seekBar.setEnabled(true);
                seekBar.setVisibility(View.VISIBLE);
                if(!isFullScreen){
                    fullscreenImage.setVisibility(View.VISIBLE);
                }
                //开始播放
                player.start();

            }
        });

        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                tv_loading.setText("视频加载失败 + what=" + what);
//                switch (what) {
//                    case -1004:
//                        LogUtils.e( "MEDIA_ERROR_IO");
//                        break;
//                    case -1007:
//                        LogUtils.e( "MEDIA_ERROR_MALFORMED");
//                        break;
//                    case 200:
//                        LogUtils.e( "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
//                        break;
//                    case 100:
//                        LogUtils.e( "MEDIA_ERROR_SERVER_DIED");
//                        break;
//                    case -110:
//                        LogUtils.e( "MEDIA_ERROR_TIMED_OUT");
//                        break;
//                    case 1:
//                        LogUtils.e( "MEDIA_ERROR_UNKNOWN");
//                        break;
//                    case -1010:
//                        LogUtils.e( "MEDIA_ERROR_UNSUPPORTED");
//                        break;
//                }
//                switch (extra) {
//                    case 800:
//                        LogUtils.e( "MEDIA_INFO_BAD_INTERLEAVING");
//                        break;
//                    case 702:
//                        LogUtils.e( "MEDIA_INFO_BUFFERING_END");
//                        break;
//                    case 701:
//                        LogUtils.e( "MEDIA_INFO_METADATA_UPDATE");
//                        break;
//                    case 802:
//                        LogUtils.e( "MEDIA_INFO_METADATA_UPDATE");
//                        break;
//                    case 801:
//                        LogUtils.e( "MEDIA_INFO_NOT_SEEKABLE");
//                        break;
//                    case 1:
//                        LogUtils.e( "MEDIA_INFO_UNKNOWN");
//                        break;
//                    case 3:
//                        LogUtils.e( "MEDIA_INFO_VIDEO_RENDERING_START");
//                        break;
//                    case 700:
//                        LogUtils.e( "MEDIA_INFO_VIDEO_TRACK_LAGGING");
//                        break;
//                }

                return false;
            }
        });
        surface.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(!isFullScreen){
                    playerProcess();
                }else{
                    if(isVisble){
                        ll_title.setVisibility(View.GONE);
                        ll_buttom.setVisibility(View.GONE);
                        btnplayOrPause.setVisibility(View.GONE);
                        isVisble=false;
                    }else{
                        ll_title.setVisibility(View.VISIBLE);
                        ll_buttom.setVisibility(View.VISIBLE);
                        btnplayOrPause.setVisibility(View.VISIBLE);
                        isVisble=true;
                        mHandler.sendEmptyMessageDelayed(UPDATE_PLAY_CONTROL_BAR, 3000);//隔3秒标题，进度条等消失
                    }
                }

            }
        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                handler.removeCallbacks(updateThread);
                seekBar.setProgress(0);
                seekBar.setSecondaryProgress(0);
                seekBar.setEnabled(false);
            }
        });

        // 设置显示视频显示在SurfaceView上
        try {
            // 设置播放源
            player.setDataSource(mVideoUrl);
            player.prepareAsync();
        } catch (Exception e) {
            tv_loading.setText("视频加载失败");
            Log.e(TAG,e.toString());
        }
    }

    // 进度条控制
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            // TODO Auto-generated method stub
            if (progress >= 0) {
                // 如果是用户手动拖动控件，则设置视频跳转。
                if (fromUser) {
                    player.seekTo(progress);
                }

                int allCount = player.getDuration();
                int currentCount = player.getCurrentPosition();

                tv_currentTime.setText(String.format("%02d:%02d",
                        (int) currentCount / 60000,
                        (int) (currentCount / 1000) % 60));
                tv_allTime.setText(String.format("%02d:%02d", allCount / 60000,
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


}
