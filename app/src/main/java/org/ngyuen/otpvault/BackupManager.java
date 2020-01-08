package org.ngyuen.otpvault;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import net.sqlcipher.database.SQLiteDatabase;

import org.ngyuen.otpvault.common.Callback;
import org.ngyuen.otpvault.common.Constants;
import org.ngyuen.otpvault.common.Utils;
import org.ngyuen.otpvault.storage.TokenPersistence;
import org.ngyuen.otpvault.views.ProgressDialogBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@SuppressLint("ApplySharedPref")
public class BackupManager {

    private static final String LOG_TAG = Utils.class.getName();

    public static boolean backup(OTPVaultApplication application, String outputSuffix){
        if (outputSuffix == null)
            outputSuffix = "";
        boolean backupFileWithTimeStamp = application.getSettingsPreference().getBoolean("backupFileWithTimeStamp", false);
        String backupFileName = application.getSettingsPreference().getString("backupFileName", "");
        String backupLocation = application.getSettingsPreference().getString("backupLocation", "");
        String backupOutputFile;

        if (backupFileName.isEmpty() || backupLocation.isEmpty())
            return false;
        if (backupFileWithTimeStamp)
            backupOutputFile = backupLocation + "/" + backupFileName + System.currentTimeMillis() + outputSuffix + ".zip";
        else
            backupOutputFile = backupLocation + "/" + backupFileName + outputSuffix + ".zip";

        File dbStorageFile = application.getDbStorageFile();
        List<String> files = new ArrayList<>();

        files.add(dbStorageFile.getAbsolutePath());

        return BackupManager.zip(files, backupOutputFile);
    }

    private static boolean zip(List<String> files, String zipFileOutput){
        int BUFFER_SIZE = 1024;
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFileOutput);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER_SIZE];

            for (String file: files) {
                Log.d(LOG_TAG, "Adding: " + file);
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);

                ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void importBackup(OTPVaultApplication application, File zipInputFile, String backupPasswd) throws Exception {
        try {
            //- Clean work folders before import.====================
            BackupManager.createFallbackBackup(application);
            Utils.clearFolder(application.getTmpFolder(), 0);
//            File dbStorageTemporaryFile = application.getDbStorageTemporaryFile();
//            if (dbStorageTemporaryFile.exists()) {
//                dbStorageTemporaryFile.delete();
//            }
            //- =====================================================

            boolean unzipResult = BackupManager.unzip(zipInputFile, application.getTmpFolder());
            if (!unzipResult)
                throw new Exception("Unable to open backup.");


            //- Get backup SharePreference from zip backup.
            File dbStorageBackupInputFile = new File(application.getTmpFolder(),
                    Constants.DbStorageFile);
            if (!dbStorageBackupInputFile.exists())
                throw new Exception("Backup data not found.");
//            Utils.copy(dbStorageBackupInputFile, dbStorageTemporaryFile);//- copy backupSharedPreferences to shared_prefs to read it.

            TokenPersistence tokenPersistence = application.getTokenPersistence();
            SQLiteDatabase backupSQLiteDatabase = application.openBackupTemporaryDatabaseFile(dbStorageBackupInputFile, backupPasswd);

            Token[] newTokens = tokenPersistence.getAll(backupSQLiteDatabase);
            backupSQLiteDatabase.close();
            for (Token newToken: newTokens){
                boolean isNewTokenExisted = false;

                //- Loop till it findout a new tokenId for newToken.
                //- Or a token with same data as newToken in app's data then it skip(import) this newToken.

                while(true){
                    Token token = tokenPersistence.exist(newToken.getIssuer(), newToken.getLabel());
                    if (token == null){//- Not exsit yet.
                        break;
                    } else if (token.equals(newToken)){//- This token already exsit in app's data.
                        isNewTokenExisted = true;
                        break;
                    } else {
                        isNewTokenExisted = false;
                        newToken.setLabel(newToken.getLabel()+Constants.CONFLICT_PREFIX);
                    }
                }

                if (isNewTokenExisted == false){//- The newToken isn't exist in app's data. So import it.
                    tokenPersistence.add(newToken);
                }
            }

            //- Clean work folders and data after import.================
//            if (dbStorageTemporaryFile.exists())
//                dbStorageTemporaryFile.delete();
            Utils.clearFolder(application.getTmpFolder(), 0);
            BackupManager.clearFallbackBackup(application);
            //- =========================================================
        } catch(Exception e){
            e.printStackTrace();
            String message = e.getMessage();
            try {
                Log.d(LOG_TAG, "Import backup failed, trying to restore data to the last state...");
                BackupManager.restoreFallbackBackup(application);
            } catch(Exception ee){
                Log.d(LOG_TAG, "Restore data failed.");
                ee.printStackTrace();
                message = " " + ee.getMessage();
            }
            throw new Exception(message);
        }
    }

    //- In case backup fail. Use this to restore app to last state.
    private static void createFallbackBackup(OTPVaultApplication application) throws IOException {
        Utils.clearFolder(application.getBackupFolder(), 0);
        File src;
        File dst;

        src = application.getDbStorageFile();
        dst = new File(application.getBackupFolder(), src.getName());
        Utils.copy(src, dst);
    }

    private static void restoreFallbackBackup(OTPVaultApplication application) throws IOException {
        File src;
        File dst;

        dst = application.getDbStorageFile();
        src = new File(application.getBackupFolder(), dst.getName());
        Utils.copy(src, dst);

        Utils.clearFolder(application.getBackupFolder(), 0);
    }

    private static void clearFallbackBackup(OTPVaultApplication application){
        Utils.clearFolder(application.getBackupFolder(), 0);
    }

    private static boolean unzip(File zipFileInput, File outputFolder){
        int size;
        int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            if (!outputFolder.isDirectory() || !outputFolder.exists()) {
                outputFolder.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFileInput), BUFFER_SIZE));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    File unzipFile = new File(outputFolder, ze.getName());

                    if (!ze.isDirectory()) {
                        // unzip the file
                        FileOutputStream out = new FileOutputStream(unzipFile, false);
                        BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                        try {
                            while ((size = zin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                                fout.write(buffer, 0, size);
                            }
                            zin.closeEntry();
                        } finally {
                            fout.flush();
                            fout.close();
                            out.close();
                        }
                    }
                }
            } finally {
                zin.close();
            }
            return true;
        } catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static void backupAsync(Activity activity, Callback callback){
        BackupAsyncTask backupAsyncTask = new BackupAsyncTask(activity, callback);
        backupAsyncTask.execute();
    }

    public static void importAsync(Activity activity, File zipInputFile, String password, Callback callback){
        ImportAsyncTask importAsyncTask = new ImportAsyncTask(activity, zipInputFile, password, callback);
        importAsyncTask.execute();
    }

    public static class BackupAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<Activity> activityRef;
        private Callback callback;
        private AlertDialog dialog;

        public BackupAsyncTask(Activity activity, Callback callback){
            this.activityRef = new WeakReference<>(activity);
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.dialog = ProgressDialogBuilder.build(this.activityRef.get(), "Backup...");
            this.dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            //- Wait 1s before backup.
            try {
                Thread.sleep(1000);
                return BackupManager.backup((OTPVaultApplication) this.activityRef.get().getApplication(),
                        Constants.MANUALLY_BACKUP_SUFFIX_NAME);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            this.dialog.dismiss();

            if (!result)
                callback.error("Backup failed.");
            else
                callback.success(null);
        }
    }

    public static class ImportAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<Activity> activityRef;
        private Callback callback;
        private File zipInputFile;
        private String password;
        private AlertDialog dialog;

        private String exceptMessage = "";

        public ImportAsyncTask(Activity activity, File zipInputFile, String password, Callback callback){
            this.activityRef = new WeakReference<>(activity);
            this.zipInputFile = zipInputFile;
            this.password = password;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.dialog = ProgressDialogBuilder.build(this.activityRef.get(), "Importing...");
            this.dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            //- Wait 1s before backup.
            try {
                Thread.sleep(1000);
                BackupManager.importBackup((OTPVaultApplication) this.activityRef.get().getApplication(),
                        this.zipInputFile, this.password);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                this.exceptMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            this.dialog.dismiss();

            if (!result)
                this.callback.error(this.exceptMessage);
            else
                this.callback.success(null);

        }
    }

}
