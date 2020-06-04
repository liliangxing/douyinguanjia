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
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.zhy.http.okhttp.OkHttpUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.douyin.guanjia.R;
import me.douyin.guanjia.adapter.VideoRecyclerViewAdapter;
import me.douyin.guanjia.bean.VideoBean;
import me.douyin.guanjia.fragment.LocalMusicFragment;
import me.douyin.guanjia.model.Music;
import me.douyin.guanjia.service.PasteCopyService;
import me.douyin.guanjia.storage.db.DBManager;
import me.douyin.guanjia.storage.db.greendao.MusicDao;
import me.douyin.guanjia.utils.FileUtils;
import me.douyin.guanjia.utils.Modify;
import okhttp3.Response;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class MainActivity extends AppCompatActivity {

    private  IjkVideoView ijkVideoView;
    private RecyclerView recyclerView;
    private VideoRecyclerViewAdapter videoRecyclerViewAdapter;
    private  List<VideoBean> videoList;
    private String pushURL = "http://www.time24.cn/test/index_push.php";

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
        videoList = new ArrayList<>();
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
                            ijkVideoView.start();
                        }
                        httpRequestVideo();
                        break;
                }
            }
        });
        startPosition();
    }

    private void  httpRequestVideo(){
        //getRedirect(ijkVideoView.mCurrentUrl);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response =OkHttpUtils.get().url(pushURL)
                            .addParams("cover_path", getCover(ijkVideoView.mCurrentUrl))
                            .addParams("video", ijkVideoView.mCurrentUrl)
                            .addParams("title", ijkVideoView.mCurrentTitle)
                            .build()
                            .execute();
                    System.out.println("发送内容：" + response.body().string());
                }catch (IOException e){
                    System.out.println(e.getMessage());
                }
            }
        }).start();
    }

    private  String getCover(String url){
        for(VideoBean videoBean:videoList){
            if(videoBean.getUrl().equals(url)){
                return videoBean.getThumb();
            }
        }
        return null;
    }

    private  void getRedirect(String url){
        new HttpUtils().configUserAgent("Mozilla/5.0 (Windows NT 6.3; WOW64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 "
                + "Safari/537.36 OPR/30.0.1835.59").send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> objectResponseInfo) {
                String html = objectResponseInfo.result;
                Document document = Jsoup.parse(html);
                System.out.println("html：" + html);
            }
            @Override
            public void onFailure(HttpException e, String s) {

            }
        });
    }

    private void startPosition(){
        recyclerView.scrollToPosition(MusicActivity.position);
        recyclerView.post(() -> {
            //自动播放第一个
            View view = recyclerView.getChildAt(0);
            ijkVideoView = view.findViewById(R.id.video_view);
            ijkVideoView.start();
            httpRequestVideo();
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
