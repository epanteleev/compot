#include <stdio.h>

int main() {
    char code[] = {"Hello World!\n"};
    fprintf(stderr, "%s", code);
    return 0;
}