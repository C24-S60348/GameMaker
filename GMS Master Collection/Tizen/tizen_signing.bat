@echo off
set DESTDIR=%1
set TIZENSDK=%2
set JRE=%3%
set PROFILE_NAME=%4%
set PROFILE_PATH=%5%
set TIZEN_TOOLS=%TIZENSDK%\tools\ide\bin

rem required to find java runtime (sigh)
set PATH=%JRE%;%PATH%

pushd %DESTDIR%
echo signing with profile:%PROFILE_NAME%:%PROFILE_PATH%
call %TIZEN_TOOLS%\web-signing -n --profile %PROFILE_NAME%:%PROFILE_PATH%

popd