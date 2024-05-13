#include "runtime/runtime.h"

int sub3(int a, int b, int c) {
    return a - b - c;
}

int main() {
    printInt(sub3(9, 3, 2));
    return 0;
}