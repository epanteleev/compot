extern void printf(char format[], ...);

typedef struct Point_ {
    int x;
    int y;
} Point;

int main() {
    Point p = {.y = 1, .x = 2};
    printf("x: %d, y: %d\n", p.x, p.y);
    return 0;
}
