extern int printf(const char *format, ...);

int data1[4] = { 2, 3 };

int main() {
    printf("%d %d %d %d\n", data1[0], data1[1], data1[2], data1[3]);
    return 0;
}