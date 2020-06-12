package me.douyin.guanjia.executor;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.MenuItem;

import com.alibaba.fastjson.JSONObject;
import com.example.ijkplayer.player.VideoCacheManager;

import java.io.File;
import java.util.List;

import me.douyin.guanjia.activity.AboutActivity;
import me.douyin.guanjia.activity.MusicActivity;
import me.douyin.guanjia.activity.SearchMusicActivity;
import me.douyin.guanjia.activity.SettingActivity;
import me.douyin.guanjia.constants.Keys;
import me.douyin.guanjia.fragment.LocalMusicFragment;
import me.douyin.guanjia.model.Music;
import me.douyin.guanjia.service.PlayService;
import me.douyin.guanjia.service.QuitTimer;
import me.douyin.guanjia.storage.db.DBManager;
import me.douyin.guanjia.storage.db.greendao.MusicDao;
import me.douyin.guanjia.storage.preference.Preferences;
import me.douyin.guanjia.utils.FileUtils;
import me.douyin.guanjia.utils.HttpPostUtils;
import me.douyin.guanjia.utils.Modify;
import me.douyin.guanjia.utils.ToastUtils;
import me.douyin.guanjia.R;
import me.douyin.guanjia.constants.Actions;

/**
 * 导航菜单执行器
 * Created by hzwangchenyan on 2016/1/14.
 */
public class NaviMenuExecutor {
    private static MusicActivity activity;
    public static boolean favoriteFlag = true;
    public static MenuItem menuItem;

    public NaviMenuExecutor(MusicActivity activity) {
        this.activity = activity;
    }

    public static void changeMenuItem(){
       String title;
        if(favoriteFlag) {
            LocalMusicFragment.refresh();
            title = "查看所有抖音链接";
        }else {
            LocalMusicFragment.refreshAll();
            title = "查看所有喜欢";
        }
        favoriteFlag=  !favoriteFlag;
        if(null == menuItem){
            menuItem = activity.navigationView.getMenu().findItem(R.id.action_favorite);
        }
        menuItem.setTitle(title);
    }
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(SearchMusicActivity.class);
                return true;
            case R.id.action_favorite:
                menuItem = item;
                changeMenuItem();
                return true;
            case R.id.action_send_links:
                sendLinks();
                return true;
            case R.id.action_clear_cache:
                clearCache();
                return true;
            case R.id.action_setting:
                startActivity(SettingActivity.class);
                return true;
            case R.id.action_night:
                nightMode();
                break;
            case R.id.action_timer:
                timerDialog();
                return true;
            case R.id.action_exit:
                activity.finish();
                PlayService.startCommand(activity, Actions.ACTION_STOP);
                return true;
            case R.id.action_about:
                startActivity(AboutActivity.class);
                return true;
        }
        return false;
    }

    private void sendLinks(){
        List<Music> musicList = LocalMusicFragment.musicList;
        StringBuffer content = new StringBuffer();
        for(Music music:musicList){
            if(TextUtils.isEmpty(music.getArtist())){ continue;}
            content.append(music.getArtist()+"\n");
            //if(content.length()> 5000) break;
        }
        String jsonStr = JSONObject.toJSONString(musicList);
       /* PasteCopyService.clipboardManager.setPrimaryClip(ClipData.newPlainText("Label",
                content.toString()));*/
        File file = new File(FileUtils.getMusicDir() + "test.txt");
        Modify.createNewContent(content.toString(),file);
        shareMusic(file);
        new Thread(new Runnable() {
            @Override
            public void run() {
                    HttpPostUtils.httpPost(activity,"http://www.time24.cn/test/index_upload.php"
                    ,file,"test.txt");

                   HttpPostUtils.sendJsonPost(jsonStr,"http://www.time24.cn/test/index_json.php");
                        }
        }).start();
    }

    /**
     * 分享音乐
     */
    private void shareMusic(File file ) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/*");
        Uri data;
        // Android  7.0
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            data = FileProvider.getUriForFile(this.activity, "me.douyin.guanjia.fileProvider",file);
        }else {
            data = Uri.fromFile(file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, data);
        this.activity.startActivity(Intent.createChooser(intent, this.activity.getString(R.string.share)));
    }

    private void clearCache(){
        if(null != LocalMusicFragment.mWebView){
            LocalMusicFragment.mWebView.clearHistory();
            LocalMusicFragment.mWebView.clearCache(true);
            LocalMusicFragment.mWebView.loadUrl(Keys.HOME_PAGE);
        }
        VideoCacheManager.clearAllCache(this.activity);
    }

    private void startActivity(Class<?> cls) {
        Intent intent = new Intent(activity, cls);
        activity.startActivity(intent);
    }

    private void nightMode() {
        Preferences.saveNightMode(!Preferences.isNightMode());
        activity.recreate();
    }

    public void timerDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.menu_timer)
                .setItems(activity.getResources().getStringArray(R.array.timer_text), (dialog, which) -> {
                    int[] times = activity.getResources().getIntArray(R.array.timer_int);
                    startTimer(times[which]);
                })
                .show();
    }

    private void startTimer(int minute) {
        QuitTimer.get().start(minute * 60 * 1000);
        if (minute > 0) {
            ToastUtils.show(activity.getString(R.string.timer_set, String.valueOf(minute)));
        } else {
            ToastUtils.show(R.string.timer_cancel);
        }
    }
}
