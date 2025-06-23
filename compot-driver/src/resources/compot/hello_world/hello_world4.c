#include <stdio.h>

char code[] = {"Hello World!\n"};

int main() {
    fprintf(stderr, "%s", code);
    return 0;
}