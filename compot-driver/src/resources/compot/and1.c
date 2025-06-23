#include <runtime/runtime.h>

int and(int a, int b) {
    return a > 0 && b > 0;
}


int main() {
    int a = 1;
    int b = 2;
    int c = and(a, b);
    if (c) {
        return 3;
    }
    return 0;
}