#include <stdio.h>
#include <stdint.h>

static void print(int16_t G[][3], int size) {
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < 3; j++) {
            printf("%d ", G[i][j]);
        }
        printf("\n");
    }
}

int main() {
    int16_t G[][3] = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}, {10, 11, 12}};
    print(&(*G), 4);
    return 0;
}