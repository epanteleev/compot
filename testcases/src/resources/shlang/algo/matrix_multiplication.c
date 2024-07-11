extern void printf(char format[], ...);


void matrix_multiply(int **a, int **b, int **c, int n) {
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            c[i][j] = 0;
            for (int k = 0; k < n; k++) {
                c[i][j] = c[i][j] + a[i][k] * b[k][j];
            }
        }
    }
}

int main() {
    int bb = 3;
    int a[3][3] = {{1, bb, 3}, {4, 5, 6}, {7, 8, 9}};
    int b[3][3] = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
    int res[3][3];
    int expected[3][3] = {{30, 36, 42}, {66, 81, 96}, {102, 126, 150}};

    matrix_multiply((int**)&a, (int**)&b, (int**)&res, 3);

    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            if (res[i][j] != expected[i][j]) {
                printf("Error: %d\n", res[i][j]);
                return 1;
            }
        }
    }
    printf("Tests passed\n");
    return 0;
}