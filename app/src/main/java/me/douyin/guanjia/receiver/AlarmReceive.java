package me.douyin.guanjia.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.douyin.guanjia.service.PasteCopyService;
import me.douyin.guanjia.storage.db.SQLiteDatabase;

public class AlarmReceive extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, PasteCopyService.class);
        intent.setFlags(SQLiteDatabase.CREATE_IF_NECESSARY);
        context.startService(i);
    }
}