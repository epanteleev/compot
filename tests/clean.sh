#!/bin/bash

rm *.o
rm opt
rm base
find . -maxdepth 1 -mindepth 1 -type d -exec rm -rf '{}' \;
