package org.fedorahosted.freeotp.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import org.fedorahosted.freeotp.BackupManager;
import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.activities.abstractclasses.AbstractAppCompatActivity;

import java.io.File;

import lib.folderpicker.FolderPicker;

@SuppressLint("ApplySharedPref")
public class SettingsActivity extends AbstractAppCompatActivity {

    private static final int SELECT_FOLDER_REQUEST_CODE = 99;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        this.settingsFragment = new SettingsFragment(this.application);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, this.settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FOLDER_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            try {
                File file = new File(data.getExtras().getString("data"));
                if (file.exists() && file.isDirectory())
                    this.settingsFragment.setBackupLocation(file.getAbsolutePath());
            } catch (Exception e){
                e.printStackTrace();
            }
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

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {

        private FreeOTPApplication application;

        private Preference backupNowPreference;
        private Preference importNowPreference;
        private SeekBarPreference lockTimeOutPreference;
        private Preference backupLocationPreference;

        public SettingsFragment(FreeOTPApplication application){
            this.application = application;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            this.lockTimeOutPreference = findPreference("lockTimeOut");
            this.backupLocationPreference = findPreference("backupLocation");
            this.backupNowPreference = findPreference("backupNow");
            this.importNowPreference = findPreference("importNow");

            if (this.backupLocationPreference.getSharedPreferences().getString(this.backupLocationPreference.getKey(), "").isEmpty())
                this.backupLocationPreference.getSharedPreferences().edit()
                        .putString(this.backupLocationPreference.getKey(), Environment.getExternalStorageDirectory().getPath())
                        .commit();

            this.backupNowPreference.setOnPreferenceClickListener(this);
            this.importNowPreference.setOnPreferenceClickListener(this);
            this.backupLocationPreference.setOnPreferenceClickListener(this);
            this.lockTimeOutPreference.setOnPreferenceChangeListener(this);

            this.updateLockTimeoutSummary(this.lockTimeOutPreference, this.lockTimeOutPreference.getValue());
            this.updateBackupLocationSummary();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            updateLockTimeoutSummary(lockTimeOutPreference, (int)newValue);
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getKey().compareTo(backupLocationPreference.getKey()) == 0) {
                Intent intent = new Intent(this.getActivity(), FolderPicker.class);
                this.getActivity().startActivityForResult(intent, SettingsActivity.SELECT_FOLDER_REQUEST_CODE);
                return true;
            } else if (preference.getKey().compareTo(backupNowPreference.getKey()) == 0) {
                BackupManager.backupAsync(this.getActivity());
                return true;
            } else {
                return false;
            }
        }

        public void setBackupLocation(String location){
            this.backupLocationPreference.getSharedPreferences()
                    .edit().putString(this.backupLocationPreference.getKey(), location).commit();
            this.updateBackupLocationSummary();
        }

        private void updateLockTimeoutSummary(SeekBarPreference lockTimeOutPreference, int value){
            lockTimeOutPreference.setSummary("Automatic lock in " + value + " seconds when idle.");
        }

        private void updateBackupLocationSummary(){
            this.backupLocationPreference.setSummary("Current: " +
                    this.backupLocationPreference.getSharedPreferences().getString(this.backupLocationPreference.getKey(), ""));
        }
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