#include <stdio.h>

int fun() { return 20; }

int main() {
    int (*fun_ptr)() = &fun;
    int ret = (*fun_ptr)();
    printf("Value: %d", ret);
    return 0;
}
