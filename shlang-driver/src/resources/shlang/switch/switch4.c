#include <stdio.h>
#include <assert.h>

int test1(int a) {
    int res = 900;;;;
    switch (a) {
        case 1:
        case 3:
            if (a == 1) {
                break;
            } else {
                res = 30;
            }
            break;
        default:
            res = 0;
            break;
    }
    return res;
}

int main() {
    assert(test1(100000) == 0);
    assert(test1(1) == 900);
    assert(test1(2) == 0);
    assert(test1(3) == 30);

    printf("All tests passed\n");
    return 0;
}