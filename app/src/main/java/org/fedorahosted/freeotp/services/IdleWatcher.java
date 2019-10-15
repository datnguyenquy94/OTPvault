package org.fedorahosted.freeotp.services;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.activities.LoginActivity;

import androidx.annotation.Nullable;

//public class IdleWatcher extends IntentService {

//    private FreeOTPApplication application;
//    private static boolean cancel = false;
//
//    public static void setCancel(boolean c){
//        cancel = c;
//    }
//
//    @Override
//    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
//        cancel = false;
//        return super.onStartCommand(intent, flags, startId);
//    }
//
//    @Override
//    public void onStart(@Nullable Intent intent, int startId) {
//        super.onStart(intent, startId);
//        cancel = false;
//    }
//
//    /**
//     * A constructor is required, and must call the super <code><a href="/reference/android/app/IntentService.html#IntentService(java.lang.String)">IntentService(String)</a></code>
//     * constructor with a name for the worker thread.
//     */
//    public IdleWatcher() {
//        super("IdleWatcher");
//        this.application = (FreeOTPApplication) this.getApplicationContext();
//    }
//
//    /**
//     * The IntentService calls this method from the default worker thread with
//     * the intent that started the service. When this method returns, IntentService
//     * stops the service, as appropriate.
//     */
//    @Override
//    protected void onHandleIntent(Intent intent) {
//        long lockTimeOut;
//        try {
//            while(!cancel){
//                lockTimeOut = PreferenceManager
//                        .getDefaultSharedPreferences(application).getInt("lockTimeOut", 30);
//                if ( !this.application.isLogged() )
//                    this.cancel = true;
//                else if ( this.application.getLastTimeInteraction() > 0 &&
//                        (System.currentTimeMillis() - application.getLastTimeInteraction())
//                                > lockTimeOut*1000 ){
//                    break;
//                }
//                Thread.sleep(3000);
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        cancel = true;
//
//        //- Not run it if its not logged yet or enableLockTimeout is false.
//        if (!this.application.isLogged()  &&
//                this.application.getSettingsPreference().getBoolean("enableLockTimeout", false)){
//            this.application.logout();
//            if (this.application.isAppOnForeground()){
//                Intent intent = new Intent(this.application, LoginActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                this.application.startActivity(intent);
//            }
//        }
//    }
//}
