#include <stdio.h>
#include <assert.h>

int test1(int a) {
    int res;
    switch (a) {
        case 1:
        case 2:
        case 3:
            res = 2;
        default:
            res = 3;
            break;
    }
    return res;
}

int main() {
    assert(test1(1) == 3);
    assert(test1(2) == 3);
    assert(test1(3) == 3);

    printf("All tests passed\n");
    return 0;
}