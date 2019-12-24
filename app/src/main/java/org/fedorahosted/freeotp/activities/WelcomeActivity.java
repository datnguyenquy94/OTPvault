package org.fedorahosted.freeotp.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.activities.MainActivity;
import org.fedorahosted.freeotp.common.Constants;
import org.fedorahosted.freeotp.common.Utils;

import java.io.File;

public class WelcomeActivity extends Activity implements View.OnClickListener, TextWatcher{

    private static String SET_THEME_AND_REFRESH_FLAG = "SET_THEME_AND_REFRESH_FLAG";

    private ImageView       appLogo;
    private RelativeLayout  welcomeLayout1;
    private RelativeLayout  welcomeLayout2;
    private RelativeLayout  welcomeLayout3;
    private RelativeLayout  welcomeLayout4;
    private RelativeLayout  welcomeLayout5;
    private int currentLayout = 1;

    private Button          nextButton;
    private Button          previousButton;
    private Button          lightThemeButton;
    private Button          darkThemeButton;

//    private Button          nextButton1;
//    private Button          nextButton2;
//    private Button          nextButton3;
//    private Button          nextButton4;
//
//    private Button          previousButton1;
//    private Button          previousButton2;
//    private Button          previousButton3;
//    private Button          previousButton4;

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
        Utils.setTheme(this, false);
        setContentView(R.layout.welcome);
        this.application = (FreeOTPApplication) this.getApplication();

        this.appLogo = this.findViewById(R.id.app_logo);

        this.welcomeLayout1 = this.findViewById(R.id.welcome01);
        this.welcomeLayout2 = this.findViewById(R.id.welcome02);
        this.welcomeLayout3 = this.findViewById(R.id.welcome03);
        this.welcomeLayout4 = this.findViewById(R.id.welcome04);
        this.welcomeLayout5 = this.findViewById(R.id.welcome05);

        this.nextButton = this.findViewById(R.id.next_button);
        this.previousButton = this.findViewById(R.id.previous_button);
        this.lightThemeButton = this.findViewById(R.id.light_theme_button);
        this.darkThemeButton = this.findViewById(R.id.dark_theme_button);

//        this.nextButton1 = this.findViewById(R.id.welcome_next_button_1);
//        this.nextButton2 = this.findViewById(R.id.welcome_next_button_2);
//        this.nextButton3 = this.findViewById(R.id.welcome_next_button_3);
//        this.nextButton4 = this.findViewById(R.id.welcome_next_button_4);
//
//        this.previousButton1 = this.findViewById(R.id.welcome_previous_button_1);
//        this.previousButton2 = this.findViewById(R.id.welcome_previous_button_2);
//        this.previousButton3 = this.findViewById(R.id.welcome_previous_button_3);
//        this.previousButton4 = this.findViewById(R.id.welcome_previous_button_4);

        this.passwordText = this.findViewById(R.id.passwordText);
        this.passwordTextRepeat = this.findViewById(R.id.passwordTextRepeat);

        this.welcomeDoneImage = this.findViewById(R.id.done_logo);

        this.nextButton.setOnClickListener(this);
        this.previousButton.setOnClickListener(this);
        this.lightThemeButton.setOnClickListener(this);
        this.darkThemeButton.setOnClickListener(this);
//        this.nextButton1.setOnClickListener(this);
//        this.nextButton2.setOnClickListener(this);
//        this.nextButton3.setOnClickListener(this);
//        this.nextButton4.setOnClickListener(this);
//        this.previousButton1.setOnClickListener(this);
//        this.previousButton2.setOnClickListener(this);
//        this.previousButton3.setOnClickListener(this);
//        this.previousButton4.setOnClickListener(this);
        this.welcomeDoneImage.setOnClickListener(this);

        this.passwordText.addTextChangedListener(this);
        this.passwordTextRepeat.addTextChangedListener(this);

        //- If encrypt storage is created, this mean user had been went though this welcome screen and created their password.
        //- We can to next screen.
        if (this.application.isEncryptStorageCreated() == true) {
            LoginActivity.openThis(this);
            this.finish();
        } else {
            boolean isThemeRefresh = this.getIntent().getBooleanExtra(WelcomeActivity.SET_THEME_AND_REFRESH_FLAG, false);
            if (isThemeRefresh){//- Activity be restarted to update theme. So skip to theme layout.
                this.currentLayout = 3;
                this.updateCurrentScreen();
            } else {
                this.currentLayout = 1;
                this.updateCurrentScreen();
            }

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
                this.gotoNextScreen();//- perform click on nextButton3 to move to next screen.
        }
    }

    private void updateCurrentScreen(){
        this.gotoScreen(this.currentLayout);
    }

    private void gotoNextScreen(){
        this.currentLayout++;
        this.gotoScreen(this.currentLayout);
    }

    private void gotoPreviousScreen(){
        this.currentLayout--;
        this.gotoScreen(this.currentLayout);
    }

    private void gotoScreen(int screen){
        if (screen < 1)
            screen = 1;
        if (screen > 5)
            screen = 5;

        this.nextButton.setVisibility(View.VISIBLE);
        this.nextButton.setEnabled(true);
        this.previousButton.setVisibility(View.VISIBLE);
        this.previousButton.setEnabled(true);

        switch (screen){
            case 1: {
                this.previousButton.setVisibility(View.GONE);
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
                this.passwordText.setText("");
                this.passwordTextRepeat.setText("");
                this.welcomeLayout1.setVisibility(View.GONE);
                this.welcomeLayout2.setVisibility(View.GONE);
                this.welcomeLayout3.setVisibility(View.GONE);
                this.welcomeLayout4.setVisibility(View.VISIBLE);
                this.welcomeLayout5.setVisibility(View.GONE);
                this.passwordInputChecker();
                break;
            }
            case 5: {
                this.nextButton.setVisibility(View.GONE);
                this.previousButton.setVisibility(View.GONE);
                this.appLogo.setVisibility(View.GONE);
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
        try {
            if (view.getId() == R.id.light_theme_button){
                this.setThemeAndRefresh("Light");
            } else if (view.getId() == R.id.dark_theme_button){
                this.setThemeAndRefresh("Dark");
            } else {
                switch (this.currentLayout){
                    case 1: {//- welcome layout
                        if (view.getId() == R.id.next_button){
                            if (this.isRequiredPermissionGranted()) {//- permission already granted skip the next layout
                                this.gotoNextScreen();
                                this.gotoNextScreen();
                            } else {//- need to grant permission, move to next layout.
                                this.gotoNextScreen();
                            }
                        }
                        break;
                    }
                    case 2: {//- permission layout
                        if (view.getId() == R.id.next_button){//- The switch layout action will be handle by "onRequestPermissionsResult".
                            ActivityCompat.requestPermissions(this,
                                    this.requiredPermissions,
                                    this.PERMISSION_REQUEST_CODE);
                        } else if (view.getId() == R.id.previous_button){
                            this.gotoPreviousScreen();
                        }
                        break;
                    }
                    case 3: {//- theme layout
                        if (view.getId() == R.id.next_button){
                            this.gotoNextScreen();
                        } else if (view.getId() == R.id.previous_button){//- back to password layout.
                            if (this.isRequiredPermissionGranted()) {//- permission already granted skip the previous layout
                                this.gotoPreviousScreen();
                                this.gotoPreviousScreen();
                            } else {//- need to grant permission, move back to previous layout.
                                this.gotoPreviousScreen();
                            }
                        }
                        break;
                    }
                    case 4: { //- password layout
                        if (view.getId() == R.id.next_button){
                            String passwd = this.passwordText.getText().toString();
                            this.application.createPasswordFirstTime(passwd);
                            this.gotoNextScreen();
                        } else if (view.getId() == R.id.previous_button){
                            this.gotoPreviousScreen();
                        }
                        break;
                    }
                    case 5: {//- done layout
                        MainActivity.openThis(this);
                        this.finish();
                        break;
                    }
                    default:{
                        break;
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        if (password.length() >= Constants.PASSWORD_MINIMUM_LENGTH && password.compareTo(passwordRepeat) == 0){
            this.nextButton.setEnabled(true);
        } else {
            this.nextButton.setEnabled(false);
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

    private void setThemeAndRefresh(String theme){
        finish();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("theme", theme).commit();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra(WelcomeActivity.SET_THEME_AND_REFRESH_FLAG, true);
        startActivity(intent);
        this.overridePendingTransition(0, 0);

    }
}
