
extern int printf(char format[], ...);

typedef union Point_ {
    float x;
    int y;
} Variant;

Variant getVariant() {
    Variant p;
    p.x = 1;
    return p;
}

int main() {
    Variant (*fn)() = getVariant;
    Variant p = fn();
    printf("x: %f\n", p.x);
    return 0;
}