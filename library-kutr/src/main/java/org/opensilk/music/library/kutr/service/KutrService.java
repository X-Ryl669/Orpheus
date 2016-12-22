package org.opensilk.music.library.kutr.service;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by cyril on 09/12/2016.
 */
public class KutrService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean firstStart = (intent.getFlags() & START_FLAG_RETRY) == 0;
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }
}
