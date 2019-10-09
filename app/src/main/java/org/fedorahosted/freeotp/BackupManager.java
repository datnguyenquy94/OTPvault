package org.fedorahosted.freeotp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.fedorahosted.freeotp.common.Constants;
import org.fedorahosted.freeotp.common.Utils;
import org.fedorahosted.freeotp.storage.TokenPersistence;
import org.fedorahosted.freeotp.views.ProgressDialogBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    private static final String LOG_TAG = Utils.class.getName();

    private static boolean backup(FreeOTPApplication application){
        boolean backupFileWithTimeStamp = application.getSettingsPreference().getBoolean("backupFileWithTimeStamp", false);
        String backupFileName = application.getSettingsPreference().getString("backupFileName", "");
        String backupLocation = application.getSettingsPreference().getString("backupLocation", "");
        String backupOutputFile;

        if (backupFileName.isEmpty() || backupLocation.isEmpty())
            return false;
        if (backupFileWithTimeStamp)
            backupOutputFile = backupLocation + "/" + backupFileName + System.currentTimeMillis() + ".zip";
        else
            backupOutputFile = backupLocation + "/" + backupFileName + ".zip";

        File[] images = application.getImageFolder().listFiles();
        File sharedPreferenceStoreFile = application.getSharedPreferenceStoreFile();
        List<String> files = new ArrayList<>();

        for (File e: images){
            files.add(e.getAbsolutePath());
        }
        files.add(sharedPreferenceStoreFile.getAbsolutePath());

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

    private static boolean importBackup(FreeOTPApplication application, String zipFileInput, String backupPasswd){
        try {
            BackupManager.createFallbackBackup(application);
            Utils.clearFolder(application.getTmpFolder(), 0);

            boolean unzipResult = BackupManager.unzip(zipFileInput, application.getTmpFolder());
            if (!unzipResult)
                throw new Exception("Unable to unzip backup.");

            File sharedPreferenceStoreImportFile = application.getSharedPreferenceStoreImportFile();
            if (sharedPreferenceStoreImportFile.exists())
                sharedPreferenceStoreImportFile.delete();



            //- Get backup SharePreference from zip backup.
            File sharedPreferenceStoreImportFileInput = new File(application.getTmpFolder(),
                    Constants.SharedPreferenceStoreFile+".xml");
            Utils.copy(sharedPreferenceStoreImportFileInput, sharedPreferenceStoreImportFile);//- copy backupSharedPreferences to shared_prefs to read it.

            TokenPersistence tokenPersistence = application.getTokenPersistence();
            List<String> tokenIndex=null;
            Token importToken;//- From backup files
            Token token;//- From app.
            String str;//- tmp string.
            SharedPreferences backupSharedPreferences = application.loginImportFile(backupPasswd);
            str = backupSharedPreferences.getString(Constants.LST_TOKENS, "");
            if (!str.isEmpty())
                tokenIndex = new ArrayList<>(Arrays.asList(str.split(",")));

            for (String id: tokenIndex){
                str = backupSharedPreferences.getString(id, null);
                importToken = application.getGson().fromJson(str, Token.class);
                if (tokenPersistence.tokenExists(importToken.getID())){//- token alread exsit on
                    continue;
                } else {
                    tokenPersistence.add(importToken);
                    File importTokenImage = new File(application.getTmpFolder(), importToken.getImageFileName());
                    if (importTokenImage.exists()){
                        File importTokenImageNewLocation = new File(application.getImageFolder(), importToken.getImageFileName());
                        Utils.copy(importTokenImage, importTokenImageNewLocation);
                    }
                }
            }



            if (sharedPreferenceStoreImportFile.exists())
                sharedPreferenceStoreImportFile.delete();
            Utils.clearFolder(application.getTmpFolder(), 0);
            BackupManager.clearFallbackBackup(application);
            return true;
        } catch(Exception e){
            e.printStackTrace();
            try {
                BackupManager.restoreFallbackBackup(application);
            } catch(Exception ee){ ee.printStackTrace();Log.d(LOG_TAG, "Unable to restore fallback's backup."); }
            return false;
        }
    }

    //- In case backup fail. Use this to restore app to last state.
    private static boolean createFallbackBackup(FreeOTPApplication application) throws IOException {
        Utils.clearFolder(application.getBackupFolder(), 0);
        File src;
        File dst;

        src = application.getSharedPreferenceStoreFile();
        dst = new File(application.getBackupFolder(), src.getName());
        Utils.copy(src, dst);

        File[] images = application.getImageFolder().listFiles();
        for (File image: images){
            src = image;
            dst = new File(application.getBackupFolder(), src.getName());
            Utils.copy(src, dst);
        }
        return true;
    }

    private static boolean restoreFallbackBackup(FreeOTPApplication application) throws IOException {
        Utils.clearFolder(application.getImageFolder(), 0);
        File src;
        File dst;

        dst = application.getSharedPreferenceStoreFile();
        src = new File(application.getBackupFolder(), dst.getName());
        Utils.copy(src, dst);

        File[] images = application.getBackupFolder().listFiles();
        for (File image: images){
            if (image.getName().compareTo(application.getSharedPreferenceStoreFile().getName()) != 0){//- Isn't sharedpreference file.
                src = image;
                dst = new File(application.getImageFolder(), src.getName());
                Utils.copy(src, dst);
            }
        }

        Utils.clearFolder(application.getBackupFolder(), 0);
        return true;
    }

    private static boolean clearFallbackBackup(FreeOTPApplication application){
        Utils.clearFolder(application.getBackupFolder(), 0);
        return true;
    }

    private static boolean unzip(String zipFileInput, File outputFolder){
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
                    String path = outputFolder.getAbsolutePath() + ze.getName();
                    File unzipFile = new File(path);

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

    public static void backupAsync(Activity activity){
        BackupAsyncTask backupAsyncTask = new BackupAsyncTask(activity);
        backupAsyncTask.execute();
    }

    public static class BackupAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private Activity activity;
        private AlertDialog dialog;

        public BackupAsyncTask(Activity activity){
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.dialog = ProgressDialogBuilder.build(activity, "Backup...");
            this.dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return BackupManager.backup((FreeOTPApplication) this.activity.getApplication());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            String resultMessage;

            if (!result)
                resultMessage = "Backup is failed.";
            else
                resultMessage = "Backup is successed.";

            Toast.makeText(this.activity,
                    resultMessage, Toast.LENGTH_LONG).show();

            this.dialog.dismiss();
        }
    }

}
