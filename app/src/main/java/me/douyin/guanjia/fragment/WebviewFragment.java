package me.douyin.guanjia.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import org.jsoup.Jsoup;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.douyin.guanjia.R;
import me.douyin.guanjia.activity.FullScreenActivity;
import me.douyin.guanjia.activity.MusicActivity;
import me.douyin.guanjia.adapter.PlaylistAdapter;
import me.douyin.guanjia.application.AppCache;
import me.douyin.guanjia.constants.Keys;
import me.douyin.guanjia.http.HttpUtils;
import me.douyin.guanjia.model.AwemeVO;
import me.douyin.guanjia.model.Music;
import me.douyin.guanjia.model.ResponseVO;
import me.douyin.guanjia.model.UriVO;
import me.douyin.guanjia.model.VideoVO;
import me.douyin.guanjia.service.AudioPlayer;
import me.douyin.guanjia.service.PasteCopyService;
import me.douyin.guanjia.utils.FileUtils;
import me.douyin.guanjia.utils.MusicUtils;
import me.douyin.guanjia.utils.ToastUtils;
import me.douyin.guanjia.utils.binding.Bind;
import me.douyin.guanjia.webview.MyWebViewClient;
import okhttp3.Call;
import okhttp3.Request;

/**
 * 在线音乐
 * Created by wcy on 2015/11/26.
 */
public class WebviewFragment extends BaseFragment {
    @Bind(R.id.lv_webview)
    public WebView mWebView;
    private WebSettings webSettings;
    private ProgressDialog progressDialog;//加载界面的菊花
    private PlaylistAdapter adapter;
    private Handler handler1;
    private String LAST_OPEN_URL;
    public static Music currentMusic;

    private Integer loopCount;
    private View xCustomView;
    private WebChromeClient.CustomViewCallback   xCustomViewCallback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new PlaylistAdapter(AppCache.get().getLocalMusicList());
        init();
        handler1 = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                String data =  msg.getData().getString("data");
                String signc =  msg.getData().getString("signc");
                if(!TextUtils.isEmpty(data)){
                    mWebView.loadUrl(data);
                    downloadAndPlay(data);
                    return;
                }
                if(!TextUtils.isEmpty(signc)){
                    String ajaxData = "https://www.iesdouyin.com/web/api/v2/aweme/post/?user_id="+currentMusic.getSongId()+"&sec_uid=&count=300&max_cursor=0&app_id=1128&_signature="+signc;
                    currentMusic.setArtist(ajaxData);
                    mWebView.loadUrl(ajaxData);
                    return;
                }
                String url = LAST_OPEN_URL;
                if (url!=null) {
                    downloadAndPlay(url);
                }
            }
        };
       // PasteCopyService.startCommand(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Keys.WEBVIEW_URL,  mWebView.getUrl());

    }

    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        mWebView.post(() -> {
            String position = savedInstanceState.getString(Keys.WEBVIEW_URL);
        });
    }

    private void init(){
        webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        mWebView.setVerticalScrollBarEnabled(true);
        mWebView.setWebViewClient(new MyWebViewClient(this.getContext(),progressDialog){
            /**
             * 当打开超链接的时候，回调的方法
             * WebView：自己本身mWebView
             * url：即将打开的url
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(HttpUtils.IsVideoUrl(url)){
                    Intent intent = new Intent(getActivity(), FullScreenActivity.class);
                    intent.putExtra("url",url);
                    startActivity(intent);
                    return true;
                }
                //自己处理新的url
                LAST_OPEN_URL = url;
                mWebView.loadUrl(url);
                return true;//true就是自己处理
            }
        });

        mWebView.addJavascriptInterface(
                new JSInterface()
                , "itcast");
        mWebView.addJavascriptInterface(
                new InJavaScriptLocalObj()
                , "java_obj");
        mWebView.loadUrl(Keys.HOME_PAGE);
    }
    /**
     * 逻辑处理
     * @author linzewu
     */
    private final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void getSource(String html) {
            html = Jsoup.parse(html).text();
            if(html.contains("_signature")){
                System.out.println("_signature..."+html);
                Matcher m =Pattern.compile("_signature=([\\S-]+)").matcher(html);
                String signc = null;
                if(m.find()){
                    signc = m.group(1);
                }
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("signc", signc);
                message.setData(bundle);
                handler1.sendMessage(message);
            }
            if(!html.contains("aweme_list")){
                return;
            }
            System.out.println("aweme_list..."+html);
            ResponseVO responseVO = JSONObject.parseObject(html, ResponseVO.class);
            for(AwemeVO awemeVO:responseVO.getAweme_list()){
                if((currentMusic.getFileSize()+"").equals(awemeVO.getAweme_id()+"")) {
                    VideoVO videoVO = awemeVO.getVideo();
                    UriVO play_addr = videoVO.getPlay_addr();
                    List<String> url_list = play_addr.getUrl_list();
                    for (String url : url_list) {
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("data", url);
                        message.setData(bundle);
                        handler1.sendMessage(message);
                        break;
                    }
                    break;
                }
            }
        }
    }
    private final class JSInterface{
        @SuppressLint("JavascriptInterface")
        @JavascriptInterface
        public void showToast(String url){
            Message message = new Message();
            LAST_OPEN_URL=url;
            handler1.sendMessage(message);
        }
    }

    public void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }
    public void cancelProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    public void downloadAndPlay(String url){
        String fileName1 = HttpUtils.getFileName(url);
        if(TextUtils.isEmpty(fileName1)){
            Matcher m =Pattern.compile("video_id=([\\w-]+)").matcher(url);
            if(m.find()){
                fileName1 = m.group(1)+".mp4";
            }
        }
        String fileName = fileName1;
        String path  = FileUtils.getMusicDir().concat(fileName);
        File file = new File(path);
        if(!file.exists()){
                    OkHttpUtils.get().url(url).build()
                    .execute(new FileCallBack(FileUtils.getMusicDir(), fileName) {
                        boolean finishScanned =false;
                        @Override
                        public void onBefore(Request request, int id) {
                            showProgress(getString(R.string.now_download,fileName));
                        }

                        @Override
                        public void inProgress(float progress, long total, int id) {
                            showProgress("正在下载……"+((float)Math.round(progress*100*100)/100)+"%");
                        }

                        @Override
                        public void onResponse(File file, int id) {

                        }

                        @Override
                        public void onError(Call call, Exception e, int id) {

                        }

                        @Override
                        public void onAfter(int id) {
                            if(!finishScanned){
                                // 刷新媒体库
                                Intent intent =
                                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(file.toURI().toString()));
                                getContext().sendBroadcast(intent);
                                finishScanned = true;
                            }
                            loopCount = 3;
                            cancelProgress();
                            if(fileName.endsWith(".mp4")){
                                addPathAndPaste(path);
                                return;
                            }
                            checkCounter(fileName,true);
                        }
                    });
        }else {
            if(fileName.endsWith(".mp4")){
                addPathAndPaste(path);
                return;
            }
            checkCounter(fileName,false);
        }

    }

    private void addPathAndPaste(String path){
        currentMusic.setPath(path);
        if(!TextUtils.isEmpty(currentMusic.getTitle())) {
            PasteCopyService.clipboardManager.setPrimaryClip(ClipData.newPlainText("Label",
                    currentMusic.getTitle().replaceAll("[@|#][ |]([\\S]{1,10})", "").trim()));
        }
        MusicActivity.instance.mViewPager.setCurrentItem(1);
    }

    private void  checkCounter(String fileName,boolean loop){
        String path  = FileUtils.getMusicDir().concat(fileName);
        List<Music> musicList = MusicUtils.scanMusic(getContext());
        for(Music m:musicList) {
            if(m.getPath().equals(path)) {
                AudioPlayer.get().addAndPlay(m);
                ToastUtils.show("已添加到播放列表");
                loop = false;
                MusicActivity.instance.showPlayingFragment();
                break;
            }else {
                loop = true;
            }
        }
        if (loop) {
            if(null == loopCount){
                //直接拿，又匹配不到媒体库，删除文件，重新走下载流程
                File  file=new File(path);
                if(file.delete()){
                    downloadAndPlay(LAST_OPEN_URL);
                    return;
                }
            }
            loopCount --;
            if(loopCount<0) return;
            try {
                //耗时的操作
                ToastUtils.show("下载完毕，尝试播放中...");
                Thread.sleep(500);
                //handler主要用于异步消息的处理,使用sendMessage()后，方法立即返回，Message放入消息队列，
                //等待Message出消息队列，由handlerMessage(Message msg)通知UI线程子线程已经挂起，并使用返回的msg。
                checkCounter(fileName, false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            }
        }

}
