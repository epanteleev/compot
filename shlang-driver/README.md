# Shlang-driver
Command line interface for [shlang](../shlang/README.md) compiler frontend.

## Usage
Shlang is a command line tool that pretends to be a GCC-like compiler. So that you can use it in the same way.
```shell
bin/shlang -o out.o in.c
```

## Building
Open the project in IntelliJ IDEA and run the `installDist` task. It will create a distribution in the `build/install/shlang-driver` directory.
Or you can use Docker to build the project:
```shell
docker run -ti --rm -v .:/app ghcr.io/epanteleev/ubuntu-gcc14 cd /app; gradle installDist
```

## Options:
- `-c` - Compile or assemble the source files, but do not link.
- `-o` - Place the output into `<file>`.