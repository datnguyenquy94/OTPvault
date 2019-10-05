package org.fedorahosted.freeotp.activities.abstractclasses;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import org.fedorahosted.freeotp.BuildConfig;
import org.fedorahosted.freeotp.FreeOTPApplication;

public abstract class AbstractActivity extends Activity {

    protected FreeOTPApplication application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.application = (FreeOTPApplication) this.getApplication();
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
