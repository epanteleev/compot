#include <runtime/runtime.h>

extern void printf(const char* fmt, ...);

struct Point {
    int x;
    int y;
};

struct Rect {
    struct Point p1;
    struct Point p2;
};

void printRect(struct Rect* rec) {
    printf("Rect: (%d, %d) - (%d, %d)\n", rec->p1.x, rec->p1.y, rec->p2.x, rec->p2.y);
}

int main() {
    struct Rect p;
    p.p1.x = 10;
    p.p1.y = 20;
    p.p2.x = 30;
    p.p2.y = 40;
    printRect(&p);
    return 0;
}