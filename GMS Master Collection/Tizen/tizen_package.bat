set DESTDIR=%1
set PROJECT=%2
set ZIPDIR=%3%

pushd %DESTDIR%
del %PROJECT%.wgt
rem ogg supported so dont need the mp3s
del html5game\*.mp3
%ZIPDIR%zip -r %PROJECT%.wgt . -x *.wgt .*
popd