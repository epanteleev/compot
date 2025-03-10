#include <math.h>
#include <stdio.h>

#define NAN -(0.0 / 0.0)

int is_nan(double x) {
    if (x != x) {
        return 1;
    }

    return 0;
}

int main() {
    double nan = NAN;
    if (nan == HUGE_VAL) {
        return 2;
    }

    if (nan == -HUGE_VAL) {
        return 2;
    }

    printf("%lf\n", nan);
    return is_nan(nan);
}