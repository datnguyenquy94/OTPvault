package org.fedorahosted.freeotp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telecom.Call;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.fedorahosted.freeotp.BackupManager;
import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.activities.abstractclasses.AbstractActivity;
import org.fedorahosted.freeotp.activities.settings.SettingsActivity;
import org.fedorahosted.freeotp.common.Callback;
import org.fedorahosted.freeotp.common.Constants;
import org.fedorahosted.freeotp.common.Utils;
import org.fedorahosted.freeotp.views.ProgressDialogBuilder;

import java.lang.ref.WeakReference;

public class ChangePasswordActivity extends AbstractActivity implements TextWatcher, View.OnClickListener{

    private EditText    currentPassword;
    private EditText    newPassword;
    private EditText    repeatNewPassword;
    private Button      cancelButton;
    private Button      changePasswordButton;
    private CheckBox    backupBeforeChangeCheckbox;

    private ChangePasswordAsyncTask changePasswordAsyncTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this, true);
        setContentView(R.layout.change_password);

        this.currentPassword =              this.findViewById(R.id.currentPassword);
        this.newPassword =                  this.findViewById(R.id.newPassword);
        this.repeatNewPassword =            this.findViewById(R.id.repeatNewPassword);
        this.cancelButton =                 this.findViewById(R.id.cancel);
        this.changePasswordButton =         this.findViewById(R.id.changePassword);
        this.backupBeforeChangeCheckbox =   this.findViewById(R.id.backupBeforeChangeCheckbox);

        currentPassword.addTextChangedListener(this);
        newPassword.addTextChangedListener(this);
        repeatNewPassword.addTextChangedListener(this);
        cancelButton.setOnClickListener(this);
        changePasswordButton.setOnClickListener(this);

        this.passwordTextChecker();
    }

    private void passwordTextChecker(){
        String currentPassword = this.currentPassword.getText().toString();
        String newPassword = this.newPassword.getText().toString();
        String repeatNewPassword = this.repeatNewPassword.getText().toString();

        try {
            if (this.application.passwdVerify(currentPassword)
                && newPassword.compareTo(repeatNewPassword) == 0
                && newPassword.length() >= Constants.PASSWORD_MINIMUM_LENGTH ){
                this.changePasswordButton.setEnabled(true);
            } else {
                this.changePasswordButton.setEnabled(false);
            }
        } catch (Exception e){
            e.printStackTrace();
            this.changePasswordButton.setEnabled(false);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        this.passwordTextChecker();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.cancel: {
                this.finish();
                break;
            }
            case R.id.changePassword: {
                String currentPassword = this.currentPassword.getText().toString();
                String newPassword = this.newPassword.getText().toString();
                String repeatNewPassword = this.repeatNewPassword.getText().toString();
                boolean isBackupBeforeChange = this.backupBeforeChangeCheckbox.isChecked();

                if (newPassword.compareTo(repeatNewPassword) != 0){
                    Toast.makeText(this,
                            "Repeat password isn't match with your new password.",
                            Toast.LENGTH_LONG).show();
                    break;
                } else if (this.changePasswordAsyncTask != null){
                        Toast.makeText(this,
                                "Internal error, unsync task...",
                                Toast.LENGTH_LONG).show();
                        this.finish();
                } else {
                    this.disableForm();
                    this.changePasswordAsyncTask = new ChangePasswordAsyncTask(this,
                            currentPassword,
                            newPassword,
                            isBackupBeforeChange,
                            new Callback() {
                                @Override
                                public void success(Object obj) {
                                    Toast.makeText(ChangePasswordActivity.this,
                                            "Change password successful",
                                            Toast.LENGTH_LONG).show();
                                    ChangePasswordActivity.this.finish();
                                }

                                @Override
                                public void error(String errorMessage) {
                                    Toast.makeText(ChangePasswordActivity.this,
                                            errorMessage,
                                            Toast.LENGTH_LONG).show();
                                    ChangePasswordActivity.this.enableForm();
                                }
                    });
                    this.changePasswordAsyncTask.execute();
                    break;
                }
            }
            default:{
                break;
            }
        }
    }

    public void disableForm(){
        this.currentPassword.setEnabled(false);
        this.newPassword.setEnabled(false);
        this.repeatNewPassword.setEnabled(false);
        this.backupBeforeChangeCheckbox.setEnabled(false);
        this.cancelButton.setEnabled(false);
        this.changePasswordButton.setEnabled(false);
    }

    public void enableForm(){
        this.currentPassword.setEnabled(true);
        this.newPassword.setEnabled(true);
        this.repeatNewPassword.setEnabled(true);
        this.backupBeforeChangeCheckbox.setEnabled(true);
        this.cancelButton.setEnabled(true);
        this.passwordTextChecker();
    }

    public static class ChangePasswordAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<Activity> activityRef;
        private FreeOTPApplication application;
        private Callback callback;
        private AlertDialog dialog;

        private String currentPassword;
        private String newPassword;
        private boolean isBackupBeforeChange;
        private String errorMessage="";

        public ChangePasswordAsyncTask(Activity activity, String currentPassword, String newPassword, boolean isBackupBeforeChange, Callback callback){
            this.activityRef = new WeakReference<>(activity);
            this.application = (FreeOTPApplication) activity.getApplicationContext();
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
            this.isBackupBeforeChange = isBackupBeforeChange;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.dialog = ProgressDialogBuilder.build(this.activityRef.get(), "Changing password...");
            this.dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            //- Wait 1s before backup.
            try {
                Thread.sleep(1000);
                boolean result;
                if (this.isBackupBeforeChange){
                    result = BackupManager.backup((FreeOTPApplication) this.activityRef.get().getApplication(),
                            Constants.BEFORE_CHANGE_PASSWORD_BACKUP_SUFFIX_NAME);
                    if (!result)
                        throw new Exception("Unable to backup data before chanage password.");
                }

                this.application.changePassword(this.currentPassword, this.newPassword);
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                this.errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            this.dialog.dismiss();

            if (!result)
                callback.error("Change password failed. Error="+this.errorMessage);
            else
                callback.success(null);
        }
    }

    public static void openThis(Context context){
        Intent intent = new Intent(context, ChangePasswordActivity.class);
        context.startActivity(intent);
    }
}
