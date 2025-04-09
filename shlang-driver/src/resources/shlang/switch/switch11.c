#include <stdio.h>
#include <assert.h>

int test1(int* a, int* b) {
    int res;
    switch (b - a) {
        case 1:
            res = 1;
            break;
        case 2:
            res = 2;
            break;
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
    int arr[3];
    assert(test1(&arr[0], &arr[1]) == 1);
    return 0;
}