#include <stdio.h>
#include <assert.h>

int test1(int a) {
    int res;
    switch (a) {
        case 1:
        case 2:
        case 3:
            return 2;
        default:
            res = 3;
            return res;
    }
    return res;
}

int main() {
    assert(test1(1) == 2);
    assert(test1(2) == 2);
    assert(test1(3) == 2);

    printf("All tests passed\n");
    return 0;
}