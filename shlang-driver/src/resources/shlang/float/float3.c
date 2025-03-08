#include <math.h>
#include <stdio.h>

int is_nan(double x) {
    if (x != x) {
        return 1;
    }

    return 0;
}

int main() {
    double nan = NAN;
    printf("%lf\n", nan);
    return is_nan(nan);
}