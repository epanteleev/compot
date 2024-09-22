extern int printf(const char *format, ...);

typedef struct Point_ {
    long a;
    long b;
} Point;

typedef struct Rectangle_ {
    Point p1;
    Point p2;
} Rectangle;

Rectangle data = {{ 2, 3 }};

int main() {
    printf("%ld %ld %ld %ld\n", data.p1.a, data.p1.b, data.p2.a, data.p2.b);
    return 0;
}