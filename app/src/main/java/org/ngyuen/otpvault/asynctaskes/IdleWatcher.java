package org.ngyuen.otpvault.asynctaskes;

import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.activities.ChangePasswordActivity;
import org.ngyuen.otpvault.activities.settings.SettingsActivity;

public class IdleWatcher extends AsyncTask<OTPVaultApplication, Void, Void> {
    private OTPVaultApplication application;

    @Override
    protected Void doInBackground(OTPVaultApplication... OTPVaultApplications) {
        if (OTPVaultApplications == null || OTPVaultApplications.length <=0 || OTPVaultApplications[0] == null)
            return null;
        else {
            application = OTPVaultApplications[0];
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
