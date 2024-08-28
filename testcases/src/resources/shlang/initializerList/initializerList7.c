#include <stdio.h>
#include <stdint.h>

static void print(uint16_t G[][3]) {
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 3; j++)
            printf("%d", G[i][j]);
    }
}

int main() {
    uint16_t G[4][3] = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}, {10, 11, 12}};
    print(&(*G));
    return 0;
}