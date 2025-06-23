
extern int printf(char format[], ...);

int getInt();

int getInt() {
    return 2;
}

int main() {
    int (*fn)() = getInt;
    int p;
    p = fn();

    int (*print)(char format[], ...) = printf;
    print("v=%d\n", p);
    return 0;
}