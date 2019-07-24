@echo off
pushd "%~dp0" 
bin\java.exe --module-path lib -m openxliff/com.maxprograms.stats.RepetitionAnalysis %* 