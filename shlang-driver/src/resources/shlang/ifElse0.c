#include "runtime/runtime.h"

int constant() {
    return 1;
}

void test1() {
    int a = 90;
    if (constant() == 1) {
        if (4 == 8) {
            unreachable();
        } else {
            if (a != 90) {
                unreachable();
            }
        }
    } else {
        a = 1;
    }

    check(a, 90);
}

int main() {
    test1();
    return 0;
}