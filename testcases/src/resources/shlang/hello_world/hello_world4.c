#include <stdio.h>

int main() {
    char code[] = {"Hello", "World!\n"};
    fprintf(stderr, "%s %s", code[0], code[1]);
    return 0;
}