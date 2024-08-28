#include <stdio.h>

int fun(int v) { return v; }

typedef struct {
    int (*fun_ptr)(int);
} Fun;

void print(Fun* f) {
    printf("Value: %d", f->fun_ptr(10));
}

int main() {
    Fun f;
    f.fun_ptr = fun;
    print(&f);
    return 0;
}
