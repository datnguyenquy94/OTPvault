package org.fedorahosted.freeotp.storage;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.fedorahosted.freeotp.FreeOTPApplication;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.common.Constants;
import org.fedorahosted.freeotp.common.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TokenDbStorage {
    private static final String LOG_TAG = TokenDbStorage.class.getName();
    private FreeOTPApplication application;
    private Gson gson;

    public TokenDbStorage(FreeOTPApplication application){
        this.application = application;
        this.gson = this.application.getGson();
    }

    public List<Long> getTokenIndex(){
        List<Long> index = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Cursor cursor = sqLiteDatabase.query(Token.TokenEntry.TABLE_NAME,
                    new String[] {Token.TokenEntry.COLUMN_NAME_ID},
                    null,
                    null,
                    null,
                    null,
                    Token.TokenEntry.COLUMN_NAME_ID);

            if (cursor.getCount() > 0){
                while(cursor.moveToNext())
                    index.add(cursor.getLong(0));
            }
            cursor.close();
        }
        return index;
    }

    //- With issuer filter on
    public List<Long> getTokenIndex(String issuer){
        List<Long> index = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Cursor cursor = sqLiteDatabase.query(Token.TokenEntry.TABLE_NAME,
                    new String[] {Token.TokenEntry.COLUMN_NAME_ID},
                    Token.TokenEntry.COLUMN_NAME_ISSUER + "=?",
                    new String[] { issuer },
                    null,
                    null,
                    Token.TokenEntry.COLUMN_NAME_ID);

            if (cursor.getCount() > 0){
                while(cursor.moveToNext())
                    index.add(cursor.getLong(0));
            }
            cursor.close();
        }
        return index;
    }

    //- Get all token's issuers. (Google, Facebook, Github, etc...)
    public List<String> getIssuers(){
        List<String> index = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Cursor cursor = sqLiteDatabase.query(true,
                    Token.TokenEntry.TABLE_NAME,
                    new String[] {Token.TokenEntry.COLUMN_NAME_ISSUER},
                    null,
                    null,
                    null,
                    null,
                    Token.TokenEntry.COLUMN_NAME_ID,
                    null);

            if (cursor.getCount() > 0){
                while(cursor.moveToNext())
                    index.add(cursor.getString(0));
            }
            cursor.close();
        }
        return index;
    }

    public Token get(Long tokenIndex){
        Token token = null;
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Cursor cursor = sqLiteDatabase.query(Token.TokenEntry.TABLE_NAME,
                    null,
                    Token.TokenEntry.COLUMN_NAME_ID+"=?",
                    new String[] {tokenIndex.toString()},
                    null,
                    null,
                    null);

            if (cursor.getCount() < 1)
                Log.d(LOG_TAG, "Token not found. TokenIndex="+tokenIndex);
            else if (cursor.getCount() > 1)
                Log.d(LOG_TAG, "Found more than one token with TokenIndex="+tokenIndex + ". Return null as its error.");
            else {//= TokenIndex is primary key, the result should be only one.
                cursor.moveToFirst();
                //- Lazy.EXE
                JsonObject jsonObject = new JsonObject();
                for (int i = 0; i<cursor.getColumnCount(); i++){
                    switch (cursor.getType(i)){
                        case Cursor.FIELD_TYPE_NULL:{
                            jsonObject.add(cursor.getColumnName(i), null);
                            break;
                        }
                        case Cursor.FIELD_TYPE_STRING:{
                            jsonObject.addProperty(cursor.getColumnName(i), cursor.getString(i));
                            break;
                        }
                        case Cursor.FIELD_TYPE_INTEGER:{
                            jsonObject.addProperty(cursor.getColumnName(i), cursor.getLong(i));
                            break;
                        }
                        case Cursor.FIELD_TYPE_BLOB: {
                            byte[] bytes = cursor.getBlob(i);
                            JsonArray jsonArray = new JsonArray();
                            for  (byte e: bytes)
                                jsonArray.add(e);
                            jsonObject.add(cursor.getColumnName(i), jsonArray);
                        }
                    }
                }
                token = this.jsonToToken(jsonObject);
            }
            cursor.close();
        }
        return token;
    }

    public Token[] getAll(SQLiteDatabase sqLiteDatabase){
        List<Token> tokens = new ArrayList<Token>();
        if (sqLiteDatabase != null) {
            Cursor cursor = sqLiteDatabase.rawQuery("select * from " + Token.TokenEntry.TABLE_NAME,
                    null);

            while(cursor.moveToNext()){
                //- Lazy.EXE
                JsonObject jsonObject = new JsonObject();
                for (int i = 0; i<cursor.getColumnCount(); i++){
                    switch (cursor.getType(i)){
                        case Cursor.FIELD_TYPE_NULL:{
                            jsonObject.add(cursor.getColumnName(i), null);
                            break;
                        }
                        case Cursor.FIELD_TYPE_STRING:{
                            jsonObject.addProperty(cursor.getColumnName(i), cursor.getString(i));
                            break;
                        }
                        case Cursor.FIELD_TYPE_INTEGER:{
                            jsonObject.addProperty(cursor.getColumnName(i), cursor.getLong(i));
                            break;
                        }
                        case Cursor.FIELD_TYPE_BLOB: {
                            byte[] bytes = cursor.getBlob(i);
                            JsonArray jsonArray = new JsonArray();
                            for  (byte e: bytes)
                                jsonArray.add(e);
                            jsonObject.add(cursor.getColumnName(i), jsonArray);
                        }
                    }
                }
                Token token = this.jsonToToken(jsonObject);
                tokens.add(token);
            }
            cursor.close();
        }
        return tokens.toArray(new Token[tokens.size()]);
    }

    public void add(Token newToken) throws Exception {
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
//            String tmp = gson.toJson(newToken);
            ContentValues values = new ContentValues();
            values.put(Token.TokenEntry.COLUMN_NAME_ISSUER, newToken.getIssuer());
            values.put(Token.TokenEntry.COLUMN_NAME_LABEL, newToken.getLabel());
            values.put(Token.TokenEntry.COLUMN_NAME_TYPE, newToken.getType());
            values.put(Token.TokenEntry.COLUMN_NAME_ALGO, newToken.getAlgo());
            values.put(Token.TokenEntry.COLUMN_NAME_SECRET, newToken.getSecret());
            values.put(Token.TokenEntry.COLUMN_NAME_DIGITS, newToken.getDigits());
            values.put(Token.TokenEntry.COLUMN_NAME_COUNTER, newToken.getCounter());
            values.put(Token.TokenEntry.COLUMN_NAME_PERIOD, newToken.getPeriod());
//            values.put(Token.TokenEntry.COLUMN_NAME_ID, newToken.getId());

            if (newToken.getImage() != null)
                values.put(Token.TokenEntry.COLUMN_NAME_IMAGE, newToken.getImage());
            else
                values.put(Token.TokenEntry.COLUMN_NAME_IMAGE, "");

            long result = sqLiteDatabase.insert(Token.TokenEntry.TABLE_NAME, null, values);

            if (result < 0)
                throw new Exception("Unable to add token.");
        }
    }

    public void update(Long id, Token modifiedToken) throws Exception{
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {

            if (modifiedToken == null)
                throw new Exception("ModifiedToken is null.");
            else if (modifiedToken.getIssuer() == null || modifiedToken.getIssuer().isEmpty())
                throw new Exception("ModifiedToken's issuer is empty.");
            else if (modifiedToken.getLabel() == null || modifiedToken.getLabel().isEmpty())
                throw new Exception("ModifiedToken's label is empty.");
            else if (modifiedToken.getCounter() < 0)
                throw new Exception("ModifiedToken's count smaller than zero.");

            ContentValues values = new ContentValues();
            values.put(Token.TokenEntry.COLUMN_NAME_ISSUER, modifiedToken.getIssuer());
            values.put(Token.TokenEntry.COLUMN_NAME_LABEL, modifiedToken.getLabel());
            values.put(Token.TokenEntry.COLUMN_NAME_COUNTER, modifiedToken.getCounter());
//            values.put(Token.TokenEntry.COLUMN_NAME_ID, modifiedToken.getId());

            if (modifiedToken.getImage() != null)
                values.put(Token.TokenEntry.COLUMN_NAME_IMAGE, modifiedToken.getImage());
            else
                values.put(Token.TokenEntry.COLUMN_NAME_IMAGE, "");

//            values.put(Token.TokenEntry.COLUMN_NAME_TYPE, modifiedToken.getType());
//            values.put(Token.TokenEntry.COLUMN_NAME_ALGO, modifiedToken.getAlgo());
//            values.put(Token.TokenEntry.COLUMN_NAME_SECRET, Utils.bytesToString(modifiedToken.getSecret()));
//            values.put(Token.TokenEntry.COLUMN_NAME_DIGITS, modifiedToken.getDigits());
//            values.put(Token.TokenEntry.COLUMN_NAME_PERIOD, modifiedToken.getPeriod());

            long result = sqLiteDatabase.update(Token.TokenEntry.TABLE_NAME,
                    values,
                    Token.TokenEntry.COLUMN_NAME_ID+"=?", new String[] { id.toString() });

            if (result < 0)
                throw new Exception("Unable to update token.");
        }
    }

    public void delete(Long tokenIndex) throws Exception {
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Token token = this.get(tokenIndex);
            if (token == null)
                throw new Exception("Unable to get token. TokenIndex="+tokenIndex);

            long result = sqLiteDatabase.delete(Token.TokenEntry.TABLE_NAME,
                    Token.TokenEntry.COLUMN_NAME_ID+"=?",
                    new String[] { tokenIndex.toString() });

            if (result < 0)
                throw new Exception("Unable to delete token.");
//            this.deleteTokenImage(token);
        }
    }

    public boolean exist(Long tokenIndex) throws Exception {
        boolean result = false;
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Cursor cursor = sqLiteDatabase.query(Token.TokenEntry.TABLE_NAME,
                    new String[] {Token.TokenEntry.COLUMN_NAME_ID },
                    Token.TokenEntry.COLUMN_NAME_ID+"=?",
                    new String[] {tokenIndex.toString()},
                    null,
                    null,
                    null);

            if (cursor.getCount() < 1)
                result = false;
            else if (cursor.getCount() == 1)//= TokenIndex is primary key, the result should be only one.
                result = true;
            else {
                throw new Exception("There is more than one token with same TokenIndex. TokenIndex=" + tokenIndex);
            }
            cursor.close();
        }
        return result;
    }

    public Token exist(String issuer, String label) throws Exception {
        Token result = null;
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Cursor cursor = sqLiteDatabase.query(Token.TokenEntry.TABLE_NAME,
                    null,
                    Token.TokenEntry.COLUMN_NAME_ISSUER+"=? and " +
                            Token.TokenEntry.COLUMN_NAME_LABEL+"=?",
                    new String[] {issuer, label},
                    null,
                    null,
                    null);

            if (cursor.getCount() == 1) {//= Issuer and Label combie is unquie, it should exsit only one.
                cursor.moveToFirst();
                //- Lazy.EXE
                JsonObject jsonObject = new JsonObject();
                for (int i = 0; i<cursor.getColumnCount(); i++){
                    switch (cursor.getType(i)){
                        case Cursor.FIELD_TYPE_NULL:{
                            jsonObject.add(cursor.getColumnName(i), null);
                            break;
                        }
                        case Cursor.FIELD_TYPE_STRING:{
                            jsonObject.addProperty(cursor.getColumnName(i), cursor.getString(i));
                            break;
                        }
                        case Cursor.FIELD_TYPE_INTEGER:{
                            jsonObject.addProperty(cursor.getColumnName(i), cursor.getLong(i));
                            break;
                        }
                        case Cursor.FIELD_TYPE_BLOB: {
                            byte[] bytes = cursor.getBlob(i);
                            JsonArray jsonArray = new JsonArray();
                            for  (byte e: bytes)
                                jsonArray.add(e);
                            jsonObject.add(cursor.getColumnName(i), jsonArray);
                        }
                    }
                }
                result = this.jsonToToken(jsonObject);
            } else if (cursor.getCount() < 1)
                result = null;
            else {
                throw new Exception("There is more than one token with same Issuer and Label. Issuer=" + issuer + " label="+label);
            }
            cursor.close();
        }
        return result;
    }

    private Token jsonToToken(JsonObject jsonObject){
        String str = jsonObject.toString();
        try {
            return gson.fromJson(str, Token.class);
        } catch (JsonSyntaxException jse) {
            jse.printStackTrace();
            return null;
        }
    }

    public void move(Long tokenIndex1, Long tokenIndex2) throws Exception {
        if (tokenIndex1.equals(tokenIndex2))
            return;
        SQLiteDatabase sqLiteDatabase = this.application.getDbStorage();
        if (sqLiteDatabase != null) {
            Token token1 = this.get(tokenIndex1);
            Token token2 = this.get(tokenIndex2);

            if (token1 == null || token2 == null)
                throw new Exception("Unable to get token data. TokenIndex1="+tokenIndex1+ ", TokenIndex2="+tokenIndex2);

            //- Switch tokens index.
            long tmp = token1.getId();
            token1.setId(token2.getId());
            token2.setId(tmp);

            //- Save tokens with index had been switched.

            //- To avoid UNIQUE constraint on Issuer and Label when switch. Set token1's issuer to something not exsit in database yet.
            String originalIssuer = token1.getIssuer();
            while(this.exist(token1.getIssuer(), token1.getLabel()) != null){
                token1.setIssuer(token1.getIssuer()+".tmp");
            }
            this.update(token1.getId(), token1);//- Save token1 with temporary issuer.
            this.update(token2.getId(), token2);//- Save token2

            token1.setIssuer(originalIssuer);//- Restore token1 issuer.
            this.update(token1.getId(), token1);//- And save it.

        }
    }

    /**
     * delete image, which is attached to the token from storage
     */
//    public void deleteTokenImage(Token token) {
//        Uri imageUri = token.getImage();
//        if (imageUri != null) {
//
//            File image = new File(imageUri.getPath());
//            File imageFolder = this.application.getImageFolder();
//            //- Only delete if image belong to application's imageFolder.
//            if (image.exists() &&
//                    image.getAbsolutePath().indexOf(imageFolder.getAbsolutePath()) == 0){
//                image.delete();
//            }
//        }
//        token.setImage(null);
//    }
}
