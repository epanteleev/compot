#include <stdio.h>
#include <assert.h>

int test1(int a) {
    int res;
    switch (a) {
        case 1:
        case 3:
            if (a == 1) {
                res = 3;
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
    assert(test1(1) == 3);
    assert(test1(2) == 0);
    assert(test1(3) == 30);

    printf("All tests passed\n");
    return 0;
}