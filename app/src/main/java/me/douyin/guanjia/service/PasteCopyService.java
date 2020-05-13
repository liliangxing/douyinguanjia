package me.douyin.guanjia.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.BaseAdapter;

import com.alibaba.fastjson.JSON;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.douyin.guanjia.fragment.LocalMusicFragment;
import me.douyin.guanjia.model.Music;
import me.douyin.guanjia.utils.DownFile;
import me.douyin.guanjia.utils.ToastUtils;

/**
 * 音乐播放后台服务
 * Created by wcy on 2015/11/27.
 */
public class PasteCopyService extends Service {
    private static final String TAG = "Service";
    private static  LocalMusicFragment adapter;

    ClipboardManager clipboardManager;

    private String mPreviousText = "";
    private static Handler handler1;
    public class PlayBinder extends Binder {
        public PasteCopyService getService() {
            return PasteCopyService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: " + getClass().getSimpleName());
        clipboardManager =(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                ClipData clipData = clipboardManager.getPrimaryClip();
                ClipData.Item item = clipData.getItemAt(0);
                if(null == item || null == item.getText()){
                    return;
                }
                if(mPreviousText.equals(item.getText().toString())){ return;}
                else{
                    mPreviousText = item.getText().toString();
                    ToastUtils.show("您有新的剪贴了！");

                    String html  = mPreviousText;
                    String mode = "(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))";
                    Pattern p = Pattern.compile(mode);
                    Matcher m = p.matcher(html);
                    if(m.find()) {
                        String url = m.group(1);

                        douyinCrawler(url);
                    }
                }
            }
        });
    }


    private final String agent = "Mozilla/5.0 (Windows NT 6.3; WOW64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 "
            + "Safari/537.36 OPR/30.0.1835.59";
    private final String uidAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1";
    private String mode = "(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))";
    private Pattern dytkPattern = Pattern.compile("dytk: ('|\")(\\w*?)('|\")");
    private Pattern uidPattern = Pattern.compile("uid: \"(\\w*?)\"");
    private Pattern p = Pattern.compile(mode);
    private Pattern playAddrPattern = Pattern.compile("playAddr: \"(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))\"");
    private Pattern coverPattern = Pattern.compile("cover: \"(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))\"");
    private Pattern itemIdPattern = Pattern.compile("itemId: \"(\\w*?)\"");


    public static final String AWEME_LIST_API = "https://www.iesdouyin.com/web/api/v2/aweme/post/?user_id=%s&sec_uid=&count=21&max_cursor=0&app_id=1128&_signature=%s&dytk=%s";

    private void douyinCrawler(String url) {
            new HttpUtils().configUserAgent(uidAgent).send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
                @Override
                public void onSuccess(ResponseInfo<String> objectResponseInfo) {

                    // 拿到2级页面源码
                    String html2 = objectResponseInfo.result;
                    /***********************解析<img>图片标签**************************/
                    // 框架JSoup:GitHub
                    Document doc3 = Jsoup.parse(html2);// 解析HTML页面
                    // 获取图片
                    Elements list =doc3.getElementsByTag("script");
                    for(Element element:list) {
                        String html = element.toString();
                        if(!(html.contains("playAddr") || html.contains("uid"))){
                            continue;
                        }/*
                        if (element.childNodeSize() == 0) {
                            continue;
                        }*/
                        Music videoVO = new Music();
                        videoVO.setPath(url);
                        try {
                            String title = mPreviousText;
                            Elements list2 = doc3.getElementsByTag("input");
                            for(Element element2:list2) {
                                String tagName =  element2.attr("name").toLowerCase();
                                if(tagName.equals("shareAppDesc".toLowerCase())){
                                    title = element2.val();

                                }else if(tagName.equals("shareImage".toLowerCase())){
                                    videoVO.setCoverPath(element2.val());
                                }
                            }
                            videoVO.setTitle(title);
                        }catch (Exception e){
                            videoVO.setTitle(mPreviousText);
                            System.out.println(e.getMessage());
                        }
                        Matcher m2 = dytkPattern.matcher(html);
                        Matcher m3 =uidPattern.matcher(html);
                        Matcher m4 =playAddrPattern.matcher(html);
                        Matcher m5 = coverPattern.matcher(html);
                        Matcher m6 = itemIdPattern.matcher(html);
                        if(m2.find()) {
                            String dytk = m2.group(2);
                            System.out.println("dytk..."+dytk);
                            videoVO.setAlbum(dytk);
                        }

                        if(m3.find()) {
                            String uid = m3.group(1);
                            System.out.println("uid..."+uid);
                            videoVO.setSongId(Long.valueOf(uid));
                        }
                        if(m4.find()) {
                            String playAddr = m4.group(1);
                            System.out.println("playAddr..."+playAddr);
                            douyinCrawler(playAddr,videoVO);
                        }

                        if(m5.find()) {
                            String cover = m5.group(1);
                            videoVO.setCoverPath(cover);
                        }
                        if(m6.find()) {
                            String cover = m6.group(1);
                            videoVO.setFileSize(Long.valueOf(cover));
                        }
                        String videoUrl = String.format(AWEME_LIST_API, videoVO.getSongId(), "{signature}", videoVO.getAlbum());
                        videoVO.setArtist(videoUrl);
                        sendMsgVO(videoVO);
                        }
                    }



                @Override
                public void onFailure(HttpException e, String s) {

                }
            });

    }

    private void douyinCrawler(String url,Music videoVO){
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Response response =OkHttpUtils.get().url(url).build()
                            .execute();
                    String html = response.body().string();
                }catch (IOException e){
                    System.out.println(e.getMessage());
                }
            }
        }).start();*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                Document doc3 = DownFile.doGetDoc(url);
                Elements links = doc3.select("a[href]");
                for (Element vLink : links)
                {
                    String lin = vLink.attr("abs:href");
                    if(lin.contains("video/tos")){
                       // videoVO.setPath(lin);
                    }
                }

            }
        }).start();
    }

    private void sendMsgVO(Music videoVO){
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("data", JSON.toJSONString(videoVO));
        message.setData(bundle);
        handler1.sendMessage(message);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PlayBinder();
    }

    public static void startCommand(Context context, Handler handler) {
        Intent intent = new Intent(context, PasteCopyService.class);
        context.startService(intent);
        handler1 = handler;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {

            }
        }
        return START_NOT_STICKY;
    }
}