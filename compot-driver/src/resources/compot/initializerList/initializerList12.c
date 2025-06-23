extern int printf(const char *, ...);

int main() {
    int arr[10] = {};
    printf("%d %d", arr[8], arr[9]);
    return 0;
}