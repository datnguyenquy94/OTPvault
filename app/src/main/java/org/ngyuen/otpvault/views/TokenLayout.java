/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
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

package org.ngyuen.otpvault.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.R;
import org.ngyuen.otpvault.Token;
import org.ngyuen.otpvault.TokenCode;
import org.ngyuen.otpvault.common.Utils;

public class TokenLayout extends FrameLayout implements View.OnClickListener, Runnable {
    private OTPVaultApplication application;
    private ProgressCircle mProgressInner;
    private ProgressCircle mProgressOuter;
    private ImageView mImage;
    private TextView mCode;
    private TextView mIssuer;
    private TextView mLabel;
    private ImageView mMenu;
    private PopupMenu mPopupMenu;

    private TokenCode mCodes;
    private String mType;
    private String mPlaceholder;
    private long mStartTime;

    public TokenLayout(Context context) {
        super(context);
        this.application = (OTPVaultApplication) context.getApplicationContext();
    }

    public TokenLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.application = (OTPVaultApplication) context.getApplicationContext();
    }

    public TokenLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.application = (OTPVaultApplication) context.getApplicationContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mProgressInner = findViewById(R.id.progressInner);
        mProgressOuter = findViewById(R.id.progressOuter);
        mImage = findViewById(R.id.image);
        mCode = findViewById(R.id.code);
        mIssuer = findViewById(R.id.issuer);
        mLabel = findViewById(R.id.label);
        mMenu = findViewById(R.id.menu);

        mPopupMenu = new PopupMenu(getContext(), mMenu);
        mMenu.setOnClickListener(this);
    }

    public void bind(Token token, int menu, PopupMenu.OnMenuItemClickListener micl) {
        mCodes = null;

        // Setup menu.
        mPopupMenu.getMenu().clear();
        mPopupMenu.getMenuInflater().inflate(menu, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(micl);

        // Cancel all active animations.
        setEnabled(true);
        removeCallbacks(this);
        mImage.clearAnimation();
        mProgressInner.clearAnimation();
        mProgressOuter.clearAnimation();
        mProgressInner.setVisibility(View.GONE);
        mProgressOuter.setVisibility(View.GONE);

        // Get the code placeholder.
        char[] placeholder;
        if (this.application.getSettingsPreference().getBoolean("splitTokenCode", false)){
            placeholder = new char[token.getDigits()+1];
            for (int i = 0; i < placeholder.length; i++)
                if (i == 3)
                    placeholder[i] = ' ';
                else
                    placeholder[i] = '-';
        } else {
            placeholder = new char[token.getDigits()];
            for (int i = 0; i < placeholder.length; i++)
                placeholder[i] = '-';
        }
        mPlaceholder = new String(placeholder);

        // Show the image.
        if (token.getImage() != null && !token.getImage().isEmpty())
            Utils.base64String2ImageView(token.getImage(), mImage);
        else
            mImage.setImageResource(R.mipmap.ic_freeotp_logo_foreground);

        // Set the labels.
        mLabel.setText(token.getLabel());
        mIssuer.setText(token.getIssuer());
        mCode.setText(mPlaceholder);
        if (mIssuer.getText().length() == 0) {
            mIssuer.setText(token.getLabel());
            mLabel.setVisibility(View.GONE);
        } else {
            mLabel.setVisibility(View.VISIBLE);
        }
    }

    private void animate(View view, int anim, boolean animate) {
        Animation a = AnimationUtils.loadAnimation(view.getContext(), anim);
        if (!animate)
            a.setDuration(0);
        view.startAnimation(a);
    }

    public void start(String type, TokenCode codes, boolean animate) {
        mCodes = codes;
        mType = type;

        // Start animations.
        mProgressInner.setVisibility(View.VISIBLE);
        animate(mProgressInner, R.anim.fadein, animate);
        animate(mImage, R.anim.token_image_fadeout, animate);

        // Handle type-specific UI.
        switch (type) {
            case Token.TokenType.HOTP:
                setEnabled(false);
                break;
            case Token.TokenType.TOTP:
                mProgressOuter.setVisibility(View.VISIBLE);
                animate(mProgressOuter, R.anim.fadein, animate);
                break;
        }

        mStartTime = System.currentTimeMillis();
        post(this);
    }

    @Override
    public void onClick(View v) {
        mPopupMenu.show();
    }

    @Override
    public void run() {
        // Get the current data
        String code = mCodes == null ? null : mCodes.getCurrentCode();
        if (code != null) {
            // Determine whether to enable/disable the view.
            if (!isEnabled())
                setEnabled(System.currentTimeMillis() - mStartTime > 5000);

            if (this.application.getSettingsPreference().getBoolean("splitTokenCode", false) && code.length() > 3)
                code = code.substring(0, 3) + " " + code.substring(3);
            // Update the fields
            mCode.setText(code);
            mProgressInner.setProgress(mCodes.getCurrentProgress());
            if (mType.compareTo(Token.TokenType.HOTP) != 0)
                mProgressOuter.setProgress(mCodes.getTotalProgress());

            postDelayed(this, 100);
            return;
        }

        mCode.setText(mPlaceholder);
        mProgressInner.setVisibility(View.GONE);
        mProgressOuter.setVisibility(View.GONE);
        animate(mImage, R.anim.token_image_fadein, true);
    }
}
