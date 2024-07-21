
extern void printf(char format[], ...);

typedef struct Point_ {
    double x;
    double y;
    double z;
} Point;

void printPoint(Point p) {
    printf("x: %lf, y: %lf, z: %lf\n", p.x, p.y, p.z);
}

int main() {
    Point p;
    p.x = 1;
    p.y = 2;
    p.z = 3;
    printPoint(p);
    return 0;
}