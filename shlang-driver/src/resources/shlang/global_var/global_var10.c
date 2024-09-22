extern int printf(const char *format, ...);

int data[4] = { 2, 3 };

int main() {
    printf("%d %d %d %d\n", data[0], data[1], data[2], data[3]);
    return 0;
}