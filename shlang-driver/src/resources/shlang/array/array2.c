

extern int printf(const char *, ...);

int printArray(int arr[]) {
    return printf("{%d, %d, %d}\n", arr[0], arr[1], arr[2]);
}

int main(void) {
    int foo[] = {1, 2, 3};
    printArray(foo);
    return 0;
}