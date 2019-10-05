package org.fedorahosted.freeotp;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
