#include "runtime/runtime.h"

int sub3(int a, int b, int c) {
    return a - b - c;
}

int sub3_0(int a, int b, int c) {
    return a - (b - c);
}

int main() {
    int sum = sub3_0(9, 3, 2);
    check(sum, 8);

    int sum2 = sub3(9, 3, 2);
    check(sum2, 4);

    printInt(sub3(9, 3, 2));
    return 0;
}