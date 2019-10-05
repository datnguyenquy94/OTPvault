package org.fedorahosted.freeotp;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;

import org.fedorahosted.freeotp.activities.edit.EditActivity;
import org.fedorahosted.freeotp.asynctaskes.IdleTimeChecker;
import org.fedorahosted.freeotp.storage.TokenPersistence;

import java.io.File;

public class FreeOTPApplication extends Application implements LifecycleObserver {

    private String LOG_TAG = this.getClass().getName();

    private EncryptedSharedPreferences  sharedPreferencesStorage = null;
    private SharedPreferences           settingsPreference;
    private TokenPersistence            tp = null;
    private IdleTimeChecker idleTimeChecker;
    private boolean                     isAppOnForeground = false;
    private long                        lastTimeInteraction = 0;
    private String                      currentActivityClassName = "";
    private File                        imageFolder;
    private File                        backupFolder;
    private File                        tmpFolder;
    private Gson                        gson;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.settingsPreference = PreferenceManager.getDefaultSharedPreferences(this);
        this.gson = new Gson();

        this.imageFolder = new File(this.getFilesDir(), Constants.IMAGE_FOLDER);
        this.backupFolder = new File(this.getFilesDir(), Constants.BACKUP_FOLDER);
        this.tmpFolder = new File(this.getFilesDir(), Constants.TMP_FOLDER);
        this.imageFolder.mkdirs();
        this.backupFolder.mkdirs();
        this.tmpFolder.mkdirs();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        Log.d(LOG_TAG, "onAppBackgrounded");
        this.isAppOnForeground = false;
        //- Lock on Background only if setting allow it.
        //- And current activity isn't EditActivity.
        //- (In case user change token's image. EditActivity
        //- will request it on another app,
        //- so it need to be allow run on background.).
        if (settingsPreference.getBoolean("lockOnBackground", false) &&
            this.currentActivityClassName.compareTo(EditActivity.class.getName()) != 0){
            this.logout();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        Log.d(LOG_TAG, "onAppForegrounded");
        this.isAppOnForeground = true;
    }

    public TokenPersistence getTokenPersistence(){
        if (this.tp ==null)
            this.tp = new TokenPersistence(this);
        return tp;
    }

    public Gson getGson() {
        return gson;
    }

    public boolean createPasswordFirstTime(String passwd){
        try {
            File sharedPreferenceStoreFile = this.getSharedPreferenceStoreFile();
            boolean isFirstTime = !sharedPreferenceStoreFile.exists();
            if (!isFirstTime)
                throw new Exception("Not the first time");
            else {
                this.sharedPreferencesStorage = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        Constants.SharedPreferenceStoreFile,
                        passwd,
                        this,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
                this.sharedPreferencesStorage.edit().putBoolean(Constants.PASSWD_PREFIX_TEST_KEY + passwd, true).commit();
//                boolean result = this.sharedPreferencesStorage.contains(Constants.PASSWD_PREFIX_TEST_KEY + passwd);
//                boolean result2 = this.sharedPreferencesStorage.getBoolean(Constants.PASSWD_PREFIX_TEST_KEY + passwd, false);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.sharedPreferencesStorage = null;
            return false;
        }
    }

    public boolean isEncryptStorageCreated(){
        File sharedPreferenceStoreFile = this.getSharedPreferenceStoreFile();
        return sharedPreferenceStoreFile.exists();
    }

    public boolean login(String passwd){
        try {
            File sharedPreferenceStoreFile = this.getSharedPreferenceStoreFile();
            boolean isFirstTime = !sharedPreferenceStoreFile.exists();
            if (isFirstTime)
                throw new Exception("Data and password not exsit yet.");
            else {
                this.sharedPreferencesStorage = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        Constants.SharedPreferenceStoreFile,
                        passwd,
                        this,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                if (this.sharedPreferencesStorage.contains(Constants.PASSWD_PREFIX_TEST_KEY + passwd)
                    && this.sharedPreferencesStorage.getBoolean(Constants.PASSWD_PREFIX_TEST_KEY + passwd, false)){
                    return true;
                } else {
                    throw new Exception("Could not decrypt key.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.sharedPreferencesStorage = null;
            return false;
        }
    }

    public SharedPreferences loginImportFile(String passwd){
        try {
            SharedPreferences sharedPreferencesBackupStorage;
            File sharedPreferenceStoreBackupFile = this.getSharedPreferenceStoreImportFile();
            boolean isFirstTime = !sharedPreferenceStoreBackupFile.exists();
            if (isFirstTime)
                throw new Exception("Backup not found.");
            else {

                sharedPreferencesBackupStorage = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        Constants.SharedPreferenceStoreImportFile,
                        passwd,
                        this,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                if (sharedPreferencesBackupStorage.contains(Constants.PASSWD_PREFIX_TEST_KEY + passwd)
                        && sharedPreferencesBackupStorage.getBoolean(Constants.PASSWD_PREFIX_TEST_KEY + passwd, false)){
                    return sharedPreferencesBackupStorage;
                } else {
                    throw new Exception("Could not decrypt key.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public SharedPreferences getSharedPreferencesStorage(){
        return this.sharedPreferencesStorage;
    }

    public void logout(){
        this.sharedPreferencesStorage = null;
        this.stopIdleChecker();
    }

    public boolean isLogged(){
        return this.sharedPreferencesStorage != null;
    }

    public void startIdleChecker(){
        if (this.idleTimeChecker != null){
            this.idleTimeChecker.cancel(true);
            this.idleTimeChecker = null;
        }
        if (this.settingsPreference.getBoolean("enableLockTimeout", false)){
            this.idleTimeChecker = new IdleTimeChecker();
            this.idleTimeChecker.execute(this);
        }
    }

    public void stopIdleChecker(){
        if (this.idleTimeChecker != null){
            this.idleTimeChecker.cancel(true);
            this.idleTimeChecker = null;
        }
    }

    public void updateLastTimeInteraction(){
        this.lastTimeInteraction = System.currentTimeMillis();
    }

    public long getLastTimeInteraction(){
        return this.lastTimeInteraction;
    }

    public boolean isAppOnForeground(){
        return this.isAppOnForeground;
    }

    public SharedPreferences getSettingsPreference(){
        return this.settingsPreference;
    }

    public void updateCurrentActivitiyClassName(Activity activity){
        Log.d(LOG_TAG, "Current Activity: " + this.currentActivityClassName);
        this.currentActivityClassName = activity.getClass().getName();
    }

    public String getCurrentActivityClassName(){
        return this.currentActivityClassName;
    }

    public File getImageFolder() {
        return imageFolder;
    }

    public File getBackupFolder() {
        return backupFolder;
    }

    public File getTmpFolder() {
        return tmpFolder;
    }

    public File getSharedPreferenceStoreFile(){
        File sharedPreferenceStoreFile = new File(this.getDataDir().getAbsolutePath() +
                "/shared_prefs/" +
                Constants.SharedPreferenceStoreFile+".xml");
        return sharedPreferenceStoreFile;
    }

    public File getSharedPreferenceStoreImportFile(){
        File sharedPreferenceStoreFile = new File(this.getDataDir().getAbsolutePath() +
                "/shared_prefs/" +
                Constants.SharedPreferenceStoreImportFile+".xml");
        return sharedPreferenceStoreFile;
    }
}
