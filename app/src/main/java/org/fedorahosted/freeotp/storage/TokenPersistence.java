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

package org.fedorahosted.freeotp.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.gson.Gson;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.common.Callback;
import org.fedorahosted.freeotp.common.Utils;
import org.fedorahosted.freeotp.activities.MainActivity;

import java.io.File;

public class TokenPersistence {

    private final Gson gson;
    private final IssuerStorage issuerStorage;
    private final TokenStorage tokenStorage;

    private boolean isFilterOn = false;
    private String issuerFilterParameter = "";

    public TokenPersistence(Context ctx) {
        this.gson = ((FreeOTPApplication) ctx.getApplicationContext()).getGson();
        this.issuerStorage = new IssuerStorage((FreeOTPApplication) ctx.getApplicationContext(), gson);
        this.tokenStorage = new TokenStorage((FreeOTPApplication) ctx.getApplicationContext(), gson);
    }

    public int length() {
        if (this.isFilterOn)
            return this.issuerStorage.getIssuerTokenIndexLength(this.issuerFilterParameter);
        else
            return this.tokenStorage.getTokenIndexLength();
    }

    public boolean tokenExists(String tokenId) {
        return this.tokenStorage.tokenExists(tokenId);
    }

    public Token get(int position) {
        if (this.isFilterOn)
            return this.tokenStorage.get(this.issuerStorage.getKey(this.issuerFilterParameter, position));
        else
            return this.tokenStorage.get(position);
    }

    public Token get(String key){
        return this.tokenStorage.get(key);
    }

    public void add(Token newToken) throws Exception {
        try {
            if (this.tokenStorage.tokenExists(newToken.getID()))
                throw new Exception("Token exist.");
            this.issuerStorage.addTokenKeyOnIssuerIndex(newToken.getIssuer(), newToken.getID());//- add token key on isserTokenIndex.
            this.tokenStorage.add(newToken);
        } catch(NullPointerException npE){
            npE.printStackTrace();
            throw new Exception("Internal error...");
        }
    }

    public Token update(int position, Token editedToken) throws Exception {
        try {
            Token oldToken = this.get(position);
            if (oldToken == null)
                throw new Exception("Token not found.");
            if (editedToken.getIssuer() == null || editedToken.getLabel() == null)
                throw new Exception("Issuer or Label not found.");

            editedToken = this.tokenStorage.update(oldToken.getID(), editedToken);
            if (editedToken == null)
                throw new Exception("Update token failed.");

            if (oldToken.getIssuer().compareTo(editedToken.getIssuer()) != 0) {//- update issuer token index.
                //- remove token key on old issuerTokenIndex.
                this.issuerStorage.removeTokenKeyOnIssuerIndex(oldToken.getIssuer(), oldToken.getID());
                //- add token key on new isserTokenIndex.
                this.issuerStorage.addTokenKeyOnIssuerIndex(editedToken.getIssuer(), editedToken.getID());
            }
            return editedToken;
        } catch(NullPointerException npE){
            npE.printStackTrace();
            throw new Exception("Internal error.");
        }
    }

    public void move(int fromPosition, int toPosition) {
        if (this.isFilterOn)
            this.issuerStorage.move(this.issuerFilterParameter, fromPosition, toPosition);
        else
            this.tokenStorage.move(fromPosition, toPosition);
    }

    public void delete(int position) {
        Token token = this.get(position);

        String key = token.getID();

        this.tokenStorage.delete(key);
        this.issuerStorage.removeTokenKeyOnIssuerIndex(token.getIssuer(), key);//- remove token key on issuerTokenIndex.
    }

    public String[] getIssuers(){
        return this.issuerStorage.getIssuers();
    }

    public void setFilter(String issuer){
        if (issuer != null && !issuer.isEmpty())
            this.isFilterOn = true;
        else
            this.isFilterOn = false;
        this.issuerFilterParameter = issuer;
    }

    /**
     * Update token async, because Image needs to be downloaded/copied to storage
     * @param context Application Context
     * @param editedToken Token (with Image, Image will be saved by the async task)
     */
    public static void updateAsync(Context context,
                                   final int position,
                                   final Token editedToken,
                                   final Callback callback) {
        File outFile = null;
        FreeOTPApplication application = (FreeOTPApplication) context.getApplicationContext();
        if(editedToken.getImage() != null)
            outFile = new File(application.getImageFolder(), editedToken.getImageFileName());
        new TokenAsyncTask(application, outFile, editedToken){
            @Override
            protected void onPostExecute(ReturnParams returnParams) {
                super.onPostExecute(returnParams);
                //we downloaded the image, now save/update it normally
                try {
                    Token resultToken = ((FreeOTPApplication)returnParams.context.getApplicationContext())
                            .getTokenPersistence().update(position, returnParams.getToken());
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
        File outFile = null;
        FreeOTPApplication application = (FreeOTPApplication) context.getApplicationContext();
        if(token.getImage() != null)
            outFile = new File(application.getImageFolder(), token.getImageFileName());
        new TokenAsyncTask(application, outFile, token){
            @Override
            protected void onPostExecute(ReturnParams returnParams) {
                super.onPostExecute(returnParams);
                //we downloaded the image, now save/update it normally
                try {
                    ((FreeOTPApplication)returnParams.context.getApplicationContext())
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

        private FreeOTPApplication application;
        private File imageOutput;
        private Token token;

        public TokenAsyncTask(FreeOTPApplication application, File imageOutput, Token token){
            this.application = application;
            this.imageOutput = imageOutput;
            this.token = token;
        }

        @Override
        protected ReturnParams doInBackground(Void... voids) {
            if(this.token.getImage() != null) {
                this.token = Utils.copyImageToStorage(this.application,
                        this.token,
                        this.imageOutput);
            }
            return new ReturnParams(this.token, this.application);
        }
    }
}
