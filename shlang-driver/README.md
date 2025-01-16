# Shlang-driver
Command line interface for [shlang](../shlang/README.md) compiler frontend.

## Usage
Shlang is a command line tool that pretends to be a GCC-like compiler. So that you can use it in the same way.
```shell
bin/shlang -o out.o in.c
```

## Options:
- `-c` - Compile or assemble the source files, but do not link.
- `-o` - Place the output into `<file>`.