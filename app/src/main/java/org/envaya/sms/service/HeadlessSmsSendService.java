package org.envaya.sms.service;

import android.app.IntentService;
import android.content.Intent;

/**
 * Required stub for default SMS app.
 * Handles respond-via-message requests (e.g., from car Bluetooth).
 */
public class HeadlessSmsSendService extends IntentService {
    public HeadlessSmsSendService() {
        super("HeadlessSmsSendService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Not implemented - we're an SMS gateway, not a full SMS app
    }
}
