@echo off
set DESTDIR=%1
set PROJECT=%2
set TIZEN_APP_ID=%3
set TIZENSDK=%4
set JRE=%5
set TIZEN_TOOLS=%TIZENSDK%\tools\ide\bin
set PROJ_WGT=%DESTDIR%%PROJECT%\%PROJECT%.wgt

rem might need to split up and check for failures 
echo uninstalling...
rem trouble with java...
set PATH=%JRE%;%PATH%
call %TIZEN_TOOLS%\webtizen uninstall -i %TIZEN_APP_ID%

rem NO LONGER WORKS with new sdk
rem set CLI_HOME=%TIZENSDK%\tools\ide
rem set MAIN=org.tizen.cli.exec.uninstall.Main
rem set OPT_TRACE=
rem set OPT_LOGGING=-Dlog4j.configuration=log4j.xml
rem set OPT_PRG_NAME=-Dcli.name=%SCRIPT%
rem set OPT=%OPT_TRACE% %OPT_LOGGING% %OPT_PRG_NAME%
rem set EXEC=%JRE%\java -cp %CLI_HOME%\conf -Djava.ext.dirs=%CLI_HOME%\lib %OPT% %MAIN% -i %TIZEN_APP_ID%
rem %EXEC%
rem %EXEC%

rem echo installing...
call %TIZEN_TOOLS%\webtizen install -w %PROJ_WGT%
echo launching...
call %TIZEN_TOOLS%\webtizen run -i %TIZEN_APP_ID%

rem NO LONGER WORKS with new sdk
rem set MAIN=org.tizen.cli.exec.run.Main
rem set EXEC=%JRE%\java -cp %CLI_HOME%\conf -Djava.ext.dirs=%CLI_HOME%\lib %OPT% %MAIN% -w %PROJ_WGT% -i %TIZEN_APP_ID%
rem %EXEC%
