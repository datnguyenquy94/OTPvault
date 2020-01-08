package org.ngyuen.otpvault;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.preference.PreferenceManager;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.ngyuen.otpvault.activities.ChangePasswordActivity;
import org.ngyuen.otpvault.activities.LoginActivity;
import org.ngyuen.otpvault.activities.add.AddActivity;
import org.ngyuen.otpvault.activities.edit.EditActivity;
import org.ngyuen.otpvault.asynctaskes.IdleWatcher;
import org.ngyuen.otpvault.common.Constants;
import org.ngyuen.otpvault.common.Utils;
import org.ngyuen.otpvault.storage.TokenDbHelper;
import org.ngyuen.otpvault.storage.TokenPersistence;

import java.io.File;

@SuppressLint("ApplySharedPref")
public class FreeOTPApplication extends Application implements LifecycleObserver {

    private static final String GET_PASSWD_SQL = "select passwd from passwd;";
    private static final String[] SAVE_PASSWD_SQL = new String[] {
            "DROP TABLE IF EXISTS 'passwd';",
            "CREATE TABLE 'passwd' ('id' INTEGER PRIMARY KEY CHECK (id = 0),'passwd' TEXT);",
            "INSERT INTO 'passwd'('id', 'passwd')VALUES (0, ?);"
    };

    private String LOG_TAG = this.getClass().getName();

//    private EncryptedSharedPreferences  sharedPreferencesStorage = null;
    private SQLiteDatabase              dbStorage;
    private SharedPreferences           settingsPreference;
    private TokenPersistence            tp = null;
    private IdleWatcher                 idleWatcher;
//    private IdleWatcher                 idleWatcher;
    private boolean                     isAppOnForeground = false;
    private long                        lastTimeInteraction = 0;
    private String                      currentActivityClassName = "";
    private File                        backupFolder;
    private File                        tmpFolder;
    private File                        databaseFolder;
    private Gson                        gson;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.settingsPreference = PreferenceManager.getDefaultSharedPreferences(this);
        this.gson = new Gson();

        this.backupFolder = new File(this.getFilesDir(), Constants.BACKUP_FOLDER);
        this.tmpFolder = new File(this.getFilesDir(), Constants.TMP_FOLDER);
        this.databaseFolder = new File(this.getFilesDir(), Constants.DATABASE_FOLDER);
        this.backupFolder.mkdirs();
        this.tmpFolder.mkdirs();
        this.databaseFolder.mkdirs();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        Log.d(LOG_TAG, "onAppBackgrounded");
        this.isAppOnForeground = false;
        //- Lock on Background only if setting allow it.
        //- And current activity isn't EditActivity, AddActivity, ChangePasswordActivity
        //- (In case user change token's image. EditActivity, AddActivity. It will request it on another app. So it need to be allow run on background.).
        //- (Not logout if user are changing password. Avoid confiled or any god know what it is... )
        if (settingsPreference.getBoolean("lockOnBackground", false) &&
            this.currentActivityClassName.compareTo(EditActivity.class.getName()) != 0 &&
            this.currentActivityClassName.compareTo(AddActivity.class.getName()) != 0 &&
            this.currentActivityClassName.compareTo(ChangePasswordActivity.class.getName()) != 0){
            this.logout();
        }
    }

    public void onScreenTurnedOff(){
        //- Not logout if user are changing password. Avoid confiled or any god know what it is...
        if (this.getSettingsPreference().getBoolean("lockOnScreenOff", false) &&
            this.currentActivityClassName.compareTo(ChangePasswordActivity.class.getName()) != 0){
            this.logout();
        }
    }

    public void onIdleTimeout(){
        if (this.isLogged() &&
            this.getSettingsPreference().getBoolean("enableLockTimeout", false)){
            this.logout();
            if (this.isAppOnForeground()){
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);
            }
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

    public SQLiteDatabase createPasswordFirstTime(String passwd) throws Exception {
        try {
            if (this.isEncryptStorageCreated())
                throw new Exception("Not the first time");
            else {
                this.dbStorage = TokenDbHelper.getInstance(this).getWritableDatabase(passwd);

                if (this.dbStorage == null){
                    throw new Exception("Can't get database.");
                } else {
//                    this.dbStorage.execSQL("SELECT count(*) FROM sqlite_master;");//- Test password.
                    for (String sql: SAVE_PASSWD_SQL){//- Test, also save current passwd.
                        if (sql.contains("?"))
                            this.dbStorage.execSQL(sql, new String[] { passwd });
                        else
                            this.dbStorage.execSQL(sql);
                    }

                    return this.dbStorage;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.dbStorage = null;
            throw e;
        }
//        try {
//            File sharedPreferenceStoreFile = this.getSharedPreferenceStoreFile();
//            boolean isFirstTime = !sharedPreferenceStoreFile.exists();
//            if (!isFirstTime)
//                throw new Exception("Not the first time");
//            else {
//                this.sharedPreferencesStorage = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
//                        Constants.SharedPreferenceStoreFile,
//                        passwd,
//                        this,
//                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//                );
//                this.sharedPreferencesStorage.edit().putBoolean(Constants.PASSWD_PREFIX_TEST_KEY + passwd, true).commit();
////                boolean result = this.sharedPreferencesStorage.contains(Constants.PASSWD_PREFIX_TEST_KEY + passwd);
////                boolean result2 = this.sharedPreferencesStorage.getBoolean(Constants.PASSWD_PREFIX_TEST_KEY + passwd, false);
//                return this.sharedPreferencesStorage;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            this.sharedPreferencesStorage = null;
//            throw e;
//        }
    }

    public boolean isEncryptStorageCreated(){
        File dbStorageFile = this.getDbStorageFile();
        return dbStorageFile.exists();
//        File sharedPreferenceStoreFile = this.getSharedPreferenceStoreFile();
//        return sharedPreferenceStoreFile.exists();
    }

    public boolean login(String passwd) throws Exception {
        try {
            if (this.isEncryptStorageCreated() == false)//- Db hadn't been created yet.
                throw new Exception("Data and password isn't created or exist yet.");
            else {
                this.dbStorage = TokenDbHelper.getInstance(this).getWritableDatabase(passwd);

                if (this.dbStorage == null){
                    throw new Exception("Can't get database.");
                } else {
//                    this.dbStorage.execSQL("SELECT count(*) FROM sqlite_master;");//- Test password.
                    return this.passwdVerify(passwd);//- Test passwd.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.dbStorage = null;
            throw e;
        }

//        try {
//            File sharedPreferenceStoreFile = this.getSharedPreferenceStoreFile();
//            boolean isFirstTime = !sharedPreferenceStoreFile.exists();
//            if (isFirstTime)
//                throw new Exception("Data and password not exsit yet.");
//            else {
//                this.sharedPreferencesStorage = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
//                        Constants.SharedPreferenceStoreFile,
//                        passwd,
//                        this,
//                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//                );
//
//                if (this.sharedPreferencesStorage.contains(Constants.PASSWD_PREFIX_TEST_KEY + passwd)
//                    && this.sharedPreferencesStorage.getBoolean(Constants.PASSWD_PREFIX_TEST_KEY + passwd, false)){
//                    return true;
//                } else {
//                    throw new Exception("Could not decrypt key.");
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            this.sharedPreferencesStorage = null;
//            return false;
//        }
    }

    //- Test input string is the current password or not.
    public boolean passwdVerify(String passwd) throws Exception {
        if (!this.isLogged())
            throw new Exception("Not login yet.");

        if (this.dbStorage == null)
            return false;

        try {
            Cursor cursor = this.dbStorage.rawQuery(GET_PASSWD_SQL, null);
            if (cursor.getCount() < 1)
                throw new Exception("Password not found in database.");
            else if (cursor.getCount() > 1)
                throw new Exception("There are more than 1 password in database.");
            else {
                cursor.moveToFirst();
                String pwd = cursor.getString(0);
                cursor.close();
                return passwd.compareTo(pwd) == 0;
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //- Test input string is the current password or not.
    public boolean passwdVerify(SQLiteDatabase sqLiteDatabase, String passwd) throws Exception {
        if (sqLiteDatabase == null)
            return false;

        try {
            Cursor cursor = sqLiteDatabase.rawQuery(GET_PASSWD_SQL, null);
            if (cursor.getCount() < 1)
                throw new Exception("Password not found in database.");
            else if (cursor.getCount() > 1)
                throw new Exception("There are more than 1 password in database.");
            else {
                cursor.moveToFirst();
                return passwd.compareTo(cursor.getString(0)) == 0;
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public void changePassword(String currentPassword, String newPassword) throws Exception {
        if (!this.isLogged())
            throw new Exception("Not login yet.");

        if (!this.isEncryptStorageCreated())
            throw new Exception("Data and password not exsit yet.");

        if (!this.passwdVerify(currentPassword))
            throw new Exception("Curent password is incorrect.");

        File dbStorageFile = this.getDbStorageFile();
        File dbStorageTemporaryFile = new File(this.getTmpFolder(), dbStorageFile.getName());
        if (dbStorageTemporaryFile.exists())
            dbStorageTemporaryFile.delete();

        try {
            this.logout();
            Utils.copy(dbStorageFile, dbStorageTemporaryFile);
            this.login(currentPassword);

            this.dbStorage.changePassword(newPassword);
            for (String sql: SAVE_PASSWD_SQL){//- Test, also save the new passwd.
                if (sql.contains("?"))
                    this.dbStorage.execSQL(sql, new String[] { newPassword });
                else
                    this.dbStorage.execSQL(sql);
            }

        } catch (Exception e){
            e.printStackTrace();
            //- fallback.
            this.logout();
            Utils.copy(dbStorageTemporaryFile, dbStorageFile);
            throw e;
        } finally {
            if (dbStorageTemporaryFile.exists())
                dbStorageTemporaryFile.delete();

            this.logout();
        }

    }

    public SQLiteDatabase openBackupTemporaryDatabaseFile(File dbStorageBackupFile, String passwd) throws Exception {
        boolean isFirstTime = !dbStorageBackupFile.exists();
        if (isFirstTime)
            throw new Exception("Backup not found.");
        else {
            try {

                SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(
                        dbStorageBackupFile.getAbsolutePath(), passwd, null, SQLiteDatabase.OPEN_READONLY
                );

                if (!this.passwdVerify(sqLiteDatabase, passwd))
                    throw new Exception("Password is incorrect");

                Cursor cursor = sqLiteDatabase.rawQuery("PRAGMA user_version;", null);
                if (cursor.getCount() <= 0)
                    throw new Exception("Cant get database's version number.");
                cursor.moveToFirst();
                int databaseVersion = cursor.getInt(0);
                if (databaseVersion != TokenDbHelper.DATABASE_VERSION)
                    throw new Exception("Backup database version isn't equals with current database");

                if(cursor.isClosed() == false)
                    cursor.close();

                return sqLiteDatabase;
            } catch(Exception e){
                e.printStackTrace();
                throw new Exception("Could not decrypt or open backup database. Error="+e.getMessage());
            }

        }
    }

//    public SharedPreferences getSharedPreferencesStorage(){
//        return this.sharedPreferencesStorage;
//    }


    public SQLiteDatabase getDbStorage(){
        return this.dbStorage;
    }

    public void logout(){
//        this.sharedPreferencesStorage = null;
        if (this.dbStorage != null){
            this.dbStorage.close();
            this.dbStorage = null;
        }
        this.stopIdleChecker();
    }

    public boolean isLogged(){
        return this.dbStorage != null && this.dbStorage.isOpen();
    }

    public void startIdleChecker(){
        if (this.idleWatcher != null){
            this.idleWatcher.cancel(true);
            this.idleWatcher = null;
        }
        if (this.settingsPreference.getBoolean("enableLockTimeout", false)){
            this.idleWatcher = new IdleWatcher();
            this.idleWatcher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
        }
    }

    public void stopIdleChecker(){
        if (this.idleWatcher != null){
            this.idleWatcher.cancel(true);
            this.idleWatcher = null;
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

    public File getBackupFolder() {
        return backupFolder;
    }

    public File getTmpFolder() {
        return tmpFolder;
    }

    public File getDatabaseFolder() {
        return databaseFolder;
    }

    public File getDbStorageFile() {
        return new File(this.getDatabaseFolder(), Constants.DbStorageFile);
    }

//    public File getDbStorageTemporaryFile() {
//        return new File(this.getDatabaseFolder(), Constants.DbStorageTemporaryFile);
//    }

//    public File getSharedPreferenceStoreFile(){
//        File sharedPreferenceStoreFile = new File(this.getDataDir().getAbsolutePath() +
//                "/shared_prefs/" +
//                Constants.SharedPreferenceStoreFile+".xml");
//        return sharedPreferenceStoreFile;
//    }
//
//    public File getSharedPreferenceTemporaryFile(){
//        File sharedPreferenceStoreFile = new File(this.getDataDir().getAbsolutePath() +
//                "/shared_prefs/" +
//                Constants.SharedPreferenceTemporary+".xml");
//        return sharedPreferenceStoreFile;
//    }
//
//    public void deleteSharedPreferenceFile(File file) throws Exception {
//        if  (!file.exists())
//            throw new Exception("File isn't exist");
//
//        String sharedPreferenceFolderAbsolutePath = this.getDataDir().getAbsolutePath() + "/shared_prefs";
//        String filePath = file.getAbsolutePath();
//        String fileName = file.getName();
//        if (filePath.indexOf(sharedPreferenceFolderAbsolutePath) != 0 ||
//            fileName.lastIndexOf(".xml") != fileName.length() - 4 )
//            throw new Exception("This file isn't SharedPreferenceFile");
//
//        fileName = fileName.substring(0, fileName.lastIndexOf(".xml"));
//
//        this.getSharedPreferences(fileName, Context.MODE_PRIVATE)
//            .edit().clear().commit();
//
//        file.delete();
//
//        if  (file.exists())
//            throw new Exception("Can't delete SharedPreferenceFile="+fileName);
//    }
}
