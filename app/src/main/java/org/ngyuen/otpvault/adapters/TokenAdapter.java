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

package org.ngyuen.otpvault.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.R;
import org.ngyuen.otpvault.Token;
import org.ngyuen.otpvault.TokenCode;
import org.ngyuen.otpvault.views.TokenLayout;
import org.ngyuen.otpvault.activities.edit.DeleteActivity;
import org.ngyuen.otpvault.activities.edit.EditActivity;
import org.ngyuen.otpvault.storage.TokenPersistence;

import java.util.HashMap;
import java.util.Map;

public class TokenAdapter extends BaseReorderableAdapter {
    private final String LOG_TAG = this.getClass().getName();

    private final TokenPersistence mTokenPersistence;
    private final LayoutInflater mLayoutInflater;
    private final ClipboardManager mClipMan;
    private final Map<Long, TokenCode> mTokenCodes;

    public TokenAdapter(Context ctx) {
        mTokenPersistence = ((OTPVaultApplication)ctx.getApplicationContext())
                .getTokenPersistence();;
        mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mClipMan = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        mTokenCodes = new HashMap<>();
        registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                mTokenCodes.clear();
            }

            @Override
            public void onInvalidated() {
                mTokenCodes.clear();
            }
        });
    }

    @Override
    public int getCount() {
        return mTokenPersistence.length();
    }

    @Override
    public Token getItem(int position) {
        return mTokenPersistence.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    protected void move(int fromPosition, int toPosition) {
        try {
            if (fromPosition != toPosition)
                mTokenPersistence.move(fromPosition, toPosition);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
        notifyDataSetChanged();
    }

    @Override
    protected void bindView(final View view, final int position) {
        final Context ctx = view.getContext();
        TokenLayout tl = (TokenLayout) view;
        Token token = getItem(position);

        tl.bind(token, R.menu.token, new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i;

                switch (item.getItemId()) {
                    case R.id.action_edit:
                        i = new Intent(ctx, EditActivity.class);
                        i.putExtra(EditActivity.EXTRA_ID, token.getId());
                        ctx.startActivity(i);
                        break;

                    case R.id.action_delete:
                        i = new Intent(ctx, DeleteActivity.class);
                        i.putExtra(DeleteActivity.EXTRA_ID, token.getId());
                        ctx.startActivity(i);
                        break;
                }

                return true;
            }
        });

        tl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TokenPersistence tp = ((OTPVaultApplication)ctx.getApplicationContext())
                            .getTokenPersistence();;

                    // Increment the token.
                    Token token = tp.get(position);
                    TokenCode codes = token.generateCodes();
                    //save token. Image wasn't changed here, so just save it in sync
                    ((OTPVaultApplication)ctx.getApplicationContext())
                            .getTokenPersistence().update(token);

                    // Copy code to clipboard.
                    mClipMan.setPrimaryClip(ClipData.newPlainText(null, codes.getCurrentCode()));
                    Toast.makeText(v.getContext().getApplicationContext(),
                            R.string.code_copied,
                            Toast.LENGTH_SHORT).show();

                    mTokenCodes.put(token.getId(), codes);
                    ((TokenLayout) v).start(token.getType(), codes, true);
                } catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        TokenCode tc = mTokenCodes.get(token.getId());
        if (tc != null && tc.getCurrentCode() != null)
            tl.start(token.getType(), tc, false);
    }

    @Override
    protected View createView(ViewGroup parent, int type) {
        return mLayoutInflater.inflate(R.layout.token, parent, false);
    }

    @Override
    public void notifyDataSetChanged() {
        this.mTokenPersistence.updateTokenIndex();
        super.notifyDataSetChanged();
    }
}
