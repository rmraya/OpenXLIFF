#!/bin/bash
rm -rf open_filters
jlink --module-path "lib:$JAVA_HOME/jmods" --add-modules xliffFilters --output open_filters
rm open_filters/lib/jrt-fs.jar

cp -r catalog/ open_filters/catalog
cp -r xmlfilter/ open_filters/xmlfilter
cp -r srx/ open_filters/srx

cp convert.sh open_filters/
cp merge.sh open_filters/
cp server.sh open_filters/
cp LICENSE open_filters/

