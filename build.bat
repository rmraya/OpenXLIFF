rmdir /S /Q .\dist\
jlink --module-path "lib;%JAVA_HOME%\jmods" --add-modules openxliff --output dist
del .\dist\lib\jrt-fs.jar

mkdir .\dist\catalog
xcopy catalog dist\catalog /S

mkdir .\dist\srx
xcopy srx dist\srx /S

mkdir .\dist\xmlfilter
xcopy xmlfilter dist\xmlfilter /S

xcopy convert.bat dist\
xcopy merge.bat dist\
xcopy server.bat dist\
xcopy xliffchecker.bat dist\
xcopy analysis.bat dist\
xcopy LICENSE dist\


