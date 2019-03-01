rmdir /S /Q .\open_filters\
jlink --module-path "lib;%JAVA_HOME%\jmods" --add-modules xliffFilters --output open_filters
del .\open_filters\lib\jrt-fs.jar

mkdir .\open_filters\catalog
xcopy catalog open_filters\catalog /S

mkdir .\open_filters\srx
xcopy srx open_filters\srx /S

mkdir .\open_filters\xmlfilter
xcopy xmlfilter open_filters\xmlfilter /S

xcopy convert.bat open_filters\
xcopy merge.bat open_filters\
xcopy server.bat open_filters\
xcopy xliffchecker.bat open_filters\
xcopy analysis.bat open_filters\
xcopy LICENSE open_filters\


