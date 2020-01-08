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

package org.ngyuen.otpvault.storage;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import net.sqlcipher.database.SQLiteDatabase;

import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.Token;
import org.ngyuen.otpvault.common.Callback;
import org.ngyuen.otpvault.common.Utils;
import org.ngyuen.otpvault.activities.MainActivity;

import java.util.List;

public class TokenPersistence {

    private final TokenDbStorage tokenDbStorage;

    private List<Long> tokenIndex;
    private boolean isFilterOn = false;
    private String issuerFilterParameter = "";

    public TokenPersistence(Context ctx) {
        this.tokenDbStorage = new TokenDbStorage((OTPVaultApplication) ctx.getApplicationContext());
        this.updateTokenIndex();
    }

    public void updateTokenIndex(){
        if (this.isFilterOn){
            tokenIndex = this.tokenDbStorage.getTokenIndex(this.issuerFilterParameter);
        } else {
            tokenIndex = this.tokenDbStorage.getTokenIndex();
        }
    }

    public int length() {
        if (tokenIndex == null)
            this.updateTokenIndex();
        return this.tokenIndex.size();
    }

    public boolean exist(Long tokenId) {
        try {
            return this.tokenDbStorage.exist(tokenId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Token exist(String issuer, String label) throws Exception {
        return this.tokenDbStorage.exist(issuer, label);
    }

    public Token get(int position) {
        if (this.tokenIndex == null ||
            this.tokenIndex.size() <= 0 ||
            position < 0 ||
            position >= this.tokenIndex.size())
            return null;
        else
            return this.tokenDbStorage.get(this.tokenIndex.get(position));
    }

    public Token get(long tokenId){
        return this.tokenDbStorage.get(tokenId);
    }

    public void add(Token newToken) throws Exception {
        if (this.exist(newToken.getIssuer(), newToken.getLabel()) != null)
            throw new Exception("Token exist.");
        this.tokenDbStorage.add(newToken);
    }

    public Token update(long tokenIndex, Token editedToken) throws Exception {
        this.tokenDbStorage.update(tokenIndex, editedToken);
        return editedToken;
    }

    public void move(int fromPosition, int toPosition) throws Exception {
        if (this.tokenIndex == null || tokenIndex.size() == 0 ||
            fromPosition < 0 || toPosition < 0 ||
            fromPosition >= this.tokenIndex.size() || toPosition >= this.tokenIndex.size() )
            throw new Exception("Unknown internal error.");
        else
            this.tokenDbStorage.move(this.tokenIndex.get(fromPosition), this.tokenIndex.get(toPosition));
    }

    public void delete(int position) throws Exception {
        if (this.tokenIndex == null || tokenIndex.size() == 0 ||
            position < 0 || position >= this.tokenIndex.size())
            throw new Exception("Unknown internal error.");
        else {
            this.tokenDbStorage.delete(this.tokenIndex.get(position));
        }
    }

    public void delete(long tokenIndex) throws Exception {
        this.tokenDbStorage.delete(tokenIndex);
    }

    public String[] getIssuers(){
        List<String> issuers = this.tokenDbStorage.getIssuers();
        return issuers.toArray(new String[issuers.size()]);
    }

    public void setFilter(String issuer){
        if (issuer != null && !issuer.isEmpty())
            this.isFilterOn = true;
        else
            this.isFilterOn = false;
        this.issuerFilterParameter = issuer;
        this.updateTokenIndex();
    }

    public Token[] getAll(SQLiteDatabase sqLiteDatabase){
        return this.tokenDbStorage.getAll(sqLiteDatabase);
    }

    /**
     * Update token async, because Image needs to be downloaded/copied to storage
     * @param context Application Context
     * @param editedToken Token (with Image, Image will be saved by the async task)
     */
    public static void updateAsync(Context context,
                                   final long tokenIndex,
                                   final Token editedToken,
                                   final Callback callback) {

        OTPVaultApplication application = (OTPVaultApplication) context.getApplicationContext();
        new TokenAsyncTask(application, editedToken){
            @Override
            protected void onPostExecute(ReturnParams returnParams) {
                super.onPostExecute(returnParams);
                //we downloaded the image, now save/update it normally
                try {
                    Token resultToken = ((OTPVaultApplication)returnParams.context.getApplicationContext())
                            .getTokenPersistence().update(tokenIndex, returnParams.getToken());
                    callback.success(resultToken);
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.error(e.getMessage());
                } finally {
                    //refresh TokenAdapter
                    returnParams.context.sendBroadcast(new Intent(MainActivity.ACTION_IMAGE_SAVED));
                }
            }
        }.execute();
    }

    /**
     * Add token async, because Image needs to be downloaded/copied to storage
     * @param context Application Context
     * @param token Token (with Image, Image will be saved by the async task)
     */
    public static void addAsync(Context context,
                                final Token token,
                                final Callback callback) {
        OTPVaultApplication application = (OTPVaultApplication) context.getApplicationContext();
        new TokenAsyncTask(application, token){
            @Override
            protected void onPostExecute(ReturnParams returnParams) {
                super.onPostExecute(returnParams);
                //we downloaded the image, now save/update it normally
                try {
                    ((OTPVaultApplication)returnParams.context.getApplicationContext())
                            .getTokenPersistence().add(returnParams.getToken());
                    callback.success(returnParams.getToken());
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.error(e.getMessage());
                } finally {
                    //refresh TokenAdapter
                    returnParams.context.sendBroadcast(new Intent(MainActivity.ACTION_IMAGE_SAVED));
                }
            }
        }.execute();
    }

    /**
     * Data class for SaveTokenTask
     */
    private static class ReturnParams {
        private final Token token;
        private final Context context;

        public ReturnParams(Token token, Context context) {
            this.token = token;
            this.context = context;
        }

        public Token getToken() {
            return token;
        }

        public Context getContext() {
            return context;
        }
    }

    /**
     * Downloads/copies images to FreeOTP storage
     * Saves token in PostExecute
     */
    private static abstract class TokenAsyncTask extends AsyncTask<Void, Void, ReturnParams> {

        private OTPVaultApplication application;
        private Token token;

        public TokenAsyncTask(OTPVaultApplication application, Token token){
            this.application = application;
            this.token = token;
        }

        @Override
        protected ReturnParams doInBackground(Void... voids) {
            if(this.token.getImage() != null) {
                this.token = Utils.image2Base64(this.application,
                        this.token);
            }
            return new ReturnParams(this.token, this.application);
        }
    }
}
