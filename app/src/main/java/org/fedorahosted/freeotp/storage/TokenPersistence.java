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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.Token.TokenUriInvalidException;
import org.fedorahosted.freeotp.activities.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

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

    public void save(Token newToken) {
        try {
            String key = newToken.getID();

            if (this.tokenStorage.tokenExists(key)) {//if token exists, check issuer labels before save and update token.
                Token oldToken = this.tokenStorage.get(key);
                if (oldToken != null &&
                    oldToken.getIssuer().compareTo(newToken.getIssuer()) != 0) {//- update issuer token index.
                    //- remove token key on old issuerTokenIndex.
                    this.issuerStorage.removeTokenKeyOnIssuerIndex(oldToken.getIssuer(), key);
                    //- add token key on new isserTokenIndex.
                    this.issuerStorage.addTokenKeyOnIssuerIndex(newToken.getIssuer(), key);
                }
            } else {//- new token, add issuer labels before save token.
                this.issuerStorage.addTokenKeyOnIssuerIndex(newToken.getIssuer(), key);//- add token key on isserTokenIndex.
            }
            this.tokenStorage.save(newToken);
        } catch(NullPointerException npE){
            npE.printStackTrace();
        }
    }

    public void move(int fromPosition, int toPosition) {
        if (this.isFilterOn)
            this.issuerStorage.move(this.issuerFilterParameter, fromPosition, toPosition);
        else
            this.tokenStorage.move(fromPosition, toPosition);
    }

    public void delete(int position) {
        Token token;
        if (this.isFilterOn)
            token = this.tokenStorage.get(this.issuerStorage.getKey(this.issuerFilterParameter, position));
        else
            token = this.tokenStorage.get(position);

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
     * Save token async, because Image needs to be downloaded/copied to storage
     * @param context Application Context
     * @param token Token (with Image, Image will be saved by the async task)
     */
    public static void saveAsync(Context context, final Token token) {
        File outFile = null;
        FreeOTPApplication application = (FreeOTPApplication) context.getApplicationContext();
        if(token.getImage() != null)
            outFile = new File(application.getImageFolder(), token.getImageFileName());
        new SaveTokenTask().execute(new TaskParams(token, outFile, context));
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
     * Data class for SaveTokenTask
     */
    private static class TaskParams {
        private final File outFile;
        private final Context mContext;
        private final Token token;

        public TaskParams(Token token, File outFile, Context mContext) {
            this.token = token;
            this.outFile = outFile;
            this.mContext = mContext;
        }

        public Context getContext() {
            return mContext;
        }

        public Token getToken() {
            return token;
        }

        public File getOutFile() {
            return outFile;
        }
    }

    /**
     * Downloads/copies images to FreeOTP storage
     * Saves token in PostExecute
     */
    private static class SaveTokenTask extends AsyncTask<TaskParams, Void, ReturnParams> {
        private TokenPersistence tokenPersistence;
        protected ReturnParams doInBackground(TaskParams... params) {
            final TaskParams taskParams = params[0];
            this.tokenPersistence = ((FreeOTPApplication)taskParams.getContext().getApplicationContext())
                    .getTokenPersistence();
            if(taskParams.getToken().getImage() != null) {
                try {
                    Bitmap bitmap = Picasso.with(taskParams.getContext())
                            .load(taskParams.getToken()
                            .getImage())
                            .resize(200, 200)   // it's just an icon
                            .onlyScaleDown()    //resize image, if bigger than 200x200
                            .get();
                    File outFile = taskParams.getOutFile();
                    //saveAsync image
                    FileOutputStream out = new FileOutputStream(outFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, out);
                    out.close();
                    taskParams.getToken().setImage(Uri.fromFile(outFile));
                } catch (IOException e) {
                    e.printStackTrace();
                    //set image to null to prevent internet link in image, in case image
                    //was scanned, when no connection existed
                    taskParams.getToken().setImage(null);
                }
            }
            return new ReturnParams(taskParams.getToken(), taskParams.getContext());
        }

        @Override
        protected void onPostExecute(ReturnParams returnParams) {
            super.onPostExecute(returnParams);
            //we downloaded the image, now save it normally
            this.tokenPersistence.save(returnParams.getToken());
            //refresh TokenAdapter
            returnParams.context.sendBroadcast(new Intent(MainActivity.ACTION_IMAGE_SAVED));
        }
    }
}
