#include <stdio.h>

void printInt1(int a) {
    printf("1\n");
}

void printInt2(int a) {
    printf("2\n");
}

int main() {
    void (*fptr[])(int) = {printInt1, printInt2};
    fptr[0](1);
    fptr[1](2);
    return 0;
}