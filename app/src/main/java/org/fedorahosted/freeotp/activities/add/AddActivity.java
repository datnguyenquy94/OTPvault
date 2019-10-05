package org.fedorahosted.freeotp.activities.add;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.activities.abstractclasses.AbstractActivity;
import org.fedorahosted.freeotp.storage.TokenPersistence;
import org.fedorahosted.freeotp.Utils;

public class AddActivity extends AbstractActivity implements TextWatcher, View.OnClickListener, AdapterView.OnItemSelectedListener {
    private EditText            mIssuer;
    private EditText            mLabel;
    private Spinner             mType;
    private EditText            mSecret;
    private EditText            mCounter;
    private EditText            mPeriod;
    private EditText            mDigits;
    private Spinner             mAlgorithm;
    private TextView            mTestPasscode;
    private ImageButton         mImage;
    private Button              mAdd;


//    private String mIssuerCurrent;
//    private String mIssuerDefault;
//    private String mLabelCurrent;
//    private String mLabelDefault;
//    private Uri mImageCurrent;
//    private Uri mImageDefault;
    private Uri mImageDisplay;
//    private Token token;
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

    private boolean isAddable(){
        boolean status = true;
        status = status && !mIssuer.getText().toString().isEmpty();
        status = status && !mLabel.getText().toString().isEmpty();
        status = status && !mSecret.getText().toString().isEmpty();
        status = status && !mCounter.getText().toString().isEmpty();
        status = status && !mPeriod.getText().toString().isEmpty();
        status = status && !mDigits.getText().toString().isEmpty();
        return status;
    }

    private void onClickProcess(View v){
        switch (v.getId()) {
            case R.id.image:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_IMAGE_OPEN);
                break;

            case R.id.type:
                String type = mType.getSelectedItem().toString();
                if (type.compareTo("TOTP") == 0){
                    findViewById(R.id.HOTPCounter).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.HOTPCounter).setVisibility(View.VISIBLE);
                }
                mCounter.setText("1");
                break;

            case R.id.add:
                try {
//                    TokenPersistence tp = ((FreeOTPApplication)this.getApplicationContext())
//                            .getTokenPersistence();
                    Token token = new Token(Utils.OTP_URI_BUILDER(
                            mType.getSelectedItem().toString(),
                            mIssuer.getText().toString(),
                            mLabel.getText().toString(),
                            mSecret.getText().toString(),
                            mCounter.getText().toString(),
                            mDigits.getText().toString(),
                            mAlgorithm.getSelectedItem().toString()
                    ));
                    token.setImage(mImageDisplay);

                    TokenPersistence.saveAsync(this, token);
                    finish();
                } catch(Exception e){
                    e.printStackTrace();
                }

            case R.id.test_passcode:
                try {
                    boolean status = this.isAddable();
                    if (status){
                        Token token = new Token(Utils.OTP_URI_BUILDER(
                                mType.getSelectedItem().toString(),
                                mIssuer.getText().toString(),
                                mLabel.getText().toString(),
                                mSecret.getText().toString(),
                                mCounter.getText().toString(),
                                mDigits.getText().toString(),
                                mAlgorithm.getSelectedItem().toString()
                        ));

                        String output = this.getString(R.string.test_passcode) + ": "+ token.generateCodes().getCurrentCode();
                        mTestPasscode.setText(output);
                    }
                } catch (Token.TokenUriInvalidException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.cancel:
                finish();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add);

        // Get references to widgets.
        mIssuer = findViewById(R.id.issuer);
        mLabel = findViewById(R.id.label);
        mType = findViewById(R.id.type);
        mSecret = findViewById(R.id.secret);
        mCounter = findViewById(R.id.counter);
        mPeriod = findViewById(R.id.period);
        mDigits = findViewById(R.id.digits);
        mAlgorithm = findViewById(R.id.algorithm);
        mTestPasscode = findViewById(R.id.test_passcode);

        mImage = findViewById(R.id.image);
        mAdd = findViewById(R.id.add);

        // Setup text changed listeners.
        mIssuer.addTextChangedListener(this);
        mLabel.addTextChangedListener(this);
        mSecret.addTextChangedListener(this);
        mCounter.addTextChangedListener(this);
        mPeriod.addTextChangedListener(this);
        mDigits.addTextChangedListener(this);

        // Setup click callbacks.
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.test_passcode).setOnClickListener(this);
        findViewById(R.id.add).setOnClickListener(this);
        mImage.setOnClickListener(this);
        mType.setOnItemSelectedListener(this);

        // Setup initial state.
        showImage(null);
        mLabel.setText("");
        mIssuer.setText("");
        mIssuer.setSelection(mIssuer.getText().length());
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finishAndRemoveTask();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_OPEN) {
                //mImageDisplay is set in showImage
                mImageDisplay = data.getData();
                showImage(data.getData());
            }
            else {
                Toast.makeText(AddActivity.this, R.string.error_image_open, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        boolean status = isAddable();
        mAdd.setEnabled(status);
        if (status)
            mTestPasscode.setVisibility(View.VISIBLE);
        else
            mTestPasscode.setVisibility(View.GONE);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        this.onClickProcess(v);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        this.onClickProcess(adapterView);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
