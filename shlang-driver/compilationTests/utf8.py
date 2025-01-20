#!/usr/bin/env python3

import os
import sys
import subprocess as sp


def build(compiler):
    os.chdir("utf8.c")
    sp.run(["{} -o utf8.o -c utf8.c -O1".format(compiler)], shell=True)
    sp.run(["{} -o test.o -c test.c -O1".format(compiler)], shell=True)
    sp.run(["gcc utf8.o test.o -o a.out"], shell=True)


def run():
    completed_process = sp.run(["./a.out"])
    if completed_process.returncode == 0:
        print("Success")
    else:
        print("Fail")
        exit(1)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 utf8.py <compiler>")
        sys.exit(1)

    cc = sys.argv[1]
    build(cc)
    run()
