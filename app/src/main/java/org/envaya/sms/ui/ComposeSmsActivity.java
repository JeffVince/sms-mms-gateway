package org.envaya.sms.ui;

import android.app.Activity;
import android.os.Bundle;

/**
 * Required stub for default SMS app.
 * Handles SENDTO intents. We just finish immediately since this is a gateway app.
 */
public class ComposeSmsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This app is a gateway, not a full SMS client - just close
        finish();
    }
}
