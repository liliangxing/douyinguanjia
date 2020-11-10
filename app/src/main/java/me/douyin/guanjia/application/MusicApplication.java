package me.douyin.guanjia.application;

import android.app.Application;
import android.content.Intent;

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
    }
}
