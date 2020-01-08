/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 * Authors: Siemens AG <max.wittig@siemens.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 * Copyright (C) 2017  Max Wittig, Siemens AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ngyuen.otpvault.activities.add;

import org.ngyuen.otpvault.R;
import org.ngyuen.otpvault.Token;
import org.ngyuen.otpvault.activities.abstractclasses.AbstractActivity;
import org.ngyuen.otpvault.common.Utils;
import org.ngyuen.otpvault.storage.TokenPersistence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.parameter.selector.FocusModeSelectors;
import io.fotoapparat.view.CameraView;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.autoFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.fixed;
import static io.fotoapparat.parameter.selector.LensPositionSelectors.back;
import static io.fotoapparat.parameter.selector.Selectors.firstAvailable;
import static io.fotoapparat.parameter.selector.SizeSelectors.biggestSize;

public class ScanActivity extends AbstractActivity {
    private Fotoapparat fotoapparat;
    private static ScanBroadcastReceiver receiver;

    public class ScanBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION = "org.ngyuen.otpvault.ACTION_CODE_SCANNED";

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("scanResult");
            addTokenAndFinish(text);
        }
    }

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void addTokenAndFinish(String text) {
        Token token = null;
        try {
            token = new Token(text);
        } catch (Token.TokenUriInvalidException e) {
            Toast.makeText(this, "Create token failed. Reason: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        //do not receive any more broadcasts
        this.unregisterReceiver(receiver);

        //check if token already exists
//        if (((OTPVaultApplication)this.getApplicationContext())
//                .getTokenPersistence().tokenExists(token.getID())) {
//            finish();
//            return;
//        }

        TokenPersistence.addAsync(this.application, token,
                new org.ngyuen.otpvault.common.Callback() {
                    @Override
                    public void success(Object obj) {
                        Token resultToken = (Token) obj;
                        if (resultToken == null || resultToken.getImage() == null) {
                            finish();
                            return;
                        }

                        final ImageView image = ScanActivity.this.findViewById(R.id.image);
                        Picasso.with(ScanActivity.this)
                                .load(resultToken.getImage())
                                .memoryPolicy(MemoryPolicy.NO_CACHE)
                                .placeholder(R.drawable.scan)
                                .into(image, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        findViewById(R.id.progress).setVisibility(View.INVISIBLE);
                                        image.setAlpha(0.9f);
                                        image.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                finish();
                                            }
                                        }, 2000);
                                    }

                                    @Override
                                    public void onError() {
                                        finish();
                                    }
                                });
                    }

                    @Override
                    public void error(String errorMessage) {
                        Toast.makeText(application, errorMessage, Toast.LENGTH_LONG).show();
                        ScanActivity.this.finish();
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(receiver);
        }
        catch (IllegalArgumentException e) {
            // catch exception, when trying to unregister receiver again
            // there seems to be no way to check, if receiver if registered
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this, false);
        receiver = new ScanBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter(ScanBroadcastReceiver.ACTION));
        setContentView(R.layout.scan);
        CameraView cameraView = findViewById(R.id.camera_view);

        fotoapparat = Fotoapparat
                .with(this)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .photoSize(biggestSize())
                .lensPosition(back())
                .focusMode(firstAvailable(
                        FocusModeSelectors.continuousFocus(),
                        autoFocus(),
                        fixed()
                ))
                .frameProcessor(new ScanFrameProcessor(this))
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fotoapparat.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finishAndRemoveTask();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fotoapparat.stop();
    }
}
