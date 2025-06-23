
extern int printf(char format[], ...);

typedef struct P {
    long x;
    long y;
    long z;
} Point;

typedef union {
    double x;
    Point p;
} Variant;

Variant getVariant() {
    Variant p;
    p.x = 1;
    return p;
}

int main() {
    Variant (*fn)() = getVariant;
    Variant p = fn();
    printf("x: %lf\n", p.x);
    return 0;
}