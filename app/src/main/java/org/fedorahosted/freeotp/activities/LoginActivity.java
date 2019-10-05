package org.fedorahosted.freeotp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;

public class LoginActivity extends Activity {

    private EditText passwordText;

    private FreeOTPApplication application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login);
        this.application = (FreeOTPApplication) this.getApplication();

        this.passwordText = this.findViewById(R.id.passwordText);
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
        boolean result = LoginActivity.this.application.login(LoginActivity.this.passwordText.getText().toString());
        if (result){
            MainActivity.openThis(this);
            this.finish();
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
}
