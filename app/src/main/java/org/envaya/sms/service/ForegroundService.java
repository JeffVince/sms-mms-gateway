/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.envaya.sms.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import org.envaya.sms.App;
import org.envaya.sms.R;
import org.envaya.sms.ui.Main;

/*
 *  Service running in foreground to make sure App instance stays
 *  in memory (to avoid losing pending messages and timestamps of
 *  sent messages).
 *
 *  Also adds notification to status bar.
 */
public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "envayasms_foreground";
    private static final int NOTIFICATION_ID = 1;

    private App app;
    private NotificationManager mNM;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        app = (App)getApplication();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getText(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            mNM.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        return START_STICKY;
    }

    void handleCommand(Intent intent)
    {
        if (app.isEnabled())
        {
            CharSequence text = getText(R.string.service_started);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, Main.class), PendingIntent.FLAG_IMMUTABLE);

            CharSequence info = getText(R.string.running);

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon)
                    .setTicker(text)
                    .setContentTitle(info)
                    .setContentText(text)
                    .setContentIntent(contentIntent)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        }
        else
        {
            stopForeground(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
