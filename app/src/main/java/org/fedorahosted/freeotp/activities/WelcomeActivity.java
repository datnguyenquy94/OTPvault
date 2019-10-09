package org.fedorahosted.freeotp.activities;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.activities.MainActivity;

import java.io.File;

public class WelcomeActivity extends Activity implements View.OnClickListener, TextWatcher{

    private RelativeLayout  welcomeLayout1;
    private RelativeLayout  welcomeLayout2;
    private RelativeLayout  welcomeLayout3;
    private RelativeLayout  welcomeLayout4;
    private RelativeLayout  welcomeLayout5;

    private Button          nextButton1;
    private Button          nextButton2;
    private Button          nextButton3;
    private Button          nextButton4;

    private Button          previousButton1;
    private Button          previousButton2;
    private Button          previousButton3;
    private Button          previousButton4;

    private EditText        passwordText;
    private EditText        passwordTextRepeat;

    private ImageView       welcomeDoneImage;

    private FreeOTPApplication application;

    //- For read and write backup/restore files.
    private String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private final int PERMISSION_REQUEST_CODE = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        this.application = (FreeOTPApplication) this.getApplication();

        this.welcomeLayout1 = this.findViewById(R.id.welcome1);
        this.welcomeLayout2 = this.findViewById(R.id.welcome2);
        this.welcomeLayout3 = this.findViewById(R.id.welcome3);
        this.welcomeLayout4 = this.findViewById(R.id.welcome4);
        this.welcomeLayout5 = this.findViewById(R.id.welcome5);

        this.nextButton1 = this.findViewById(R.id.welcome_next_button_1);
        this.nextButton2 = this.findViewById(R.id.welcome_next_button_2);
        this.nextButton3 = this.findViewById(R.id.welcome_next_button_3);
        this.nextButton4 = this.findViewById(R.id.welcome_next_button_4);

        this.previousButton1 = this.findViewById(R.id.welcome_previous_button_1);
        this.previousButton2 = this.findViewById(R.id.welcome_previous_button_2);
        this.previousButton3 = this.findViewById(R.id.welcome_previous_button_3);
        this.previousButton4 = this.findViewById(R.id.welcome_previous_button_4);

        this.passwordText = this.findViewById(R.id.passwordText);
        this.passwordTextRepeat = this.findViewById(R.id.passwordTextRepeat);

        this.welcomeDoneImage = this.findViewById(R.id.welcome_done_image_view);

        this.nextButton1.setOnClickListener(this);
        this.nextButton2.setOnClickListener(this);
        this.nextButton3.setOnClickListener(this);
        this.nextButton4.setOnClickListener(this);
        this.previousButton1.setOnClickListener(this);
        this.previousButton2.setOnClickListener(this);
        this.previousButton3.setOnClickListener(this);
        this.previousButton4.setOnClickListener(this);
        this.welcomeDoneImage.setOnClickListener(this);

        this.passwordText.addTextChangedListener(this);
        this.passwordTextRepeat.addTextChangedListener(this);

        //- If encrypt storage is created, this mean user had been went though this welcome screen and created their password.
        //- We can to next screen.
        if (this.application.isEncryptStorageCreated() == false) {
            this.gotoScreen(1);
        } else {
            LoginActivity.openThis(this);
            this.finish();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == this.PERMISSION_REQUEST_CODE) {
            int i = 0;
            for (i = 0; i<grantResults.length; i++)
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    break;
            if (i>=grantResults.length)//- all permissions are granted.
                this.nextButton3.performClick();//- perform click on nextButton3 to move to next screen.
        }
    }


    private void gotoScreen(int screen){
        switch (screen){
            case 1: {
                this.welcomeLayout1.setVisibility(View.VISIBLE);
                this.welcomeLayout2.setVisibility(View.GONE);
                this.welcomeLayout3.setVisibility(View.GONE);
                this.welcomeLayout4.setVisibility(View.GONE);
                this.welcomeLayout5.setVisibility(View.GONE);
                break;
            }
            case 2: {
                this.welcomeLayout1.setVisibility(View.GONE);
                this.welcomeLayout2.setVisibility(View.VISIBLE);
                this.welcomeLayout3.setVisibility(View.GONE);
                this.welcomeLayout4.setVisibility(View.GONE);
                this.welcomeLayout5.setVisibility(View.GONE);
                break;
            }
            case 3: {
                this.welcomeLayout1.setVisibility(View.GONE);
                this.welcomeLayout2.setVisibility(View.GONE);
                this.welcomeLayout3.setVisibility(View.VISIBLE);
                this.welcomeLayout4.setVisibility(View.GONE);
                this.welcomeLayout5.setVisibility(View.GONE);
                break;
            }
            case 4: {
                this.welcomeLayout1.setVisibility(View.GONE);
                this.welcomeLayout2.setVisibility(View.GONE);
                this.welcomeLayout3.setVisibility(View.GONE);
                this.welcomeLayout4.setVisibility(View.VISIBLE);
                this.welcomeLayout5.setVisibility(View.GONE);
                break;
            }
            case 5: {
                this.welcomeLayout1.setVisibility(View.GONE);
                this.welcomeLayout2.setVisibility(View.GONE);
                this.welcomeLayout3.setVisibility(View.GONE);
                this.welcomeLayout4.setVisibility(View.GONE);
                this.welcomeLayout5.setVisibility(View.VISIBLE);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.welcome_next_button_1: {
                this.gotoScreen(2);
                break;
            }
            case R.id.welcome_next_button_2: {
                this.passwordInputChecker();
                if (this.isRequiredPermissionGranted())//- permission already granted
                    this.gotoScreen(4);
                else//- need to grant permission, move to welcome screen 3.
                    this.gotoScreen(3);
                break;
            }
            case R.id.welcome_next_button_3: {
                this.passwordInputChecker();
                if (this.isRequiredPermissionGranted()){//- Move to next screen if permission already granted
                    this.gotoScreen(4);
                } else {//- else request those permissions.
                    ActivityCompat.requestPermissions(this,
                            this.requiredPermissions,
                            this.PERMISSION_REQUEST_CODE);
                }
                break;
            }
            case R.id.welcome_next_button_4: {
                String passwd = this.passwordText.getText().toString();
                this.application.createPasswordFirstTime(passwd);
                this.gotoScreen(5);
                break;
            }
            case R.id.welcome_previous_button_1: {
                break;
            }
            case R.id.welcome_previous_button_2: {
                this.gotoScreen(1);
                break;
            }
            case R.id.welcome_previous_button_3: {
                this.gotoScreen(2);
                break;
            }
            case R.id.welcome_previous_button_4: {
                if (this.isRequiredPermissionGranted())//- permission already granted, move back to welcome screen 2
                    this.gotoScreen(2);
                else//- need to grant permission, so move back welcome screen 3.
                    this.gotoScreen(3);
                break;
            }
            case R.id.welcome_done_image_view: {
//                this.settingsPreference.edit()
//                        .putBoolean("isFirstWelcomeScreenDone", true)
//                        .apply();
                MainActivity.openThis(this);
                this.finish();
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

    }

    @Override
    public void afterTextChanged(Editable editable) {
        this.passwordInputChecker();
    }

    private void passwordInputChecker(){
        String password = this.passwordText.getText().toString();
        String passwordRepeat = this.passwordTextRepeat.getText().toString();
        if (password.length() >= 8 && password.compareTo(passwordRepeat) == 0){
            this.nextButton4.setEnabled(true);
        } else {
            this.nextButton4.setEnabled(false);
        }
    }

    private boolean isRequiredPermissionGranted(){
        int i = 0;
        for (i=0; i<this.requiredPermissions.length; i++)
            if (this.checkSelfPermission(this.requiredPermissions[i]) == PackageManager.PERMISSION_GRANTED)
                break;
        if (i>=this.requiredPermissions.length)//- not grant yet.
            return false;
        else//- permission already granted.
            return true;
    }
}
