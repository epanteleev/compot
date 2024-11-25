#!/usr/bin/env python3

import os
import sys
import subprocess as sp


def build(compiler):
    os.chdir("chibicc")
    sp.run(["make", "clean"])
    sp.run(["CC=gcc make", "chibicc"], shell=True)
    sp.run([compiler, "-c", "unicode.c", "-o", "unicode.o", "--dump-ir", "."])
    sp.run(["CC=gcc make", "chibicc"], shell=True)
    sp.run(["make", "test"])


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 chibicc.py <compiler>")
        sys.exit(1)

    cc = sys.argv[1]
    build(cc)
