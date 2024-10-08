extern int printf(const char *, ...);
extern void *memset(void *, int, unsigned long);

void polluteStack() {
    volatile long arr[100];
    memset(arr, -1, sizeof(arr));
}

int main() {
    polluteStack();
    int arr[10] = { 10 };
    printf("%d %d", arr[8], arr[9]);
    return 0;
}