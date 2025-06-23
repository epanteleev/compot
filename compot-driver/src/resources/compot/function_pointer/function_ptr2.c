#include <stdio.h>

int fun(int v) { return v; }

int main() {
    int (*fun_ptr)(int) = &fun;
    printf("Value: %d", (*fun_ptr)(10));
    return 0;
}
