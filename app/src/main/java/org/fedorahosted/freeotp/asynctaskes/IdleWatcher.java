package org.fedorahosted.freeotp.asynctaskes;

import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.activities.ChangePasswordActivity;
import org.fedorahosted.freeotp.activities.LoginActivity;
import org.fedorahosted.freeotp.activities.settings.SettingsActivity;

import java.util.List;
import java.util.concurrent.Executor;

import static android.content.Context.ACTIVITY_SERVICE;

public class IdleWatcher extends AsyncTask<FreeOTPApplication, Void, Void> {
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
                    if ( !this.application.isLogged() ) {
                        this.cancel(true);
                    } else if ( this.application.getCurrentActivityClassName().compareTo(SettingsActivity.class.getName()) == 0 &&
                                this.application.getCurrentActivityClassName().compareTo(ChangePasswordActivity.class.getName()) == 0 ){
                      //- Also, not logout if user is changing password. Avoid confiled or any god know what it is can destroy app's data...

                      //- Not apply IdleWatcher to SettingsActivity.
                      //- Because IdleWatcher cant record lastTimeInteraction on SettingsActivity's folder and backup fileChooser
                      //- Temporary disable IdleWatcher by update lastTimeInteraction to current time.
                      application.updateLastTimeInteraction();
                    } else if ( this.application.getLastTimeInteraction() > 0 &&
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
        //- Not run it if its stop by cancel request.
        if (!isCancelled()){
            this.application.onIdleTimeout();
        }
    }


}
