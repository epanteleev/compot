#include <stdio.h>
#include <assert.h>

int test1(int a) {
    int res;
    switch (a) {
        case 1:
            res = 1;
        case 2:
            res = 2;
        case 3:
            res = 3;
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
    assert(test1(2) == 3);
    assert(test1(3) == 3);

    printf("All tests passed\n");
    return 0;
}