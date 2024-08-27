extern int printf(const char *format, ...);

int data[] = { 2, 3, 4, 5 };

int main() {
    printf("%d %d %d %d\n", data[0], data[1], data[2], data[3]);
    return 0;
}