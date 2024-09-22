#include <stdio.h>
#include <assert.h>

int test1(int a) {
    int res;
    switch (a) {
        case 1:
            return 1;
        case 2:
            return 2;
        case 3:
            return 3;
        default:
            return 0;
    }
}

int main() {
    assert(test1(100000) == 0);
    assert(test1(1) == 1);
    assert(test1(2) == 2);
    assert(test1(3) == 3);

    printf("All tests passed\n");
    return 0;
}