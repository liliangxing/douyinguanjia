package me.douyin.guanjia.application;

import android.app.Application;
import android.content.Intent;

import me.douyin.guanjia.service.PasteCopyService;
import me.douyin.guanjia.service.PlayService;
import me.douyin.guanjia.storage.db.DBManager;

/**
 * 自定义Application
 * Created by wcy on 2015/11/27.
 */
public class MusicApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCache.get().init(this);
        ForegroundObserver.init(this);
        Intent intent2 = new Intent(this, PasteCopyService.class);
        startService(intent2);
    }
}
