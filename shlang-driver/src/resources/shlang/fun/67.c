
struct Point {
    int a;
    int b;
};

struct Point p = { .a = 1, .b = 2 };

int main() {
    return p.a + p.b;
}