:: Getting Android key hash for Facebook app on Windows
:: Requirement: OpenSSL for Windows (http://code.google.com/p/openssl-for-windows/downloads/list)
:: Usage: set paths and run facebookkeydebug.bat
:: Usage: Set USERNAME to the current user and PASSWORD to the keystore passkey
:: Note: jre folder may be different (jre5, jre6, or jre7 for example) and you should change the Windows SSL folder to the correct one for your system

@echo Exporting keystore cert
"c:\Program Files\Java\jre7\bin\keytool.exe" -exportcert -alias alias -keystore "C:\Users\Mark\AppData\Local\GameMaker-Studio\NG_Keystore.keystore" -storepass kai12405 > debug.keystore.bin

@echo Converting to sha1
C:\Windows\SSL\bin\openssl sha1 -binary debug.keystore.bin > debug.keystore.sha1

@echo Converting to base64
C:\Windows\SSL\bin\openssl base64 -in debug.keystore.sha1 -out debug.keystore.base64

@echo Done, Android hash key for Facebook app is:
C:\Windows\SSL\bin\openssl base64 -in debug.keystore.sha1
@pause