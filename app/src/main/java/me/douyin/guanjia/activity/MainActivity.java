package me.douyin.guanjia.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;

import com.example.ijkplayer.player.IjkVideoView;

import java.util.ArrayList;
import java.util.List;

import me.douyin.guanjia.R;
import me.douyin.guanjia.adapter.VideoRecyclerViewAdapter;
import me.douyin.guanjia.bean.VideoBean;
import me.douyin.guanjia.model.Music;
import me.douyin.guanjia.service.AudioPlayer;
import me.douyin.guanjia.service.QuitTimer;
import me.douyin.guanjia.storage.db.DBManager;
import me.douyin.guanjia.storage.db.greendao.MusicDao;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class MainActivity extends AppCompatActivity {

    private  IjkVideoView ijkVideoView;
    private RecyclerView recyclerView;
    private VideoRecyclerViewAdapter videoRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setStatusBarTransparent();
    }

    /**
     * 把状态栏设成透明
     */
    private void setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = MainActivity.this.getWindow().getDecorView();
            decorView.setOnApplyWindowInsetsListener((v, insets) -> {
                WindowInsets defaultInsets = v.onApplyWindowInsets(insets);
                return defaultInsets.replaceSystemWindowInsets(
                        defaultInsets.getSystemWindowInsetLeft(),
                        0,
                        defaultInsets.getSystemWindowInsetRight(),
                        defaultInsets.getSystemWindowInsetBottom());
            });
            ViewCompat.requestApplyInsets(decorView);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
    private void initView() {
        recyclerView = findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
//        recyclerView.setNestedScrollingEnabled(false);
        final PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        List<Music> musicList = DBManager.get().getMusicDao().queryBuilder().orderDesc(MusicDao.Properties.Id).build().list();
        List<VideoBean> videoList = new ArrayList<>();
        for(Music music:musicList){
            if(TextUtils.isEmpty(music.getArtist())){ continue;}
            videoList.add(new VideoBean(music.getTitle(),
                    music.getCoverPath(),
                    music.getArtist()));
        }
         videoRecyclerViewAdapter =   new VideoRecyclerViewAdapter(videoList, this);
        recyclerView.setAdapter(videoRecyclerViewAdapter);
        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                ijkVideoView = view.findViewById(R.id.video_view);
                if (ijkVideoView != null) {
                    ijkVideoView.stopPlayback();
                }
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case SCROLL_STATE_IDLE: //滚动停止
                         ijkVideoView = snapHelper.findSnapView(layoutManager).findViewById(R.id.video_view);
                        if (ijkVideoView != null) {
                            ijkVideoView.setScreenScale(IjkVideoView.SCREEN_SCALE_DEFAULT);
                            ijkVideoView.start();
                        }
                        break;
                }
            }
        });
        startPosition();

    }

    private void startPosition(){
        recyclerView.scrollToPosition(MusicActivity.position);
        recyclerView.post(() -> {
            //自动播放第一个
            View view = recyclerView.getChildAt(0);
            ijkVideoView = view.findViewById(R.id.video_view);
            ijkVideoView.start();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        ijkVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ijkVideoView != null) {
            ijkVideoView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ijkVideoView.release();
    }
}
