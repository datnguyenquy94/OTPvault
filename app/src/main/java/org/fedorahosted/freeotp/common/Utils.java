package org.fedorahosted.freeotp.common;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.UUID;

public class Utils {

    private static final String LOG_TAG = Utils.class.getName();

    public static String generateUniqueId() {
        long time = System.currentTimeMillis();
        return UUID.randomUUID().toString().concat(time+"").toUpperCase();
    }

    public static String OTP_URI_BUILDER(String type, String issuer, String label,
                                         String secret, String counter, String digits,
                                         String algorithm){

        type        = type.toLowerCase().trim();
        issuer      = issuer.trim();
        label       = label.trim();
        secret      = secret.trim();
        counter     = counter.trim();
        digits      = digits.trim();
        algorithm   = algorithm.toLowerCase().trim();

        return MessageFormat.format("otpauth://{0}/{1}%3A{2}?secret={3}&counter={4}&digits={5}&algorithm={6}&issuer={1}",
                new Object[]{
                        type, issuer, label,
                        secret, counter, digits,
                        algorithm
                });
    }

    public static void clearFolder(File file, int depth){
        File[] files = file.listFiles();
        for (File e: files){
            if (e.isDirectory())
                clearFolder(e, depth+1);
            else
                e.delete();
        }
        if (depth > 0)
            file.delete();
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static Token copyImageToStorage(Context context, Token token, File outFile){
        if (token == null)
            return null;
        if (outFile == null || context == null){
            token.setImage(null);
            return token;
        }

        File tokenImage = new  File(token.getImage().getPath());
        if (Uri.fromFile(outFile).compareTo(token.getImage()) == 0) {//- Same file, then do nothing and just return.
            return token;
        } else if ( //- Both files's location are on application's imageFolder. In this case move token's image to outFile.
                tokenImage.exists() &&
                outFile.getParentFile().getAbsolutePath().compareTo(tokenImage.getParentFile().getAbsolutePath()) == 0) {
            if (outFile.exists())
                outFile.delete();
            tokenImage.renameTo(outFile);
            token.setImage(Uri.fromFile(outFile));
            return token;
        } else {
            try {
                Bitmap bitmap = Picasso.with(context)
                        .load(token.getImage())
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .resize(200, 200)   // it's just an icon
                        .onlyScaleDown()    //resize image, if bigger than 200x200
                        .get();
                //saveAsync image
                FileOutputStream out = new FileOutputStream(outFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, out);
                out.close();
                token.setImage(Uri.fromFile(outFile));
            } catch (IOException e) {
                e.printStackTrace();
                //set image to null to prevent internet link in image, in case image
                //was scanned, when no connection existed
                token.setImage(null);
            }
            return token;
        }
    }

    public static String bytesToString(byte[] bytes){
        if (bytes == null || bytes.length <=0)
            return "";

        StringBuilder result = new StringBuilder();
        result.append('[');
        result.append(bytes[0]);
        for (int i = 1; i<bytes.length; i++){
            result = result.append(",");
            result = result.append(bytes[i]);
        }
        result.append(']');

        return result.toString();
    }

    public static byte[] stringToBytes(String str){
        String[] bytes = str.split(",");
        byte[] result = new byte[bytes.length];

        for (int i = 0; i<bytes.length; i++)
            result[i] = Byte.parseByte(bytes[i]);

        return result;
    }

    public static int getThemeColor(Context context, int attr){
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, typedValue, true))
            return typedValue.data;
        else
            return Color.TRANSPARENT;
    }

    public static void setTheme(Activity activity, boolean isDialogMode){
        String theme = PreferenceManager.getDefaultSharedPreferences(activity).getString("theme", "light").toLowerCase();
        if (theme.compareTo("light") == 0 && isDialogMode == true){
            activity.setTheme(R.style.Light_Dialog);
        } else if (theme.compareTo("light") == 0 && isDialogMode == false) {
            activity.setTheme(R.style.Light);
        } else if (theme.compareTo("dark") == 0 && isDialogMode == true){
            activity.setTheme(R.style.Dark_Dialog);
//            activity.setTheme(R.style.Dark);
        } else if (theme.compareTo("dark") == 0 && isDialogMode == false){
            activity.setTheme(R.style.Dark);
        } else {
            activity.setTheme(R.style.Light);
        }
    }

//    public static final char[] specialChars = new char[] {
//            ',',
//            '.',
//            '/',
//            '\\',
//            '{',
//            '}'
//    };
//
//    public static boolean isContainAnySpecialChars(String str){
//        for (char c: specialChars)
//            if (str.indexOf(c)>=0)
//                return true;
//        return false;
//    }

    public static void main(String[] args) throws Token.TokenUriInvalidException {
        String uri = Utils.OTP_URI_BUILDER("totp", "google", "castiel.q27", "654eokisof7ofbkltwliunjxbzmaatf4", "1", "6","sha1");
        Token token = new Token(uri);
        System.out.println(token.generateCodes());
    }
}
