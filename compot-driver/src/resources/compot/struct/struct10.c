extern int printf(const char *, ...);

struct Pointers {
    int *p1;
    int *p2;
    int *p3;
};

int main() {
    int a = 1;
    struct Pointers ptrs;
    ptrs.p1 = ptrs.p2 = ptrs.p3 = &a;

    printf("p1=%d, p2=%d, p3=%d\n", *ptrs.p1, *ptrs.p2, *ptrs.p3);
    return 0;
}