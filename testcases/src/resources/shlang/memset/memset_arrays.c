
void memset(void *dest, char val, int n) {
    char *d = dest;
    for (int i = 0; i < n; i++) {
        d[i] = val;
    }
}

extern int test();

int main() {
    test();
    return 0;
}