#!/bin/bash
rm -rf dist
jlink --module-path "lib:$JAVA_HOME/jmods" --add-modules xliffFilters --output dist
rm dist/lib/jrt-fs.jar

cp -r catalog/ dist/catalog
cp -r xmlfilter/ dist/xmlfilter
cp -r srx/ dist/srx

cp convert.sh dist/
cp merge.sh dist/
cp server.sh dist/
cp xliffchecker.sh dist/
cp analysis.sh dist/
cp LICENSE dist/

