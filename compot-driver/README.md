# Compot-driver
Command line interface for [compot](../compot/README.md) compiler frontend.

## Building
Open the project in IntelliJ IDEA and run the `makeDist` task. It will create a distribution in the `build/install/compot-driver-jvm` directory.
Or you can use Docker to build the project:
```shell
docker run -ti --rm -v .:/app:Z -w /app ghcr.io/epanteleev/ubuntu-gcc14 /bin/bash ./gradlew :compot-driver:makeDist
```

## Testing
There are some tests included in the project. You can also run compilation of large C projects to check the correctness of the compiler. 
See (`shlang-tests`)[https://github.com/epanteleev/shlang-tests.git] script repository for details.

## Options:
- `-c` - Compile or assemble the source files, but do not link.
- `-o` - Place the output into `<file>`.