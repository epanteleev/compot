#include <stdio.h>

typedef enum {
    A = 1,
    B = 2,
    C = 3
} Enum;

typedef struct Data Data;

struct Data {
    Enum x;
    int y;
} Data;

Data *p = &(Data){A, 1};

int main() {
    p->x = B;
    p->y = 20;
    printf("Data: (%d, %d)\n", p->x, p->y);
    return 0;
}