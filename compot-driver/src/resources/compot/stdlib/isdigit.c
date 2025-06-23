#define _POSIX_C_SOURCE 200809L
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>


int main() {
    char* p = "1u'Hello, world!\"";
    char* copy = p;
    if (isdigit(*p)) {
        printf("isdigit\n");
        return 0;
    }

    return 1;
}