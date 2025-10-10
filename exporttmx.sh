#!/bin/bash
export CURRENT=$PWD
cd `dirname "$0"`
export OpenXLIFF_HOME=$PWD
cd $CURRENT
$OpenXLIFF_HOME/bin/java --module-path $OpenXLIFF_HOME/lib -m openxliff/com.maxprograms.converters.TmxExporter $@


