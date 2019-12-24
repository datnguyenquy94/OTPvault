package org.fedorahosted.freeotp.common;

public class Constants {

    public static String SharedPreferenceStoreFile = "SharedPreferenceStoreFile";
    public static String SharedPreferenceTemporary = "SharedPreferenceTemporary";//- Use this to read backup's SharedPreferenceStoreFile, Before import it.

    public static String DbStorageFile = "DbStorageFile";
    public static String DbStorageTemporaryFile = "DbStorageTemporaryFile";

    public static int PASSWORD_MINIMUM_LENGTH = 8;
    /*
    ###### tokens.xml FORMAT
        token.[id1]: data,
        token.[id2]: data,
        token.[id3]: data,

        lst.issuers: [//- labels
                issuer1,
                issuer2
        ],

        lst.tokens: [//- ids
                [id1],
                [id2],
                [id3]
        ],

        lst.tokens.by.issuer.issuer1: [//- ids
                [id1]
        ],

        lst.tokens.by.issuer.issuer2: [//- ids
                [id2],
                [id3]
        ],

        passwd.xxxxxxx: true
     */
    //- SharedPreference Prefix Key
    public static final String TOKEN_PREFIX_ID = "token.";
    public static final String LST_TOKENS      = "lst.tokens"; //==> lst ids of all tokens.
    public static final String LST_ISSUERS      = "lst.issuers";// ==> array of labels
    public static final String ISSUER_PREFIX_KEY    = "lst.tokens.by.issuer."; //- "lst.tokens.by.label.[LABEL_NAME] ==> lst ids of all token with same label.
    public static final String PASSWD_PREFIX_TEST_KEY = "passwd.";

    //- Backup suffix name
    public static final String MANUALLY_BACKUP_SUFFIX_NAME = "";
    public static final String THIRDPARTY_BACKUP_SUFFIX_NAME = ".third.party.backup";
    public static final String BEFORE_CHANGE_PASSWORD_BACKUP_SUFFIX_NAME = ".old.password";

    /*
        /root/
            /files/images/xxxx.png
            /shared_prefs/SharedPreferenceStoreFile.xml
     */
    public static final String IMAGE_FOLDER = "images";
    public static final String BACKUP_FOLDER = "backups";
    public static final String TMP_FOLDER = "tmp";
    public static final String DATABASE_FOLDER = "database";

    //- A prefix be added to importToken's label. If its conflict with another tokens already exist on app's data.
    public static final String CONFLICT_PREFIX = ".conflict";
}
