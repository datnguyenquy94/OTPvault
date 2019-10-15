package org.fedorahosted.freeotp.activities.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.fedorahosted.freeotp.BackupManager;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.activities.ImportActivity;
import org.fedorahosted.freeotp.activities.LoginActivity;
import org.fedorahosted.freeotp.activities.abstractclasses.AbstractAppCompatActivity;
import org.fedorahosted.freeotp.common.Callback;

import java.io.File;

@SuppressLint("ApplySharedPref")
public class SettingsActivity extends AbstractAppCompatActivity {

    private String LOG_TAG = this.getClass().getName();

    private static final int SELECT_FOLDER_REQUEST_CODE = 99;
    private static final int SELECT_IMPORT_FILE_REQUEST_CODE = 98;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.settings_activity);
            this.settingsFragment = new SettingsFragment(this);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, this.settingsFragment)
                    .commit();
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!this.application.isLogged()){
            this.finish();
            LoginActivity.openThis(this);
        }
    }

    private void setBackupLocation(File backupLocation){
        if (backupLocation.exists() && backupLocation.isDirectory())
            this.settingsFragment.setBackupLocation(backupLocation.getAbsolutePath());
    }

    private void openImportDialog(File backupLocation){
        ImportActivity.openThis(this, backupLocation);
    }

    public boolean requestToPickBackupFolder(){
        new ChooserDialog(this)
                .withFilter(true, false)
                .withStartFile(Environment.getExternalStorageDirectory().getAbsolutePath())
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        if (pathFile.exists() && pathFile.isDirectory()){
                            SettingsActivity.this.setBackupLocation(pathFile);
                        }
                    }
                })
                .build()
                .show();
        return true;
    }

    public boolean requestToPickImportFile(){
        new ChooserDialog(this)
                .withFilter(false, false)
                .withStartFile(Environment.getExternalStorageDirectory().getAbsolutePath())
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        if (pathFile.exists() && pathFile.isFile()){
                            SettingsActivity.this.openImportDialog(pathFile);
                        }
                    }
                })
                .build()
                .show();
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("application/zip");
//        startActivityForResult(intent, SettingsActivity.SELECT_IMPORT_FILE_REQUEST_CODE);
        return true;
    }

    public boolean backupToken(){
        BackupManager.backupAsync(this, new Callback() {
            @Override
            public void success(Object obj) {
                Toast.makeText(SettingsActivity.this,
                        "Backup is successful",
                        Toast.LENGTH_LONG)
                        .show();
            }
            @Override
            public void error(String errorMessage) {
                Toast.makeText(SettingsActivity.this,
                        "Backup failed. Error="+errorMessage,
                        Toast.LENGTH_LONG)
                        .show();
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public static void openThis(Context context){
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }
}