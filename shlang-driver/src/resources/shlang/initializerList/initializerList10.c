extern int printf(const char *, ...);
extern void *memset(void *, int, unsigned long);

typedef struct bfblk {
		unsigned hi;
		unsigned lo;
} bfblk_t;

void dirtyStack() {
    volatile long arr[100];
    memset(arr, -1, sizeof(arr));
}

int main() {
    dirtyStack();
    bfblk_t v = { 10 };
    printf("%d %d", v.hi, v.lo);
    return 0;
}