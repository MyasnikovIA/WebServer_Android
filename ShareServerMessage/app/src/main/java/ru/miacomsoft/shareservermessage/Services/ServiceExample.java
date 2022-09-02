package ru.miacomsoft.shareservermessage.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import ru.miacomsoft.shareservermessage.Lib.webserver.HttpSrv;


public class ServiceExample extends Service {


    HttpSrv web;

    @Override
    public void onCreate() {
        super.onCreate();
        // Toast.makeText(getApplicationContext(), "Start Signal Server", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
    }

}
