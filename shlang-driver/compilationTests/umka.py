#!/usr/bin/env python3

import os
import sys
import subprocess as sp

expected_output = """21754 / 23019 bytes
OK
412 / 412 bytes
OK
"""

def build(compiler):
    os.chdir("umka-lang")
    sp.run(["CC={} ./build_linux.sh".format(compiler)], shell=True)
    result = sp.run(["./test_linux.sh"], shell=True, stdout=sp.PIPE)
    if result.stdout.decode() == expected_output:
        print("Umka tests successful")
    else:
        print("Umka tests failed")
        print("'" + result.stdout.decode() + "'")
        exit(1)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 umka.py <compiler>")
        sys.exit(1)

    cc = sys.argv[1]
    build(cc)
