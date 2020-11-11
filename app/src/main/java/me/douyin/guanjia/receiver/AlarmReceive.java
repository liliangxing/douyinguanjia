package me.douyin.guanjia.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.douyin.guanjia.utils.SystemUtils;

public class AlarmReceive extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //判断app进程是否存活
        if(SystemUtils.isAppALive(context, "me.douyin.guanjia")){
        }else {
            Intent launchIntent = context.getPackageManager().
                    getLaunchIntentForPackage("me.douyin.guanjia");
            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launchIntent);
        }
    }
}