#include <stdarg.h>

extern void printf(char format[], ...);

typedef struct Vect_ {
    char x;
    char y;
    char z;
    char w;
    char v;
} Vect;

void printVect(const char* fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    Vect v = va_arg(ap, Vect);

    printf(fmt, v.x, v.y, v.z, v.w, v.v);
    va_end(ap);
}

int main() {
    Vect v;
    v.x = 1;
    v.y = 2;
    v.z = 3;
    v.w = 4;
    v.v = 5;
    printVect("x: %d y: %d z: %d w: %d v: %d\n", v);
    return 0;
}