extern void printf(const char* fmt, ...);

typedef struct Point_ {
    int x;
    int y;
} Point;

typedef struct Vect2_ {
    Point arr[2];
} Vect2;

void printPoint(Point* p) {
    printf("Point: (%d, %d)\n", p->x, p->y);
}

int main() {
    Vect2 p;
    p.arr[0].x = 1;
    p.arr[0].y = 2;
    p.arr[1] = p.arr[0];
    Point point = p.arr[0];

    printPoint(&point);
    printPoint(&p.arr[1]);
    return 0;
}