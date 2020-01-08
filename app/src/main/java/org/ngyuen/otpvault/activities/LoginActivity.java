package org.fedorahosted.freeotp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.common.Utils;

public class LoginActivity extends Activity implements View.OnClickListener {

    private EditText    passwordText;
    private CheckBox    showPasswordCheckBox;
    private ImageButton focusPasswordNumberOnlyButton;
    private ImageButton focusPasswordTextOnlyButton;

    private FreeOTPApplication application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this, false);
        this.setContentView(R.layout.login);
        this.application = (FreeOTPApplication) this.getApplication();

        this.showPasswordCheckBox = this.findViewById(R.id.showPasswordCheckBox);
        this.focusPasswordNumberOnlyButton = this.findViewById(R.id.focusPasswordNumberOnlyButton);
        this.focusPasswordTextOnlyButton = this.findViewById(R.id.focusPasswordTextOnlyButton);
        this.passwordText = this.findViewById(R.id.passwordText);

        this.showPasswordCheckBox.setOnClickListener(this);
        this.focusPasswordNumberOnlyButton.setOnClickListener(this);
        this.focusPasswordTextOnlyButton.setOnClickListener(this);

        this.passwordText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    LoginActivity.this.checkPassword();
                }
                return false;
            }
        });

        if (this.application.isLogged()){
            MainActivity.openThis(this);
            this.finish();
        }
    }

    private void checkPassword(){
        try {
            boolean result = LoginActivity.this.application.login(LoginActivity.this.passwordText.getText().toString());
            if (result){
                MainActivity.openThis(this);
                this.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finish();
    }

    public static void openThis(Context context){
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.showPasswordCheckBox: {
                if (this.showPasswordCheckBox.isChecked())
                    this.passwordText.setTransformationMethod(null);
                else
                    this.passwordText.setTransformationMethod(new PasswordTransformationMethod());
                break;
            }
            case R.id.focusPasswordNumberOnlyButton: {
                this.passwordText.setInputType(InputType.TYPE_CLASS_NUMBER);
                this.passwordText.setText("");
                this.passwordText.requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(this.passwordText, InputMethodManager.SHOW_IMPLICIT);
                break;
            }
            case R.id.focusPasswordTextOnlyButton: {
                this.passwordText.setInputType(InputType.TYPE_CLASS_TEXT);
                this.passwordText.setText("");
                this.passwordText.requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(this.passwordText, InputMethodManager.SHOW_IMPLICIT);
                break;
            }
        }
    }
}
