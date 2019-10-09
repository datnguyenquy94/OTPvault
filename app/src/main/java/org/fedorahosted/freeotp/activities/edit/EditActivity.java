/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2014  Nathaniel McCallum, Red Hat
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

package org.fedorahosted.freeotp.activities.edit;

import android.widget.Toast;

import org.fedorahosted.freeotp.BuildConfig;
import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.activities.abstractclasses.AbstractActivity;
import org.fedorahosted.freeotp.common.Callback;
import org.fedorahosted.freeotp.storage.TokenPersistence;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.squareup.picasso.Picasso;

public class EditActivity extends AbstractActivity implements TextWatcher, View.OnClickListener {

    public static final String  EXTRA_POSITION = "position";
    private int                 mPosition;

    private EditText           mIssuer;
    private EditText           mLabel;
    private ImageButton        mImage;
    private Button             mRestore;
    private Button             mSave;

    private String mIssuerCurrent;
    private String mIssuerDefault;
    private String mLabelCurrent;
    private String mLabelDefault;
    private Uri mImageCurrent;
    private Uri mImageDefault;
    private Uri mImageDisplay;
    private Token token;
    private final int REQUEST_IMAGE_OPEN = 1;

    private void showImage(Uri uri) {
        mImageDisplay = uri;
        onTextChanged(null, 0, 0, 0);
        Picasso.with(this)
                .load(uri)
                .placeholder(R.mipmap.ic_freeotp_logo_foreground)
                .into(mImage);
    }

    private boolean imageIs(Uri uri) {
        if (uri == null)
            return mImageDisplay == null;

        return uri.equals(mImageDisplay);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        // Get the position of the token. This MUST exist.
        mPosition = getIntent().getIntExtra(EXTRA_POSITION, -1);
        if(BuildConfig.DEBUG && mPosition < 0)
            throw new RuntimeException("Could not create Activity");

        // Get token values.
        token = ((FreeOTPApplication)this.getApplicationContext())
                .getTokenPersistence().get(getPosition());
        mIssuerCurrent = token.getIssuer();
        mLabelCurrent = token.getLabel();
        mImageCurrent = token.getImage();
        mIssuerDefault = token.getIssuer();
        mLabelDefault = token.getLabel();
        mImageDefault = token.getImage();

        // Get references to widgets.
        mIssuer = findViewById(R.id.issuer);
        mLabel = findViewById(R.id.label);
        mImage = findViewById(R.id.image);
        mRestore = findViewById(R.id.restore);
        mSave = findViewById(R.id.save);

        // Setup text changed listeners.
        mIssuer.addTextChangedListener(this);
        mLabel.addTextChangedListener(this);

        // Setup click callbacks.
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.restore).setOnClickListener(this);
        mImage.setOnClickListener(this);

        // Setup initial state.
        showImage(mImageCurrent);
        mLabel.setText(mLabelCurrent);
        mIssuer.setText(mIssuerCurrent);
        mIssuer.setSelection(mIssuer.getText().length());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_OPEN) {
                //mImageDisplay is set in showImage
                showImage(data.getData());
                //- Not set yet. Set this mean it auto delete the current image.
                // token.setImage(mImageDisplay);
            }
            else {
                Toast.makeText(EditActivity.this, R.string.error_image_open, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String label = mLabel.getText().toString();
        String issuer = mIssuer.getText().toString();
        mSave.setEnabled(!label.equals(mLabelCurrent) || !issuer.equals(mIssuerCurrent) || !imageIs(mImageCurrent));
        mRestore.setEnabled(!label.equals(mLabelDefault) || !issuer.equals(mIssuerDefault) || !imageIs(mImageDefault));
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_IMAGE_OPEN);
                break;

            case R.id.restore:
                mLabel.setText(mLabelDefault);
                mIssuer.setText(mIssuerDefault);
                mIssuer.setSelection(mIssuer.getText().length());
                showImage(mImageDefault);
                break;

            case R.id.save:
                TokenPersistence tokenPersistence = ((FreeOTPApplication)this.getApplicationContext())
                        .getTokenPersistence();;
                Token token = tokenPersistence.get(getPosition());
                token.setIssuer(mIssuer.getText().toString());
                token.setLabel(mLabel.getText().toString());
                if (token.getImage() == null || token.getImage().compareTo(mImageDisplay) != 0)
                    token.setImage(mImageDisplay);
                TokenPersistence.updateAsync(application,
                        mPosition,
                        token,
                        new Callback() {
                            @Override
                            public void success(Token token) { EditActivity.this.finish(); }
                            @Override
                            public void error(String errorMessage) {
                                Toast.makeText(application, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                break;
            case R.id.cancel:
                finish();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //- select image from outside, activity need to pause in this time.
//        this.finishAndRemoveTask();
    }

    protected int getPosition() {
        return mPosition;
    }
}
