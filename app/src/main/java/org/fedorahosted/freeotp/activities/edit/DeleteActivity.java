package org.fedorahosted.freeotp.activities.edit;

import org.fedorahosted.freeotp.BuildConfig;
import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.activities.abstractclasses.AbstractActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

public class DeleteActivity extends AbstractActivity {
    public static final String  EXTRA_POSITION = "position";
    private int                 mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.delete);

        // Get the position of the token. This MUST exist.
        mPosition = getIntent().getIntExtra(EXTRA_POSITION, -1);
        if(BuildConfig.DEBUG && mPosition < 0)
            throw new RuntimeException("Could not create Activity");

        final Token token = ((FreeOTPApplication)this.getApplicationContext())
                .getTokenPersistence().get(getPosition());
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
                ((FreeOTPApplication)DeleteActivity.this.getApplicationContext())
                        .getTokenPersistence().delete(getPosition());
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finishAndRemoveTask();
    }

    protected int getPosition() {
        return mPosition;
    }
}
