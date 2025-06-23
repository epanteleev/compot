#include <stdio.h>

typedef struct Point_ {
    int x;
    int y;
} Point;

int* p = &(int[2]){1, 1}; //TODO gcc warning here

int main() {
    p[0] = 10;
    p[1] = 20;
    printf("Array: (%d, %d)\n", p[0], p[1]);
    return 0;
}