#!/usr/bash

YYInstallDir=${YYMacInstallDir}
YYDMGName=${YYMacDMGName}
YYDmgMD5=${YYMacDMGMD5} 

# Determine if we need to copy the runner over to this Mac???
YYCopyDMG=1
if [ -d "$YYInstallDir/YoYo Runner.app" ]; then
	if [ -d "$YYInstallDir/.yoyo" ]; then
		#echo "$YYInstallDir/.yoyo" exists
		if [ -f "$YYInstallDir/.yoyo/dmg.info" ]; then
			#echo "$YYInstallDir/.yoyo/dmg.info" exists
			YYStoredMD5=`cat $YYInstallDir/.yoyo/dmg.info`
			# If the MD5's match then everything is good
			if [ "$YYDmgMD5" == "$YYStoredMD5" ]; then
				YYCopyDMG=0
			fi
		fi
	fi
fi

if [ "$YYCopyDMG" == "1" ]; then
	# create the install directory if it does not exist
	[ -d "$YYInstallDir" ] || mkdir -p $YYInstallDir
	# if the DMG exists
	if [ -f "$YYInstallDir/$YYDMGName" ]; then
		
		# Remove any app that may be present
		rm -rf "$YYInstallDir/YoYo Runner.app"

		# mount the DMG and copy the application over then update
		hdiutil mount -quiet $YYInstallDir/$YYDMGName
		cp -R "/Volumes/YoYo Runner/YoYo Runner.app" $YYInstallDir
		hdiutil unmount -quiet  "/Volumes/YoYo Runner/"

		# record the MD5 
		[ -d "$YYInstallDir/.yoyo" ] || mkdir -p $YYInstallDir/.yoyo
		md5 -q $YYInstallDir/$YYDMGName >$YYInstallDir/.yoyo/dmg.info
		rm -rf $YYInstallDir/$YYDMGName

		# and done
		echo Done

	else
		echo CopyDMG
	fi
else
	echo Done
fi




