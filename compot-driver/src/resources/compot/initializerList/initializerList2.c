
typedef struct Point {
    int x;
    float y;
} Point;

void printf(const char* str, ...);

int main() {
    Point point = {1, 2};
    Point p = point;
    printf("point.x = %d, point.y = %.3f", p.x, p.y);
    return 0;
}