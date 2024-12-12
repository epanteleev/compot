
extern void printf(char format[], ...);

typedef struct Point_ {
    double x;
    double y;
} Point;

void printPoint(Point p) {
    printf("x: %lf, y: %lf\n", p.x, p.y);
}

int main() {
    Point p;
    p.x = 1;
    p.y = 2;
    printPoint(p);
    return 0;
}