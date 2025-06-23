#include <limits.h>

unsigned long cast(double d) {
    if (d > (double)ULONG_MAX) {
        return ULONG_MAX;
    } else {
        return (long)d;
    }
}

int main() {
    double d = 1.0;
    unsigned long u = cast(d);
    return (int)u;
}