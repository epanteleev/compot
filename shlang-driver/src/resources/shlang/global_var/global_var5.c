extern void printf(const char *format, ...);

struct DATA {
    long a;
    long b;
    double c;
};

struct DATA data = { 2, 3, 4.0};

int main() {
    printf("%d %d %f\n", data.a, data.b, data.c);
    return 0;
}