package org.ngyuen.otpvault.activities.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import org.ngyuen.otpvault.R;

@SuppressLint("ApplySharedPref")
public class SettingsFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private SettingsActivity settingsActivity;

    private SeekBarPreference   lockTimeOutPreference;
    private Preference          backupNowPreference;
    private Preference          importNowPreference;
    private Preference          backupLocationPreference;
    private Preference          changePasswordPreference;

    public SettingsFragment(SettingsActivity parent) throws Exception {
        this.settingsActivity = parent;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        this.lockTimeOutPreference = findPreference("lockTimeOut");
        this.backupLocationPreference = findPreference("backupLocation");
        this.backupNowPreference = findPreference("backupNow");
        this.importNowPreference = findPreference("importNow");
        this.changePasswordPreference = findPreference("changePassword");

        if (this.backupLocationPreference.getSharedPreferences().getString(this.backupLocationPreference.getKey(), "").isEmpty())
            this.backupLocationPreference.getSharedPreferences().edit()
                    .putString(this.backupLocationPreference.getKey(), Environment.getExternalStorageDirectory().getPath())
                    .commit();

        this.backupNowPreference.setOnPreferenceClickListener(this);
        this.importNowPreference.setOnPreferenceClickListener(this);
        this.backupLocationPreference.setOnPreferenceClickListener(this);
        this.changePasswordPreference.setOnPreferenceClickListener(this);

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
            return this.settingsActivity.requestToPickBackupFolder();
        } else if (preference.getKey().compareTo(backupNowPreference.getKey()) == 0) {
            return this.settingsActivity.backupToken();
        } else if (preference.getKey().compareTo(importNowPreference.getKey()) == 0) {
            return this.settingsActivity.requestToPickImportFile();
        } else if (preference.getKey().compareTo(changePasswordPreference.getKey()) == 0) {
            return this.settingsActivity.requestToChangeAppPassword();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.settingsActivity = null;
    }
}