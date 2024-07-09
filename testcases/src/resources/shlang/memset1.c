
static inline void memset(void *dest, char val, int n) {
    char *d = dest;
    while (n--) {
        *d++ = val;
    }
}


extern void printByteArrayWithSpaces(char arr[], int n);

int main() {
    char buf[10];
    memset(buf, 0, 10);
    printByteArrayWithSpaces(buf, 10);
    return 0;
}
