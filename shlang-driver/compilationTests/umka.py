#!/usr/bin/env python3

import os
import sys
import subprocess as sp


def build(compiler):
    os.chdir("umka-lang")
    sp.run(["CC={} ./build_linux.sh".format(compiler)], shell=True)
    sp.run(["./test_linux.sh"], shell=True)
    sp.run(["make", "test"], shell=True)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 umka.py <compiler>")
        sys.exit(1)

    cc = sys.argv[1]
    build(cc)
