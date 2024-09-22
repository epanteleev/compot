#include <stdio.h>

int fun(int v) { return v; }

typedef struct {
    int (*fun_ptr)(int);
} Fun;


int main() {
    Fun f;
    f.fun_ptr = fun;
    printf("Value: %d", f.fun_ptr(10));
    return 0;
}
