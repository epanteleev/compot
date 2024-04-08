#!/bin/bash

rm *.o
rm opt
rm base
rm a.out
find . -maxdepth 1 -mindepth 1 -type d -exec rm -rf '{}' \;
