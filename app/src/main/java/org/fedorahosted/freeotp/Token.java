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

package org.fedorahosted.freeotp;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.Application;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;

import androidx.annotation.Nullable;

import com.google.android.apps.authenticator.Base32String;
import com.google.android.apps.authenticator.Base32String.DecodingException;

import org.fedorahosted.freeotp.common.Constants;

public class Token {
    public static class TokenUriInvalidException extends Exception {
        private static final long serialVersionUID = -1108624734612362345L;
    }

//    public static enum TokenType2 {
//        HOTP, TOTP
//    }

    public static class TokenType {
        public static final String HOTP = "HOTP";
        public static final String TOTP = "TOTP";
    }

    private static char[] STEAMCHARS = new char[] {
            '2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C',
            'D', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q',
            'R', 'T', 'V', 'W', 'X', 'Y'};

    //otpauth://[type]/[issuerExt]%3A[label]?secret=[secret]&counter=[counter]&digits=[digits]&algorithm=[algorithm]&issuer=[issuer]
    private String issuer;//- the orignal issuer
    private String label;//- the orignal label
    private String image;//- the orignal image
    private String type;
    private String algo;
    private byte[] secret;
    private int digits;
    private long counter;
    private int period;
    private long id;//- For sort.

    /* Inner class that defines the database table contents */
    public static abstract class TokenEntry implements BaseColumns {
        public static final String TABLE_NAME = "tokens";
        public static final String COLUMN_NAME_ISSUER = "issuer";
        public static final String COLUMN_NAME_LABEL = "label";
        public static final String COLUMN_NAME_IMAGE = "image";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_ALGO = "algo";
        public static final String COLUMN_NAME_SECRET = "secret";
        public static final String COLUMN_NAME_DIGITS = "digits";
        public static final String COLUMN_NAME_COUNTER = "counter";
        public static final String COLUMN_NAME_PERIOD = "period";
        public static final String COLUMN_NAME_ID = "id";
    }

    private Token(Uri uri, boolean internal) throws TokenUriInvalidException {
        validateTokenURI(uri);

        String path = uri.getPath();
        // Strip the path of its leading '/'
        path = path.replaceFirst("/","");

        if (path.length() == 0)
            throw new TokenUriInvalidException();

        int i = path.indexOf(':');
        issuer = i < 0 ? "" : path.substring(0, i);
        if (issuer.isEmpty())
            issuer = uri.getQueryParameter("issuer");
        label = path.substring(i >= 0 ? i + 1 : 0);

        algo = uri.getQueryParameter("algorithm");
        if (algo == null)
            algo = "sha1";
        algo = algo.toUpperCase(Locale.US);
        try {
            Mac.getInstance("Hmac" + algo);
        } catch (NoSuchAlgorithmException e1) {
            throw new TokenUriInvalidException();
        }

        try {
            String d = uri.getQueryParameter("digits");
            if (d == null)
                d = "6";
            digits = Integer.parseInt(d);
            if (!issuer.equals("Steam") && digits != 6 && digits != 8)
                throw new TokenUriInvalidException();
        } catch (NumberFormatException e) {
            throw new TokenUriInvalidException();
        }

        try {
            String p = uri.getQueryParameter("period");
            if (p == null)
                p = "30";
            period = Integer.parseInt(p);
            period = (period > 0) ? period : 30; // Avoid divide-by-zero
        } catch (NumberFormatException e) {
            throw new TokenUriInvalidException();
        }

        if (TokenType.HOTP.compareTo(type) == 0) {
            try {
                String c = uri.getQueryParameter("counter");
                if (c == null)
                    c = "0";
                counter = Long.parseLong(c);
            } catch (NumberFormatException e) {
                throw new TokenUriInvalidException();
            }
        }

        try {
            String s = uri.getQueryParameter("secret");
            secret = Base32String.decode(s);
        } catch (DecodingException e) {
            throw new TokenUriInvalidException();
        } catch (NullPointerException e) {
            throw new TokenUriInvalidException();
        }

        image = uri.getQueryParameter("image");

        if (internal) {
            setIssuer(uri.getQueryParameter("issueralt"));
            setLabel(uri.getQueryParameter("labelalt"));
        }
    }

    private void validateTokenURI(Uri uri) throws TokenUriInvalidException{
        if (uri == null) throw new TokenUriInvalidException();

        if (uri.getScheme() == null || !uri.getScheme().equals("otpauth")){
            throw new TokenUriInvalidException();
        }

        if (uri.getAuthority() == null) throw new TokenUriInvalidException();

        if (uri.getAuthority().equals("totp")) {
            type = TokenType.TOTP;
        } else if (uri.getAuthority().equals("hotp"))
            type = TokenType.HOTP;
        else {
            throw new TokenUriInvalidException();
        }

        if (uri.getPath() == null) throw new TokenUriInvalidException();
    }

    private String getHOTP(long counter) {
        // Encode counter in network byte order
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(counter);

        // Create digits divisor
        int div = 1;
        for (int i = digits; i > 0; i--)
            div *= 10;

        // Create the HMAC
        try {
            Mac mac = Mac.getInstance("Hmac" + algo);
            mac.init(new SecretKeySpec(secret, "Hmac" + algo));

            // Do the hashing
            byte[] digest = mac.doFinal(bb.array());

            // Truncate
            int binary;
            int off = digest[digest.length - 1] & 0xf;
            binary = (digest[off] & 0x7f) << 0x18;
            binary |= (digest[off + 1] & 0xff) << 0x10;
            binary |= (digest[off + 2] & 0xff) << 0x08;
            binary |= (digest[off + 3] & 0xff);

            String hotp = "";
            if (issuer.equals("Steam")) {
                for (int i = 0; i < digits; i++) {
                    hotp += STEAMCHARS[binary % STEAMCHARS.length];
                    binary /= STEAMCHARS.length;
                }
            } else {
                binary = binary % div;

                // Zero pad
                hotp = Integer.toString(binary);
                while (hotp.length() != digits)
                    hotp = "0" + hotp;
            }

            return hotp;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

//    public Token(String uri, boolean internal) throws TokenUriInvalidException {
//        this(Uri.parse(uri), internal);
//    }

    public Token(Uri uri) throws TokenUriInvalidException {
        this(uri, false);
    }

    public Token(String uri) throws TokenUriInvalidException {
        this(Uri.parse(uri));
    }

    public Token(){

    }

    // NOTE: This changes internal data. You MUST save the token immediately.
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    // NOTE: This changes internal data. You MUST save the token immediately.
    public void setLabel(String label) {
        this.label = label;
    }

    public void setImage(Uri image) {
        if (image == null)
            this.image = null;
        else
            this.image = image.toString();
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIssuer() {
        return this.issuer;
    }
    public String getLabel() {
        return this.label;
    }
    public int getDigits() {
        return digits;
    }
    public byte[] getSecret() {
        return secret;
    }
    public int getPeriod() {
        return period;
    }
    public String getType() {
        return type;
    }
    public Uri getImage() {
        if (image != null && !image.isEmpty())
            return Uri.parse(image);
        return null;
    }
    public long getCounter() {
        return counter;
    }
    public long getId() {
        return id;
    }
    public String getAlgo(){
        return this.algo;
    }

    // NOTE: This may change internal data. You MUST save the token immediately.
    public TokenCode generateCodes() {
        long cur = System.currentTimeMillis();

        switch (type) {
        case TokenType.HOTP:
            return new TokenCode(getHOTP(counter++), cur, cur + (period * 1000));

        case TokenType.TOTP:
            long counter = cur / 1000 / period;
            return new TokenCode(getHOTP(counter + 0),
                                 (counter + 0) * period * 1000,
                                 (counter + 1) * period * 1000,
                   new TokenCode(getHOTP(counter + 1),
                                 (counter + 1) * period * 1000,
                                 (counter + 2) * period * 1000));
        }

        return null;
    }

    public Uri toUri() {
        String issuerLabel = !issuer.equals("") ? issuer + ":" + label : label;

        Uri.Builder builder = new Uri.Builder().scheme("otpauth").path(issuerLabel)
                .appendQueryParameter("secret", Base32String.encode(secret))
                .appendQueryParameter("issuer", issuer)
                .appendQueryParameter("algorithm", algo)
                .appendQueryParameter("digits", Integer.toString(digits))
                .appendQueryParameter("period", Integer.toString(period));

        switch (type) {
        case TokenType.HOTP:
            builder.authority("hotp");
            builder.appendQueryParameter("counter", Long.toString(counter + 1));
            break;
        case TokenType.TOTP:
            builder.authority("totp");
            break;
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return toUri().toString();
    }

    /**
     * delete image, which is attached to the token from storage
     */
//    public void deleteImage() {
//        Uri imageUri = getImage();
//        if (imageUri != null) {
//
//            File image = new File(imageUri.getPath());
//            File applicationFolder  = image.getParentFile().getParentFile().getParentFile();
//            //- Only delete if image belong to application's folder.
//            if (image.exists() &&
//                image.getParentFile() != null &&
//                image.getParentFile().getParentFile() !=null &&
//                image.getParentFile().getParentFile().getParentFile() != null &&
//                applicationFolder.exists() &&
//                applicationFolder.getName().compareTo(BuildConfig.APPLICATION_ID) == 0){
//                image.delete();
//            }
//        }
//    }

    //- Return token image filename.
    //- Token's image can be exist or not.
    //- But if it is, then this is the name of it.
    public String getImageFileName(){
        String fileName;
        if (issuer != null && !issuer.equals(""))
            fileName = issuer+ ":" + label;
        else
            fileName = label;

        return Constants.TOKEN_PREFIX_ID + fileName + ".png";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Token))
            return false;

        //- Not compare image with image, because its only for visual.
        //- Same token can have two different image on both backup and app's data.
        Token token = (Token) obj;
        if (this.issuer.compareTo(token.issuer) != 0)
            return false;
        else if (this.label.compareTo(token.label) != 0)
            return false;
        else if (this.algo.compareTo(token.algo) != 0)
            return false;
        else if (this.digits != token.digits)
            return false;
        else if (this.period != token.period)
            return false;
        else if (!Arrays.equals(this.secret, token.secret))
            return false;
        else if (this.type.compareTo(token.type) != 0)
            return false;
        else if (this.type.compareTo(TokenType.HOTP) == 0 && this.counter != token.counter )
            return false;
        else
            return true;
    }
}
