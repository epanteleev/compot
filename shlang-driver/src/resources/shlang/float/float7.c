#include <stdio.h>
#include <float.h>

#define INFINITY (DBL_MAX + DBL_MAX)
#define NAN (INFINITY - INFINITY)

int is_nan(double x) {
    _Bool is = x == x;
    if (!is) {
        return 1;
    }

    return 0;
}

int main() {
    double nan = NAN;
    printf("%lf\n", nan);
    return is_nan(nan);
}