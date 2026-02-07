package org.envaya.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Required for default SMS app on Android 4.4+.
 * Receives WAP_PUSH_DELIVER intents for MMS.
 */
public class MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // MMS received - the system stores it in the MMS content provider.
        // The existing IncomingMms/MessagingObserver classes will pick it up.
    }
}
