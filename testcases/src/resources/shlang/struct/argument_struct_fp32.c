
extern void printf(char format[], ...);

typedef struct Point_ {
    float x;
    float y;
} Point;

void printPoint(Point p) {
    printf("x: %f, y: %f\n", p.x, p.y);
}

int main() {
    Point p;
    p.x = 1;
    p.y = 2;
    printPoint(p);
    return 0;
}