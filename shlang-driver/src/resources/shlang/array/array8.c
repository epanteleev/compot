
extern int printf(const char *format, ...);
extern void *memset(void *s, int c, unsigned long n);

void polluteStack() {
    volatile char array[1000];
    memset(array, 0xFF, 1000);
}

int main(void) {
    polluteStack();
    char array[256] = "";
    for (int i = 0; i < 256; i++) {
        if (array[i] != 0) {
            printf("Array is not zeroed\n");
            return 1;
        }
    }
    printf("Array is zeroed\n");
    return 0;
}