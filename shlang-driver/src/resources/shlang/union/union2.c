
extern int printf(const char *, ...);

union Number {
    int x;
    double y;
};

void printD(union Number x) {
    printf("%f\n", x.y);
}

int main() {
    union Number p;
    p.y = 1.0;
    printD(p);
    return 0;
}