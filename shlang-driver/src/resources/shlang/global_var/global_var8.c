extern int printf(const char *format, ...);

typedef struct Point_ {
    long a;
    long b;
} Point;

typedef struct Rectangle_ {
    Point p1;
    Point p2;
} Rectangle;

Rectangle data1 = { { 2, 3 }, { 4, 5 } };

int main() {
    printf("%ld %ld %ld %ld\n", data1.p1.a, data1.p1.b, data1.p2.a, data1.p2.b);
    return 0;
}