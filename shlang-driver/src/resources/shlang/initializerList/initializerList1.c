
typedef struct Point {
    int x;
    float y;
} Point;

void printf(const char* str, ...);

int main() {
    Point point = {1, 2};
    printf("point.x = %d, point.y = %.3f", point.x, point.y);
    return 0;
}