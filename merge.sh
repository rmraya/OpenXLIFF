#!/bin/bash

cd "$(dirname "$0")/"

bin/java --module-path lib -m xliffFilters/com.maxprograms.converters.Merge $@

