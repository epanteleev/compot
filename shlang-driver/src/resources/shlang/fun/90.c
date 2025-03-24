

struct Point {
    int x;
    int y;
};

struct Vec2 {
    struct Point p1;
    struct Point p2;
};

#define x p1.x

int main() {
    struct Vec2 v;
    v.x = 1;
    return v.x;
}