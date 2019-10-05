package org.fedorahosted.freeotp.asynctaskes;

import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.activities.LoginActivity;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

public class IdleTimeChecker extends AsyncTask<FreeOTPApplication, Void, Void> {
    private FreeOTPApplication application;

    @Override
    protected Void doInBackground(FreeOTPApplication... freeOTPApplications) {
        if (freeOTPApplications == null || freeOTPApplications.length <=0 || freeOTPApplications[0] == null)
            return null;
        else {
            application = freeOTPApplications[0];
            long lockTimeOut;
            try {
                while(!this.isCancelled()){
                    lockTimeOut = PreferenceManager
                            .getDefaultSharedPreferences(application).getInt("lockTimeOut", 30);
                    if ( !this.application.isLogged() )
                        this.cancel(true);
                    else if ( this.application.getLastTimeInteraction() > 0 &&
                        (System.currentTimeMillis() - application.getLastTimeInteraction())
                                > lockTimeOut*1000 ){
                        break;
                    }
                    Thread.sleep(3000);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //- Not run it if its stop by cancel request or enableLockTimeout is false.
        if (!isCancelled() &&
            this.application.getSettingsPreference().getBoolean("enableLockTimeout", false)){
            this.application.logout();
            if (this.application.isAppOnForeground()){
                Intent intent = new Intent(this.application, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                this.application.startActivity(intent);
            }
        }
    }
}
