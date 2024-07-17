
extern void printf(char format[], ...);

typedef struct Point_ {
    int x;
    int y;
} Point;

void printPoint(Point p) {
    printf("x: %d, y: %d\n", p.x, p.y);
}

int main() {
    Point p;
    p.x = 1;
    p.y = 2;
    printPoint(p);
    return 0;
}