package org.ngyuen.otpvault.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.ngyuen.otpvault.BackupManager;
import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.common.Constants;

public class BackupBroadcastTrigger extends BroadcastReceiver {

    private String LOG_TAG = this.getClass().getName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        OTPVaultApplication application = (OTPVaultApplication) context.getApplicationContext();

        if (application.getSettingsPreference().getBoolean("allowBackupByBroastcastIntent", false)){
            Log.d(LOG_TAG, "BackupBroadcastTrigger has been triggered by a broastcast intent from third party apps.");

            boolean result = BackupManager.backup(application, Constants.THIRDPARTY_BACKUP_SUFFIX_NAME);
            if (!result){
                Log.d(LOG_TAG, "Backup failed.");
            } else {
                Log.d(LOG_TAG, "Backup successful.");
            }
        } else {
            Log.d(LOG_TAG, "Backup by third party apps isn't allowed.");
        }
    }

}