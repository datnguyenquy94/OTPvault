OTPVault is a fork, and re-design from ground up of [FreeOTP](https://freeotp.github.io/) two factor authentication (2fa), focus on privacy and security. 

New features:
* Password protect.
    + Re-lock: on startup.
    + Re-lock: on screen turn off.
    + Re-lock: after X times.
* Full data encryption: 256-bit AES via SQLCipher encrypt by user password.
* Change encrypt password. 
* Import/backup data 
    + Manually backup
    + Auto via thirdparty (tasker, etc...) via broadcast intent.
* Support theme light and dark.

Suppoprt standards:
* HOTP (HMAC-Based One-Time Password Algorithm) [RFC 4226](http://www.ietf.org/rfc/rfc4226.txt)
* TOTP (Time-Based One-Time Password Algorithm) [RFC 6238](http://www.ietf.org/rfc/rfc6238.txt)		


<img src="/captures/1.png" width="200"> <img src="/captures/2.png" width="200"> <img src="/captures/3.png" width="200">

<img src="/captures/4.png" width="200"> <img src="/captures/5.png" width="200"> <img src="/captures/6.png" width="200">

<img src="/captures/7.png" width="200"> <img src="/captures/8.png" width="200"> <img src="/captures/9.png" width="200">

----- How to backup via third-party apps
* Other apps can trigger OTPVault to create a backup by send a broadcast intent "org.ngyuen.otpvaut.broadcast.BackupBroadcastTrigger" to OTPVault. 
* All backup files will still be encrypted. To read/import backup, you will need the app's password at backup moment.  
* The backup output can be set up in Settings, and give third-party apps/user optional to choice what will do next, (copy to new location, send backup to google drive or dropbox, or upload to your private storage, etc...)

Below is an exmaple of use tasker to backup and send backup file to google drive.

<img src="/captures/tasker.png" width="200">

* Create backup.
* Wait for 30 seconds before continue.
* Sign in google driver (first time) and upload backup with name as (%DATE - Backup.zip)
* Delete backup file in local storage.
* Make 3 beeps to to signal that backup is success.



----- Third-party libraries:
* com.google.zxing:core
* com.google.code.gson
* com.squareup.picasso
* io.fotoapparat.fotoapparat
* com.github.hedzr:android-file-chooser
* com.google.android.material
* com.google.android.material
* androidx.lifecycle:lifecycle-extensions
* net.zetetic:android-database-sqlcipher

---- License

OTPVault is licensed under the Apache 2.0, see [COPYING](COPYING).
