package org.fedorahosted.freeotp.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import org.fedorahosted.freeotp.FreeOTPApplication;

public class ScreenListener extends BroadcastReceiver {

    private String LOG_TAG = this.getClass().getName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        FreeOTPApplication application = (FreeOTPApplication) context.getApplicationContext();
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.d(LOG_TAG, "ACTION_SCREEN_OFF");
            application.onScreenTurnedOff();
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Log.d(LOG_TAG, "ACTION_SCREEN_ON");
        }
    }

}