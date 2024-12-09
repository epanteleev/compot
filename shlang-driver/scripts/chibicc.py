#!/usr/bin/env python3

import os
import sys
import subprocess as sp


def build(compiler):
    os.chdir("chibicc")
    sp.run(["make", "clean"])
    sp.run(["CC=gcc make", "chibicc"], shell=True)
    sp.run([compiler, "-c", "unicode.c", "-o", "unicode.o", "-O1"])
    sp.run([compiler, "-c", "hashmap.c", "-o", "hashmap.o", "-O1"])
    sp.run([compiler, "-c", "type.c", "-o", "type.o", "-O1"])
    sp.run([compiler, "-c", "strings.c", "-o", "strings.o", "-O1"])
    sp.run([compiler, "-c", "tokenize.c", "-o", "tokenize.o", "-O1"])
    sp.run([compiler, "-c", "main.c", "-o", "main.o", "-O1"])
    sp.run([compiler, "-c", "parse.c", "-o", "parse.o", "-O1"])
    sp.run([compiler, "-c", "preprocess.c", "-o", "preprocess.o", "-O1"])
    sp.run(["CC=gcc make", "chibicc"], shell=True)
    sp.run(["make", "test"])


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 chibicc.py <compiler>")
        sys.exit(1)

    cc = sys.argv[1]
    build(cc)
