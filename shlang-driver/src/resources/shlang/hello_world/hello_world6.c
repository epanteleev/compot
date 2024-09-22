#include <stdio.h>

char* code[] = {"Hello", "World!\n"};

int main() {
    printf("%s %s", code[0], code[1]);
    return 0;
}