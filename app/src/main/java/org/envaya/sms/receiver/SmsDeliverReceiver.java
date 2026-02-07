package org.envaya.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Required for default SMS app on Android 4.4+.
 * Receives SMS_DELIVER intents and delegates to existing SmsReceiver logic.
 */
public class SmsDeliverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Delegate to existing SmsReceiver
        SmsReceiver receiver = new SmsReceiver();
        receiver.onReceive(context, intent);
    }
}
