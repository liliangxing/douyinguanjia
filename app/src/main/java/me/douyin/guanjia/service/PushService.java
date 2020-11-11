package me.douyin.guanjia.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import me.douyin.guanjia.receiver.AlarmReceive;
import me.douyin.guanjia.storage.db.SQLiteDatabase;

public class PushService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ar(getApplicationContext());
        aq(getApplicationContext());
    }
    private static final int ONE_Miniute=30*1000;
    public static void aq(Context context) {
        //通过AlarmManager定时启动广播
        AlarmManager alarmManager= (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent i=new Intent(context, AlarmReceive.class);
        PendingIntent pIntent= PendingIntent.getBroadcast(context,0,i, SQLiteDatabase.CREATE_IF_NECESSARY);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ((long) ONE_Miniute), (long) ONE_Miniute, pIntent);
    }

    public static void ar(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceive.class), SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
        if (broadcast != null) {
            alarmManager.cancel(broadcast);
            broadcast.cancel();
        }
    }
}