package org.ngyuen.otpvault.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.ngyuen.otpvault.OTPVaultApplication;
import org.ngyuen.otpvault.Token;
import org.ngyuen.otpvault.common.Constants;

import java.io.File;

public class TokenDbHelper extends SQLiteOpenHelper {
    private static TokenDbHelper instance;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = Constants.DbStorageFile;

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BLOB_TYPE = " BLOB";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Token.TokenEntry.TABLE_NAME + " (" +
                    Token.TokenEntry.COLUMN_NAME_ISSUER + TEXT_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_LABEL + TEXT_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_IMAGE + TEXT_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_TYPE + TEXT_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_ALGO + TEXT_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_SECRET + TEXT_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_DIGITS + INTEGER_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_COUNTER + INTEGER_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_PERIOD + INTEGER_TYPE + "," +
                    Token.TokenEntry.COLUMN_NAME_ID + INTEGER_TYPE + "," +
                    "UNIQUE(" +Token.TokenEntry.COLUMN_NAME_ISSUER + "," +Token.TokenEntry.COLUMN_NAME_LABEL + ")" + "," +
                    "PRIMARY KEY(" + Token.TokenEntry.COLUMN_NAME_ID  + ")" +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Token.TokenEntry.TABLE_NAME;

    public TokenDbHelper(Context context) {
        super(new DatabaseContext(context), DATABASE_NAME, null, DATABASE_VERSION);
    }

    static public synchronized TokenDbHelper getInstance(Context context) {
        if (instance == null) {
            SQLiteDatabase.loadLibs(context);
            instance = new TokenDbHelper(context);
        }
        return instance;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    private static class DatabaseContext extends ContextWrapper {
        private static final String LOG_TAG = DatabaseContext.class.getName();
        private OTPVaultApplication application;
        public DatabaseContext(Context base) {
            super(base);
            this.application = (OTPVaultApplication) this.getApplicationContext();
        }
        @Override
        public File getDatabasePath(String name)  {
            File databaseFolder = this.application.getDatabaseFolder();

            String dbfile = databaseFolder.getAbsolutePath() + File.separator+ name;

            File result = new File(dbfile);

            if (!result.getParentFile().exists()) {
                result.getParentFile().mkdirs();
            }

            if (Log.isLoggable(LOG_TAG, Log.WARN)) {
                Log.w(LOG_TAG, "getDatabasePath(" + name + ") = " + result.getAbsolutePath());
            }

            return result;
        }
    }
}

