#include <stdio.h>
#include <assert.h>

int test1(int* res, int a) {
    if (a > 0) switch (a) {
        default:
        case 1:
            *res = 1;
            break;
        case 2:
            *res = 2;
            break;
        case 3:
            *res = 3;
            break;
    }
}

int main() {
    int res;
    test1(&res, 100000);
    assert(res == 1);
    return 0;
}