package org.ngyuen.otpvault.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.ngyuen.otpvault.BackupManager;
import org.ngyuen.otpvault.BuildConfig;
import org.ngyuen.otpvault.R;
import org.ngyuen.otpvault.activities.abstractclasses.AbstractActivity;
import org.ngyuen.otpvault.common.Callback;
import org.ngyuen.otpvault.common.Utils;

import java.io.File;

public class ImportActivity extends AbstractActivity implements TextWatcher, View.OnClickListener {
    public static final String IMPORT_FILE_PATH = "IMPORT_FILE_PATH";

    private File        importFile;
    private Button      cancelButton;
    private Button      importButton;
    private TextView    filePathTextView;
    private EditText    passwordText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this, true);
        setContentView(R.layout.import_layout);

        Intent intent = this.getIntent();
        this.importFile = new File(intent.getStringExtra(IMPORT_FILE_PATH));
        if (!this.importFile.exists())
            this.finish();

        cancelButton        = this.findViewById(R.id.cancel);
        importButton        = this.findViewById(R.id.importButton);
        filePathTextView    = this.findViewById(R.id.filePathTextView);
        passwordText        = this.findViewById(R.id.passwordText);

        importButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        passwordText.addTextChangedListener(this);

        filePathTextView.setText(importFile.getAbsolutePath());
        this.passwordTextChecker();
    }

    private void passwordTextChecker(){
        if (this.passwordText.getText().toString().isEmpty())
            this.importButton.setEnabled(false);
        else
            this.importButton.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finishAndRemoveTask();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.cancel: {
                this.finish();
                break;
            }
            case R.id.importButton: {
                BackupManager.importAsync(this,
                        this.importFile,
                        this.passwordText.getText().toString(),
                        new Callback() {
                            @Override
                            public void success(Object obj) {
                                Toast.makeText(ImportActivity.this,
                                        "Import is successful.",
                                        Toast.LENGTH_LONG).show();
                                ImportActivity.this.finish();
                            }

                            @Override
                            public void error(String errorMessage) {
                                Toast.makeText(ImportActivity.this,
                                        "Import failed. Error=" + errorMessage,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                break;
            }
            default:{
                break;
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        this.passwordTextChecker();
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    public static void openThis(Activity activity, File importFile){
        Intent intent = new Intent(activity, ImportActivity.class);
        intent.putExtra(ImportActivity.IMPORT_FILE_PATH, importFile.getAbsolutePath());
        activity.startActivity(intent);
    }
}
