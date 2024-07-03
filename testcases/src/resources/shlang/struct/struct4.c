extern void printf(const char* fmt, ...);

typedef struct Point_ {
    int x;
    int y;
} Point;

typedef struct Vect2_ {
    Point arr[2];
} Vect2;

void printVec(Vect2* rec) {
    printf("Vect2: %d %d %d %d\n", rec->arr[0].x, rec->arr[0].y, rec->arr[1].x, rec->arr[1].y);

}

int main() {
    Vect2 p;
    p.arr[0].x = 1;
    p.arr[0].y = 2;
    p.arr[1].x = 4;
    p.arr[1].y = 5;

    printVec(&p);
    return 0;
}