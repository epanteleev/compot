extern void printf(char format[], ...);

typedef struct Point_ {
    int x;
    int y;
} Point;

int main() {
    Point p = {.x = 1, 2};
    printf("x: %d, y: %d\n", p.x, p.y);
    return 0;
}
