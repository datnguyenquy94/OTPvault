package org.fedorahosted.freeotp.storage;

import android.content.SharedPreferences;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.fedorahosted.freeotp.common.Constants;
import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.Token;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TokenStorage {
    private FreeOTPApplication application;
    private Gson gson;
    private List<String> tokenIndex = null;//- Array of labels.

    public TokenStorage(FreeOTPApplication application, Gson gson){
        this.application = application;
        this.gson = gson;
        this.loadTokenIndex();
    }

    private List<String> loadTokenIndex() {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            String str = sharedPreferences.getString(Constants.LST_TOKENS, "");
            if (!str.isEmpty())
                this.tokenIndex = new ArrayList<>(Arrays.asList(str.split(",")));
            else
                this.tokenIndex = new LinkedList<String>();
            return this.tokenIndex;
        } else {
            return new ArrayList<String>();
        }
    }

    private void updateTokenIndex(List<String> tokenIndex) {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            this.tokenIndex = tokenIndex;
            String str = String.join(",", this.tokenIndex);
            sharedPreferences.edit().putString(Constants.LST_TOKENS, str).apply();
        }
    }

    public int getTokenIndexLength() {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            if (this.tokenIndex == null)
                return 0;
            else
                return this.tokenIndex.size();
        } else {
            return 0;
        }
    }

    public boolean tokenExists(String key) {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            return sharedPreferences.contains(key);
        } else {
            return false;
        }
    }

    //- Should only be called on "public Token get(int position)" of TokenPersistence.java CCC
    //- Only use this function if filer is turn off.
    //- So its better to call get(position) via TokenPersisitence than this.
    public Token get(int position) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            String key = null;
            try {
                if (this.tokenIndex == null)
                    throw new NullPointerException("this.lstTokens is null.");
                key = this.tokenIndex.get(position);
                return this.get(key);
            } catch (NullPointerException npE) {
                npE.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public Token get(String key){
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            String str = null;
            try {
                str = sharedPreferences.getString(key, null);
                return gson.fromJson(str, Token.class);
            } catch (JsonSyntaxException jse) {
                // Backwards compatibility for URL-based persistence.
                try {
                    if (str != null)
                        return new Token(str, true);
                    else
                        return null;
                } catch (Token.TokenUriInvalidException tuie) {
                    tuie.printStackTrace();
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public void add(Token newToken) throws Exception {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            String key = newToken.getID();
            if (this.tokenExists(key))
                throw new Exception("Token exist.");
            else {
                this.tokenIndex.add(0, key);
                this.updateTokenIndex(this.tokenIndex);
                sharedPreferences.edit().putString(key, gson.toJson(newToken)).apply();
            }
        }
    }

    //- Only allow user to update token's isser, label, image and counter.
    public Token update(Token token, String newIssuer, String newLabel, Uri newImage, long newCounter) throws Exception {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            token = this.get(token.getID());
            if (token == null)
                throw new Exception("Token isn't exist.");
            else {

                token.setCounter(newCounter);
                if (token.getImage() == null || (token.getImage().compareTo(newImage) != 0) )
                    token.setImage(newImage);

                if (token.getIssuer().compareTo(newIssuer) != 0 ||
                    token.getLabel().compareTo(newLabel) != 0 ) {//- issuer or label changed, need to update token index key.
//                    if (token.getImage() != null){//- The image name is tied to tokenId. So if tokenId changed. The image need to be change too.
//                        File image = new File(token.getImage().getPath());
//                        if (image.exists()){
//                            image.renameTo(image.getParentFile(), token.getID());
//                        }
//                    }

                    int position = this.tokenIndex.indexOf(token.getID());//- get position of token's id in tokenIndex before update.
                    sharedPreferences.edit().remove(token.getID()).apply();//- if id is be changed, so token need to be removed and re-add with new Id.
                    //- update issuer and label, after that token id will be changed.
                    token.setIssuer(newIssuer);
                    token.setLabel(newLabel);
                    this.tokenIndex.set(position, token.getID());//- change token's id in tokenIndex.
                    this.updateTokenIndex(this.tokenIndex);
                }

                sharedPreferences.edit().putString(token.getID(), gson.toJson(token)).apply();
            }
        }
        return token;
    }

    public void move(int fromPosition, int toPosition) {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            if (fromPosition == toPosition)
                return;

            if (fromPosition < 0 || fromPosition > this.tokenIndex.size())
                return;
            if (toPosition < 0 || toPosition > this.tokenIndex.size())
                return;

            this.tokenIndex.add(toPosition, this.tokenIndex.remove(fromPosition));
            this.updateTokenIndex(this.tokenIndex);
        }
    }

    public void delete(int position) {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            String key = this.tokenIndex.get(position);
            this.delete(key);
        }
    }

    public void delete(String key){
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            Token token = this.get(key);
            token.deleteImage();
            this.tokenIndex.remove(key);
            this.updateTokenIndex(this.tokenIndex);
            sharedPreferences.edit().remove(key).apply();
        }
    }
}
