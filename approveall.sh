#!/bin/bash

cd "$(dirname "$0")/"

bin/java --module-path lib -m openxliff/com.maxprograms.converters.ApproveAll $@

