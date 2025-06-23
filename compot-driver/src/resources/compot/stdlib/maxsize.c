#include <stdio.h>

struct Value {
    long v;
};

#define MAX_SIZET	((unsigned long)(~(unsigned long)0))

#define MAXASIZEB	(MAX_SIZET/(sizeof(struct Value) + 1))


int main() {
    printf("v: %ld", (unsigned)MAXASIZEB);
    return 0;
}