package me.douyin.guanjia.fragment;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import me.douyin.guanjia.activity.MusicActivity;
import me.douyin.guanjia.activity.MusicInfoActivity;
import me.douyin.guanjia.activity.SubscribeMessageActivity;
import me.douyin.guanjia.adapter.OnMoreClickListener;
import me.douyin.guanjia.adapter.PlaylistAdapter;
import me.douyin.guanjia.loader.MusicLoaderCallback;
import me.douyin.guanjia.model.Music;
import me.douyin.guanjia.service.AudioPlayer;
import me.douyin.guanjia.service.PasteCopyService;
import me.douyin.guanjia.storage.db.DBManager;
import me.douyin.guanjia.storage.db.greendao.MusicDao;
import me.douyin.guanjia.utils.DownFile;
import me.douyin.guanjia.utils.FileUtils;
import me.douyin.guanjia.utils.Modify;
import me.douyin.guanjia.utils.PermissionReq;
import me.douyin.guanjia.utils.ScreenUtils;
import me.douyin.guanjia.utils.ToastUtils;
import me.douyin.guanjia.utils.binding.Bind;
import me.douyin.guanjia.R;
import me.douyin.guanjia.application.AppCache;
import me.douyin.guanjia.constants.Keys;
import me.douyin.guanjia.constants.RequestCode;
import me.douyin.guanjia.constants.RxBusTags;

/**
 * 本地音乐列表
 * Created by wcy on 2015/11/26.
 */
public class LocalMusicFragment extends BaseFragment implements AdapterView.OnItemClickListener, OnMoreClickListener {
    @Bind(R.id.lv_local_music)
    private ListView lvLocalMusic;
    @Bind(R.id.v_searching)
    private TextView vSearching;
    public static WebView mWebView;
    private View vHeader;
    private Loader<Cursor> loader;
    public static PlaylistAdapter adapter;
    private Handler handler1;
    public static final String FILE_NAME = "test.html";
    private static final String DEFAULT_URL = "file:///android_asset/"+FILE_NAME;
    public static LocalMusicFragment downloadFirst;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_local_music, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<Music> musicList = DBManager.get().getMusicDao().queryBuilder().orderDesc(MusicDao.Properties.Id).build().list();
        adapter = new PlaylistAdapter(musicList);
        adapter.setOnMoreClickListener(this);
        lvLocalMusic.setAdapter(adapter);
        loadMusic();
        handler1 = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                super.handleMessage(msg);
                String data =  msg.getData().getString("data");
                Music music = JSONObject.parseObject(data,Music.class);
                adapter.addMusic(music);
                adapter.notifyDataSetChanged();
                MusicActivity.fromClicked = false;
                if(music.getAlbumId() == 1){
                    WebviewFragment.currentMusic =  music;
                    mWebView.loadUrl(music.getPath());
                    return;
                }
                downloadDouyin(music);
            }
        };
        //PasteCopyService.startCommand(getActivity(), handler1);
        PasteCopyService.handler1 = handler1;
        vHeader = LayoutInflater.from(getActivity()).inflate(R.layout.activity_local_music_list_header, null);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ScreenUtils.dp2px(150));
        vHeader.setLayoutParams(params);
        lvLocalMusic.addHeaderView(vHeader, null, false);
    }

    private void loadMusic() {
        lvLocalMusic.setVisibility(View.GONE);
        vSearching.setVisibility(View.VISIBLE);
        PermissionReq.with(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .result(new PermissionReq.Result() {
                    @Override
                    public void onGranted() {
                        initLoader();
                    }

                    @Override
                    public void onDenied() {
                        ToastUtils.show(R.string.no_permission_storage);
                        lvLocalMusic.setVisibility(View.VISIBLE);
                        vSearching.setVisibility(View.GONE);
                    }
                })
                .request();
    }

    private void initLoader() {
        lvLocalMusic.setVisibility(View.VISIBLE);
        vSearching.setVisibility(View.GONE);
        /*loader = getActivity().getLoaderManager().initLoader(0, null,new MusicLoaderCallback(getContext(), value -> {
            AppCache.get().getLocalMusicList().clear();
            AppCache.get().getLocalMusicList().addAll(value);
            adapter.notifyDataSetChanged();
        }));*/
    }

    @Subscribe(tags = { @Tag(RxBusTags.SCAN_MUSIC) })
    public void scanMusic(Object object) {
        if (loader != null) {
            loader.forceLoad();
        }
    }

    @Override
    protected void setListener() {
        lvLocalMusic.setOnItemClickListener(this);
    }

    private final static String userInfoUrl = "https://www.iesdouyin.com/share/user/";
    private final static String userAgent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36";
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MusicActivity.fromClicked = true;
        Music music = AppCache.get().getLocalMusicList().get(position-1);
        MusicActivity.instance.mViewPager.setCurrentItem(1);
        if(music.getAlbumId() == 1){
            WebviewFragment.currentMusic =  music;
            String url =  music.getPath();
            if(music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())){
                url =  music.getArtist();
            }
            mWebView.loadUrl(url);
            return;
        }
        downloadDouyin(music);
    }

    private void downloadDouyin(Music music){
        WebviewFragment.currentMusic =  music;
        Long userId = music.getSongId();
        new HttpUtils().configUserAgent(userAgent).send(HttpRequest.HttpMethod.GET, userInfoUrl + userId, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> objectResponseInfo) {
                String html = objectResponseInfo.result;
                Document document = Jsoup.parse(html);

                Elements scripts = document.getElementsByTag("script");
                String tacScript = scripts.get(1).toString();
                String tac = tacScript.replace("<script>tac='", "")
                        .replace("'</script>", "");
                File file = new File(FileUtils.getMusicDir() + FILE_NAME);
                file.delete();
                modify("var tac=", "var tac='"+tac+"';");
                modify("var user_id=","var user_id="+music.getSongId()+"");
                mWebView.loadUrl("file:///mnt/sdcard"+FileUtils.DATA_DIR+FILE_NAME);
            }
            @Override
            public void onFailure(HttpException e, String s) {

            }
        });
    }

    public void modify(String target,String  newContent){
        try {
            File file = new File(FileUtils.getMusicDir() + FILE_NAME);
            InputStream is;
            if(file.exists()){
                is = new FileInputStream(file);
            }else {
                 is = getContext().getAssets().open(FILE_NAME);
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));


            // tmpfile为缓存文件，代码运行完毕后此文件将重命名为源文件名字。
            String filename = file.getName();
            // tmpfile为缓存文件，代码运行完毕后此文件将重命名为源文件名字。
            File tmpfile = new File(file.getParentFile().getAbsolutePath()
                    + "\\" + filename + ".tmp");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));

            boolean flag = false;
            String str = null;
            while (true) {
                str = reader.readLine();

                if (str == null)
                    break;

                if (str.contains(target)) {
                    writer.write(newContent + "\n");

                    flag = true;
                } else
                    writer.write(str + "\n");
            }

            is.close();

            writer.flush();
            writer.close();
            if (flag) {
                file.delete();
                tmpfile.renameTo(new File(file.getAbsolutePath()));
            } else
                tmpfile.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMoreClick(final int position) {
        Music music = AppCache.get().getLocalMusicList().get(position);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(music.getTitle());
        dialog.setItems(R.array.local_music_dialog, (dialog1, which) -> {
            switch (which) {
                case 0:// 分享
                    if(music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())) {
                        shareMusic(music);
                    }else {
                        ToastUtils.show("文件未下载");
                    }
                    break;
                case 1:// 查看歌曲信息
                    MusicInfoActivity.start(getContext(), music);
                    break;
                case 2:// 用浏览器打开
                    if(null != music.getCoverPath()) {
                        openWithBrowser(music);
                    }else {
                        downloadFirst = this;
                        if(music.getAlbumId() == 1){
                            WebviewFragment.currentMusic =  music;
                            //分析并下载
                            mWebView.loadUrl(music.getArtist());
                        }
                    }
                    //requestSetRingtone(music);
                    break;
                case 3:// 用手机下载
                    if(music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())){
                        ToastUtils.show("已下载");
                    }else {
                        MusicActivity.forceDownload = true;
                        if (music.getAlbumId() == 1) {
                            WebviewFragment.currentMusic = music;
                            mWebView.loadUrl(music.getPath());
                        }
                    }
                    break;
                case 4:// 删除
                    deleteMusic(music);
                    break;
            }
        });
        dialog.show();
    }

    public void openWithBrowser(Music music){
        String title = music.getTitle().replaceAll("[@|#]([\\S]{1,10})","").trim();
        String url = "http://www.time24.cn/test/index_douyin.php?video="+ URLEncoder.encode(music.getArtist())
                +"&title="+URLEncoder.encode(title);
        if(null!=music.getCoverPath()){
            url += "&cover_path="+URLEncoder.encode(music.getCoverPath());
        }else {
            url = music.getPath();
        }
        SubscribeMessageActivity.createChooser(url,getContext());
    }
    /**
     * 分享音乐
     */
    private void shareMusic(Music music) {
        File file = new File(music.getPath());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/*");
        Uri data;
        // Android  7.0
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            data = FileProvider.getUriForFile(getContext(), "me.douyin.guanjia.fileProvider",file);
        }else {
            data = Uri.fromFile(file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, data);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void requestSetRingtone(final Music music) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(getContext())) {
            ToastUtils.show(R.string.no_permission_setting);
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            startActivityForResult(intent, RequestCode.REQUEST_WRITE_SETTINGS);
        } else {
            setRingtone(music);
        }
    }

    /**
     * 设置铃声
     */
    private void setRingtone(Music music) {
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(music.getPath());
        // 查询音乐文件在媒体库是否存在
        Cursor cursor = getContext().getContentResolver()
                .query(uri, null, MediaStore.MediaColumns.DATA + "=?", new String[] { music.getPath() }, null);
        if (cursor == null) {
            return;
        }
        if (cursor.moveToFirst() && cursor.getCount() > 0) {
            String _id = cursor.getString(0);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.IS_MUSIC, true);
            values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            values.put(MediaStore.Audio.Media.IS_ALARM, false);
            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
            values.put(MediaStore.Audio.Media.IS_PODCAST, false);

            getContext().getContentResolver()
                    .update(uri, values, MediaStore.MediaColumns.DATA + "=?", new String[] { music.getPath() });
            Uri newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
            RingtoneManager.setActualDefaultRingtoneUri(getContext(), RingtoneManager.TYPE_RINGTONE, newUri);
            ToastUtils.show(R.string.setting_ringtone_success);
        }
        cursor.close();
    }

    private void deleteMusic(final Music music) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        String title = music.getTitle();
        String msg = getString(R.string.delete_music, title);
        dialog.setMessage(msg);
        dialog.setPositiveButton(R.string.delete, (dialog1, which) -> {
            File file = new File(music.getPath());
            if (file.delete()) {
                // 刷新媒体库
                Intent intent =
                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://".concat(music.getPath())));
                getContext().sendBroadcast(intent);
                ToastUtils.show("删除成功");
            }else {
                ToastUtils.show("手机没有下载该文件");
            }
            AppCache.get().getLocalMusicList().remove(music);
            if(null != music.getId()) {
                DBManager.get().getMusicDao().delete(music);
            }
            adapter.notifyDataSetChanged();
        });
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCode.REQUEST_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(getContext())) {
                ToastUtils.show(R.string.grant_permission_setting);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        int position = lvLocalMusic.getFirstVisiblePosition();
        int offset = (lvLocalMusic.getChildAt(0) == null) ? 0 : lvLocalMusic.getChildAt(0).getTop();
        outState.putInt(Keys.LOCAL_MUSIC_POSITION, position);
        outState.putInt(Keys.LOCAL_MUSIC_OFFSET, offset);
    }

    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        lvLocalMusic.post(() -> {
            int position = savedInstanceState.getInt(Keys.LOCAL_MUSIC_POSITION);
            int offset = savedInstanceState.getInt(Keys.LOCAL_MUSIC_OFFSET);
            lvLocalMusic.setSelectionFromTop(position, offset);
        });
    }
}
