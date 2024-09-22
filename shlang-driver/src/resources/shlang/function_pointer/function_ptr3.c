#include <stdio.h>

void fun(int v) { printf("Value: %d", v); }

int main() {
    void (*fun_ptr)(int) = &fun;
    (*fun_ptr)(20);
    return 0;
}
