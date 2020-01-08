package org.ngyuen.otpvault.activities.abstractclasses;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.ngyuen.otpvault.OTPVaultApplication;

public class AbstractAppCompatActivity extends AppCompatActivity {

    protected OTPVaultApplication application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.application = (OTPVaultApplication) this.getApplication();
        this.application.updateLastTimeInteraction();
    }

    @Override
    protected void onResume() {
        this.application.updateCurrentActivitiyClassName(this);
        super.onResume();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        this.application.updateLastTimeInteraction();
        return super.dispatchTouchEvent(ev);
    }

}
