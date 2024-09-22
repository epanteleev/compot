#include <stdio.h>

typedef struct Point_ {
    int x;
    int y;
} Point;

Point *p = &(Point){1, 1};

int main() {
    p->x = 10;
    p->y = 20;
    printf("Point: (%d, %d)\n", p->x, p->y);
    return 0;
}