extern void printf(const char *format, ...);

struct DATA {
    long a;
    long b;
    double c;
};

struct DATA data;

int main() {
    data.a = 2;
    data.b = 3;
    data.c = 4.0;
    printf("%d %d %f\n", data.a, data.b, data.c);
    return 0;
}