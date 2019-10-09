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

package org.fedorahosted.freeotp.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenCode;
import org.fedorahosted.freeotp.views.TokenLayout;
import org.fedorahosted.freeotp.activities.edit.DeleteActivity;
import org.fedorahosted.freeotp.activities.edit.EditActivity;
import org.fedorahosted.freeotp.storage.TokenPersistence;

import java.util.HashMap;
import java.util.Map;

public class TokenAdapter extends BaseReorderableAdapter {
    private final TokenPersistence mTokenPersistence;
    private final LayoutInflater mLayoutInflater;
    private final ClipboardManager mClipMan;
    private final Map<String, TokenCode> mTokenCodes;

    public TokenAdapter(Context ctx) {
        mTokenPersistence = ((FreeOTPApplication)ctx.getApplicationContext())
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
        mTokenPersistence.move(fromPosition, toPosition);
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
                        i.putExtra(EditActivity.EXTRA_POSITION, position);
                        ctx.startActivity(i);
                        break;

                    case R.id.action_delete:
                        i = new Intent(ctx, DeleteActivity.class);
                        i.putExtra(DeleteActivity.EXTRA_POSITION, position);
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
                    TokenPersistence tp = ((FreeOTPApplication)ctx.getApplicationContext())
                            .getTokenPersistence();;

                    // Increment the token.
                    Token token = tp.get(position);
                    TokenCode codes = token.generateCodes();
                    //save token. Image wasn't changed here, so just save it in sync
                    ((FreeOTPApplication)ctx.getApplicationContext())
                            .getTokenPersistence().update(position, token);

                    // Copy code to clipboard.
                    mClipMan.setPrimaryClip(ClipData.newPlainText(null, codes.getCurrentCode()));
                    Toast.makeText(v.getContext().getApplicationContext(),
                            R.string.code_copied,
                            Toast.LENGTH_SHORT).show();

                    mTokenCodes.put(token.getID(), codes);
                    ((TokenLayout) v).start(token.getType(), codes, true);
                } catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        TokenCode tc = mTokenCodes.get(token.getID());
        if (tc != null && tc.getCurrentCode() != null)
            tl.start(token.getType(), tc, false);
    }

    @Override
    protected View createView(ViewGroup parent, int type) {
        return mLayoutInflater.inflate(R.layout.token, parent, false);
    }
}
