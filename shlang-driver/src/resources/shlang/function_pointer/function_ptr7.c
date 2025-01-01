
extern int printf(char format[], ...);

int getInt() {
    return 2;
}

int main() {
    int (*fn)() = getInt;
    int p;
    p = fn();
    printf("v=%d\n", p);
    return 0;
}