
extern int printf(const char* fmt, ...);

union FloatOrInt {
    double f80;
    long u64[2];
};

int main() {
    union FloatOrInt u;
    u.f80 = 1.0;
    printf("%ld\n", u.u64[0]);
    return 0;
}