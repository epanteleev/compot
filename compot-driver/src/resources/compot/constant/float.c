extern int printf(const char *format, ...);

int main() {
    double a = 1.0;
    double b = 2.0;
    float c = ((a + b) * 1e50) / 1e50;
    if (c == 3.0) {
        printf("%lf", c);
    }
    return 0;
}