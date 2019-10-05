package org.fedorahosted.freeotp.storage;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.fedorahosted.freeotp.Constants;
import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.Token;

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

    public Token get(int position) {
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

    public void save(Token newToken) {
        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
        if (sharedPreferences != null) {
            String key = newToken.getID();

            if (!sharedPreferences.contains(key)) {//this is a new token, add it's id to lstTokens.
                this.tokenIndex.add(0, key);
                this.updateTokenIndex(this.tokenIndex);
            }
            sharedPreferences.edit().putString(key, gson.toJson(newToken)).apply();
        }
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
            this.tokenIndex.remove(key);
            this.updateTokenIndex(this.tokenIndex);
            sharedPreferences.edit().remove(key).apply();
        }
    }
}
