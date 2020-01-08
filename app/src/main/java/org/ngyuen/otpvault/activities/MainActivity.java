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

/*
 * Portions Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ngyuen.otpvault.activities;

import android.Manifest;

import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.activities.settings.SettingsActivity;
import org.ngyuen.otpvault.adapters.TokenAdapter;
import org.ngyuen.otpvault.activities.abstractclasses.AbstractAppCompatActivity;
import org.ngyuen.otpvault.broadcast.ScreenListener;
import org.ngyuen.otpvault.common.Utils;
import org.ngyuen.otpvault.storage.TokenPersistence;
import org.ngyuen.otpvault.activities.add.AddActivity;
import org.ngyuen.otpvault.activities.add.ScanActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import org.ngyuen.otpvault.R;

public class MainActivity extends AbstractAppCompatActivity implements OnMenuItemClickListener, View.OnClickListener {

    private String LOG_TAG = this.getClass().getName();

    private TokenAdapter mTokenAdapter;
    private TokenPersistence tokenPersistence;
    public static final String ACTION_IMAGE_SAVED = "org.ngyuen.otpvault.ACTION_IMAGE_SAVED";
    private DataSetObserver mDataSetObserver;
    private final int PERMISSIONS_REQUEST_CAMERA = 1;
    private RefreshListBroadcastReceiver refreshListBroadcastReceiver;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private RadioGroup issuersRadioGroup;
    private List<RadioButton> radioButtons = new ArrayList<>();
    private ScreenListener screenListener;

    private int textColor;
    private int lineColor;

    private class RefreshListBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTokenAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this, false);
        onNewIntent(getIntent());
        setContentView(R.layout.main);

        this.textColor = Utils.getThemeColor(this, R.attr.colorSchemePrimary);
        this.lineColor = Utils.getThemeColor(this, R.attr.colorSchemeTertiary);

        this.application.startIdleChecker();
        this.tokenPersistence = ((OTPVaultApplication)this.getApplication()).getTokenPersistence();

        //----- sidebar
        this.drawerLayout = (DrawerLayout)findViewById(R.id.mainLayout);
        this.actionBarDrawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                R.string.add, R.string.app_name);
        this.drawerLayout.addDrawerListener(this.actionBarDrawerToggle);
        this.actionBarDrawerToggle.syncState();
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.navigationView = findViewById(R.id.navigation_view);

        this.issuersRadioGroup = this.navigationView.findViewById(R.id.issuers_radio_group);
        //---------------
        //------- Listener for screen on/off
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenListener = new ScreenListener();
        registerReceiver(screenListener, filter);


        mTokenAdapter = new TokenAdapter(this);
        refreshListBroadcastReceiver = new RefreshListBroadcastReceiver();
        registerReceiver(refreshListBroadcastReceiver, new IntentFilter(ACTION_IMAGE_SAVED));
        ((GridView) findViewById(R.id.grid)).setAdapter(mTokenAdapter);

        // Don't permit screenshots since these might contain OTP codes.
        getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);

        mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mTokenAdapter.getCount() == 0)
                    findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
                else
                    findViewById(android.R.id.empty).setVisibility(View.GONE);
            }
        };
        mTokenAdapter.registerDataSetObserver(mDataSetObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!this.application.isLogged()){
            this.finish();
            LoginActivity.openThis(this);
        } else {
            this.update();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.update();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.application.logout();
        mTokenAdapter.unregisterDataSetObserver(mDataSetObserver);
        if (this.refreshListBroadcastReceiver != null){
            unregisterReceiver(refreshListBroadcastReceiver);
            this.refreshListBroadcastReceiver = null;
        }
        if (this.screenListener != null) {
            unregisterReceiver(screenListener);
            screenListener = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_add).setOnMenuItemClickListener(this);
        menu.findItem(R.id.action_scan).setVisible(ScanActivity.hasCamera(this));
        menu.findItem(R.id.action_scan).setOnMenuItemClickListener(this);
        menu.findItem(R.id.action_settings).setOnMenuItemClickListener(this);
        menu.findItem(R.id.action_about).setOnMenuItemClickListener(this);
        return true;
    }

    private void tryOpenCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        } else {
            // permission is already granted
            openCamera();
        }
    }

    private void manutallyAdd() {
        startActivity(new Intent(this, AddActivity.class));
    }

    private void openCamera() {
        startActivity(new Intent(this, ScanActivity.class));
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(this.actionBarDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                manutallyAdd();
                return true;
            case R.id.action_scan:
                tryOpenCamera();
                return true;
            case R.id.action_settings:
                SettingsActivity.openThis(this);
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_permission_camera_open, Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


        //- Add token by another app's intent. Exmple: Firefox send a totp's uri to this app.
        Uri uri = intent.getData();
        if (uri != null) {
            Log.d(LOG_TAG, "Not support yet, onNewIntent:" + uri.getPath());
//            try {
//                TokenPersistence.saveAsync(this, new Token(uri));
//            } catch (Token.TokenUriInvalidException e) {
//                e.printStackTrace();
//            }
        }
    }

    View line;
    RadioButton allRadioButton;
    public void update(){
        mTokenAdapter.notifyDataSetChanged();

        if (this.navigationView != null){
            this.issuersRadioGroup.removeAllViews();
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

            if (allRadioButton == null){
                allRadioButton = new RadioButton(this);
                allRadioButton.setText("[ALL]");
                allRadioButton.setLayoutParams(layoutParams);
                allRadioButton.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                allRadioButton.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                allRadioButton.setOnClickListener(this);
                allRadioButton.setChecked(true);
                allRadioButton.setTextColor(textColor);
            }
            if (line == null){
                line = new View(this);
                ViewGroup.LayoutParams params = new LayoutParams();
                params.height = (int) (1 * this.getResources().getDisplayMetrics().density);
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                line.setLayoutParams(params);
                line.setBackgroundColor(lineColor);
            }
            this.issuersRadioGroup.addView(allRadioButton);
            this.issuersRadioGroup.addView(line);


            String[] issuers = this.tokenPersistence.getIssuers();
            for (int i = 0; i<issuers.length; i++){
                RadioButton rb;
                if ((i)<radioButtons.size()){
                    rb = radioButtons.get(i);
                } else {
                    rb = new RadioButton(this);
                    rb.setLayoutParams(layoutParams);
                    rb.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    rb.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
//                    rb.setGravity(Gravity.START);
                    rb.setOnClickListener(this);
                    rb.setTextColor(textColor);
                    this.radioButtons.add(rb);
                }
                rb.setText(issuers[i]);
                this.issuersRadioGroup.addView(rb);
            }
        }

    }

    @Override
    public void onClick(View view) {
        if (view instanceof RadioButton){
            String label = ((RadioButton) view).getText().toString();
            if (label.compareTo("[ALL]")==0)
                this.tokenPersistence.setFilter("");
            else
                this.tokenPersistence.setFilter(label);
            this.update();
            this.drawerLayout.closeDrawer(Gravity.LEFT);
        }
    }

    public static void openThis(Context context){
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }
}
