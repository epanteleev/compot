extern void printf(char format[], ...);


int main() {
    int bb = 3;
    int a[][3] = {{1, bb, 3}, {4, 5, 6}, {7, 8, 9}};

    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++)
            printf("%d ", a[i][j]);
        printf("\n");
    }
    return 0;
}