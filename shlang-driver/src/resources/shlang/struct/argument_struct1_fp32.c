
extern void printf(char format[], ...);

typedef struct Point_ {
    float x;
    float y;
    float z;
} Point;

void printPoint(Point p) {
    printf("x: %f, y: %f, z: %f\n", p.x, p.y, p.z);
}

int main() {
    Point p;
    p.x = 1;
    p.y = 2;
    p.z = 3;
    printPoint(p);
    return 0;
}