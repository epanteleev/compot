extern void printf(char format[], ...);


int main() {
    int bb = 3;
    int a[1][3] = {{1, 32, 3},};

    for (int i = 0; i < 3; i++) {
        printf("%d ", a[0][i]);
    }
    return 0;
}