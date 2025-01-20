#!/usr/bin/env python3

import os
import sys
import subprocess as sp


def build(compiler):
    os.chdir("bfish")
    sp.run(["CC={} make".format(compiler), "bftest"], shell=True)


def run():
    completed_process = sp.run(["./a.out"])
    if completed_process.returncode == 0:
        print("AES encryption successful")
    else:
        print("AES encryption failed")
        exit(1)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 bfish.py <compiler>")
        sys.exit(1)

    cc = sys.argv[1]
    build(cc)
    run()
