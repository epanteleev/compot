int printf(const char *, ...);

struct Point {
    int a;
    int b;
};

struct Vec {
    struct Point a;
    struct Point b;
};

int main() {
    struct Vec v = {1,2,3,4};
    return v.a.a + v.a.b;
}