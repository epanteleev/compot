#include <stdio.h>

int main() {
    const char* str = "";
    str = "Hello, World!\n";
    fprintf(stderr, "%s", str);
    return 0;
}