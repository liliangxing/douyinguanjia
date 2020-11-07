package me.douyin.guanjia.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.douyin.guanjia.activity.MusicActivity;

public class AlarmReceive extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //循环启动Service
        Intent i = new Intent(context, MusicActivity.class);
        context.startActivity(i);
    }
}