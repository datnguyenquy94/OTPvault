package org.ngyuen.otpvault.activities.edit;

import org.ngyuen.otpvault.BuildConfig;
import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.R;
import org.ngyuen.otpvault.Token;
import org.ngyuen.otpvault.activities.abstractclasses.AbstractActivity;
import org.ngyuen.otpvault.common.Utils;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

public class DeleteActivity extends AbstractActivity {
    public static final String  EXTRA_ID = "EXTRA_ID";
    private long                 mTokenId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this, true);
        setContentView(R.layout.delete);

        // Get the position of the token. This MUST exist.
        mTokenId = getIntent().getLongExtra(EXTRA_ID, -1);
        if(BuildConfig.DEBUG && mTokenId < 0)
            throw new RuntimeException("Could not create Activity");

        final Token token = ((OTPVaultApplication)this.getApplicationContext())
                .getTokenPersistence().get(mTokenId);
        ((TextView) findViewById(R.id.issuer)).setText(token.getIssuer());
        ((TextView) findViewById(R.id.label)).setText(token.getLabel());
        Picasso.with(this)
                .load(token.getImage())
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .placeholder(R.mipmap.ic_freeotp_logo_foreground)
                .into((ImageView) findViewById(R.id.image));

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    ((OTPVaultApplication)DeleteActivity.this.getApplicationContext())
                            .getTokenPersistence().delete(mTokenId);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DeleteActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finishAndRemoveTask();
    }
}
